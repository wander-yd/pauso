package com.andrew.hourpause;
import java.util.*;
import java.text.*;
public class RhythmTest {
 static void check(boolean c,String label){if(!c)throw new AssertionError(label);}
 static long local(String value,TimeZone zone)throws Exception{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);f.setTimeZone(zone);return f.parse(value).getTime();}
 static void boundary(Schedule.Rhythm r,long workStart,TimeZone zone){
  Schedule.Snapshot before=r.at(workStart+r.work*60000L-1,zone),quiet=r.at(workStart+r.work*60000L,zone),next=r.at(workStart+(r.work+r.rest)*60000L,zone);
  check(!before.quiet&&before.remaining==1,"last work millisecond");
  check(quiet.quiet&&quiet.remaining==r.rest*60000L,"first rest millisecond");
  check(!next.quiet&&next.remaining==r.work*60000L,"next cycle");
 }
 public static void main(String[] args)throws Exception{
  int[][] presets={{14,1},{25,5},{50,10},{90,30}};
  for(String id:new String[]{"UTC","Asia/Kuala_Lumpur","Asia/Kathmandu","America/New_York","Australia/Adelaide"}){
   TimeZone zone=TimeZone.getTimeZone(id);long midnight=local("2026-11-08 00:00:00",zone);
   for(int[] p:presets){Schedule.Rhythm r=new Schedule.Rhythm(p[0],p[1],true,0);
    for(int day=0;day<2;day++)for(int cycle=0;cycle<1440/(p[0]+p[1]);cycle++)boundary(r,midnight+day*86400000L+cycle*(p[0]+p[1])*60000L,zone);
    for(int minute=0;minute<2880;minute++){
     Schedule.Snapshot s=r.at(midnight+minute*60000L+32123,zone);int m=minute%(p[0]+p[1]);check(s.quiet==(m>=p[0]),"preset timetable "+id);
     check(s.minutes>=1&&s.minutes<=(s.quiet?p[1]:p[0]),"minute icon range");
     check(s.refresh>0&&s.refresh<=60000&&s.refresh<=s.remaining,"refresh deadline");
    }
   }
  }
  TimeZone kl=TimeZone.getTimeZone("Asia/Kuala_Lumpur");
  Schedule.Rhythm hour=new Schedule.Rhythm(50,10,true,0);
  check(hour.at(local("2026-11-08 20:50:00",kl),kl).quiet,"50 starts at :50");
  Schedule.Rhythm half=new Schedule.Rhythm(25,5,true,0);
  check(half.at(local("2026-11-08 20:25:00",kl),kl).quiet&&half.at(local("2026-11-08 20:55:00",kl),kl).quiet,"25/5 slots");
  Schedule.Rhythm longR=new Schedule.Rhythm(90,30,true,0);
  check(longR.at(local("2026-11-08 09:30:00",kl),kl).quiet&&!longR.at(local("2026-11-08 10:00:00",kl),kl).quiet,"90/30 across hours");
  long applied=local("2026-11-08 20:12:00",kl);
  Schedule.Rhythm startNow=new Schedule.Rhythm(25,5,false,applied);
  boundary(startNow,applied,kl);
  check(startNow.at(local("2026-11-08 20:12:00",kl),kl).remaining==1500000,"start now full interval");
  check(startNow.at(local("2026-11-08 20:37:00",kl),kl).quiet,"start-now preview deadline");
  Schedule.Rhythm reopened=new Schedule.Rhythm(25,5,false,applied);
  check(reopened.at(applied+65000,kl).remaining==1435000,"saved anchor survives reopen");
  long anchor=local("2026-11-08 00:00:00",kl)+kl.getOffset(applied);
  Schedule.Rhythm custom=new Schedule.Rhythm(17,4,true,anchor);
  long tomorrow=local("2026-11-09 00:00:00",kl);
  check(custom.at(tomorrow,kl).phase==(1440%21)*60000L,"custom continues across midnight without restart");
  PlaybackGate gate=new PlaybackGate();gate.pause();
  for(int[] p:presets){Schedule.Rhythm r=new Schedule.Rhythm(p[0],p[1],true,0);for(int m=0;m<1440;m++)check(!gate.shouldPlay(true,true,r.at(applied+m*60000L,kl)),"switch cannot undo manual pause");}
  gate.resume();check(!gate.shouldPlay(true,true,longR.at(local("2026-11-08 09:45:00",kl),kl)),"resume respects new quiet phase");check(gate.shouldPlay(true,true,longR.at(local("2026-11-08 10:00:00",kl),kl)),"resumed next block");
  for(int n:new int[]{-1,0,721,Integer.MAX_VALUE})for(boolean work:new boolean[]{true,false}){try{new Schedule.Rhythm(work?n:14,work?1:n,true,0);throw new AssertionError("invalid accepted");}catch(IllegalArgumentException expected){}}
  Random random=new Random(20261108);TimeZone utc=TimeZone.getTimeZone("UTC");
  for(int i=0;i<10000;i++){
   int w=1+random.nextInt(720),r=1+random.nextInt(720);Schedule.Rhythm rhythm=new Schedule.Rhythm(w,r,i%2==0,applied);
   long now=applied+random.nextInt(2000000000);Schedule.Snapshot s=rhythm.at(now,utc);
   check(s.remaining>0&&s.remaining<=720*60000L,"custom bounded countdown");
   check(rhythm.at(now+s.remaining,utc).quiet!=s.quiet,"custom exact transition");
   check(s.refresh>0&&s.refresh<=s.remaining&&s.refresh<=60000,"custom refresh");
  }
  Schedule.Rhythm legacy=new Schedule.Rhythm(14,1,true,0);
  for(int t=0;t<3600000;t++){
   Schedule.Snapshot s=legacy.at(t,utc);long phase=Schedule.phase(t/60000,t/1000%60,t%1000);
   check(s.quiet==Schedule.quiet(phase)&&s.remaining==Schedule.untilChange(phase),"legacy 14/1 regression");
  }
  System.out.println("PASS: 4 presets across 48h and 5 timezones; 90/30, custom/start-now, midnight, saved anchors, pause/switch, invalid values, 10,000 custom cases and 3.6m legacy positions.");
 }
}

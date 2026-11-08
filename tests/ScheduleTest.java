package com.andrew.hourpause;
import java.util.*;
public final class ScheduleTest {
 private static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);}
 public static void main(String[] args){
  int quietMillis=0,transitions=0;boolean previous=true;
  for(int t=0;t<3600000;t++){
   int minute=t/60000;int second=t/1000%60;int milli=t%1000;
   long phase=Schedule.phase(minute,second,milli);
   boolean quiet=Schedule.quiet(phase);
   check(quiet==(minute==14||minute==29||minute==44||minute==59),"silent minutes at "+t);
   long change=Schedule.untilChange(phase);
   check(change>0&&change<=840000,"positive bounded deadline");
   check(Schedule.quiet((phase+change)%900000)!=quiet,"deadline changes state");
   if(change>1)check(Schedule.quiet(phase+change-1)==quiet,"no premature transition");
   int minutes=Schedule.remainingMinutes(phase);
   check(minutes>=1&&minutes<=(quiet?1:14),"icon range");
   check((minutes-1)*60000L<change&&change<=minutes*60000L,"ceiling minute label");
   long refresh=Schedule.untilRefresh(phase);
   check(refresh>0&&refresh<=60000L,"refresh bounded to a minute");
   check(refresh<=change,"never skip a playback boundary");
   check((phase+refresh)%60000==0,"refresh aligns with wall-clock minute");
   if(quiet)quietMillis++;
   if(quiet!=previous)transitions++;
   previous=quiet;
  }
  check(quietMillis==240000,"four silent minutes per hour");check(transitions==8,"eight transitions per hour");
  check(Schedule.untilChange(Schedule.phase(28,30,0))==30000,"join midway");
  check(Schedule.quiet(Schedule.phase(59,59,999)),"last millisecond quiet");
  check(!Schedule.quiet(Schedule.phase(0,0,0)),"hour rollover plays");
  check(Schedule.remainingMinutes(Schedule.phase(5,28,0))==9,"8:32 displays 9");
  check(Schedule.remainingMinutes(Schedule.phase(13,0,0))==1,"exactly one minute");
  check(Schedule.remainingMinutes(Schedule.phase(12,59,999))==2,"one ms before minute change");
  check(Schedule.untilRefresh(Schedule.phase(59,59,999))==1,"refresh at hour rollover");
  TimeZone saved=TimeZone.getDefault();
  try{
   for(String zone:new String[]{"Asia/Kuala_Lumpur","Asia/Kathmandu","America/New_York","Australia/Adelaide"}){
    TimeZone.setDefault(TimeZone.getTimeZone(zone));long before=System.currentTimeMillis();long phase=Schedule.phaseNow();Calendar c=Calendar.getInstance();c.setTimeInMillis(before);long expected=Schedule.phase(c.get(Calendar.MINUTE),c.get(Calendar.SECOND),c.get(Calendar.MILLISECOND));check(Math.abs(phase-expected)<1000||Math.abs(phase-expected)>899000,"local timezone "+zone);
   }
  }finally{TimeZone.setDefault(saved);}
  System.out.println("PASS: 3,600,000 millisecond positions; boundaries, hour rollover, mid-cycle start, four timezones, minute labels and refresh deadlines.");
 }
}

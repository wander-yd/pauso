package com.andrew.hourpause;
import java.util.*;
public final class SleepTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  SleepPlan off=new SleepPlan(0,0);check(!off.active()&&!off.due(0,0),"off never fires");
  SleepPlan duration=SleepPlan.duration(30,5000);check(duration.remaining(5000,100000)==1800000,"duration starts on enable");check(!duration.due(1804999,0)&&duration.due(1805000,0),"exact elapsed deadline");
  check(duration.remaining(65000,0)==duration.remaining(65000,9999999999L),"clock/timezone changes cannot extend duration");
  Schedule.Rhythm rhythm=new Schedule.Rhythm(14,1,false,0);Schedule.Snapshot play=rhythm.at(5*60000,TimeZone.getTimeZone("UTC"));SleepPlan period=SleepPlan.period(5*60000,play);
  check(period.deadline==14*60000&&period.remaining(0,13*60000)==60000,"period ends before rest");check(period.due(0,14*60000),"boundary expires instead of rolling forward");check(SleepPlan.period(14*60000,rhythm.at(14*60000,TimeZone.getTimeZone("UTC"))).due(0,14*60000),"already resting ends now");
  SleepPlan changed=period.replan(5*60000,new Schedule.Rhythm(25,5,false,0).at(5*60000,TimeZone.getTimeZone("UTC")));check(changed.deadline==25*60000,"explicit Rhythm change recalculates boundary");check(duration.replan(0,play).deadline==duration.deadline,"duration unaffected by Rhythm change");
  for(String zone:new String[]{"UTC","Asia/Kuala_Lumpur","America/New_York"}){long wall=1762776000000L;Schedule.Snapshot t=new Schedule.Rhythm(25,5,true,0).at(wall,TimeZone.getTimeZone(zone));SleepPlan p=SleepPlan.period(wall,t);check(p.deadline==wall+(t.quiet?0:t.remaining),"local boundary "+zone);}
  PlaybackGate gate=new PlaybackGate();gate.pause();check(!gate.shouldPlay(true,true,play)&&duration.due(1805000,0),"manual pause cannot freeze Sleep");gate.resume();check(period.due(0,15*60000),"late callback still expires after entire rest");
  PlanStoreTest.TestContext c=new PlanStoreTest.TestContext();c.memory.putInt("volume",27).putInt("rhythm_work",90).apply();SleepStore store=new SleepStore(c);store.duration(1,100);check(new SleepStore(c).load().deadline==60100,"deadline persisted");store.cancel();check(!store.load().active()&&store.lastMinutes()==1,"cancel preserves last preset");store.period(5*60000,play);store.endSession();check(!store.load().active(),"Stop/teardown cancels session Sleep");store.duration(1,0);store.expired();store.endSession();check(store.message().contains("finished")&&!store.load().active(),"expiry feedback survives teardown");store.cancel();check(store.message().isEmpty(),"new Play clears finished feedback");check(c.memory.getInt("volume",0)==27&&c.memory.getInt("rhythm_work",0)==90,"other settings untouched");
  store.duration(2,0);try{store.duration(0,0);throw new AssertionError("invalid store duration accepted");}catch(IllegalArgumentException expected){}check(store.load().deadline==120000,"invalid input preserves armed deadline");
  for(int n:new int[]{0,-1,721})try{SleepPlan.duration(n,0);throw new AssertionError("invalid duration accepted");}catch(IllegalArgumentException expected){}
  c.memory.putInt("mode",7).apply();check(!store.load().active(),"corrupt mode safe");
  System.out.println("PASS: Sleep exact expiry, elapsed clock/pause, period boundary/replan/no rollover, timezones, cancellation/store isolation and invalid inputs.");
 }
}

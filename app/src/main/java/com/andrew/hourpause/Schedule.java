package com.andrew.hourpause;
import java.util.Calendar;
import java.util.TimeZone;
/** Pure schedule arithmetic. Aligned anchors use local civil milliseconds. */
public final class Schedule {
 public static final long MINUTE=60000L, DAY=86400000L;
 public static final long CYCLE=900000L,PLAY=840000L;
 public static final class Rhythm {
  public final int work,rest;public final boolean aligned;public final long anchor;
  public Rhythm(int work,int rest,boolean aligned,long anchor){
   if(work<1||work>720||rest<1||rest>720)throw new IllegalArgumentException("Use 1–720 minutes for each duration");
   this.work=work;this.rest=rest;this.aligned=aligned;this.anchor=anchor;
  }
  public Snapshot at(long now,TimeZone zone){
   long clock=aligned?now+zone.getOffset(now):now;
   long cycle=(work+rest)*MINUTE,play=work*MINUTE;
   return new Snapshot(Math.floorMod(clock-anchor,cycle),play,cycle);
  }
 }
 public static final class Snapshot {
  public final long phase,play,cycle,remaining,refresh;
  public final boolean quiet;public final int minutes;
  Snapshot(long phase,long play,long cycle){
   this.phase=phase;this.play=play;this.cycle=cycle;quiet=phase>=play;
   remaining=(quiet?cycle:play)-phase;minutes=(int)((remaining+MINUTE-1)/MINUTE);
   refresh=Math.min(remaining,MINUTE-phase%MINUTE);
  }
 }
 // Legacy 14/1 arithmetic retained as a regression fixture.
 public static long phase(int minute,int second,int millis){return (minute%15)*MINUTE+second*1000L+millis;}
 public static boolean quiet(long phase){return phase>=PLAY;}
 public static long untilChange(long phase){return (quiet(phase)?CYCLE:PLAY)-phase;}
 public static int remainingMinutes(long phase){return (int)((untilChange(phase)+MINUTE-1)/MINUTE);}
 public static long untilRefresh(long phase){return Math.min(untilChange(phase),MINUTE-phase%MINUTE);}
 public static long phaseNow(){Calendar c=Calendar.getInstance();return phase(c.get(Calendar.MINUTE),c.get(Calendar.SECOND),c.get(Calendar.MILLISECOND));}
 private Schedule(){}
}

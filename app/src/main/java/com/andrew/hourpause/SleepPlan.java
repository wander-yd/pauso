package com.andrew.hourpause;
/** One session's stop deadline; elapsed time is deliberately independent of the civil clock. */
public final class SleepPlan {
 public static final int OFF=0,DURATION=1,PERIOD=2;
 public final int mode;public final long deadline;
 public SleepPlan(int mode,long deadline){if(mode<OFF||mode>PERIOD||deadline<0)throw new IllegalArgumentException("Invalid Sleep deadline");this.mode=mode;this.deadline=deadline;}
 public static SleepPlan duration(int minutes,long elapsed){if(minutes<1||minutes>720||elapsed<0)throw new IllegalArgumentException("Use 1–720 minutes");return new SleepPlan(DURATION,Math.addExact(elapsed,minutes*60000L));}
 public static SleepPlan period(long wall,Schedule.Snapshot phase){if(wall<0)throw new IllegalArgumentException("Invalid clock");return new SleepPlan(PERIOD,Math.addExact(wall,phase.quiet?0:phase.remaining));}
 public boolean active(){return mode!=OFF;}
 public long remaining(long elapsed,long wall){return active()?Math.max(0,deadline-(mode==DURATION?elapsed:wall)):0;}
 public boolean due(long elapsed,long wall){return active()&&remaining(elapsed,wall)==0;}
 public long endTime(long elapsed,long wall){return mode==PERIOD?deadline:wall+remaining(elapsed,wall);}
 public SleepPlan replan(long wall,Schedule.Snapshot phase){return mode==PERIOD?period(wall,phase):this;}
}

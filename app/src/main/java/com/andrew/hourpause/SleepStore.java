package com.andrew.hourpause;
import android.content.*;
public final class SleepStore {
 private final SharedPreferences prefs;
 public SleepStore(Context c){prefs=c.getSharedPreferences("sleep",0);}
 public SleepPlan load(){try{return new SleepPlan(prefs.getInt("mode",0),prefs.getLong("deadline",0));}catch(IllegalArgumentException e){return new SleepPlan(0,0);}}
 public void duration(int minutes,long elapsed){SleepPlan p=SleepPlan.duration(minutes,elapsed);save(p);prefs.edit().putInt("last_minutes",minutes).apply();}
 public void period(long wall,Schedule.Snapshot phase){save(SleepPlan.period(wall,phase));}
 public void replan(long wall,Schedule.Snapshot phase){SleepPlan p=load();if(p.mode==SleepPlan.PERIOD)save(p.replan(wall,phase));}
 private void save(SleepPlan p){prefs.edit().putInt("mode",p.mode).putLong("deadline",p.deadline).putString("message","").apply();}
 public int lastMinutes(){int n=prefs.getInt("last_minutes",30);return n>=1&&n<=720?n:30;}
 public void cancel(){save(new SleepPlan(0,0));}
 public void expired(){prefs.edit().putInt("mode",0).putLong("deadline",0).putString("message","Sleep finished · playback stopped").apply();}
 public String message(){return prefs.getString("message","");}
 public void endSession(){if(load().active())cancel();}
}

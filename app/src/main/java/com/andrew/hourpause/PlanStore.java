package com.andrew.hourpause;
import android.content.*;
import java.util.TimeZone;
public final class PlanStore {
 public static final int[][] PRESETS={{14,1},{25,5},{50,10},{90,30}};
 private final SharedPreferences prefs;
 public PlanStore(Context context){prefs=context.getSharedPreferences("player",0);}
 public int selected(){return prefs.getInt("rhythm_preset",0);}
 public Schedule.Rhythm load(){
  try{return new Schedule.Rhythm(prefs.getInt("rhythm_work",14),prefs.getInt("rhythm_rest",1),prefs.getBoolean("rhythm_aligned",true),prefs.getLong("rhythm_anchor",0));}
  catch(IllegalArgumentException e){return new Schedule.Rhythm(14,1,true,0);}
 }
 public Schedule.Snapshot now(){return load().at(System.currentTimeMillis(),TimeZone.getDefault());}
 public void preset(int n){int[] p=PRESETS[n];save(n,p[0],p[1],true,0);}
 public void custom(int work,int rest,boolean aligned,long now){
  long local=now+TimeZone.getDefault().getOffset(now);
  long anchor=aligned?Math.floorDiv(local,Schedule.DAY)*Schedule.DAY:now;
  save(-1,work,rest,aligned,anchor);
  prefs.edit().putInt("custom_work",work).putInt("custom_rest",rest).putBoolean("custom_aligned",aligned).apply();
 }
 private void save(int preset,int work,int rest,boolean aligned,long anchor){
  new Schedule.Rhythm(work,rest,aligned,anchor);
  prefs.edit().putInt("rhythm_preset",preset).putInt("rhythm_work",work).putInt("rhythm_rest",rest).putBoolean("rhythm_aligned",aligned).putLong("rhythm_anchor",anchor).apply();
 }
}

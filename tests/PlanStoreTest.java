package com.andrew.hourpause;
import android.content.*;
import java.util.*;
public class PlanStoreTest {
 static class Memory implements SharedPreferences,SharedPreferences.Editor {
  Map<String,Object> values=new HashMap<>();
  public String getString(String k,String d){return values.containsKey(k)?(String)values.get(k):d;}
  public SharedPreferences.Editor putString(String k,String v){values.put(k,v);return this;}
  public int getInt(String k,int d){return values.containsKey(k)?(Integer)values.get(k):d;}
  public long getLong(String k,long d){return values.containsKey(k)?(Long)values.get(k):d;}
  public boolean getBoolean(String k,boolean d){return values.containsKey(k)?(Boolean)values.get(k):d;}
  public SharedPreferences.Editor edit(){return this;}
  public SharedPreferences.Editor putInt(String k,int v){values.put(k,v);return this;}
  public SharedPreferences.Editor putLong(String k,long v){values.put(k,v);return this;}
  public SharedPreferences.Editor putBoolean(String k,boolean v){values.put(k,v);return this;}
  public void apply(){}
 }
 static class TestContext extends Context {final Memory memory=new Memory();public SharedPreferences getSharedPreferences(String n,int m){return memory;}}
 static void check(boolean c,String s){if(!c)throw new AssertionError(s);}
 public static void main(String[] args){
  TestContext c=new TestContext();PlanStore store=new PlanStore(c);
  check(store.selected()==0&&store.load().work==14&&store.load().rest==1,"upgrade defaults preserve 14/1");
  for(int i=0;i<4;i++){store.preset(i);PlanStore reopened=new PlanStore(c);Schedule.Rhythm r=reopened.load();check(reopened.selected()==i&&r.work==PlanStore.PRESETS[i][0]&&r.rest==PlanStore.PRESETS[i][1]&&r.aligned,"remember preset");}
  long anchor=1794140000123L;store.custom(37,8,false,anchor);Schedule.Rhythm r=new PlanStore(c).load();check(store.selected()==-1&&!r.aligned&&r.anchor==anchor&&r.work==37&&r.rest==8,"custom origin persisted");
  try{store.custom(0,5,true,anchor);throw new AssertionError("invalid saved");}catch(IllegalArgumentException expected){}
  check(store.load().work==37&&store.load().anchor==anchor,"invalid apply preserves current plan");
  TimeZone old=TimeZone.getDefault();try{TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));store.custom(17,4,true,anchor);r=store.load();check(r.anchor%86400000L==0&&r.aligned,"aligned custom midnight origin");}finally{TimeZone.setDefault(old);}
  System.out.println("PASS: actual PlanStore serialization with in-memory Android interface fixtures; default upgrade, preset/custom restore and invalid-apply preservation.");
 }
}

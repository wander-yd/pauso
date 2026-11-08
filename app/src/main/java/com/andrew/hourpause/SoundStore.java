package com.andrew.hourpause;
import android.content.*;
public final class SoundStore {
 public static final String[] NOISE={"white","pink","brown"},NATURE={"rain","forest"};
 private final SharedPreferences prefs;
 public SoundStore(Context c){prefs=c.getSharedPreferences("player",0);}
 public void initialize(){String chosen=id();SharedPreferences.Editor edit=prefs.edit();if(prefs.getString("sound_id","").isEmpty())edit.putString("sound_id",chosen);if(prefs.getInt("volume",-1)<0)edit.putInt("volume",volume());edit.apply();}
 public String id(){String saved=prefs.getString("sound_id","");if(valid(saved))return saved;return prefs.getString("uri","").isEmpty()?"brown":"local";}
 public static boolean valid(String id){return id.equals("white")||id.equals("pink")||id.equals("brown")||id.equals("rain")||id.equals("forest")||id.equals("local");}
 public static boolean noise(String id){return id.equals("white")||id.equals("pink")||id.equals("brown");}
 public void select(String id){if(!valid(id))throw new IllegalArgumentException("Unknown sound");prefs.edit().putString("sound_id",id).apply();}
 public String title(){return title(id());}
 public String title(String id){
  if(id.equals("local"))return prefs.getString("title","My Audio");
  if(id.equals("white"))return "White Noise";if(id.equals("pink"))return "Pink Noise";if(id.equals("brown"))return "Brown Noise";if(id.equals("rain"))return "Forest Rain";return "Forest Birds";
 }
 public String uri(){return prefs.getString("uri","");}
 public int volume(){return prefs.getInt("volume",prefs.getString("uri","").isEmpty()?35:75);}
 public static String positionKey(String id){return id.equals("local")?"position":"sound_position_"+id;}
 public int position(){return prefs.getInt(positionKey(id()),0);}
}

package com.andrew.hourpause;
public class SoundStoreTest {
 static void check(boolean c,String s){if(!c)throw new AssertionError(s);}
 public static void main(String[] args){
  PlanStoreTest.TestContext fresh=new PlanStoreTest.TestContext();SoundStore sounds=new SoundStore(fresh);sounds.initialize();check(sounds.id().equals("brown")&&sounds.volume()==35,"first launch is ready with quiet default");
  fresh.memory.putString("uri","content://my-file").putString("title","My music").apply();sounds.select("local");check(sounds.volume()==35,"changing to file does not raise volume");
  sounds.select("forest");check(sounds.title().equals("Forest Birds"),"nature category name");check(sounds.uri().equals("content://my-file"),"built-in preserves local file");
  check(new SoundStore(fresh).id().equals("forest"),"reopen remembers sound");sounds.select("local");check(sounds.title().equals("My music"),"switch back to remembered file");
  PlanStoreTest.TestContext existing=new PlanStoreTest.TestContext();existing.memory.putString("uri","content://legacy").putString("title","Legacy music").putInt("position",12345).putInt("rhythm_work",90).apply();
  SoundStore migrated=new SoundStore(existing);migrated.initialize();check(migrated.id().equals("local")&&migrated.volume()==75&&migrated.position()==12345,"old choice position and default volume preserved");
  check(existing.memory.getInt("rhythm_work",0)==90,"sound migration does not touch rhythm");
  existing.memory.putInt("volume",19).apply();migrated.initialize();check(migrated.volume()==19,"explicit volume preserved");
  for(String id:new String[]{"white","pink","brown","rain","forest","local"})check(SoundStore.valid(id),"catalog entry");
  try{sounds.select("unknown");throw new AssertionError("invalid accepted");}catch(IllegalArgumentException expected){}
  check(sounds.id().equals("local"),"invalid selection cannot alter source");
  check(!SoundStore.positionKey("forest").equals(SoundStore.positionKey("local")),"independent saved positions");
  PlanStoreTest.TestContext upgraded=new PlanStoreTest.TestContext();upgraded.memory.putString("sound_id","pink").apply();SoundStore recall=new SoundStore(upgraded);recall.initialize();recall.select("forest");check(recall.lastInCategory(true).equals("pink"),"upgraded noise remembered across category switch");recall.select("white");check(recall.lastInCategory(false).equals("forest"),"independent nature recall");recall.select("local");recall.initialize();check(recall.lastInCategory(true).equals("white"),"local music preserves category memory");
  System.out.println("PASS: default Brown, old-user migration, volume stability, source persistence/local preservation, independent positions and invalid selection.");
 }
}

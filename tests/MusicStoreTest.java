package com.andrew.hourpause;
import android.content.*;import java.util.*;
public final class MusicStoreTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static class Context extends android.content.Context {
  final PlanStoreTest.Memory player=new PlanStoreTest.Memory(),library=new PlanStoreTest.Memory();
  public SharedPreferences getSharedPreferences(String name,int mode){return name.equals("player")?player:library;}
 }
 public static void main(String[] args){
  Context c=new Context();c.player.putString("uri","content://legacy").putString("title","Old song").putInt("position",1200).putInt("volume",17).putInt("rhythm_work",90).apply();MusicStore store=new MusicStore(c);
  check(store.state().queue.current().uri.equals("content://legacy")&&store.state().queue.position==1200,"legacy choice/position migrated");check(store.state().queue.repeat==PlaybackQueue.ALL,"legacy single track remains looping");store.save();
  LibraryState.Playlist p=new LibraryState.Playlist("Work");p.tracks.add(new Track("content://a","A","",0));p.tracks.add(new Track("content://b","B","",0));store.state().playlists.add(p);store.save();store.select(p.tracks,1);store.position("content://b",321);check(c.player.getString("uri","").equals("content://b"),"current mirror");
  MusicStore second=new MusicStore(c);check(second.state().queue.current().title.equals("B")&&second.state().queue.position==321,"new store sees queue");check(second.state().playlists.size()==1,"playlist retained");check(c.player.getInt("volume",0)==17&&c.player.getInt("rhythm_work",0)==90,"volume and rhythm untouched");
  String before=c.library.getString("state","");store.position("content://stale",77);check(c.library.getString("state","").equals(before),"late callback cannot overwrite selected position");
  store.state().playlists.clear();store.save();check(store.state().queue.tracks.size()==2,"playlist deletion retains queue");
  Context fresh=new Context();check(new MusicStore(fresh).state().queue.current()==null,"new install no inherited queue");
  c.library.putString("state",before).apply();check(new MusicStore(c).state().queue.position==321,"serialized state reload survives restart");
  System.out.println("PASS: local-file migration, queue/playlist persistence, mirrors, stale position guard, deletion and settings isolation.");
 }
}

package com.andrew.hourpause;
import android.content.*;import java.util.*;
/** All callers mutate on the Android main thread; queries return immutable track snapshots. */
public final class MusicStore {
 private final SharedPreferences prefs,player;private static LibraryState cache;private static String saved;private static SharedPreferences owner;
 public MusicStore(Context c){prefs=c.getSharedPreferences("music_library",0);player=c.getSharedPreferences("player",0);}
 public LibraryState state(){String data=prefs.getString("state","");if(cache==null||owner!=prefs||!data.equals(saved)){owner=prefs;cache=LibraryState.decode(data);saved=data;
  if(cache.queue.tracks.isEmpty()&&!player.getString("uri","").isEmpty()){cache.queue.set(Collections.singletonList(new Track(player.getString("uri",""),player.getString("title","My Audio"),"",0)),0,new Random());cache.queue.repeat=PlaybackQueue.ALL;cache.queue.position=Math.max(0,player.getInt("position",0));}}
  return cache;
 }
 public void save(){String data=state().encode();prefs.edit().putString("state",data).apply();saved=data;}
 public void mirror(){Track t=state().queue.current();if(t!=null)player.edit().putString("uri",t.uri).putString("title",t.title).putInt("position",state().queue.position).apply();}
 public void select(List<Track> tracks,int index){state().queue.set(tracks,index,new Random());save();mirror();player.edit().putString("sound_id","local").apply();}
 public void selectShuffled(List<Track> tracks){state().queue.setRandomized(tracks,new Random());save();mirror();player.edit().putString("sound_id","local").apply();}
 public void position(String uri,int position){PlaybackQueue q=state().queue;if(q.current()!=null&&q.current().uri.equals(uri)){q.position=Math.max(0,position);save();}}
}

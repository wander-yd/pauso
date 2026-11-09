package com.andrew.hourpause;
import java.util.*;
public final class MusicQueueTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static ArrayList<Track> tracks(int n){ArrayList<Track> ts=new ArrayList<>();for(int i=0;i<n;i++)ts.add(new Track("content://media/"+i,"曲目 "+i,"Artist",1000+i));return ts;}
 public static void main(String[] args){
  Random random=new Random(17);PlaybackQueue q=new PlaybackQueue();check(!q.advance(true,random)&&!q.previous(),"empty queue safe");
  q.set(tracks(3),0,random);q.repeat=PlaybackQueue.OFF;check(q.current().uri.endsWith("0"),"starts chosen track");check(q.advance(true,random)&&q.current().uri.endsWith("1"),"natural next");q.advance(true,random);check(!q.advance(true,random),"off stops exactly at end");check(q.previous()&&q.current().uri.endsWith("1"),"previous");
  q.repeat=PlaybackQueue.ONE;String uri=q.current().uri;q.position=55;check(q.advance(true,random)&&q.current().uri.equals(uri)&&q.position==0,"repeat-one restarts same track");check(q.advance(false,random)&&!q.current().uri.equals(uri),"explicit skip ignores repeat-one");
  q.repeat=PlaybackQueue.ALL;check(q.advance(true,random)&&q.current().uri.endsWith("0"),"all wraps");check(q.previous()&&q.current().uri.endsWith("2"),"previous wraps under all");
  q.set(tracks(9),4,random);q.shuffle(true,random);check(q.current().uri.endsWith("4"),"shuffle preserves current");Set<String> seen=new HashSet<>();for(int i=0;i<9;i++){check(seen.add(q.current().uri),"no repeats within shuffle round");if(i<8)check(q.advance(true,random),"shuffle advances");}String last=q.current().uri;check(q.advance(true,random)&&!q.current().uri.equals(last),"new shuffle round no immediate repeat");
  String current=q.current().uri;q.shuffle(false,random);check(q.current().uri.equals(current),"turning shuffle off retains current");
  q.shuffle(true,random);q.repeat=PlaybackQueue.OFF;q.set(tracks(5),0,random);for(int i=0;i<4;i++)check(q.advance(false,random),"shuffle off-boundary traversal");check(!q.advance(false,random),"shuffle without repeat stops");
  q.set(tracks(1),0,random);q.repeat=PlaybackQueue.ALL;for(int i=0;i<10;i++)check(q.advance(true,random)&&q.valid(),"singleton repeat");
  q.set(tracks(3),0,random);q.repeat=PlaybackQueue.ALL;int failures=1;while(q.failed(random)){if(++failures>3)throw new AssertionError("failure loop unbounded");}check(failures==3,"all unavailable stops within one round");
  LibraryState state=new LibraryState();LibraryState.Playlist p=new LibraryState.Playlist("工作 🎵");state.playlists.add(p);check(LibraryState.add(p,tracks(5))==5,"add files");check(LibraryState.add(p,tracks(5))==0,"dedup");Collections.swap(p.tracks,0,3);state.queue.set(p.tracks,2,random);state.queue.shuffle(true,random);state.queue.position=4321;state.missing(p.tracks.get(2).uri);
  String missing=p.tracks.get(2).uri;LibraryState.add(p,Collections.singletonList(p.tracks.get(2)));check(p.tracks.get(2).missing,"unavailable re-add remains flagged");
  Track repaired=new Track(missing,"Repaired","",1);LibraryState.add(p,Collections.singletonList(repaired));check(!p.tracks.get(2).missing&&p.tracks.size()==5,"re-import restores unavailable duplicate");state.restored(Collections.singleton(missing));check(!state.queue.current().missing,"permission refresh restores queue access");state.missing(missing);
  String encoded=state.encode();LibraryState restored=LibraryState.decode(encoded);check(restored.playlists.get(0).name.equals("工作 🎵"),"Unicode playlist survives");check(restored.queue.position==4321&&restored.queue.shuffle&&restored.queue.current().missing,"queue/position/unavailable persisted");check(restored.queue.current().uri.equals(state.queue.current().uri)&&restored.queue.order.equals(state.queue.order),"shuffle round preserved exactly");
  p.tracks.clear();check(state.queue.tracks.size()==5,"queue snapshot independent of edits");state.playlists.clear();check(state.queue.tracks.size()==5,"deleting playlist preserves queue");
  check(LibraryState.decode("garbage").playlists.isEmpty(),"corrupt store safely resets");restored.queue.order.set(0,999);check(LibraryState.decode(restored.encode()).queue.tracks.isEmpty(),"invalid persisted queue rejected");
  PlaybackGate gate=new PlaybackGate();Schedule.Snapshot work=new Schedule.Rhythm(14,1,false,0).at(0,TimeZone.getTimeZone("UTC")); // legacy 14/1 phase
  gate.pause();check(!gate.shouldPlay(true,true,work),"manual pause wins while choosing next track");gate.resume();
  System.out.println("PASS: queue end/repeat/skip/previous, deterministic shuffle rounds, persistence/reorder/snapshot/delete/corruption and manual-pause gate.");
 }
}

package com.andrew.hourpause;
import java.util.*;
/** Pure queue policy. Natural completion and explicit skip intentionally differ. */
public final class PlaybackQueue {
 public static final int OFF=0,ALL=1,ONE=2;
 public final ArrayList<Track> tracks=new ArrayList<>();public final ArrayList<Integer> order=new ArrayList<>();
 public int cursor,repeat=ALL,position;public int failures;public boolean shuffle;
 public Track current(){return order.isEmpty()?null:tracks.get(order.get(cursor));}
 public void set(List<Track> input,int start,Random random){
  tracks.clear();order.clear();HashSet<String> seen=new HashSet<>();String wanted=input.isEmpty()?"":input.get(Math.max(0,Math.min(start,input.size()-1))).uri;
  for(Track t:input)if(t.uri!=null&&!t.uri.isEmpty()&&seen.add(t.uri))tracks.add(t.copy());
  int selected=0;for(int i=0;i<tracks.size();i++)if(tracks.get(i).uri.equals(wanted))selected=i;
  cursor=0;position=0;failures=0;for(int i=0;i<tracks.size();i++)order.add(i);
  if(shuffle&&!order.isEmpty()){Collections.shuffle(order,random);order.remove(Integer.valueOf(selected));order.add(0,selected);}else cursor=selected;
 }
 public void shuffle(boolean on,Random random){
  if(shuffle==on)return;shuffle=on;Track keep=current();int index=keep==null?0:tracks.indexOf(keep);order.clear();for(int i=0;i<tracks.size();i++)order.add(i);
  if(on&&!order.isEmpty()){Collections.shuffle(order,random);order.remove(Integer.valueOf(index));order.add(0,index);cursor=0;}else cursor=index;
 }
 public boolean advance(boolean natural,Random random){
  if(order.isEmpty())return false;
  if(natural&&repeat==ONE){position=0;return true;}
  if(cursor+1<order.size()){cursor++;position=0;return true;}
  if(repeat!=ALL)return false;
  int last=order.get(cursor);cursor=0;position=0;
  if(shuffle&&order.size()>1){Collections.shuffle(order,random);if(order.get(0)==last)Collections.swap(order,0,1);}
  return true;
 }
 public boolean failed(Random random){if(current()!=null)current().missing=true;return ++failures<tracks.size()&&advance(false,random);}
 public boolean previous(){if(order.isEmpty())return false;if(cursor>0)cursor--;else if(repeat==ALL)cursor=order.size()-1;else return false;position=0;return true;}
 public boolean valid(){
  if(repeat<OFF||repeat>ONE||position<0||tracks.size()!=order.size()||(tracks.isEmpty()?cursor!=0:(cursor<0||cursor>=order.size())))return false;
  HashSet<Integer> seen=new HashSet<>();HashSet<String> uris=new HashSet<>();for(Track t:tracks)if(t.uri.isEmpty()||!uris.add(t.uri))return false;
  for(int i:order)if(i<0||i>=tracks.size()||!seen.add(i))return false;return true;
 }
}

package com.andrew.hourpause;
import java.io.*;import java.util.*;
/** Versioned local-only persistence. No paths or listening data leave the device. */
public final class LibraryState {
 public static final int MAX_TRACKS=10000,MAX_LISTS=100;
 public static final class Playlist {public String id=UUID.randomUUID().toString(),name;public final ArrayList<Track> tracks=new ArrayList<>();public Playlist(String n){name=n;}}
 public final ArrayList<Playlist> playlists=new ArrayList<>();public PlaybackQueue queue=new PlaybackQueue();
 public Playlist find(String id){for(Playlist p:playlists)if(p.id.equals(id))return p;return null;}
 public void missing(String uri){for(Track t:queue.tracks)if(t.uri.equals(uri))t.missing=true;for(Playlist p:playlists)for(Track t:p.tracks)if(t.uri.equals(uri))t.missing=true;}
 public static int add(Playlist p,List<Track> tracks){HashMap<String,Integer> known=new HashMap<>();for(int i=0;i<p.tracks.size();i++)known.put(p.tracks.get(i).uri,i);int n=0;for(Track t:tracks){Integer index=known.get(t.uri);if(index!=null){if(p.tracks.get(index).missing&&!t.missing)p.tracks.set(index,t.copy());}else if(p.tracks.size()<MAX_TRACKS){known.put(t.uri,p.tracks.size());p.tracks.add(t.copy());n++;}}return n;}
 public void restored(Set<String> available){for(Track t:queue.tracks)if(available.contains(t.uri))t.missing=false;for(Playlist p:playlists)for(Track t:p.tracks)if(available.contains(t.uri))t.missing=false;}
 private static void tracks(DataOutputStream out,List<Track> list)throws IOException {out.writeInt(list.size());for(Track t:list){out.writeUTF(t.uri);out.writeUTF(t.title);out.writeUTF(t.artist);out.writeLong(t.duration);out.writeBoolean(t.missing);}}
 private static int count(DataInputStream in,int max)throws IOException {int n=in.readInt();if(n<0||n>max)throw new IOException("Invalid count");return n;}
 private static ArrayList<Track> tracks(DataInputStream in)throws IOException {ArrayList<Track> a=new ArrayList<>();int n=count(in,MAX_TRACKS);for(int i=0;i<n;i++){Track t=new Track(in.readUTF(),in.readUTF(),in.readUTF(),in.readLong());t.missing=in.readBoolean();a.add(t);}return a;}
 public String encode(){try{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);o.writeInt(1);o.writeInt(playlists.size());for(Playlist p:playlists){o.writeUTF(p.id);o.writeUTF(p.name);tracks(o,p.tracks);}tracks(o,queue.tracks);o.writeBoolean(queue.shuffle);o.writeInt(queue.repeat);o.writeInt(queue.cursor);o.writeInt(queue.position);for(int i:queue.order)o.writeInt(i);o.close();return Base64.getEncoder().encodeToString(b.toByteArray());}catch(IOException e){throw new IllegalStateException(e);}}
 public static LibraryState decode(String value){try{
  if(value.length()>16000000)throw new IOException();DataInputStream in=new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(value)));if(in.readInt()!=1)throw new IOException();
  LibraryState s=new LibraryState();int n=count(in,MAX_LISTS);HashSet<String> ids=new HashSet<>();for(int i=0;i<n;i++){String id=in.readUTF(),name=in.readUTF();if(id.isEmpty()||!ids.add(id)||name.trim().isEmpty())throw new IOException();Playlist p=new Playlist(name);p.id=id;p.tracks.addAll(tracks(in));s.playlists.add(p);}
  s.queue.tracks.addAll(tracks(in));s.queue.shuffle=in.readBoolean();s.queue.repeat=in.readInt();s.queue.cursor=in.readInt();s.queue.position=in.readInt();for(int i=0;i<s.queue.tracks.size();i++)s.queue.order.add(in.readInt());if(!s.queue.valid()||in.available()!=0)throw new IOException();return s;
 }catch(Exception e){return new LibraryState();}}
}

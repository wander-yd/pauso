package com.andrew.hourpause;
public final class Track {
 public final String uri,title,artist;public final long duration;public boolean missing;
 public Track(String u,String t,String a,long d){uri=u;title=t==null||t.trim().isEmpty()?"Untitled audio":t;artist=a==null||a.equals("<unknown>")?"":a;duration=Math.max(0,d);}
 public Track copy(){Track t=new Track(uri,title,artist,duration);t.missing=missing;return t;}
}

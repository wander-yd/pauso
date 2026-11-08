package com.andrew.hourpause;
public final class UpdateInfoTest {
 static final String SHA=new String(new char[64]).replace('\0','a');
 static final String GOOD="https://github.com/wander-yd/pauso-releases/releases/download/v0.12.2/Pauso.apk";
 static void check(boolean c,String s){if(!c)throw new AssertionError(s);}
 static UpdateInfo info(int code,int sdk,String url){return new UpdateInfo(UpdateInfo.PACKAGE,code,"0.12.2",sdk,url,"Notes",SHA);}
 public static void main(String[] args){
  UpdateInfo u=info(1002,26,GOOD);check(u.newerThan(12,34),"CI version upgrades bootstrap");check(u.newerThan(1001,34),"integer version order");check(!u.newerThan(1002,34)&&!u.newerThan(1003,34),"never prompt equal/older");check(!info(1003,35,GOOD).newerThan(1002,34),"Android compatibility");
  String[] bad={GOOD.replace("https:","http:"),GOOD.replace("github.com","evil.example"),GOOD.replace("pauso-releases","other"),GOOD+"?redirect=evil",GOOD+"#x",GOOD.replace("github.com","user@github.com"),GOOD.replace("github.com","github.com:443"),GOOD.replace("Pauso.apk","source.zip"),GOOD.replace("v0.12.2","v0.12.2%2F.."),GOOD.replace("/releases/","/releases%2f")};
  for(String url:bad){try{info(1002,26,url);throw new AssertionError("unsafe URL accepted");}catch(IllegalArgumentException expected){}}
  for(Object n:new Object[]{"12",12.5,0,-1,Long.MAX_VALUE,2100000001L,Boolean.TRUE}){try{UpdateInfo.integer(n);throw new AssertionError("malformed integer accepted");}catch(IllegalArgumentException expected){}}
  check(UpdateInfo.integer(1001L)==1001,"valid long");
  try{new UpdateInfo("wrong.package",1002,"0.12.2",26,GOOD,"",SHA);throw new AssertionError("wrong app accepted");}catch(IllegalArgumentException expected){}
  try{new UpdateInfo(UpdateInfo.PACKAGE,1002,"0.12.2",26,GOOD,"","bad");throw new AssertionError("bad digest accepted");}catch(IllegalArgumentException expected){}
  String longNotes=new String(new char[5000]).replace('\0','x');check(new UpdateInfo(UpdateInfo.PACKAGE,1002,"0.12.2",26,GOOD,longNotes,SHA).notes.length()==4000,"bounded plain notes");
  System.out.println("PASS: update integer/version/sdk policy, exact trusted release assets, malformed metadata and bounded notes.");
 }
}

package com.andrew.hourpause;
import java.net.*;import java.io.*;import java.util.*;
public final class UpdateFetchTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static class Response extends HttpURLConnection {
  int code;String location;byte[] body;boolean disconnected;
  Response(int code,String location,byte[] body)throws Exception{super(new URL(UpdateFetch.DIRECT));this.code=code;this.location=location;this.body=body;}
  public void connect(){}public boolean usingProxy(){return false;}public void disconnect(){disconnected=true;}
  public int getResponseCode(){return code;}public String getHeaderField(String name){return location;}public InputStream getInputStream(){return new ByteArrayInputStream(body);}
 }
 static void fails(UpdateFetch f,String s)throws Exception{try{f.fetch(s,10000);throw new AssertionError("unsafe response accepted");}catch(IOException expected){}}
 public static void main(String[] args)throws Exception {
  Response good=new Response(200,null,"{\"versionCode\":1005}".getBytes("UTF-8"));UpdateFetch direct=new UpdateFetch(u->good);check(direct.fetch(UpdateFetch.DIRECT,12000).contains("1005"),"direct metadata");check(good.disconnected&&!good.getInstanceFollowRedirects()&&!good.getUseCaches(),"connections closed and explicit redirect/cache control");
  check(!UpdateFetch.trusted(new URL("http://github.com/x")),"HTTPS only");check(!UpdateFetch.trusted(new URL("https://github.com.evil.test/x")),"host suffix rejected");check(!UpdateFetch.trusted(new URL("https://raw.githubusercontent.com/other/repo/main/update.json")),"raw path pinned");
  Response redirect=new Response(302,"https://evil.test/update.json",new byte[0]);fails(new UpdateFetch(u->redirect),UpdateFetch.RELEASE);check(redirect.disconnected,"rejected redirect disconnected");
  Response huge=new Response(200,null,new byte[65537]);fails(new UpdateFetch(u->huge),UpdateFetch.DIRECT);
  Response missing=new Response(404,null,new byte[0]);fails(new UpdateFetch(u->missing),UpdateFetch.DIRECT);
  ArrayDeque<Response> chain=new ArrayDeque<>();chain.add(new Response(302,"https://release-assets.githubusercontent.com/file",new byte[0]));chain.add(new Response(200,null,"metadata".getBytes("UTF-8")));check(new UpdateFetch(u->chain.remove()).fetch(UpdateFetch.RELEASE,30000).equals("metadata"),"trusted asset redirect succeeds");
  fails(new UpdateFetch(u->good),"https://user:secret@github.com/x");fails(direct,UpdateFetch.DIRECT+"?unexpected=1");try{direct.fetch(UpdateFetch.DIRECT,0);throw new AssertionError("deadline ignored");}catch(SocketTimeoutException expected){}
  final int[] calls={0};UpdateFetch fallback=new UpdateFetch(u->{if(calls[0]++==0)throw new SocketTimeoutException("fixture timeout");return good;});check(fallback.validated(t->t).contains("1005")&&calls[0]==2,"timeout falls back to release source");
  final int[] parsed={0};check(direct.validated(t->{if(parsed[0]++==0)throw new IOException("bad primary JSON");return t;}).contains("1005")&&parsed[0]==2,"invalid primary metadata also falls back");
  System.out.println("PASS: direct metadata, pinned HTTPS hosts/path, trusted redirects, connection cleanup, oversized/missing/unsafe response rejection.");
 }
}

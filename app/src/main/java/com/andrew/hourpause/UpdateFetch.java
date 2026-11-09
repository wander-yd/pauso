package com.andrew.hourpause;
import java.net.*;import java.io.*;
/** Bounded HTTPS transport with explicit trusted destinations; independent of Android UI. */
public final class UpdateFetch {
 public static final String DIRECT="https://raw.githubusercontent.com/"+UpdateInfo.REPOSITORY+"/main/update.json";
 public static final String RELEASE="https://github.com/"+UpdateInfo.REPOSITORY+"/releases/latest/download/update.json";
 public interface Open {HttpURLConnection open(URL url)throws IOException;}
 public interface Parse<T> {T parse(String text)throws Exception;}
 private final Open open;
 public <T> T validated(Parse<T> parser)throws Exception {
  Exception failure=null;String[] sources={DIRECT,RELEASE};int[] budgets={12000,30000};
  for(int i=0;i<sources.length;i++)try{return parser.parse(fetch(sources[i],budgets[i]));}catch(Exception e){failure=e;}
  throw failure==null?new IOException("No update source"):failure;
 }
 public UpdateFetch(){this(url->(HttpURLConnection)url.openConnection());}
 public UpdateFetch(Open factory){open=factory;}
 public static boolean trusted(URL url){
  String host=url.getHost();if(!"https".equals(url.getProtocol())||url.getUserInfo()!=null||(url.getPort()!=-1&&url.getPort()!=443))return false;
  if(host.equals("raw.githubusercontent.com"))return url.getPath().equals("/"+UpdateInfo.REPOSITORY+"/main/update.json")&&url.getQuery()==null;
  return host.equals("github.com")||host.equals("release-assets.githubusercontent.com")||host.equals("objects.githubusercontent.com");
 }
 public String fetch(String source,int budgetMillis)throws IOException{
  URL url=new URL(source);long deadline=System.nanoTime()+budgetMillis*1000000L;
  for(int redirects=0;redirects<5;redirects++){
   if(!trusted(url))throw new IOException("Untrusted update redirect");
   int left=remaining(deadline);HttpURLConnection c=open.open(url);c.setInstanceFollowRedirects(false);c.setUseCaches(false);c.setConnectTimeout(Math.min(10000,left));c.setReadTimeout(Math.min(12000,left));
   c.setRequestProperty("User-Agent","Pauso update checker");c.setRequestProperty("Cache-Control","no-cache");c.setRequestProperty("Accept","application/json");
   try{int status=c.getResponseCode();if(status==404)throw new FileNotFoundException("Update file unavailable");
    if(status==301||status==302||status==303||status==307||status==308){String location=c.getHeaderField("Location");if(location==null)throw new IOException("Missing update redirect");url=new URL(url,location);continue;}
    if(status!=200)throw new IOException("Update server HTTP "+status);
    try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
     byte[] bytes=new byte[4096];int n;while(true){c.setReadTimeout(Math.min(12000,remaining(deadline)));n=in.read(bytes);if(n==-1)break;if(out.size()+n>65536)throw new IOException("Update file too large");out.write(bytes,0,n);}return out.toString("UTF-8");
    }
   }finally{c.disconnect();}
  }throw new IOException("Too many update redirects");
 }
 private static int remaining(long deadline)throws SocketTimeoutException {long left=(deadline-System.nanoTime())/1000000L;if(left<=0)throw new SocketTimeoutException("Update timed out");return (int)Math.min(Integer.MAX_VALUE,left);}
}

package com.andrew.hourpause;
import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.*;
import android.widget.Toast;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UpdateClient {
 private static final long DAY=86400000L;
 private static final String MANIFEST="https://github.com/"+UpdateInfo.REPOSITORY+"/releases/latest/download/update.json";
 private final Activity activity;private final android.content.SharedPreferences prefs;
 private final Handler ui=new Handler(Looper.getMainLooper());private final AtomicBoolean busy=new AtomicBoolean();
 public UpdateClient(Activity a){activity=a;prefs=a.getSharedPreferences("updates",0);}
 private boolean visible(){return !activity.isFinishing()&&!activity.isDestroyed();}
 public String currentVersion(){try{return activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName;}catch(Exception e){return "";}}
 private long installed(){try{PackageInfo p=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0);return Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;}catch(Exception e){return Long.MAX_VALUE;}}
 public void check(boolean manual){
  long now=System.currentTimeMillis(),last=prefs.getLong("last_attempt",0);
  if(!manual&&now>=last&&now-last<DAY)return;
  if(!busy.compareAndSet(false,true)){if(manual)Toast.makeText(activity,"Already checking for updates…",Toast.LENGTH_SHORT).show();return;}
  prefs.edit().putLong("last_attempt",now).apply();
  if(manual)Toast.makeText(activity,"Checking for updates…",Toast.LENGTH_SHORT).show();
  new Thread(()->{
   UpdateInfo result=null;String message=null;
   try{JSONObject j=new JSONObject(fetch());result=new UpdateInfo(j.getString("packageName"),UpdateInfo.integer(j.get("versionCode")),j.getString("versionName"),UpdateInfo.integer(j.get("minSdk")),j.getString("downloadUrl"),j.optString("releaseNotes",""),j.getString("apkSha256"));}
   catch(FileNotFoundException e){message="No public release has been published yet.";}
   catch(Exception e){message="Couldn't check for updates. Your sounds and timers still work offline. Try again later.";}
   final UpdateInfo update=result;final String error=message;
   ui.post(()->{busy.set(false);if(!visible())return;
    if(error!=null){if(manual)notice(error);return;}
    if(update.newerThan(installed(),Build.VERSION.SDK_INT)){
     if(!manual&&prefs.getInt("prompted_version",0)==update.versionCode)return;
     prefs.edit().putInt("prompted_version",update.versionCode).apply();
     new AlertDialog.Builder(activity).setTitle("Pauso update available").setMessage("Version "+currentVersion()+" → "+update.versionName+"\n\n"+update.notes+"\n\nDownload in your browser, then confirm installation. Your current audio is not interrupted.")
      .setNegativeButton("Later",null).setPositiveButton("Download update",(d,w)->{
       try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(update.downloadUrl)));}catch(ActivityNotFoundException e){notice("No browser is available to open the download.");}
      }).show();
    }else if(manual)notice(update.minSdk>Build.VERSION.SDK_INT?"The latest version needs a newer Android version. You can keep using this one.":"You're up to date ("+currentVersion()+").");
   });
  },"Pauso-Update").start();
 }
 private void notice(String text){if(visible())new AlertDialog.Builder(activity).setTitle("Pauso updates").setMessage(text).setPositiveButton("OK",null).show();}
 private String fetch()throws Exception{
  URL url=new URL(MANIFEST);long deadline=System.currentTimeMillis()+20000;
  for(int redirects=0;redirects<5;redirects++){
   String host=url.getHost();if(!"https".equals(url.getProtocol())||url.getUserInfo()!=null||(url.getPort()!=-1&&url.getPort()!=443)||!(host.equals("github.com")||host.equals("release-assets.githubusercontent.com")||host.equals("objects.githubusercontent.com")))throw new IOException("Untrusted redirect");
   int timeout=(int)Math.min(6000,deadline-System.currentTimeMillis());if(timeout<=0)throw new IOException("Update timeout");
   HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(timeout);c.setReadTimeout(timeout);c.setRequestProperty("User-Agent","Pauso update checker");c.setRequestProperty("Cache-Control","no-cache");
   try{int status=c.getResponseCode();
    if(status==404)throw new FileNotFoundException();
    if(status==301||status==302||status==303||status==307||status==308){String next=c.getHeaderField("Location");if(next==null)throw new IOException("Missing redirect");url=new URL(url,next);continue;}
    if(status!=200)throw new IOException("Update request failed");
    try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
     byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))!=-1){if(out.size()+n>65536||System.currentTimeMillis()>deadline)throw new IOException("Invalid update response");out.write(bytes,0,n);}return out.toString("UTF-8");
    }
   }finally{c.disconnect();}
  }
  throw new IOException("Too many redirects");
 }
}

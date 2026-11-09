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
 private static final long RETRY=3600000L;
 private final Activity activity;private final android.content.SharedPreferences prefs;
 private final Handler ui=new Handler(Looper.getMainLooper());private final AtomicBoolean busy=new AtomicBoolean();
 public UpdateClient(Activity a){activity=a;prefs=a.getSharedPreferences("updates",0);}
 private boolean visible(){return !activity.isFinishing()&&!activity.isDestroyed();}
 public String currentVersion(){try{return activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName;}catch(Exception e){return "";}}
 private long installed(){try{PackageInfo p=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0);return Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;}catch(Exception e){return Long.MAX_VALUE;}}
 public void check(boolean manual){
  long now=System.currentTimeMillis(),last=prefs.getLong("last_attempt",0);
  if(!manual&&now>=last&&now-last<(prefs.getBoolean("last_success",false)?DAY:RETRY))return;
  if(!busy.compareAndSet(false,true)){if(manual)Toast.makeText(activity,"Already checking for updates…",Toast.LENGTH_SHORT).show();return;}
  prefs.edit().putLong("last_attempt",now).apply();
  if(manual)Toast.makeText(activity,"Checking for updates…",Toast.LENGTH_SHORT).show();
  new Thread(()->{
   UpdateInfo result=null;String message=null;
   try{result=fetchInfo();prefs.edit().putBoolean("last_success",true).apply();}
   catch(FileNotFoundException e){prefs.edit().putBoolean("last_success",false).apply();message="The update file is temporarily unavailable. Try again later, or download the latest APK from the Pauso release page.";}
   catch(Exception e){prefs.edit().putBoolean("last_success",false).apply();message=(e instanceof java.net.SocketTimeoutException?"The update connection timed out.":"Couldn't read the update information.")+" Your sounds and timers still work offline. Try another connection or download the latest APK from the Pauso release page.";}
   final UpdateInfo update=result;final String error=message;
   ui.post(()->{busy.set(false);if(!visible())return;
    if(error!=null){if(manual)problem(error);return;}
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
 private void problem(String message){if(visible())new AlertDialog.Builder(activity).setTitle("Pauso updates").setMessage(message)
  .setNegativeButton("Close",null).setPositiveButton("Retry",(d,w)->check(true)).setNeutralButton("Download page",(d,w)->{
   try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/"+UpdateInfo.REPOSITORY+"/releases")));}catch(ActivityNotFoundException e){Toast.makeText(activity,"No browser is available.",Toast.LENGTH_LONG).show();}
  }).show();}
 private void notice(String text){if(visible())new AlertDialog.Builder(activity).setTitle("Pauso updates").setMessage(text).setPositiveButton("OK",null).show();}
 private UpdateInfo fetchInfo()throws Exception{
  return new UpdateFetch().validated(text->{
   JSONObject j=new JSONObject(text);
   return new UpdateInfo(j.getString("packageName"),UpdateInfo.integer(j.get("versionCode")),j.getString("versionName"),UpdateInfo.integer(j.get("minSdk")),j.getString("downloadUrl"),j.optString("releaseNotes",""),j.getString("apkSha256"));
  });
 }
}

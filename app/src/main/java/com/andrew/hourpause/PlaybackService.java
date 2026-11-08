package com.andrew.hourpause;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.media.session.*;
import android.net.Uri;
import android.os.*;
import android.widget.RemoteViews;
import android.view.View;

public class PlaybackService extends Service {
 public static final String START="com.andrew.hourpause.START",STOP="com.andrew.hourpause.STOP",VOLUME="com.andrew.hourpause.VOLUME",PAUSE="com.andrew.hourpause.PAUSE",RESUME="com.andrew.hourpause.RESUME",PLAN="com.andrew.hourpause.PLAN",SOUND="com.andrew.hourpause.SOUND";
 public static volatile boolean running=false;
 public static volatile String status="Ready",error="";
 private static final int ID=14;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private AudioOutput output;
 private String sourceId="brown";
 private int generation;
 private MediaSession session;
 private AudioManager audio;
 private AudioFocusRequest focus;
 private PowerManager.WakeLock wake;
 private boolean prepared=false,hasFocus=false,registered=false,ending=false;
 private String uri="",title="";
 private final PlaybackGate gate=new PlaybackGate();
 private final Runnable tick=new Runnable(){public void run(){sync();}};
 private final BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
  if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(i.getAction())) finishPlayback();else sync();
 }};
 @Override public void onCreate(){super.onCreate();
  audio=(AudioManager)getSystemService(AUDIO_SERVICE);
  NotificationChannel channel=new NotificationChannel("playback","Music schedule",NotificationManager.IMPORTANCE_LOW);
  channel.setSound(null,null);channel.enableVibration(false);
  ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
  session=new MediaSession(this,"Pauso");
  session.setCallback(new MediaSession.Callback(){@Override public void onStop(){finishPlayback();}@Override public void onPause(){pausePlayback();}@Override public void onPlay(){resumePlayback();}});
  session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
  IntentFilter filter=new IntentFilter();filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);filter.addAction(Intent.ACTION_TIME_CHANGED);filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
  if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,filter);
  registered=true;
 }
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent==null){finishPlayback();return START_NOT_STICKY;}
  String action=intent.getAction();
  if(STOP.equals(action)){finishPlayback();return START_NOT_STICKY;}
  if(PAUSE.equals(action)){pausePlayback();return START_NOT_STICKY;}
  if(RESUME.equals(action)){resumePlayback();return START_NOT_STICKY;}
  if(PLAN.equals(action)){if(running)sync();else stopSelf();return START_NOT_STICKY;}
  if(SOUND.equals(action)){if(running){try{loadSound();}catch(Exception e){fail("Cannot open that sound. Choose another sound.");}}else stopSelf();return START_NOT_STICKY;}
  if(VOLUME.equals(action)){setVolume();return START_NOT_STICKY;}
  if(START.equals(action)&&!running){
   ending=false;error="";status="Loading music";
   SoundStore sounds=new SoundStore(this);sourceId=sounds.id();uri=sounds.uri();title=sounds.title();
   if(sourceId.equals("local")&&uri.isEmpty()){fail("Choose a local audio file first.");return START_NOT_STICKY;}
   try{
    if(Build.VERSION.SDK_INT>=29)startForeground(ID,notification(),ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);else startForeground(ID,notification());
    running=true;session.setActive(true);
    wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"HourPause:Schedule");wake.setReferenceCounted(false);wake.acquire(3600000L);
    AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
    focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs).setWillPauseWhenDucked(true).setOnAudioFocusChangeListener(change->{
     if(ending||gate.isPaused())return;
     if(change==AudioManager.AUDIOFOCUS_GAIN){hasFocus=true;sync();}
     else if(change==AudioManager.AUDIOFOCUS_LOSS){finishPlayback();}
     else {hasFocus=false;sync();}
    },handler).build();
    hasFocus=audio.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    if(!hasFocus){fail("Audio is busy. Try Play again when other audio or calls finish.");return START_NOT_STICKY;}
    loadSound();
   }catch(Exception e){fail("Cannot open the selected audio. Choose it again, or try another file.");}
  }
  return START_NOT_STICKY;
 }
 private void pausePlayback(){
  if(!running||ending)return;
  gate.pause();hasFocus=false;
  savePosition();
  if(focus!=null)audio.abandonAudioFocusRequest(focus);
  sync();
 }
 private void resumePlayback(){
  if(!running||ending||!gate.isPaused())return;
  hasFocus=focus!=null&&audio.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
  if(!hasFocus){error="Audio is busy. Tap Resume again after other audio or calls finish.";sync();return;}
  gate.resume();error="";sync();
 }
 private void savePosition(){
  if(output==null||!prepared||SoundStore.noise(sourceId))return;
  if(sourceId.equals("local")&&!uri.equals(new SoundStore(this).uri()))return;
  getSharedPreferences("player",0).edit().putInt(SoundStore.positionKey(sourceId),output.position()).apply();
 }
 private void loadSound()throws Exception{
  savePosition();generation++;final int token=generation;
  if(output!=null){output.release();output=null;}prepared=false;
  SoundStore sounds=new SoundStore(this);sourceId=sounds.id();uri=sounds.uri();title=sounds.title();
  output=new AudioOutput(this,sourceId,uri,sounds.position(),()->{if(token!=generation||ending)return;prepared=true;setVolume();sync();},()->{if(token==generation&&!ending)fail("Could not play this sound. Choose another sound.");});
  setVolume();sync();
 }
 private void setVolume(){if(output!=null)output.setVolume(new SoundStore(this).volume()/100f);}

 private void sync(){
  if(!running||ending)return;
  handler.removeCallbacks(tick);
  // Renew the bounded lock while the explicitly started session is running.
  if(wake!=null)wake.acquire(3600000L);
  long sampledAt=SystemClock.uptimeMillis();
  Schedule.Snapshot timing=new PlanStore(this).now();boolean quiet=timing.quiet;
  try{
   if(prepared&&output!=null)output.setPlaying(gate.shouldPlay(prepared,hasFocus,timing));
   if(ending||!running)return;
   status=gate.isPaused()?"Paused":!prepared?"Loading music":!hasFocus?"Interrupted":quiet?"Resting":"Playing";
   session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,title).putString(MediaMetadata.METADATA_KEY_ARTIST,"Pauso").build());
   session.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_STOP|(gate.isPaused()?PlaybackState.ACTION_PLAY:PlaybackState.ACTION_PAUSE)).setState(gate.shouldPlay(prepared,hasFocus,timing)?PlaybackState.STATE_PLAYING:PlaybackState.STATE_PAUSED,prepared&&output!=null?output.position():0,1f).build());
   ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(ID,notification());
   handler.postAtTime(tick,sampledAt+timing.refresh);
  }catch(Exception e){fail("Playback stopped. Try selecting the file again.");}
 }
 private int minuteIcon(int minutes){
  String name=minutes>99?"ic_minutes_many":"ic_minutes_"+minutes;
  int resource=getResources().getIdentifier(name,"drawable",getPackageName());
  return resource==0?R.drawable.ic_music:resource;
 }
 private Notification notification(){
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,PlaybackService.class).setAction(STOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent toggle=PendingIntent.getService(this,gate.isPaused()?3:2,new Intent(this,PlaybackService.class).setAction(gate.isPaused()?RESUME:PAUSE),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Schedule.Snapshot timing=new PlanStore(this).now();boolean quiet=timing.quiet;
  long remaining=timing.remaining;
  String label=quiet?"Music resumes in":"Quiet time in";
  if("Interrupted".equals(status))label="Interrupted · next change";
  if("Loading music".equals(status))label="Loading · next change";
  if(gate.isPaused())label=quiet?"Paused · quiet time remaining":"Paused · quiet time in";
  int icon=gate.isPaused()||quiet||"Interrupted".equals(status)?R.drawable.ic_quiet:minuteIcon(timing.minutes);
  RemoteViews content=new RemoteViews(getPackageName(),R.layout.notification_countdown);
  content.setContentDescription(R.id.countdown_timer,label);
  // Manual pause wins over the clock phase; never show green while audio is unavailable.
  int stateIcon;String stateDescription;
  if(gate.isPaused()){stateIcon=R.drawable.ic_status_pause;stateDescription="Manually paused";}
  else if(!prepared){stateIcon=R.drawable.ic_status_loading;stateDescription="Loading music";}
  else if(!hasFocus){stateIcon=R.drawable.ic_status_pause;stateDescription="Audio interrupted";}
  else if(quiet){stateIcon=R.drawable.ic_status_rest;stateDescription="Scheduled rest";}
  else{stateIcon=R.drawable.ic_status_music;stateDescription="Playing";}
  content.setImageViewResource(R.id.countdown_phase,stateIcon);
  content.setContentDescription(R.id.countdown_phase,stateDescription);
  content.setViewVisibility(R.id.countdown_phase,View.VISIBLE);
  content.setViewVisibility(R.id.countdown_paused,View.GONE);
  content.setTextViewText(R.id.countdown_track,title);
  content.setOnClickPendingIntent(R.id.countdown_stop,stop);
  content.setImageViewResource(R.id.countdown_toggle,gate.isPaused()?R.drawable.ic_control_play:R.drawable.ic_control_pause);
  content.setContentDescription(R.id.countdown_toggle,gate.isPaused()?"Resume schedule":"Pause schedule");
  content.setOnClickPendingIntent(R.id.countdown_toggle,toggle);
  content.setViewVisibility(R.id.countdown_timer,View.VISIBLE);
  content.setChronometerCountDown(R.id.countdown_timer,true);
  content.setChronometer(R.id.countdown_timer,SystemClock.elapsedRealtime()+remaining,null,true);
  return new Notification.Builder(this,"playback")
   .setSmallIcon(icon).setContentTitle(status+" · Pauso").setContentText(label)
   .setWhen(System.currentTimeMillis()+remaining).setShowWhen(true)
   .setUsesChronometer(true).setChronometerCountDown(true)
   .setCustomContentView(content).setCustomBigContentView(content)
   .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
   .setVisibility(Notification.VISIBILITY_PUBLIC)
   .setStyle(new Notification.DecoratedCustomViewStyle()).build();
 }
 private void fail(String message){error=message;finishPlayback();}
 private void finishPlayback(){
  if(ending)return;ending=true;running=false;status="Ready";handler.removeCallbacksAndMessages(null);
  generation++;savePosition();if(output!=null){output.release();output=null;}
  prepared=false;
  if(focus!=null){audio.abandonAudioFocusRequest(focus);focus=null;}hasFocus=false;
  if(wake!=null&&wake.isHeld())wake.release();
  if(session!=null)session.setActive(false);
  stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
 }
 @Override public void onDestroy(){finishPlayback();if(registered)unregisterReceiver(receiver);if(session!=null)session.release();super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}

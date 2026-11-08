package com.andrew.hourpause;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
/** One source, either PCM synthesis or an asynchronously prepared recording/file. */
public final class AudioOutput {
 private final Handler handler=new Handler(Looper.getMainLooper());
 private MediaPlayer media;private NoiseTrack noise;private boolean released,ready,wanted;
 private float volume=.35f,gain;private final Runnable failed;
 private final Runnable ramp=new Runnable(){public void run(){
  if(released||media==null||!ready)return;
  try{float target=wanted?volume:0;gain+=Math.max(-.12f,Math.min(.12f,target-gain));media.setVolume(gain,gain);
   if(Math.abs(gain-target)>.0001f)handler.postDelayed(this,10);else if(!wanted&&media.isPlaying())media.pause();
  }catch(Exception e){failed.run();}
 }};
 public AudioOutput(Context c,String id,String uri,int position,Runnable onReady,Runnable onError)throws Exception{
  failed=()->handler.post(()->{if(!released)onError.run();});
  if(SoundStore.noise(id)){
   noise=new NoiseTrack(id,()->handler.post(failed));ready=true;handler.post(()->{if(!released)onReady.run();});return;
  }
  media=new MediaPlayer();
  try{
   media.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
   if(id.equals("local"))media.setDataSource(c,Uri.parse(uri));
   else{int raw=id.equals("rain")?R.raw.forest_rain:R.raw.forest_birds;try(android.content.res.AssetFileDescriptor afd=c.getResources().openRawResourceFd(raw)){media.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());}}
   media.setLooping(true);media.setVolume(0,0);media.setOnErrorListener((p,w,e)->{failed.run();return true;});
   media.setOnPreparedListener(p->{if(released)return;
    Runnable done=()->{if(!released){ready=true;onReady.run();}};
    if(position>0&&position<p.getDuration()){p.setOnSeekCompleteListener(mp->{mp.setOnSeekCompleteListener(null);done.run();});p.seekTo(position,MediaPlayer.SEEK_CLOSEST);}else done.run();
   });media.prepareAsync();
  }catch(Exception e){media.release();media=null;throw e;}
 }
 public void setPlaying(boolean value){
  if(released)return;if(noise!=null){noise.setPlaying(value);return;}if(!ready)return;
  if(wanted==value)return;wanted=value;handler.removeCallbacks(ramp);
  try{if(value&&!media.isPlaying())media.start();ramp.run();}catch(Exception e){failed.run();}
 }
 public boolean isPlaying(){return noise!=null?noise.isPlaying():wanted;}
 public void setVolume(float value){volume=Math.max(0,Math.min(1,value));if(noise!=null)noise.setVolume(volume);else if(ready){handler.removeCallbacks(ramp);ramp.run();}}
 public int position(){if(media!=null&&ready&&!released)try{return media.getCurrentPosition();}catch(Exception ignored){}return 0;}
 public void release(){
  if(released)return;released=true;handler.removeCallbacksAndMessages(null);if(noise!=null)noise.release();
  if(media!=null){final MediaPlayer old=media;media=null;
   if(!ready){old.release();return;}
   final float start=gain;final Handler fade=new Handler(Looper.getMainLooper());
   fade.post(new Runnable(){int step;public void run(){try{float v=start*Math.max(0,1-(++step)/6f);old.setVolume(v,v);if(step<6)fade.postDelayed(this,10);else old.release();}catch(Exception e){try{old.release();}catch(Exception ignored){}}}});
  }
 }
}

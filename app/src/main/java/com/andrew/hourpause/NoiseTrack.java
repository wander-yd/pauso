package com.andrew.hourpause;
import android.media.*;
/** Dedicated streaming thread. Ramp to silence before pausing; no generating while idle. */
public final class NoiseTrack {
 private final Object lock=new Object();private volatile boolean playing,closing;
 private volatile float volume=.35f;private final AudioTrack track;private final NoiseGenerator generator;
 public NoiseTrack(String type,Runnable onError){
  generator=new NoiseGenerator(type,System.nanoTime());
  int minimum=AudioTrack.getMinBufferSize(NoiseGenerator.RATE,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
  if(minimum<=0)throw new IllegalStateException("PCM output unavailable");
  track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(NoiseGenerator.RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(Math.max(minimum,4096)).build();
  if(track.getState()!=AudioTrack.STATE_INITIALIZED){track.release();throw new IllegalStateException("Cannot initialize noise output");}
  new Thread(()->run(onError),"Pauso-Noise").start();
 }
 private void run(Runnable onError){
  float gain=0;boolean started=false;short[] buffer=new short[512];
  try{
   android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
   while(true){
    synchronized(lock){while(!playing&&!closing&&gain==0)lock.wait();}
    if(closing&&gain==0)break;
    if(!started){track.play();started=true;}
    for(int i=0;i<buffer.length;i++){
     float target=playing&&!closing?volume:0;float step=1f/(NoiseGenerator.RATE*.06f);
     gain+=Math.max(-step,Math.min(step,target-gain));if(Math.abs(gain)<.000001f)gain=0;
     buffer[i]=(short)(generator.next()*gain*32767);
    }
    int written=0;while(written<buffer.length){int n=track.write(buffer,written,buffer.length-written,AudioTrack.WRITE_BLOCKING);if(n<=0)throw new IllegalStateException("PCM write failed");written+=n;}
    if((!playing||closing)&&gain==0){track.pause();track.flush();started=false;}
   }
  }catch(Exception e){if(!closing)onError.run();}
  finally{try{track.pause();}catch(Exception ignored){}track.release();}
 }
 public void setPlaying(boolean value){if(closing)return;playing=value;synchronized(lock){lock.notifyAll();}}
 public boolean isPlaying(){return playing&&!closing;}
 public void setVolume(float value){volume=Math.max(0,Math.min(1,value));}
 public void release(){closing=true;playing=false;synchronized(lock){lock.notifyAll();}}
}

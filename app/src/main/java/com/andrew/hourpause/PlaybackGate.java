package com.andrew.hourpause;
/** Manual pause survives schedule boundaries and audio-focus changes. */
public final class PlaybackGate {
 private boolean paused;
 public void pause(){paused=true;}
 public void resume(){paused=false;}
 public boolean isPaused(){return paused;}
 public boolean shouldPlay(boolean prepared,boolean focus,Schedule.Snapshot timing){return prepared&&focus&&!paused&&!timing.quiet;}
 public boolean shouldPlay(boolean prepared,boolean focus,long phase){return prepared&&focus&&!paused&&!Schedule.quiet(phase);}
}

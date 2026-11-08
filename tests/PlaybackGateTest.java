package com.andrew.hourpause;
public class PlaybackGateTest {
 private static void check(boolean c,String label){if(!c)throw new AssertionError(label);}
 public static void main(String[] args){
  PlaybackGate gate=new PlaybackGate();
  check(gate.shouldPlay(true,true,Schedule.phase(13,59,999)),"play just before quiet");
  gate.pause();
  for(int t=0;t<900000;t++){
   check(!gate.shouldPlay(true,true,t),"manual pause survives every clock phase");
   check(!gate.shouldPlay(true,false,t),"focus changes cannot undo pause");
  }
  check(gate.isPaused(),"still manually paused after boundaries");
  gate.resume();
  check(!gate.shouldPlay(true,true,Schedule.phase(14,30,0)),"resume during quiet stays silent");
  check(gate.shouldPlay(true,true,Schedule.phase(15,0,0)),"plays at next block");
  gate.pause();gate.pause();gate.resume();gate.resume();
  check(gate.shouldPlay(true,true,Schedule.phase(0,0,0)),"repeated actions are idempotent");
  check(!gate.shouldPlay(false,true,0),"not prepared");
  check(!gate.shouldPlay(true,false,0),"no audio focus");
  check(gate.shouldPlay(true,true,0),"audio focus returned");
  System.out.println("PASS: manual pause across all 900,000 cycle positions, resume inside/outside quiet, focus and loading gates.");
 }
}

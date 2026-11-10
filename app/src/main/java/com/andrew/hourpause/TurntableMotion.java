package com.andrew.hourpause;
/** Presentation-only motion, driven by monotonic time. No player or schedule state. */
public final class TurntableMotion {
 private double angle,arm=-7,energy;private long last=-1;
 public double angle(){return angle;} public double arm(){return arm;}
 public double wobbleY(){return .35*energy*Math.sin(Math.toRadians(angle));}
 public double wobbleScale(){return 1+.0012*energy*Math.sin(Math.toRadians(angle));}
 public void freezeClock(){last=-1;}
 public void step(long millis,boolean playing,boolean reduced){
  double dt=last<0?0:Math.max(0,Math.min(.1,(millis-last)/1000.0));last=millis;
  double target=playing?0:-7;
  if(reduced){arm=target;energy=0;return;}
  if(playing)angle=(angle-10*dt)%360;
  double blend=1-Math.exp(-dt/.18);arm+=(target-arm)*blend;energy+=((playing?1:0)-energy)*blend;
  if(Math.abs(arm-target)<.005)arm=target;if(!playing&&energy<.001)energy=0;
 }
 public boolean needsFrame(boolean playing,boolean reduced){return !reduced&&(playing||Math.abs(arm+7)>.005||energy>.001);}
 public static double remainingFraction(long remaining,long segment){return segment<=0?0:Math.max(0,Math.min(1,(double)remaining/segment));}
}

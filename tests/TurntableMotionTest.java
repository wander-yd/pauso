package com.andrew.hourpause;
public final class TurntableMotionTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  TurntableMotion m=new TurntableMotion();m.step(0,true,false);for(int t=20;t<=36000;t+=20)m.step(t,true,false);
  check(Math.abs(m.angle())<.001||Math.abs(m.angle()+360)<.001,"one negative revolution per 36 seconds");
  m.step(36020,true,false);check(m.angle()<0,"counterclockwise");double held=m.angle();
  for(int t=36040;t<=39000;t+=20)m.step(t,false,false);
  check(m.angle()==held,"pause retains angle");check(m.arm()==-7&&m.wobbleY()==0,"park and settle");check(!m.needsFrame(false,false),"stationary draws stop");
  m.freezeClock();m.step(300000,true,false);check(m.angle()==held,"no catch-up after hidden interval");
  for(int t=300020;t<=301000;t+=20){m.step(t,true,false);check(Math.abs(m.wobbleY())<=.35001&&Math.abs(m.wobbleScale()-1)<=.001201,"subpixel record-only bounds");}
  double before=m.angle();m.step(301020,true,true);check(m.angle()==before&&m.wobbleY()==0&&m.arm()==0,"reduced motion keeps state but no spin");
  check(TurntableMotion.remainingFraction(60000,60000)==1&&TurntableMotion.remainingFraction(30000,60000)==.5&&TurntableMotion.remainingFraction(0,60000)==0,"remaining ring endpoints");
  check(TurntableMotion.remainingFraction(-1,60000)==0&&TurntableMotion.remainingFraction(70000,60000)==1,"clamped fraction");
  System.out.println("PASS: CCW revolution, pause/park/settle, offscreen reset, bounded wobble, reduced motion and remaining ring.");
 }
}

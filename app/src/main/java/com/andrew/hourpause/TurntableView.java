package com.andrew.hourpause;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;
import java.util.Locale;

/** Layered material scene: rotating vinyl, stationary light/chassis, independently lifted arm. */
public final class TurntableView extends View {
 private static final int GOLD=0xffd7bd86,BLUE=0xffafc1cd;
 private static final float CX=277.5f,CY=304,RADIUS=233;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final RectF sheet=new RectF(0,0,600,600),discRect=new RectF(CX-243,CY-243,CX+243,CY+243),ringRect=new RectF(CX-RADIUS,CY-RADIUS,CX+RADIUS,CY+RADIUS);
 private final Path recordClip=new Path(),bearingClip=new Path();
 private final TurntableMotion motion=new TurntableMotion();
 private final Bitmap chassis,disc,arm;
 private final ColorFilter shadow=new PorterDuffColorFilter(Color.BLACK,PorterDuff.Mode.SRC_IN);
 private final Shader lightA=new RadialGradient(0,0,1,new int[]{0x65d2a66c,0x20ae8953,0x00ae8953},new float[]{0,.48f,1},Shader.TileMode.CLAMP);
 private final Shader lightB=new RadialGradient(0,0,1,new int[]{0x55d1a568,0x168f7045,0x008f7045},new float[]{0,.45f,1},Shader.TileMode.CLAMP);
 private final Shader vignette=new RadialGradient(300,300,410,new int[]{0x0009120d,0x0009120d,0xff09120d},new float[]{0,.70f,1},Shader.TileMode.CLAMP);
 private Drawable statusIcon;private String label="Ready",clock="00:00";
 private long remaining=1,segment=1;private boolean audible,resumed,reduced,immersive,quiet;
 private int currentIcon=-1;private float unitScale=1;
 public TurntableView(Context context){super(context);setFocusable(true);setContentDescription("Ready. Choose music");
  BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
  chassis=BitmapFactory.decodeResource(getResources(),R.drawable.turntable_chassis,options);
  disc=BitmapFactory.decodeResource(getResources(),R.drawable.turntable_disc,options);
  arm=BitmapFactory.decodeResource(getResources(),R.drawable.turntable_arm,options);
  recordClip.addCircle(CX,CY,RADIUS-1,Path.Direction.CW);bearingClip.addCircle(518,81,39,Path.Direction.CW);
 }
 public void setState(String text,long left,long total,boolean playing,boolean rest,int icon){
  label=text;remaining=left;segment=total;audible=playing;quiet=rest;
  long seconds=Math.max(0,(left+999)/1000);clock=seconds>=3600?String.format(Locale.getDefault(),"%d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60):String.format(Locale.getDefault(),"%02d:%02d",seconds/60,seconds%60);
  if(icon!=currentIcon){statusIcon=getResources().getDrawable(icon,getContext().getTheme()).mutate();currentIcon=icon;}
  setContentDescription(clock+", "+text+(immersive?". Show controls":". Choose music"));invalidate();
 }
 public void setPresentation(boolean visible,boolean reduce,boolean full){if(resumed!=visible){resumed=visible;motion.freezeClock();}reduced=reduce;immersive=full;invalidate();}
 @Override protected void onDetachedFromWindow(){motion.freezeClock();super.onDetachedFromWindow();}
 @Override public void onVisibilityAggregated(boolean visible){super.onVisibilityAggregated(visible);motion.freezeClock();if(visible)invalidate();}
 private void image(Canvas c,Bitmap bitmap,RectF bounds){paint.setShader(null);paint.setStyle(Paint.Style.FILL);c.drawBitmap(bitmap,null,bounds,paint);}
 private void glow(Canvas c,Shader shader,float x,float y,float rx,float ry,float rotation){c.save();c.translate(x,y);c.rotate(rotation);c.scale(rx,ry);paint.setShader(shader);paint.setStyle(Paint.Style.FILL);paint.setColor(-1);c.drawCircle(0,0,1,paint);paint.setShader(null);c.restore();}
 private void arm(Canvas c){c.save();c.translate(518,81);c.rotate(-10);c.scale(.78f,.78f);c.translate(-538,-98);image(c,arm,sheet);c.restore();}
 @Override protected void onDraw(Canvas c){super.onDraw(c);if(getWidth()==0||getHeight()==0)return;
  boolean visible=resumed&&isShown()&&getWindowVisibility()==VISIBLE;
  if(visible)motion.step(SystemClock.elapsedRealtime(),audible,reduced);else motion.freezeClock();
  float size=Math.min(getWidth()*(immersive?1.04f:1f),getHeight()*1.06f);unitScale=size/600f;
  c.save();c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(unitScale,unitScale);
  c.save();c.rotate(-10,300,300);c.scale(1,.94f,300,300);
  image(c,chassis,sheet);
  c.save();c.translate(0,(float)motion.wobbleY());c.scale(1,(float)motion.wobbleScale(),CX,CY);c.rotate((float)motion.angle(),CX,CY);image(c,disc,discRect);c.restore();
  // Grazing light stays in world space; it never spins with the texture.
  c.save();c.clipPath(recordClip);glow(c,lightA,128,170,135,49,43);glow(c,lightB,400,461,105,36,43);c.restore();
  int accent=quiet?BLUE:GOLD;
  paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setStrokeCap(Paint.Cap.BUTT);paint.setColor(accent);paint.setAlpha(215);
  c.drawArc(ringRect,-90,(float)(360*TurntableMotion.remainingFraction(remaining,segment)),false,paint);paint.setAlpha(255);
  float lift=(float)Math.max(0,Math.min(1,-motion.arm()/7));
  c.save();c.translate(1+lift,2+lift*2);paint.setColorFilter(shadow);paint.setAlpha(90);arm(c);paint.setColorFilter(null);paint.setAlpha(255);c.restore();
  c.save();c.scale(1,1-lift*.005f,518,81);arm(c);c.restore();
  // Repaint the bearing at its fixed attachment point, independently of the arm lift.
  c.save();c.clipPath(bearingClip);arm(c);c.restore();
  paint.setStyle(Paint.Style.FILL);paint.setColor(-1);paint.setShader(vignette);c.drawRect(sheet,paint);paint.setShader(null);c.restore();
  // Readable timer/icon overlay never inherits record rotation, wobble or scene tilt.
  paint.setStyle(Paint.Style.FILL);paint.setShader(null);paint.setColor(accent);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
  float textSize=clock.length()>5?54:76,minText=28*getResources().getDisplayMetrics().scaledDensity/Math.max(.1f,unitScale);paint.setTextSize(Math.min(100,Math.max(textSize,minText)));
  float maxWidth=168;if(paint.measureText(clock)>maxWidth)paint.setTextSize(paint.getTextSize()*maxWidth/paint.measureText(clock));
  c.drawText(clock,CX,CY-(paint.ascent()+paint.descent())/2,paint);
  if(statusIcon!=null){statusIcon.setTint(accent);statusIcon.setBounds(266,347,290,371);statusIcon.draw(c);}
  if(!immersive){paint.setTextSize(Math.max(16,10*getResources().getDisplayMetrics().scaledDensity/Math.max(.1f,unitScale)));paint.setTypeface(Typeface.create("sans-serif",0));paint.setColor(0xffa7ac98);c.drawText(label,CX,397,paint);}
  c.restore();if(visible&&motion.needsFrame(audible,reduced))postInvalidateOnAnimation();
 }
}

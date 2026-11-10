package com.andrew.hourpause;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;
import java.util.Locale;
import java.util.Random;

/** Native layered C-direction scene. Only the disc receives rotation and micro-wobble. */
public final class TurntableView extends View {
 private static final int GOLD=0xffd7bd86,BLUE=0xffafc1cd;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final RectF recordRect=new RectF(30,62,526,558),labelRect=new RectF(178,210,378,410);
 private final RectF glowRect=new RectF(),baseRect=new RectF(-40,25,630,600);private final Path armPath=new Path(),recordClip=new Path();
 private final Shader chassis=new RadialGradient(290,313,340,new int[]{0xff142015,0x700f190f,0x0009120d},new float[]{0,.65f,1},Shader.TileMode.CLAMP);
 private final TurntableMotion motion=new TurntableMotion();
 private final Shader paper=new RadialGradient(252,282,125,new int[]{0xff353620,0xff202617},null,Shader.TileMode.CLAMP);
 private final Shader metal=new LinearGradient(538,0,558,0,new int[]{0xff252419,0xff8b7853,0xffcbbb94,0xff6d5b3c,0xff28271b},null,Shader.TileMode.CLAMP);
 private final Shader pivot=new RadialGradient(540,63,36,new int[]{0xffb29e76,0xff6d5c3d,0xff242319},null,Shader.TileMode.CLAMP);
 private final Shader head=new LinearGradient(-15,-5,15,33,new int[]{0xffb09a70,0xff6f5d3f,0xff353124},null,Shader.TileMode.CLAMP);
 private final Shader lightA=new RadialGradient(0,0,1,new int[]{0x50c6a777,0x0fb49460,0x00ac8658},new float[]{0,.45f,1},Shader.TileMode.CLAMP);
 private final Shader lightB=new RadialGradient(0,0,1,new int[]{0x49dab786,0x108e7045,0x008e7045},new float[]{0,.45f,1},Shader.TileMode.CLAMP);
 private final Shader edge=new LinearGradient(0,0,600,0,new int[]{0x0074633e,0x6077633e,0x22303320,0x0009120d},new float[]{0,.32f,.65f,1},Shader.TileMode.CLAMP);
 private Bitmap texture;private Drawable statusIcon;private String label="Ready",clock="00:00";
 private long remaining=1,segment=1;private boolean audible,resumed,reduced,immersive,quiet;
 private int currentIcon=-1;private float unitScale=1;
 public TurntableView(Context context){super(context);setFocusable(true);setContentDescription("Ready. Choose music");setLayerType(View.LAYER_TYPE_HARDWARE,null);
  recordClip.addCircle(278,310,240,Path.Direction.CW);armPath.moveTo(548,95);armPath.lineTo(548,300);armPath.cubicTo(546,356,507,405,464,458);
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
 private void fill(Canvas c,int color){paint.setShader(null);paint.setColor(color);paint.setStyle(Paint.Style.FILL);}
 private void circle(Canvas c,float x,float y,float r,int color){fill(c,color);c.drawCircle(x,y,r,paint);}
 private void line(Canvas c,float x1,float y1,float x2,float y2,int color,float width){paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(color);c.drawLine(x1,y1,x2,y2,paint);}
 private void texture(){if(texture!=null)return;texture=Bitmap.createBitmap(600,600,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(texture);
  circle(c,278,310,241,0xff080e09);
  paint.setStyle(Paint.Style.STROKE);Random rng=new Random(17);
  for(int i=0;i<172;i++){paint.setColor(i%9==0?0x189e9369:0x0d777b52);paint.setStrokeWidth(i%9==0?.7f:.4f);c.drawCircle(278,310,102+i*.8f,paint);}
  for(int i=0;i<50;i++){double a=i*2.39996;float r=115+rng.nextInt(115),x=278+(float)Math.cos(a)*r,y=310+(float)Math.sin(a)*r;line(c,x,y,x+(float)Math.sin(a)*2,y-(float)Math.cos(a)*2,0x1ac3b88e,.5f);}
  paint.setStyle(Paint.Style.FILL);paint.setColor(-1);paint.setShader(paper);c.drawCircle(278,310,100,paint);paint.setShader(null);
  c.save();Path clip=new Path();clip.addCircle(278,310,98,Path.Direction.CW);c.clipPath(clip);
  for(int i=0;i<1400;i++){paint.setColor(i%2==0?0x097e7953:0x0a080d08);float x=178+rng.nextFloat()*200,y=210+rng.nextFloat()*200;c.drawCircle(x,y,.25f+rng.nextFloat()*.5f,paint);}c.restore();
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.8f);paint.setColor(0x4a9b9166);glowRect.set(255,237,301,249);c.drawArc(glowRect,205,130,false,paint);line(c,273,248,283,248,0x399b9166,.7f);
 }
 private void glow(Canvas c,Shader shader,float x,float y,float rx,float ry,float rotation){c.save();c.translate(x,y);c.rotate(rotation);c.scale(rx,ry);paint.setShader(shader);paint.setStyle(Paint.Style.FILL);paint.setColor(-1);c.drawCircle(0,0,1,paint);paint.setShader(null);c.restore();}
 @Override protected void onDraw(Canvas c){super.onDraw(c);if(getWidth()==0||getHeight()==0)return;texture();
  boolean visible=resumed&&isShown()&&getWindowVisibility()==VISIBLE;
  if(visible)motion.step(SystemClock.elapsedRealtime(),audible,reduced);else motion.freezeClock();
  float size=Math.min(getWidth()*(immersive?1.04f:1f),getHeight()*1.06f);unitScale=size/600f;
  float ox=(getWidth()-size)/2,oy=(getHeight()-size)/2;c.save();c.translate(ox,oy);c.scale(unitScale,unitScale);
  c.save();c.rotate(-10,300,300);c.scale(1,.94f,300,300);
  paint.setColor(-1);paint.setStyle(Paint.Style.FILL);paint.setShader(chassis);c.drawOval(baseRect,paint);paint.setShader(edge);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);c.drawLine(-20,70,600,70,paint);c.drawLine(0,584,570,584,paint);paint.setShader(null);
  circle(c,278,313,248,0xff030805);circle(c,278,310,244,0xff191e13);
  c.save();c.translate(0,(float)motion.wobbleY());c.scale(1,(float)motion.wobbleScale(),278,310);c.rotate((float)motion.angle(),278,310);paint.setStyle(Paint.Style.FILL);paint.setColor(-1);c.drawBitmap(texture,0,0,paint);c.restore();
  c.save();c.clipPath(recordClip);glow(c,lightA,100,180,155,76,45);glow(c,lightB,414,475,112,59,42);c.restore();
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setColor(0x31554c2d);c.drawOval(recordRect,paint);
  int accent=quiet?BLUE:GOLD;paint.setColor(accent);paint.setAlpha(210);paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.BUTT);c.drawArc(recordRect,-90,(float)(360*TurntableMotion.remainingFraction(remaining,segment)),false,paint);paint.setAlpha(255);
  circle(c,548,72,41,0xff080e09);paint.setStyle(Paint.Style.FILL);paint.setColor(-1);paint.setShader(pivot);c.drawCircle(548,72,28,paint);paint.setShader(null);
  c.save();c.rotate((float)motion.arm(),548,72);
  line(c,548,45,548,19,0xff28271b,19);paint.setColor(-1);paint.setShader(metal);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(15);c.drawLine(548,45,548,19,paint);paint.setShader(null);
  c.save();c.translate(3,6);paint.setColor(0x80000000);paint.setStrokeWidth(14);c.drawPath(armPath,paint);c.restore();
  paint.setColor(-1);paint.setShader(metal);paint.setStrokeWidth(9);c.drawPath(armPath,paint);paint.setShader(null);paint.setColor(0x55d7c69d);paint.setStrokeWidth(1.1f);c.drawPath(armPath,paint);
  paint.setStyle(Paint.Style.FILL);paint.setColor(-1);paint.setShader(metal);c.drawRect(539,101,557,120,paint);paint.setShader(null);
  c.save();c.translate(464,453);c.rotate(39);fill(c,0xff12150e);c.drawRect(-14,-3,14,41,paint);paint.setColor(-1);paint.setShader(head);c.drawRect(-15,-5,15,33,paint);paint.setShader(null);circle(c,-6,5,2.2f,0xff393425);circle(c,6,5,2.2f,0xff393425);line(c,0,36,0,48,0xffa69b78,1.1f);c.restore();c.restore();c.restore();
  // Fixed overlay: independent of the rotated/wobbling physical plane.
  paint.setStyle(Paint.Style.FILL);paint.setShader(null);paint.setColor(accent);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
  float textSize=clock.length()>5?54:76;float minText=28*getResources().getDisplayMetrics().scaledDensity/Math.max(.1f,unitScale);paint.setTextSize(Math.min(100,Math.max(textSize,minText)));
  float maxWidth=immersive?260:290;if(paint.measureText(clock)>maxWidth)paint.setTextSize(paint.getTextSize()*maxWidth/paint.measureText(clock));
  c.drawText(clock,280,312-(paint.ascent()+paint.descent())/2,paint);
  if(statusIcon!=null){statusIcon.setTint(accent);statusIcon.setBounds(268,355,292,379);statusIcon.draw(c);}
  if(!immersive){paint.setTextSize(Math.max(16,10*getResources().getDisplayMetrics().scaledDensity/Math.max(.1f,unitScale)));paint.setTypeface(Typeface.create("sans-serif",0));paint.setColor(0xffa7ac98);c.drawText(label,280,405,paint);}
  c.restore();
  if(visible&&motion.needsFrame(audible,reduced))postInvalidateOnAnimation();
 }
}

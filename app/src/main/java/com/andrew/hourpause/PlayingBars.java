package com.andrew.hourpause;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

final class PlayingBars extends View {
 private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
 private final boolean animated;

 PlayingBars(Context context, boolean animated) {
  super(context);
  this.animated = animated;
  paint.setColor(0xffdfd3b3);
  setContentDescription(animated ? "Playing" : "Current track");
 }

 @Override protected void onDraw(Canvas canvas) {
  super.onDraw(canvas);
  float unit = getWidth() / 8f;
  double phase = SystemClock.uptimeMillis() / 130.0;
  for (int bar = 0; bar < 3; bar++) {
   float height = animated ? (float)(0.35 + 0.3 * (1 + Math.sin(phase + bar * 1.8))) : 0.35f + bar * 0.2f;
   float x = (1 + 2 * bar) * unit;
   float bottom = getHeight() * 0.77f;
   canvas.drawRoundRect(x, bottom - getHeight() * 0.55f * height, x + unit, bottom, unit / 2, unit / 2, paint);
  }
  if (animated && isShown()) postInvalidateDelayed(40);
 }
}

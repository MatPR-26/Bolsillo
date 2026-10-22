package cl.bolsillo.app;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Broad bank pane: one glass body, a satin reflection and a fine light-facing rim. */
final class AccountGlass extends Drawable {
 GlassMaterial body;
 final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 final Path lens=new Path(),clip=new Path();final RectF bounds=new RectF(),rim=new RectF();
 final float radius,unit;final boolean dark;int tint,accent;final int intensity,corner;final Context context;
 Shader wash,sheen,edge;int opacity=255;
 AccountGlass(Context c,int tint){this(c,tint,28);}
 AccountGlass(Context c,int tint,int corner){context=c;this.tint=tint;accent=tint;this.corner=corner;dark=Glass.dark(c);intensity=Glass.intensity(c);unit=c.getResources().getDisplayMetrics().density;radius=corner*unit;body=new GlassMaterial(c,230,corner,tint);}
 void palette(int first,int second){tint=Glass.mix(first,Glass.secondary(context),.08f);accent=Glass.mix(second,Glass.primary(context),.08f);body=new GlassMaterial(context,230,corner,tint);body.setAlpha(opacity);onBoundsChange(getBounds());invalidateSelf();}
 @Override protected void onBoundsChange(Rect b){
  body.setBounds(b);float w=Math.max(1,b.width()),h=Math.max(1,b.height());bounds.set(0,0,w,h);rim.set(bounds);rim.inset(.65f*unit,.65f*unit);
  wash=new LinearGradient(0,h*.1f,w,h,new int[]{Glass.alpha(tint,dark?40:24),Glass.alpha(tint,2),Glass.alpha(accent,dark?32:19)},new float[]{0,.54f,1},Shader.TileMode.CLAMP);
  sheen=new LinearGradient(0,0,w*.86f,h,new int[]{Glass.alpha(Color.WHITE,dark?30:62),Glass.alpha(Color.WHITE,dark?9:18),0x00ffffff},new float[]{0,.42f,1},Shader.TileMode.CLAMP);
  edge=new LinearGradient(0,0,w,h,new int[]{Glass.alpha(Color.WHITE,dark?102:195),Glass.alpha(Color.WHITE,dark?19:78),Glass.alpha(tint,dark?48:66)},new float[]{0,.55f,1},Shader.TileMode.CLAMP);
  lens.reset();lens.moveTo(0,0);lens.lineTo(w,0);lens.lineTo(w,h*.09f);lens.cubicTo(w*.7f,h*.13f,w*.3f,h*.7f,0,h*.83f);lens.close();clip.reset();clip.addRoundRect(bounds,radius,radius,Path.Direction.CW);lens.op(clip,Path.Op.INTERSECT);
 }
 @Override public void draw(Canvas c){
  body.draw(c);if(opacity==0||intensity==0||bounds.isEmpty())return;
  int save=c.save();c.translate(getBounds().left,getBounds().top);
  paint.setStyle(Paint.Style.FILL);paint.setAlpha(opacity);paint.setShader(wash);c.drawRoundRect(bounds,radius,radius,paint);paint.setShader(sheen);c.drawPath(lens,paint);c.restoreToCount(save);
  save=c.save();c.translate(getBounds().left,getBounds().top);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.65f*unit);paint.setShader(edge);c.drawRoundRect(rim,radius-.65f*unit,radius-.65f*unit,paint);paint.setShader(null);paint.setStyle(Paint.Style.FILL);c.restoreToCount(save);
 }
 @Override public void getOutline(Outline o){body.getOutline(o);}
 @Override public void setAlpha(int a){opacity=Math.max(0,Math.min(255,a));body.setAlpha(opacity);invalidateSelf();}
 @Override public int getAlpha(){return opacity;}
 @Override public void setColorFilter(ColorFilter f){body.setColorFilter(f);paint.setColorFilter(f);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}

package cl.bolsillo.app;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** One optical surface: translucent body, directional reflection and one fine rim.
 * Geometry and shaders are prepared when bounds change, never during animation.
 */
final class GlassMaterial extends Drawable {
 // Dithering is canvas-anchored on Android: translating a pane must not shimmer.
 final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 final RectF rect=new RectF(),rim=new RectF();final Path lip=new Path();
 final float radius,unit;final boolean dark;
 final int tint,alpha,intensity,fillAlpha;
 Shader fill,light,edge,reflection,lipLight;int opacity=255;
 GlassMaterial(Context c,int alpha,int radius,int tint){
  this.alpha=alpha;unit=c.getResources().getDisplayMetrics().density;
  this.radius=Glass.dp(c,radius);this.tint=tint;dark=Glass.dark(c);intensity=Glass.intensity(c);
  // Dense modal sheets isolate their figures from text in the activity behind them.
  // Ordinary cards and hero panes keep their existing, more transmissive body.
  fillAlpha=alpha>=255?(dark?246:250):Math.min(250,Math.max(dark?154:186,alpha-(intensity==2?34:intensity==0?-10:19)));
 }
 @Override protected void onBoundsChange(Rect bounds){
  float w=Math.max(1,bounds.width()),h=Math.max(1,bounds.height());
  rect.set(0,0,w,h);rim.set(rect);rim.inset(.65f*unit,.65f*unit);
  int ground=dark?0xff151e30:Color.WHITE;
  int upper=Glass.mix(ground,tint,dark?.105f:.055f),lower=Glass.mix(ground,tint,dark?.025f:.018f);
  fill=new LinearGradient(0,0,w*.85f,h,new int[]{Glass.alpha(upper,fillAlpha),Glass.alpha(lower,Math.min(252,fillAlpha+14))},null,Shader.TileMode.CLAMP);
  float strength=intensity==2?1.08f:.76f;
  light=new LinearGradient(0,0,w*.65f,h,new int[]{Glass.alpha(Color.WHITE,(int)((dark?27:77)*strength)),0x01ffffff,0x00ffffff},new float[]{0,.48f,1},Shader.TileMode.CLAMP);
  reflection=new RadialGradient(w*.08f,-h*.16f,Math.max(w*.88f,h*.62f),new int[]{Glass.alpha(Color.WHITE,(int)((dark?29:65)*strength)),0x00ffffff},null,Shader.TileMode.CLAMP);
  edge=new LinearGradient(0,0,w,h,new int[]{Glass.alpha(Color.WHITE,dark?76:185),Glass.alpha(tint,dark?42:64),Glass.alpha(Color.WHITE,dark?13:72),Glass.alpha(tint,dark?57:85)},new float[]{0,.29f,.68f,1},Shader.TileMode.CLAMP);
  float corner=Math.max(unit,Math.min(radius,Math.min(w,h)*.5f)-.65f*unit);
  lip.reset();lip.moveTo(rim.left,rim.top+corner);
  lip.cubicTo(rim.left,rim.top+corner*.43f,rim.left+corner*.43f,rim.top,rim.left+corner,rim.top);
  lip.lineTo(Math.max(rim.left+corner,rim.left+w*.58f),rim.top);
  lipLight=new LinearGradient(0,0,w*.64f,0,new int[]{Glass.alpha(Color.WHITE,dark?80:170),0x00ffffff},null,Shader.TileMode.CLAMP);
 }
 @Override public void draw(Canvas canvas){
  if(opacity==0||rect.isEmpty())return;
  int saved=canvas.save();canvas.translate(getBounds().left,getBounds().top);
  paint.setAlpha(opacity);paint.setStyle(Paint.Style.FILL);paint.setShader(fill);
  canvas.drawRoundRect(rect,radius,radius,paint);
  if(intensity>0){paint.setShader(light);canvas.drawRoundRect(rect,radius,radius,paint);paint.setShader(reflection);canvas.drawRoundRect(rect,radius,radius,paint);}
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth((intensity==0?.6f:.8f)*unit);paint.setShader(edge);
  canvas.drawRoundRect(rim,Math.max(0,radius-.65f*unit),Math.max(0,radius-.65f*unit),paint);
  if(intensity>0){paint.setStrokeWidth(.55f*unit);paint.setStrokeCap(Paint.Cap.ROUND);paint.setShader(lipLight);canvas.drawPath(lip,paint);paint.setStrokeCap(Paint.Cap.BUTT);}
  paint.setShader(null);paint.setStyle(Paint.Style.FILL);canvas.restoreToCount(saved);
 }
 // Outline alpha is an opacity guarantee, not a request to fade the view.
 @Override public void getOutline(Outline outline){outline.setRoundRect(getBounds(),radius);outline.setAlpha(fillAlpha/255f*opacity/255f);}
 @Override public void setAlpha(int value){opacity=Math.max(0,Math.min(255,value));invalidateSelf();}
 @Override public int getAlpha(){return opacity;}
 @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}

package cl.bolsillo.app;
import android.content.Context;import android.graphics.*;import android.graphics.drawable.Drawable;
/** Ambient canvas, with a cached flowing glass scene in the accounts view. */
final class PrismaBackdrop extends Drawable {
 final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);final RectF box=new RectF();
 final boolean dark;final int primary,secondary,intensity;Shader atmosphere,ambientLeft,ambientRight;int opacity=255;
 final Path ribbon=new Path(),fold=new Path(),seam=new Path();Shader ribbonTint,foldTint,seamTint;boolean accounts;
 PrismaBackdrop(Context c){dark=Glass.dark(c);primary=Glass.primary(c);secondary=Glass.secondary(c);intensity=Glass.intensity(c);}
 void accounts(boolean visible){if(accounts!=visible){accounts=visible;invalidateSelf();}}
 @Override protected void onBoundsChange(Rect bounds){
  box.set(bounds);float x=bounds.left,y=bounds.top,w=Math.max(1,bounds.width()),h=Math.max(1,bounds.height());
  atmosphere=new LinearGradient(x,y,x+w*.18f,y+h,new int[]{dark?0xff0c1220:0xfff4f6fa,dark?0xff101a29:0xffeff4f8,dark?0xff0c1422:0xfff4f6fa},new float[]{0,.56f,1},Shader.TileMode.CLAMP);
  ambientLeft=new RadialGradient(x-w*.05f,y+h*.05f,Math.max(w,h*.57f),new int[]{Glass.alpha(secondary,dark?28:27),Glass.alpha(secondary,0)},null,Shader.TileMode.CLAMP);
  ambientRight=new RadialGradient(x+w*1.13f,y+h*.48f,Math.max(w*.86f,h*.48f),new int[]{Glass.alpha(primary,dark?23:20),Glass.alpha(primary,0)},null,Shader.TileMode.CLAMP);
  ribbon.reset();ribbon.moveTo(x-w*.3f,y-h*.08f);ribbon.cubicTo(x+w*.17f,y+h*.08f,x+w*1.03f,y+h*.03f,x+w*.83f,y+h*.29f);ribbon.cubicTo(x+w*.62f,y+h*.55f,x-w*.18f,y+h*.58f,x+w*.18f,y+h*.86f);ribbon.lineTo(x+w*.41f,y+h*1.13f);ribbon.cubicTo(x-w*.24f,y+h*.8f,x-w*.28f,y+h*.62f,x+w*.27f,y+h*.36f);ribbon.cubicTo(x+w*.74f,y+h*.12f,x+w*.18f,y+h*.15f,x-w*.3f,y+h*.18f);ribbon.close();
  fold.reset();fold.moveTo(x+w*1.14f,y+h*.44f);fold.cubicTo(x+w*.67f,y+h*.5f,x+w*.76f,y+h*.72f,x+w*.11f,y+h*1.03f);fold.lineTo(x+w*.6f,y+h*1.15f);fold.cubicTo(x+w*1.13f,y+h*.89f,x+w*.73f,y+h*.74f,x+w*1.17f,y+h*.65f);fold.close();
  seam.reset();seam.moveTo(x-w*.17f,y+h*.135f);seam.cubicTo(x+w*.76f,y+h*.012f,x+w*1.02f,y+h*.1f,x+w*.55f,y+h*.36f);seam.cubicTo(x-w*.04f,y+h*.64f,x+w*.02f,y+h*.76f,x+w*.29f,y+h*.99f);
  ribbonTint=new LinearGradient(x,y,x+w*.86f,y+h,new int[]{Glass.alpha(secondary,dark?67:57),Glass.alpha(primary,dark?49:42),Glass.alpha(secondary,8)},new float[]{0,.6f,1},Shader.TileMode.CLAMP);
  foldTint=new LinearGradient(x+w,y+h*.4f,x,y+h,new int[]{Glass.alpha(primary,0),Glass.alpha(primary,dark?36:35),Glass.alpha(secondary,0)},new float[]{0,.62f,1},Shader.TileMode.CLAMP);
  seamTint=new LinearGradient(x,y,x+w,y+h,new int[]{Glass.alpha(primary,dark?74:98),Glass.alpha(secondary,0),Glass.alpha(primary,dark?33:55)},new float[]{0,.62f,1},Shader.TileMode.CLAMP);
 }
 @Override public void draw(Canvas canvas){
  if(opacity==0)return;paint.setAlpha(opacity);paint.setShader(atmosphere);canvas.drawRect(box,paint);
  if(intensity>0){paint.setShader(ambientLeft);canvas.drawRect(box,paint);paint.setShader(ambientRight);canvas.drawRect(box,paint);if(accounts){paint.setShader(ribbonTint);canvas.drawPath(ribbon,paint);paint.setShader(foldTint);canvas.drawPath(fold,paint);paint.setShader(seamTint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);float unit=box.width()/420f;for(int i=3;i>=1;i--){paint.setAlpha(opacity/12);paint.setStrokeWidth(i*7*unit);canvas.drawPath(seam,paint);}paint.setAlpha(opacity);paint.setStrokeWidth((intensity==2?1.1f:.65f)*unit);canvas.drawPath(seam,paint);paint.setStyle(Paint.Style.FILL);paint.setStrokeCap(Paint.Cap.BUTT);}}paint.setShader(null);
 }
 @Override public void setAlpha(int alpha){opacity=Math.max(0,Math.min(255,alpha));invalidateSelf();}
 @Override public int getAlpha(){return opacity;}
 @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
 @Override public int getOpacity(){return opacity==255?PixelFormat.OPAQUE:PixelFormat.TRANSLUCENT;}
}

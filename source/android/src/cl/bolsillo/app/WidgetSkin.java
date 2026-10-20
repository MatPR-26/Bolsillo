package cl.bolsillo.app;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.*;
import org.json.JSONObject;

/** A launcher-safe tinted glass layer: no live wallpaper access or blur claims. */
final class WidgetSkin {
 final boolean dark;final int ink,subtle,accent,secondary,base,tint,intensity;
 WidgetSkin(Context c,JSONObject options){String mode=options.optString("appearance","system");dark=mode.equals("dark")||mode.equals("system")&&(c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;String palette=options.optString("palette","prisma");int a,s,t;if(palette.equals("ocean")){a=dark?0xff84d7fa:0xff08729c;s=dark?0xffa3baff:0xffb5d7fe;t=dark?0xff163758:0xffbddbf2;}else if(palette.equals("amber")){a=dark?0xffffd091:0xffa64c31;s=dark?0xffffa9b4:0xffffc69e;t=dark?0xff503a39:0xfff4d7d5;}else{a=dark?0xffa7f0dc:0xff356c72;s=dark?0xffbdadff:0xffd5c9fa;t=dark?0xff393052:0xffded4f1;}String purpose=options.optString("purpose");if(purpose.equals("expense")){a=dark?0xffffa6b2:0xffa63f58;t=dark?0xff47303f:0xfff1dce5;}else if(purpose.equals("income"))t=dark?0xff20423f:0xffd3eee7;accent=a;secondary=s;tint=t;ink=dark?0xfff3f6ff:0xff24304a;subtle=dark?0xffc0cce3:0xff53647e;base=dark?0xff141b2e:0xffedf3fc;intensity=Glass.intensity(c);}
 Bitmap surface(int width,int height,boolean button){float scale=Math.min(1f,Math.min(720f/Math.max(1,width),440f/Math.max(1,height)));int w=Math.max(32,Math.round(width*scale)),h=Math.max(24,Math.round(height*scale));Bitmap image=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);float radius=Math.min(h*(button?.3f:.17f),w*.065f),edge=Math.max(.75f,w/650f),blur=Math.max(1,w/360f),padding=button?edge*2:blur*2.5f+edge;RectF rect=new RectF(padding,padding,w-padding,h-padding);Path clip=new Path();clip.addRoundRect(rect,radius,radius,Path.Direction.CW);
  int surface=button?mix(base,accent,dark?.16f:.13f):base;
  // The ambient raster shadow and rim stay inside the bitmap, so launchers cannot crop them.
  if(!button){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(edge);paint.setColor(dark?0x29000000:0x30718aae);paint.setShadowLayer(blur,0,blur*.4f,dark?0x24000000:0x1c405572);canvas.drawRoundRect(rect,radius,radius,paint);paint.clearShadowLayer();paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);}canvas.save();canvas.clipPath(clip);
  paint.setShader(new LinearGradient(0,0,w,h,button?new int[]{mix(surface,secondary,.1f),surface,mix(surface,accent,.11f)}:new int[]{mix(surface,tint,.45f),surface,mix(surface,tint,.26f)},new float[]{0,.56f,1},Shader.TileMode.CLAMP));canvas.drawRect(0,0,w,h,paint);paint.setShader(null);
  if(!button&&intensity>0){
   // A restrained silk ribbon keeps the balance area clear and its rim continuous.
   Path ribbon=new Path();ribbon.moveTo(w*.5f,-h*.1f);ribbon.cubicTo(w*.92f,h*.05f,w*.56f,h*.55f,w*1.12f,h*.86f);ribbon.lineTo(w*1.12f,h*1.1f);ribbon.cubicTo(w*.49f,h*.69f,w*.80f,h*.23f,w*.32f,-h*.1f);ribbon.close();paint.setShader(new LinearGradient(w*.4f,0,w,h,new int[]{alpha(secondary,dark?40:35),alpha(accent,dark?24:22),alpha(secondary,0)},new float[]{0,.64f,1},Shader.TileMode.CLAMP));canvas.drawPath(ribbon,paint);paint.setShader(null);
   Path seam=new Path();seam.moveTo(w*.5f,-h*.1f);seam.cubicTo(w*.92f,h*.05f,w*.56f,h*.55f,w*1.12f,h*.86f);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(edge*1.2f);paint.setShader(new LinearGradient(w*.5f,0,w,h,new int[]{alpha(secondary,dark?90:75),alpha(accent,dark?20:25),0},new float[]{0,.65f,1},Shader.TileMode.CLAMP));canvas.drawPath(seam,paint);paint.setShader(null);paint.setStyle(Paint.Style.FILL);
  }
  paint.setShader(new LinearGradient(0,0,0,h,new int[]{dark?0x15ffffff:0x54ffffff,0x00ffffff,0x00000000},new float[]{0,.22f,1},Shader.TileMode.CLAMP));canvas.drawRect(0,0,w,h,paint);paint.setShader(null);canvas.restore();
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(edge);paint.setShader(new LinearGradient(rect.left,rect.top,rect.right,rect.bottom,new int[]{dark?0x66def0ff:0xc4ffffff,dark?0x177b8fba:0x248c9dc4,dark?0x3992b2d4:0x70ffffff},new float[]{0,.55f,1},Shader.TileMode.CLAMP));canvas.drawRoundRect(rect,radius,radius,paint);paint.setShader(null);paint.setStyle(Paint.Style.FILL);return image;
 }
 static int mix(int from,int to,float n){return Color.argb((int)(Color.alpha(from)*(1-n)+Color.alpha(to)*n),(int)(Color.red(from)*(1-n)+Color.red(to)*n),(int)(Color.green(from)*(1-n)+Color.green(to)*n),(int)(Color.blue(from)*(1-n)+Color.blue(to)*n));}
 static int alpha(int color,int alpha){return (alpha<<24)|(color&0x00ffffff);}
}

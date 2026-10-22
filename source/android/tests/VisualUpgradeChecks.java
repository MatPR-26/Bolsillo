package cl.bolsillo.app;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.*;
import android.widget.TextView;
import java.util.*;

/** Raster and type checks for the shared material, using no financial fixture or writes. */
final class VisualUpgradeChecks {
 static int checks;
 static void yes(boolean value,String label)throws Exception{checks++;if(!value)throw new Exception("Visual system: "+label);}
 static double luminance(int color){double[] channels={Color.red(color)/255d,Color.green(color)/255d,Color.blue(color)/255d};double result=0;double[] weights={.2126,.7152,.0722};for(int i=0;i<3;i++)result+=(channels[i]<=.04045?channels[i]/12.92:Math.pow((channels[i]+.055)/1.055,2.4))*weights[i];return result;}
 static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
 static int run(Instrumentation test,Context c)throws Exception{
  checks=0;Map<String,Object> saved=VisualChecks.preferences(NativeStore.settings(c));
  try{
   for(boolean dark:new boolean[]{true,false})for(int intensity=0;intensity<3;intensity++){
    NativeStore.settings(c).edit().putString("appearance",dark?"dark":"light").putString("palette","prisma").putInt("glassIntensity",intensity).commit();
    final int level=intensity;VisualChecks.main(test,()->{
     yes(Glass.ink(c)==(dark?0xfff2f5fa:0xff172238),"approved text token "+dark);
     yes(Glass.primary(c)==(dark?0xff9ce8d5:0xff126a59),"approved accent token "+dark);
     yes(Glass.semantic(c,Glass.MINT)==(dark?0xff77dfc0:0xff116b4f),"success stays semantic "+dark);
     yes(Glass.semantic(c,Glass.CORAL)==(dark?0xffff9aab:0xffb52b4e),"expense stays semantic "+dark);
     int width=360,height=220;Bitmap image=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888),repeat=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
     try{
      Canvas canvas=new Canvas(image);canvas.drawColor(Glass.canvas(c));GlassMaterial material=new GlassMaterial(c,224,24,Glass.secondary(c));material.setBounds(0,0,width,height);Shader fill=material.fill,edge=material.edge;
      material.draw(canvas);
      // These are final composited pixels under the reading area, including reflections.
      for(int y=55;y<=165;y+=55)for(int x=72;x<=288;x+=72){int background=image.getPixel(x,y);yes(contrast(Glass.ink(c),background)>=4.5,"body text contrast on composited material "+dark+"/"+level+" "+x+","+y);yes(contrast(Glass.subtle(c),background)>=4.5,"secondary text contrast on composited material "+dark+"/"+level+" "+x+","+y);}
      for(int i=0;i<12;i++){Canvas again=new Canvas(repeat);again.drawColor(Glass.canvas(c));material.draw(again);}
      yes(fill==material.fill&&edge==material.edge,"drawing cannot rebuild shader geometry "+dark+"/"+level);
      int[] first=new int[width*height],second=new int[first.length];image.getPixels(first,0,width,0,0,width,height);repeat.getPixels(second,0,width,0,0,width,height);yes(Arrays.equals(first,second),"idle redraw is deterministic "+dark+"/"+level);
      material.setAlpha(0);repeat.eraseColor(Color.TRANSPARENT);material.draw(new Canvas(repeat));boolean empty=true;repeat.getPixels(second,0,width,0,0,width,height);for(int pixel:second)if(pixel!=0){empty=false;break;}yes(empty,"fading removes body and optical rim "+dark+"/"+level);
     }finally{image.recycle();repeat.recycle();}
     // High-contrast text underneath a modal must not remain readable through its figures.
     Bitmap black=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888),white=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
     try{
      black.eraseColor(Color.BLACK);white.eraseColor(Color.WHITE);GlassMaterial modal=new GlassMaterial(c,255,28,Glass.secondary(c));modal.setBounds(0,0,width,height);modal.draw(new Canvas(black));modal.draw(new Canvas(white));int bleed=0;
      for(int y=55;y<=165;y+=11)for(int x=72;x<=288;x+=12){int a=black.getPixel(x,y),b=white.getPixel(x,y);bleed=Math.max(bleed,Math.max(Math.abs(Color.red(a)-Color.red(b)),Math.max(Math.abs(Color.green(a)-Color.green(b)),Math.abs(Color.blue(a)-Color.blue(b)))));}
      yes(bleed<=10,"modal reading area isolates underlying high-contrast text "+dark+"/"+level+" bleed="+bleed);
     }finally{black.recycle();white.recycle();}
    });
   }
   VisualChecks.main(test,()->{
    yes(Glass.font(c,500)==Glass.font(c,500),"font weights are cached");
    yes(Glass.font(c,400).getWeight()==400&&Glass.font(c,500).getWeight()==500&&Glass.font(c,600).getWeight()==600,"Inter exposes distinct native font weights");
    TextView amount=Glass.text(c,"$12.345.678",32,false);amount.setTypeface(Glass.font(c,500));yes(amount.getTypeface().getWeight()==500,"optical sizing cannot override an explicit monetary weight");yes(amount.getFontFeatureSettings().contains("tnum"),"amount uses aligned tabular numerals");yes(amount.getPaint().hasGlyph("ñ")&&amount.getPaint().hasGlyph("$"),"local font supports Spanish and money");
   });
  }finally{VisualChecks.restore(NativeStore.settings(c),saved);}
  return checks;
 }
}

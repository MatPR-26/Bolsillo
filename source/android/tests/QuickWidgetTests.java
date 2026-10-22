package cl.bolsillo.app;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.json.*;

/** Read-only finance fixtures. Runs only in the synthetic instrumentation APK. */
final class QuickWidgetTests {
 static int checks;
 static void yes(boolean condition,String why)throws Exception{checks++;if(!condition)throw new Exception(why);}
 static int run(Instrumentation test,Context c)throws Exception{
  checks=0;JSONObject financeBefore=LocalData.read(c);Map<String,?> settingsBefore=new HashMap<>(NativeStore.settings(c).getAll());Map<String,?> secureBefore=new HashMap<>(c.getSharedPreferences("secure",0).getAll());int first=90311,second=90312;
  try{
  LocalData.restore(c,VisualChecks.fixture());financeBefore=LocalData.read(c);
  NativeStore.settings(c).edit().putString("appearance","dark").putBoolean("detectionTutorialSeen",true).commit();
  WidgetPreferences.write(c,first,new JSONObject().put("display","account").put("account","bank-mach").put("palette","prisma").put("appearance","dark").put("hidden",true).put("layout","compact").put("actions",true));
  WidgetPreferences.write(c,second,new JSONObject().put("display","account").put("account","bank-bancoestado").put("palette","ocean").put("appearance","light").put("hidden",false).put("layout","full").put("actions",true));
   yes(WidgetPreferences.read(c,first).optBoolean("hidden")&&!WidgetPreferences.read(c,second).optBoolean("hidden"),"Widget privacy must be independent");
   yes(WidgetPreferences.read(c,first).optString("palette").equals("prisma")&&WidgetPreferences.read(c,second).optString("palette").equals("ocean"),"Widget palette must be independent");
   yes(!BolsilloWidget.activity(c,first,"expense").equals(BolsilloWidget.activity(c,second,"expense")),"Widgets must keep distinct PendingIntents");
   yes(!BolsilloWidget.activity(c,first,"expense").equals(BolsilloWidget.activity(c,first,"income")),"Widget actions must keep distinct PendingIntents");
   java.util.Map<String,Long> balances=Ledger.balances(financeBefore,Ledger.today());
   yes(BolsilloWidget.snapshot(c,WidgetPreferences.read(c,first)).value==balances.get("bank-mach"),"Widget must show selected MACH balance");
   yes(BolsilloWidget.snapshot(c,WidgetPreferences.read(c,second)).value==balances.get("bank-bancoestado"),"Widget must show selected BancoEstado balance");
   yes(BolsilloWidget.snapshot(c,new JSONObject().put("display","account").put("account","missing")).missing,"Missing widget account must not select another account");
   archivedAccounts(test,c,first);semanticMaterial(test,c);
   MainActivity a=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));test.waitForIdleSync();
   final Dialog[] dialog={null};final QuickEntryDialog.Entry[] entry={null};
   test.runOnMainSync(()->{entry[0]=new QuickEntryDialog.Entry(a,0,"bank-bancoestado",null);dialog[0]=entry[0].dialog;dialog[0].show();Glass.window(dialog[0],a);entry[0].amount.setText("999999999990");entry[0].note.setText("Prueba visual, sin guardar");});test.waitForIdleSync();
   yes(entry[0].accounts.optJSONObject(entry[0].account).optString("id").equals("bank-bancoestado"),"Widget-selected account must prefill the quick entry");
   yes(entry[0].amount.getText().toString().replaceAll("[^0-9]","").equals("999999999990"),"Large CLP amount must not change value while formatted");
   final boolean[] nativeAmount={false};test.runOnMainSync(()->nativeAmount[0]=(entry[0].amount.getInputType()&android.text.InputType.TYPE_MASK_CLASS)==android.text.InputType.TYPE_CLASS_NUMBER&&entry[0].amount.getTypeface().equals(Glass.font(entry[0].c,400))&&entry[0].amountRule.getHeight()>0&&entry[0].amountRule.getImportantForAccessibility()==View.IMPORTANT_FOR_ACCESSIBILITY_NO);yes(nativeAmount[0],"Amount must retain the native numeric keyboard, regular digits and a visible decorative focus rule");
   Bundle draft=QuickEntryDialog.draft(dialog[0]);yes(draft!=null&&draft.getString("note").equals("Prueba visual, sin guardar"),"Quick form must retain its draft");
   shot(test,c,"quick-dark-large");
   test.runOnMainSync(()->{dialog[0].dismiss();entry[0]=new QuickEntryDialog.Entry(a,1,"bank-mach",draft);dialog[0]=entry[0].dialog;dialog[0].show();Glass.window(dialog[0],a);});test.waitForIdleSync();
   yes(entry[0].type==0&&entry[0].accounts.optJSONObject(entry[0].account).optString("id").equals("bank-bancoestado"),"Draft must retain movement type and account after recreation");
   final boolean[] reachable={false};test.runOnMainSync(()->{dialog[0].getWindow().setLayout(Glass.dp(a,360),Glass.dp(a,350));});test.waitForIdleSync();test.runOnMainSync(()->{int[] footer=new int[2],surface=new int[2];entry[0].save.getLocationOnScreen(footer);entry[0].root.getLocationOnScreen(surface);reachable[0]=footer[1]+entry[0].save.getHeight()<=surface[1]+entry[0].root.getHeight();});yes(reachable[0],"Quick save action must remain inside a constrained viewport");shot(test,c,"quick-small-viewport");
   test.runOnMainSync(()->dialog[0].dismiss());
   NativeStore.settings(c).edit().putString("appearance","light").commit();
   test.runOnMainSync(()->{entry[0]=new QuickEntryDialog.Entry(a,1,"bank-mach",null);dialog[0]=entry[0].dialog;dialog[0].show();Glass.window(dialog[0],a);entry[0].amount.setText("12600");});test.waitForIdleSync();shot(test,c,"quick-light-income");test.runOnMainSync(()->dialog[0].dismiss());
   for(int id:new int[]{first,second}){
    final View[] view={null};Bundle size=new Bundle();size.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,300);size.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,id==first?150:220);
    test.runOnMainSync(()->{Dialog preview=new Dialog(a);preview.requestWindowFeature(Window.FEATURE_NO_TITLE);FrameLayout frame=new FrameLayout(a);view[0]=BolsilloWidget.views(a,id,size).apply(a,frame);frame.addView(view[0],new FrameLayout.LayoutParams(Glass.dp(a,300),Glass.dp(a,id==first?150:220)));preview.setContentView(frame);preview.show();preview.getWindow().setBackgroundDrawableResource(android.R.color.transparent);preview.getWindow().setLayout(Glass.dp(a,300),Glass.dp(a,id==first?150:220));dialog[0]=preview;});test.waitForIdleSync();
    String visible=((TextView)view[0].findViewById(R.id.widget_balance)).getText().toString();yes(id==first?visible.equals("••••••"):visible.equals(BolsilloWidget.money(balances.get("bank-bancoestado"))),"Widget RemoteViews must respect privacy and selected balance");yes(view[0].findViewById(R.id.widget_actions).getVisibility()==View.VISIBLE,"Widget quick actions must be visible");shot(test,c,id==first?"widget-dark-private-compact":"widget-light-ocean-full");test.runOnMainSync(()->dialog[0].dismiss());
   }
   yes(LocalData.read(c).toString().equals(financeBefore.toString()),"Quick form and widget previews must not create real movements");test.runOnMainSync(a::finish);
  }finally{try{restore(c.getSharedPreferences("secure",0),secureBefore);}finally{restore(NativeStore.settings(c),settingsBefore);}}
  return checks;
 }
 /** Totals follow Home; an explicit active account may be excluded from those totals. */
 static void archivedAccounts(Instrumentation test,Context c,int widget)throws Exception{
  Map<String,?> secure=new HashMap<>(c.getSharedPreferences("secure",0).getAll()),settings=new HashMap<>(NativeStore.settings(c).getAll());
  try{
   JSONObject fixture=Ledger.initial(),state=NativeStore.state(c);JSONArray accounts=fixture.getJSONArray("accounts");
   Ledger.find(accounts,"bank-bancoestado").put("opening",12000);Ledger.find(accounts,"bank-mach").put("opening",34000).put("archived",true);Ledger.find(accounts,"mach-savings").put("opening",56000).put("archived",true);Ledger.find(accounts,"cash").put("opening",78000);
   state.getJSONObject("cache").put("data",fixture);state.put("queue",new JSONArray());NativeStore.write(c,state);
   yes(BolsilloWidget.snapshot(c,new JSONObject().put("display","total")).value==90000,"Archived bank and savings must be excluded from widget total exactly as on Home");
   yes(BolsilloWidget.snapshot(c,new JSONObject().put("display","available")).value==90000,"Archived usable account must not inflate available funds");
   yes(BolsilloWidget.snapshot(c,new JSONObject().put("display","savings")).value==0,"Archived savings must not inflate widget savings");
   BolsilloWidget.Snapshot archived=BolsilloWidget.snapshot(c,new JSONObject().put("display","account").put("account","bank-mach"));yes(archived.missing&&archived.value==0,"Explicit archived account must report unavailable without another account's balance");
   final View[] view={null};Bundle size=new Bundle();size.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,300);size.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,220);test.runOnMainSync(()->view[0]=BolsilloWidget.views(c,widget,size).apply(c,new FrameLayout(c)));
   yes(((TextView)view[0].findViewById(R.id.widget_balance)).getText().toString().equals("—")&&view[0].findViewById(R.id.widget_actions).getVisibility()==View.GONE,"Unavailable widget must show no stale balance and no actions for a different account");
   Ledger.find(accounts,"bank-mach").put("archived",false).put("included",false);NativeStore.write(c,state);
   BolsilloWidget.Snapshot explicit=BolsilloWidget.snapshot(c,new JSONObject().put("display","account").put("account","bank-mach"));yes(!explicit.missing&&explicit.value==34000,"Active account excluded from wealth remains available as an explicit widget account");
   yes(BolsilloWidget.snapshot(c,new JSONObject().put("display","total")).value==90000,"Account excluded from wealth must stay excluded from total after unarchiving");
  }finally{try{restore(c.getSharedPreferences("secure",0),secure);}finally{restore(NativeStore.settings(c),settings);}}
 }
 static void semanticMaterial(Instrumentation test,Context c)throws Exception{
  Map<String,?> settings=new HashMap<>(NativeStore.settings(c).getAll());
  try{
   NativeStore.settings(c).edit().putString("appearance","dark").commit();yes(Glass.semantic(c,Glass.MINT)==Glass.MINT&&Glass.semantic(c,Glass.CORAL)==Glass.CORAL,"Dark semantic colours must retain their original tones");
   NativeStore.settings(c).edit().putString("appearance","light").commit();final int[] iconColours=new int[3];test.runOnMainSync(()->{iconColours[0]=((Icons.Line)Icons.view(c,"income",Glass.MINT,17).getDrawable()).color();iconColours[1]=((Icons.Line)Icons.view(c,"expense",Glass.CORAL,17).getDrawable()).color();iconColours[2]=Modern.text(c,"Ingreso",12,Glass.MINT,true).getCurrentTextColor();});
   yes(iconColours[0]==0xff116b4f&&iconColours[1]==0xffb52b4e&&iconColours[2]==iconColours[0],"Light semantic icons and text must share the contrasting foreground tone");
   final boolean[] untouched={false};test.runOnMainSync(()->untouched[0]=!(Icons.badge(c,"mach",32).getDrawable() instanceof Icons.Line)&&Glass.semantic(c,0xffff526b)==0xffff526b);yes(untouched[0],"Bank logo assets and brand colours must remain unchanged");
   for(String mode:new String[]{"dark","light"}){
    NativeStore.settings(c).edit().putString("appearance",mode).commit();Bitmap origin=Bitmap.createBitmap(160,120,Bitmap.Config.ARGB_8888),shifted=Bitmap.createBitmap(220,190,Bitmap.Config.ARGB_8888);
    try{
     GlassMaterial material=new GlassMaterial(c,230,12,Glass.VIOLET);material.setBounds(0,0,160,120);material.draw(new Canvas(origin));material.setBounds(41,37,201,157);material.draw(new Canvas(shifted));
     int delta=0,maximumX=0,maximumY=0,maximumLeft=0,maximumRight=0,premultiplied=0,alphaDelta=0,opaqueDelta=0,opaqueSamples=0;
     for(int y=0;y<120;y++)for(int x=0;x<160;x++){
      int left=origin.getPixel(x,y),right=shifted.getPixel(x+41,y+37),leftAlpha=left>>>24,rightAlpha=right>>>24;
      alphaDelta=Math.max(alphaDelta,Math.abs(leftAlpha-rightAlpha));boolean opaque=leftAlpha>230&&rightAlpha>230;if(opaque)opaqueSamples++;
      for(int bit=0;bit<=24;bit+=8){int channel=Math.abs((left>>>bit&255)-(right>>>bit&255));if(channel>delta){delta=channel;maximumX=x;maximumY=y;maximumLeft=left;maximumRight=right;}if(opaque)opaqueDelta=Math.max(opaqueDelta,channel);if(bit<24)premultiplied=Math.max(premultiplied,Math.abs(((left>>>bit&255)*leftAlpha+127)/255-((right>>>bit&255)*rightAlpha+127)/255));}
     }
     String detail=mode+" raw="+delta+" at("+maximumX+","+maximumY+") argb="+Integer.toHexString(maximumLeft)+"/"+Integer.toHexString(maximumRight)+" alpha="+(maximumLeft>>>24)+"/"+(maximumRight>>>24)+" alphaDelta="+alphaDelta+" premultiplied="+premultiplied+" opaqueDelta="+opaqueDelta+" opaqueSamples="+opaqueSamples;
     Bundle diagnostic=new Bundle();diagnostic.putString("glassMaterialPixels",detail);test.sendStatus(0,diagnostic);
     materialPixelChecks(test,c,mode,false);materialPixelChecks(test,c,mode,true);materialAlpha(c,mode);
    }finally{origin.recycle();shifted.recycle();}
   }
  }finally{restore(NativeStore.settings(c),settings);}
 }
 /** Android raster AA varies at integer translations; compare premultiplied coverage
  * against an opaque Android roundRect drawn at the same positions in this run.
  * Alpha >230 is still translucent, not opaque. At composed anti-aliased rims,
  * Skia's coverage/compositing quantization can change one channel by 1/255.
  * Only those near-opaque BORDER pixels receive that allowance. Fully opaque,
  * same-position/cache and interior pixels retain exact equality requirements. */
 static void materialPixelChecks(Instrumentation test,Context c,String mode,boolean padded)throws Exception{
  int ax=padded?20:0,ay=padded?20:0,bx=padded?61:41,by=padded?57:37;
  int aw=padded?260:160,ah=padded?220:120,bw=padded?260:220,bh=padded?220:190;
  Bitmap a=Bitmap.createBitmap(aw,ah,Bitmap.Config.ARGB_8888),repeat=Bitmap.createBitmap(aw,ah,Bitmap.Config.ARGB_8888),b=Bitmap.createBitmap(bw,bh,Bitmap.Config.ARGB_8888),fresh=Bitmap.createBitmap(bw,bh,Bitmap.Config.ARGB_8888),nativeA=Bitmap.createBitmap(aw,ah,Bitmap.Config.ARGB_8888),nativeB=Bitmap.createBitmap(bw,bh,Bitmap.Config.ARGB_8888);
  try{
   GlassMaterial material=new GlassMaterial(c,230,12,Glass.VIOLET);material.setBounds(ax,ay,ax+160,ay+120);material.draw(new Canvas(a));material.draw(new Canvas(repeat));
   material.setBounds(bx,by,bx+160,by+120);material.draw(new Canvas(b));GlassMaterial other=new GlassMaterial(c,230,12,Glass.VIOLET);other.setBounds(bx,by,bx+160,by+120);other.draw(new Canvas(fresh));
   Paint primitive=new Paint(Paint.ANTI_ALIAS_FLAG);primitive.setColor(0xff668899);Canvas first=new Canvas(nativeA),moved=new Canvas(nativeB);first.translate(ax,ay);moved.translate(bx,by);first.drawRoundRect(new RectF(0,0,160,120),material.radius,material.radius,primitive);moved.drawRoundRect(new RectF(0,0,160,120),material.radius,material.radius,primitive);
   int inset=(int)Math.ceil(material.radius)+2;PixelDelta same=measurePixels(a,ax,ay,repeat,ax,ay,inset),cache=measurePixels(b,bx,by,fresh,bx,by,inset),baseline=measurePixels(nativeA,ax,ay,nativeB,bx,by,inset),actual=measurePixels(a,ax,ay,b,bx,by,inset);
   int allowed=Math.min(12,baseline.rgba+2);String detail=mode+(padded?" equal padded canvases":" canvas-edge origin")+" material="+actual+" native="+baseline+" allowedRGBA="+allowed;
   Bundle status=new Bundle();status.putString("glassMaterialCoverage",detail);test.sendStatus(0,status);
   yes(same.raw==0,"Repeated drawing at identical bounds must preserve every ARGB pixel: "+detail);
   yes(cache.raw==0,"Fresh and reused material must produce identical pixels at the same bounds: "+detail);
   yes(baseline.rgba<=12,"Native Android anti-alias quantization must stay within the absolute 12/255 coverage limit: "+detail);
   yes(actual.rgba<=allowed,"Translated material coverage must stay within measured native AA plus 2/255, never beyond 12/255: "+detail);
   yes(actual.nearOpaque<=1&&actual.nearOpaqueSamples>=20,"Near-opaque composed border pixels may vary by at most 1/255 under translation: "+detail);
   yes(actual.fullyOpaque==0,"Fully opaque pixels, when present, must remain exactly stable under translation: "+detail);
   yes(actual.interior==0&&actual.interiorSamples>=1000,"The material interior, including translucent gradients, must remain exactly stable: "+detail);
  }finally{a.recycle();repeat.recycle();b.recycle();fresh.recycle();nativeA.recycle();nativeB.recycle();}
 }
 static final class PixelDelta {
  int raw,alpha,rgb,rgba,nearOpaque,nearOpaqueSamples,nearOpaqueMismatches,fullyOpaque,fullyOpaqueSamples,interior,interiorSamples;
  String firstNearOpaqueMismatch="none";
  @Override public String toString(){return "raw="+raw+" alpha="+alpha+" premulRGB="+rgb+" RGBA="+rgba+" nearOpaqueBorder="+nearOpaque+"/"+nearOpaqueSamples+" nearOpaqueMismatches="+nearOpaqueMismatches+" firstNearOpaque="+firstNearOpaqueMismatch+" fullyOpaque="+fullyOpaque+"/"+fullyOpaqueSamples+" interior="+interior+"/"+interiorSamples;}
 }
 static PixelDelta measurePixels(Bitmap a,int ax,int ay,Bitmap b,int bx,int by,int inset){
  PixelDelta result=new PixelDelta();
  for(int y=0;y<120;y++)for(int x=0;x<160;x++){
   int left=a.getPixel(x+ax,y+ay),right=b.getPixel(x+bx,y+by),la=left>>>24,ra=right>>>24;boolean inside=x>=inset&&x<160-inset&&y>=inset&&y<120-inset,nearOpaqueBorder=la>230&&ra>230&&!inside,fullyOpaque=la==255&&ra==255;
   result.alpha=Math.max(result.alpha,Math.abs(la-ra));if(nearOpaqueBorder)result.nearOpaqueSamples++;if(fullyOpaque)result.fullyOpaqueSamples++;if(inside)result.interiorSamples++;int pixelDelta=0;
   for(int bit=0;bit<=24;bit+=8){int raw=Math.abs((left>>>bit&255)-(right>>>bit&255));pixelDelta=Math.max(pixelDelta,raw);result.raw=Math.max(result.raw,raw);if(nearOpaqueBorder)result.nearOpaque=Math.max(result.nearOpaque,raw);if(fullyOpaque)result.fullyOpaque=Math.max(result.fullyOpaque,raw);if(inside)result.interior=Math.max(result.interior,raw);if(bit<24)result.rgb=Math.max(result.rgb,Math.abs(((left>>>bit&255)*la+127)/255-((right>>>bit&255)*ra+127)/255));}
   if(nearOpaqueBorder&&pixelDelta>0){result.nearOpaqueMismatches++;if(result.firstNearOpaqueMismatch.equals("none"))result.firstNearOpaqueMismatch="("+x+","+y+") argb="+Integer.toHexString(left)+"/"+Integer.toHexString(right)+" alpha="+la+"/"+ra+" delta="+pixelDelta;}
  }
  result.rgba=Math.max(result.alpha,result.rgb);return result;
 }
 static void materialAlpha(Context c,String mode)throws Exception{
  GlassMaterial material=new GlassMaterial(c,230,12,Glass.VIOLET);drawableAlpha(material,material.radius,"GlassMaterial "+mode);
  LiquidArtwork artwork=new LiquidArtwork(c,Glass.VIOLET,12);drawableAlpha(artwork,artwork.radius,"LiquidArtwork "+mode);
 }
 /** Outline opacity is a compositor guarantee: verify it against rasterized interior
  * coverage, rather than duplicating each material's opacity calculation. */
 static void drawableAlpha(android.graphics.drawable.Drawable drawable,float radius,String label)throws Exception{
  Bitmap original=Bitmap.createBitmap(160,120,Bitmap.Config.ARGB_8888),zero=Bitmap.createBitmap(160,120,Bitmap.Config.ARGB_8888),partial=Bitmap.createBitmap(160,120,Bitmap.Config.ARGB_8888),restored=Bitmap.createBitmap(160,120,Bitmap.Config.ARGB_8888);
  try{
   drawable.setBounds(0,0,160,120);drawable.draw(new Canvas(original));Outline outline=new Outline();drawable.getOutline(outline);float fullOutline=outline.getAlpha();int inset=(int)Math.ceil(radius)+2;drawable.draw(new Canvas(restored));boolean repeated=true;for(int y=0;y<120;y++)for(int x=0;x<160;x++)repeated&=original.getPixel(x,y)==restored.getPixel(x,y);restored.eraseColor(Color.TRANSPARENT);yes(repeated,"Repeated full-opacity drawing must preserve every material pixel: "+label);
   yes(fullOutline>0&&fullOutline<1&&fullOutline<=interiorAlpha(original,inset)/255f,"Translucent outline must promise no more than the least actual interior opacity: "+label);
   drawable.setAlpha(0);drawable.draw(new Canvas(zero));drawable.getOutline(outline);yes(drawable.getAlpha()==0&&outline.getAlpha()==0,"Zero fade must have zero drawable and outline opacity: "+label);
   drawable.setAlpha(128);drawable.draw(new Canvas(partial));drawable.getOutline(outline);
   yes(drawable.getAlpha()==128&&outline.getAlpha()>0&&outline.getAlpha()<fullOutline&&outline.getAlpha()<=interiorAlpha(partial,inset)/255f,"Half fade outline must remain below actual raster opacity and the full outline: "+label);
   drawable.setAlpha(255);drawable.draw(new Canvas(restored));drawable.getOutline(outline);
   boolean invisible=true,monotonic=true,exact=true;int reduced=0,visible=0,mismatchCount=0;String firstMismatch="none";
   for(int y=0;y<120;y++)for(int x=0;x<160;x++){int full=original.getPixel(x,y),half=partial.getPixel(x,y);invisible&=zero.getPixel(x,y)==Color.TRANSPARENT;exact&=restored.getPixel(x,y)==full;if(restored.getPixel(x,y)!=full){mismatchCount++;if(firstMismatch.equals("none"))firstMismatch=x+","+y+" "+Integer.toHexString(full)+"/"+Integer.toHexString(restored.getPixel(x,y));}monotonic&=(half>>>24)<=(full>>>24);if((half>>>24)>0)visible++;if((half>>>24)<(full>>>24))reduced++;}
   yes(invisible,"Setting alpha0 must hide every pixel, including rims and reflections: "+label);
   yes(monotonic&&reduced>1000&&visible>1000,"Partial fade must reduce measured pixel coverage without raising alpha: "+label);
   yes(exact&&outline.getAlpha()==fullOutline,"Restoring alpha255 must recover every original ARGB pixel and outline: "+label+" repeated="+repeated+" mismatches="+mismatchCount+" first="+firstMismatch+" outline="+fullOutline+"/"+outline.getAlpha());
   drawable.setAlpha(-1);drawable.getOutline(outline);yes(outline.getAlpha()==0&&drawable.getAlpha()==0,"Negative fade must clamp to zero: "+label);
   drawable.setAlpha(300);drawable.getOutline(outline);yes(drawable.getAlpha()==255&&outline.getAlpha()==fullOutline&&outline.getAlpha()<1&&outline.getAlpha()<=interiorAlpha(original,inset)/255f,"Excess fade must restore the translucent raster guarantee: "+label);
  }finally{original.recycle();zero.recycle();partial.recycle();restored.recycle();}
 }
 static int interiorAlpha(Bitmap image,int inset)throws Exception{
  int minimum=255,samples=0;for(int y=inset;y<image.getHeight()-inset;y++)for(int x=inset;x<image.getWidth()-inset;x++){minimum=Math.min(minimum,image.getPixel(x,y)>>>24);samples++;}
  yes(samples>1000,"Alpha regression must inspect a substantial interior area");return minimum;
 }
 static void restore(SharedPreferences preferences,Map<String,?> snapshot)throws Exception{SharedPreferences.Editor editor=preferences.edit().clear();for(Map.Entry<String,?> entry:snapshot.entrySet()){String key=entry.getKey();Object value=entry.getValue();if(value instanceof String)editor.putString(key,(String)value);else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);else if(value instanceof Integer)editor.putInt(key,(Integer)value);else if(value instanceof Long)editor.putLong(key,(Long)value);else if(value instanceof Float)editor.putFloat(key,(Float)value);else if(value instanceof Set)editor.putStringSet(key,new HashSet<>((Set<String>)value));else throw new Exception("Unknown preference type: "+key);}if(!editor.commit())throw new Exception("Could not restore instrumentation preferences");}
 static void shot(Instrumentation test,Context c,String name)throws Exception{Thread.sleep(500);Bitmap image=test.getUiAutomation().takeScreenshot();if(image==null)throw new Exception("Screenshot failed: "+name);File folder=new File(c.getExternalFilesDir(null),"qa");folder.mkdirs();try(FileOutputStream output=new FileOutputStream(new File(folder,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,output);}image.recycle();}
}

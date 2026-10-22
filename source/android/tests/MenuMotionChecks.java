package cl.bolsillo.app;

import android.app.*;
import android.content.*;
import android.graphics.Rect;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

/** First-frame window regression. Uses and restores only synthetic emulator data. */
final class MenuMotionChecks {
 static int checks;
 static void yes(boolean value,String message)throws Exception{checks++;if(!value)throw new Exception(message);}
 static void settle(Instrumentation t,int ms)throws Exception{Thread.sleep(ms);t.waitForIdleSync();}
 static void writePreview(Context c,Bitmap image,String name)throws Exception{if(image==null)return;File file=new File(c.getExternalFilesDir(null),name+".png");try(FileOutputStream out=new FileOutputStream(file)){image.compress(Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}}
 static int run(Instrumentation t,Context c,boolean diagnostic)throws Exception{return run(t,c,diagnostic,false);}
 static int run(Instrumentation t,Context c,boolean diagnostic,boolean capture)throws Exception{
  checks=0;
  Map<String,Object> secure=VisualChecks.preferences(c.getSharedPreferences("secure",0)),settings=VisualChecks.preferences(NativeStore.settings(c)),updates=VisualChecks.preferences(AppUpdates.prefs(c));
  MainActivity[] host={null};Dialog[] opened={null};JSONArray samples=new JSONArray();
  try{
   LocalData.restore(c,VisualChecks.fixture());NativeStore.settings(c).edit().putBoolean("detectionTutorialSeen",true).putBoolean("capture",false).putBoolean("privateScreen",false).putString("appearance","dark").commit();AppUpdates.prefs(c).edit().putLong("attempt",System.currentTimeMillis()).commit();
   host[0]=(MainActivity)t.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));MainActivity a=host[0];V16Checks.loaded(t,a);String before=LocalData.read(c).toString();
   int expected=Math.min(Glass.dp(a,480),a.getResources().getDisplayMetrics().widthPixels-Glass.dp(a,16));
   for(int repeat=0;repeat<3;repeat++){
    final JSONObject sample=new JSONObject().put("attempt",repeat+1);final List<Integer> drawnWidths=Collections.synchronizedList(new ArrayList<>());final ViewTreeObserver.OnPreDrawListener[] observer={null};final int[] chosen={-1};final Bitmap[] preview={null};final boolean takePreview=capture&&repeat==0;
    t.runOnMainSync(()->{try{
     long start=SystemClock.elapsedRealtimeNanos();SheetDialog dialog=NativeSettings.choose(a,"Movimiento",Arrays.asList("Ver detalle","Editar movimiento","Duplicar como nuevo","Eliminar movimiento"),n->chosen[0]=n);opened[0]=dialog;
     WindowManager.LayoutParams attrs=dialog.getWindow().getAttributes();sample.put("openingMs",(SystemClock.elapsedRealtimeNanos()-start)/1000000d).put("widthWhenShowReturns",attrs.width).put("gravityWhenShowReturns",attrs.gravity).put("motionWhenShowReturns",attrs.windowAnimations).put("expectedWidth",expected);
     observer[0]=()->{View decor=dialog.getWindow().getDecorView();drawnWidths.add(decor.getWidth());if(takePreview&&preview[0]==null){preview[0]=Bitmap.createBitmap(decor.getWidth(),decor.getHeight(),Bitmap.Config.ARGB_8888);decor.draw(new Canvas(preview[0]));}return true;};dialog.layout.getViewTreeObserver().addOnPreDrawListener(observer[0]);
    }catch(Exception e){throw new RuntimeException(e);}});
    settle(t,450);Dialog dialog=opened[0];t.runOnMainSync(()->{if(dialog.getWindow().getDecorView().getViewTreeObserver().isAlive())((SheetDialog)dialog).layout.getViewTreeObserver().removeOnPreDrawListener(observer[0]);});
    sample.put("drawnWidths",new JSONArray(drawnWidths));samples.put(sample);if(takePreview){writePreview(c,preview[0],"menus-first-predraw");UiSmoke.shot(t,c,"menus-stable");}
    if(!diagnostic){yes(sample.getInt("widthWhenShowReturns")==expected,"sheet width must be final before Dialog.show returns, not resized by an OnShow callback");yes((sample.getInt("gravityWhenShowReturns")&Gravity.VERTICAL_GRAVITY_MASK)==Gravity.BOTTOM,"sheet must attach at bottom on the first frame");yes(sample.getInt("motionWhenShowReturns")==R.style.SheetMotion,"sheet motion must be set before the window attaches");for(int width:drawnWidths)yes(width==expected,"every drawn frame uses the final sheet width");}
    ListView list=VisualChecks.views(((SheetDialog)dialog).layout,ListView.class).get(0);t.runOnMainSync(()->list.getChildAt(1).performClick());yes(chosen[0]==1&&!dialog.isShowing(),"menu action runs once and dismisses");opened[0]=null;settle(t,200);
   }
   t.runOnMainSync(()->{Dialog quick=QuickEntryDialog.create(a);opened[0]=quick;quick.show();try{WindowManager.LayoutParams attrs=quick.getWindow().getAttributes();samples.put(new JSONObject().put("quickWidthWhenShowReturns",attrs.width).put("quickMotionWhenShowReturns",attrs.windowAnimations));}catch(Exception e){throw new RuntimeException(e);}});settle(t,300);if(!diagnostic){JSONObject quick=samples.getJSONObject(samples.length()-1);yes(quick.getInt("quickWidthWhenShowReturns")==expected,"quick entry shares preconfigured geometry before the first frame");yes(quick.getInt("quickMotionWhenShowReturns")==R.style.SheetMotion,"quick entry shares preconfigured motion before the first frame");}t.runOnMainSync(()->opened[0].dismiss());opened[0]=null;
   yes(before.equals(LocalData.read(c).toString()),"opening, choosing and dismissing menus preserves the entire ledger");
   Bundle report=new Bundle();report.putString("menuOpening",samples.toString());report.putString("environment","Android16 emulator; QA signing, not a phone FPS benchmark");t.sendStatus(0,report);return checks;
  }finally{if(opened[0]!=null)t.runOnMainSync(()->opened[0].dismiss());if(host[0]!=null)t.runOnMainSync(()->host[0].finish());t.waitForIdleSync();NativeSync.EXEC.submit(()->{}).get(15,TimeUnit.SECONDS);VisualChecks.restore(c.getSharedPreferences("secure",0),secure);VisualChecks.restore(NativeStore.settings(c),settings);VisualChecks.restore(AppUpdates.prefs(c),updates);}
 }
}

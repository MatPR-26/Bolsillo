package cl.bolsillo.app;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Read-only light-material rendering diagnosis: CPU canvas versus actual window. */
final class LightPlaneChecks {
 static int checks;
 static void yes(boolean ok,String detail)throws Exception{checks++;if(!ok)throw new Exception(detail);}
 static String drawable(Drawable d){return d==null?"null":d.getClass().getSimpleName()+"("+d.getBounds()+", alpha="+d.getAlpha()+")";}
 static void tree(View view,String prefix,StringBuilder out){
  if(view.getVisibility()!=View.VISIBLE)return;
  out.append(prefix).append(view.getClass().getSimpleName()).append(" xy=").append(view.getLeft()).append(',').append(view.getTop()).append(" size=").append(view.getWidth()).append('x').append(view.getHeight()).append(" bg=").append(drawable(view.getBackground())).append(" fg=").append(drawable(view.getForeground())).append(" tint=").append(view.getBackgroundTintList()).append(" focus=").append(view.isFocused()).append(" highlight=").append(view.getDefaultFocusHighlightEnabled()).append(" layer=").append(view.getLayerType()).append(" alpha=").append(view.getAlpha()).append(" elevation=").append(view.getElevation()).append(" translationZ=").append(view.getTranslationZ()).append(" animator=").append(view.getStateListAnimator()==null?"null":view.getStateListAnimator().getClass().getSimpleName());
  if(view instanceof TextView)out.append(" text=").append(((TextView)view).getText());
  out.append('\n');if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)tree(((ViewGroup)view).getChildAt(i),prefix+" ",out);
 }
 static void save(Context c,Bitmap image,String name)throws Exception{yes(image!=null,"Diagnosis bitmap is available");File directory=new File(c.getExternalFilesDir(null),"qa");directory.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(directory,name+".png"))){yes(image.compress(Bitmap.CompressFormat.PNG,100,out),"Diagnosis PNG is written");}finally{image.recycle();}}
 static void snapshots(Instrumentation test,Context c,View decor,String name)throws Exception{
  VisualChecks.settled(test);final Bitmap[] software={null};final StringBuilder tree=new StringBuilder();
  VisualChecks.main(test,()->{yes(decor.getWidth()>0&&decor.getHeight()>0,"Decor is laid out before CPU rendering");software[0]=Bitmap.createBitmap(decor.getWidth(),decor.getHeight(),Bitmap.Config.ARGB_8888);decor.draw(new Canvas(software[0]));tree(decor,"",tree);});
  save(c,software[0],name+"-cpu");save(c,test.getUiAutomation().takeScreenshot(),name+"-gpu");
  Bundle status=new Bundle();status.putString("lightPlaneTree",name+"\n"+tree);test.sendStatus(0,status);
 }
 static void withoutNativeHighlights(View view){
  view.setDefaultFocusHighlightEnabled(false);
  if(view instanceof Button){view.setBackgroundTintList(null);view.setStateListAnimator(null);}
  if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)withoutNativeHighlights(((ViewGroup)view).getChildAt(i));
 }
 static void capOutlineOpacity(View view,Map<View,ViewOutlineProvider> before){
  final ViewOutlineProvider provider=view.getOutlineProvider();
  if(view.getBackground()!=null&&provider!=null){before.put(view,provider);view.setOutlineProvider(new ViewOutlineProvider(){public void getOutline(View v,Outline outline){provider.getOutline(v,outline);outline.setAlpha(Math.min(.75f,outline.getAlpha()));}});view.invalidateOutline();}
  if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)capOutlineOpacity(((ViewGroup)view).getChildAt(i),before);
 }
 static void outlineShot(Instrumentation test,Context c,View decor,String name)throws Exception{
  Map<View,ViewOutlineProvider> before=new IdentityHashMap<>();
  try{VisualChecks.main(test,()->capOutlineOpacity(decor,before));snapshots(test,c,decor,name);}finally{VisualChecks.main(test,()->{for(Map.Entry<View,ViewOutlineProvider> entry:before.entrySet()){entry.getKey().setOutlineProvider(entry.getValue());entry.getKey().invalidateOutline();}});}
 }
 static int run(Instrumentation test,Context c)throws Exception{
  checks=0;NativeSync.EXEC.submit(()->{}).get(10,TimeUnit.SECONDS);
  Map<String,Object> secure=VisualChecks.preferences(c.getSharedPreferences("secure",0)),settings=VisualChecks.preferences(NativeStore.settings(c));JSONObject finance=LocalData.read(c);
  MainActivity activity=null;final Forms.Form[] form={null};final CalendarSheet.Session[] calendar={null};
  try{
   test.setInTouchMode(true);NativeStore.settings(c).edit().putBoolean("capture",false).putBoolean("privateScreen",false).putBoolean("alertChanges",false).putBoolean("alertDetections",false).putString("appearance","light").putString("palette","prisma").commit();
   activity=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));test.waitForIdleSync();final MainActivity a=activity;
   snapshots(test,c,a.getWindow().getDecorView(),"plane-home");
   outlineShot(test,c,a.getWindow().getDecorView(),"plane-home-translucent-outline");
   VisualChecks.main(test,()->a.getWindow().getDecorView().setLayerType(View.LAYER_TYPE_SOFTWARE,null));snapshots(test,c,a.getWindow().getDecorView(),"plane-home-software");VisualChecks.main(test,()->a.getWindow().getDecorView().setLayerType(View.LAYER_TYPE_NONE,null));
   final EditText[] date={null};VisualChecks.main(test,()->{
    form[0]=new Forms.Form(a,"Nuevo gasto");form[0].field("Monto en pesos","",true);form[0].conceptField("Comercio o descripción","","expense");new Forms.Pick(form[0],"Cuenta de origen","accounts","","");new Forms.Pick(form[0],"Categoría","categories","expense","");form[0].advanced();date[0]=form[0].field("Fecha",Ledger.today(),false);form[0].collapseAdvanced();form[0].save("Guardar movimiento",()->{});
    ((InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(form[0].box.getWindowToken(),0);
   });snapshots(test,c,form[0].dialog.getWindow().getDecorView(),"plane-form");outlineShot(test,c,form[0].dialog.getWindow().getDecorView(),"plane-form-translucent-outline");
   VisualChecks.main(test,()->{calendar[0]=CalendarSheet.create(a,date[0],LocalDate.parse(Ledger.today()));calendar[0].dialog.show();});snapshots(test,c,calendar[0].dialog.getWindow().getDecorView(),"plane-calendar");outlineShot(test,c,calendar[0].dialog.getWindow().getDecorView(),"plane-calendar-translucent-outline");
   // Runtime diagnostic only. No production preference or source is changed.
   VisualChecks.main(test,()->withoutNativeHighlights(calendar[0].dialog.getWindow().getDecorView()));snapshots(test,c,calendar[0].dialog.getWindow().getDecorView(),"plane-calendar-no-native-highlights");
   VisualChecks.main(test,()->{calendar[0].dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);WindowManager.LayoutParams p=calendar[0].dialog.getWindow().getAttributes();p.setBlurBehindRadius(0);calendar[0].dialog.getWindow().setAttributes(p);});snapshots(test,c,calendar[0].dialog.getWindow().getDecorView(),"plane-calendar-no-window-blur");
   VisualChecks.main(test,()->{calendar[0].dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);WindowManager.LayoutParams p=calendar[0].dialog.getWindow().getAttributes();p.setBlurBehindRadius(a.dp(18));calendar[0].dialog.getWindow().setAttributes(p);calendar[0].dialog.layout.setLayerType(View.LAYER_TYPE_SOFTWARE,null);});snapshots(test,c,calendar[0].dialog.getWindow().getDecorView(),"plane-calendar-software-content");
   VisualChecks.main(test,()->{calendar[0].dialog.layout.setLayerType(View.LAYER_TYPE_NONE,null);calendar[0].dialog.getWindow().getDecorView().setLayerType(View.LAYER_TYPE_SOFTWARE,null);});snapshots(test,c,calendar[0].dialog.getWindow().getDecorView(),"plane-calendar-software-decor");
   yes(LocalData.read(c).toString().equals(finance.toString()),"Rendering diagnosis must preserve every financial record");
  }finally{
   try{final MainActivity a=activity;VisualChecks.main(test,()->{if(calendar[0]!=null)calendar[0].dialog.dismiss();if(form[0]!=null)form[0].dialog.dismiss();if(a!=null)a.finish();});test.waitForIdleSync();NativeSync.EXEC.submit(()->{}).get(10,TimeUnit.SECONDS);}finally{VisualChecks.restore(c.getSharedPreferences("secure",0),secure);VisualChecks.restore(NativeStore.settings(c),settings);}
  }
  yes(VisualChecks.preferences(c.getSharedPreferences("secure",0)).equals(secure)&&VisualChecks.preferences(NativeStore.settings(c)).equals(settings),"Rendering diagnosis restores encrypted data and settings exactly");return checks;
 }
}

package cl.bolsillo.app;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/** Synthetic UI-only checks. No control is connected to finance or preference listeners. */
final class ControlsChecks {
 static int checks;
 static void yes(boolean condition,String detail)throws Exception{checks++;if(!condition)throw new Exception(detail);}

 static int run(Instrumentation test,Context c)throws Exception{
  checks=0;
  Map<String,?> secure=new HashMap<>(c.getSharedPreferences("secure",0).getAll()),settings=new HashMap<>(NativeStore.settings(c).getAll());
  MainActivity activity=null;final Probe[] active={null};
  try{
   JSONObject finance=LocalData.read(c);
   NativeStore.settings(c).edit().putString("appearance","dark").putString("palette","prisma").putBoolean("privateScreen",false).commit();
   activity=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));test.waitForIdleSync();final MainActivity host=activity;
   for(String mode:new String[]{"dark","light"})for(String palette:new String[]{"prisma","ocean","amber"}){
    NativeStore.settings(c).edit().putString("appearance",mode).putString("palette",palette).commit();
    test.runOnMainSync(()->{active[0]=new Probe(host,false);active[0].sheet.show();});test.waitForIdleSync();
    Probe p=active[0];String theme=mode+"/"+palette;
    yes(p.on.isChecked()&&!p.check.isChecked()&&p.switchChanges==0&&p.checkChanges==0,"Styling must preserve the existing checked states and listeners in "+theme);
    yes(p.on.getAccessibilityClassName().toString().equals("android.widget.Switch")&&p.check.getAccessibilityClassName().toString().equals("android.widget.CheckBox"),"Modern skins must retain Android accessibility roles in "+theme);
    final boolean[] node={false},targets={false},track={false};
    test.runOnMainSync(()->{
     AccessibilityNodeInfo a=p.on.createAccessibilityNodeInfo(),b=p.check.createAccessibilityNodeInfo();
     node[0]=a.isCheckable()&&a.isChecked()&&b.isCheckable()&&!b.isChecked()&&p.on.getContentDescription().toString().equals("Registrar avisos automáticamente");a.recycle();b.recycle();
     targets[0]=p.on.getHeight()>=Glass.dp(p.c,48)&&p.on.getWidth()>=Glass.dp(p.c,48)&&p.check.getHeight()>=Glass.dp(p.c,48)&&p.check.getWidth()>=Glass.dp(p.c,48);
     Rect bounds=p.on.getTrackDrawable().getBounds();track[0]=bounds.width()==Glass.dp(p.c,52)&&bounds.height()==Glass.dp(p.c,32);
    });
    yes(node[0],"Checked state and custom accessibility descriptions must remain exposed in "+theme);
    yes(targets[0]&&track[0],"Native controls must expose 48dp hit areas and an actual 52×32dp switch track in "+theme);
    final boolean[] font={false},repeat={false};
    test.runOnMainSync(()->{
     font[0]=p.on.getTypeface().equals(Glass.font(p.c,500))&&p.check.getTypeface().equals(Glass.font(p.c,400));
     int start=p.check.getPaddingStart(),end=p.check.getPaddingEnd(),minimum=p.check.getMinimumHeight();Controls.style(p.on);Controls.style(p.check);
     repeat[0]=p.on.isChecked()&&!p.check.isChecked()&&p.switchChanges==0&&p.checkChanges==0&&start==p.check.getPaddingStart()&&end==p.check.getPaddingEnd()&&minimum==p.check.getMinimumHeight();
    });
    yes(font[0]&&repeat[0],"Restyling must be idempotent and keep deliberate regular/medium labels in "+theme);
    test.runOnMainSync(()->{p.on.performClick();p.check.performClick();});test.waitForIdleSync();
    yes(!p.on.isChecked()&&p.check.isChecked()&&p.switchChanges==1&&p.checkChanges==1,"Native click must still toggle once and call each original listener once in "+theme);
    test.runOnMainSync(()->{p.on.setChecked(true);p.on.jumpDrawablesToCurrentState();p.check.jumpDrawablesToCurrentState();});test.waitForIdleSync();
    final int[] thumbCheck={0},boxCheck={0};test.runOnMainSync(()->{thumbCheck[0]=pixels(p.on.getThumbDrawable(),Glass.accent(p.c));boxCheck[0]=pixels(p.check.getButtonDrawable(),Glass.dark(p.c)?0xff203332:Color.WHITE);});
    yes(thumbCheck[0]>4&&boxCheck[0]>4,"Official check icons must remain visible after Android reapplies theme tint in "+theme);
    yes(p.on.getBackground() instanceof RippleDrawable&&p.check.getBackground() instanceof RippleDrawable,"Native pressed/focused feedback must remain available in "+theme);
    test.runOnMainSync(()->{p.on.setChecked(false);p.on.jumpDrawablesToCurrentState();});test.waitForIdleSync();
    test.runOnMainSync(()->drag(p.on));test.waitForIdleSync();
    yes(p.on.isChecked()&&p.switchChanges==4,"Dragging the native switch thumb must still toggle and notify once in "+theme);
    final int[] enabled={0},disabled={0};
    test.runOnMainSync(()->{
     enabled[0]=center(p.on.getTrackDrawable());p.on.setEnabled(false);disabled[0]=center(p.on.getTrackDrawable());
     tap(p.on);tap(p.disabledOn);tap(p.disabledOff);
    });test.waitForIdleSync();
    yes(p.on.isChecked()&&p.switchChanges==4&&p.disabledOn.isChecked()&&!p.disabledOff.isChecked(),"Disabled touch must preserve checked state and never call an existing listener in "+theme);
    yes(enabled[0]!=disabled[0]&&Color.alpha(p.on.getCurrentTextColor())==255&&p.on.getCurrentTextColor()!=Glass.ink(p.c),"Disabled controls must use visibly distinct opaque labels and track colours in "+theme);
    test.runOnMainSync(()->{p.on.setEnabled(true);p.on.setChecked(true);});test.waitForIdleSync();
    UiSmoke.shot(test,c,"controls-"+mode+"-"+palette);test.runOnMainSync(p.sheet::dismiss);active[0]=null;
   }
   NativeStore.settings(c).edit().putString("appearance","light").putString("palette","amber").commit();
   test.runOnMainSync(()->{active[0]=new Probe(host,true);active[0].sheet.show();});test.waitForIdleSync();Probe large=active[0];
   final boolean[] complete={false},rtl={false};test.runOnMainSync(()->{
    complete[0]=lastLineFits(large.on)&&lastLineFits(large.check)&&large.on.getLineCount()>1&&large.check.getLineCount()>1;
    rtl[0]=large.on.getTrackDrawable().getBounds().centerX()<large.on.getWidth()/2&&large.check.getButtonDrawable().getBounds().centerX()>large.check.getWidth()/2;
   });
   yes(complete[0],"Long labels with 150% font size must wrap without clipping their final line");
   yes(rtl[0],"Native RTL layout must mirror the switch track and checkbox mark without moving text over them");
   UiSmoke.shot(test,c,"controls-light-amber-rtl-large");test.runOnMainSync(large.sheet::dismiss);active[0]=null;
   yes(LocalData.read(c).toString().equals(finance.toString()),"UI control previews must preserve all finance records");
  }finally{
   try{if(activity!=null){final MainActivity host=activity;test.runOnMainSync(()->{if(active[0]!=null)active[0].sheet.dismiss();host.finish();});test.waitForIdleSync();}}
   finally{try{QuickWidgetTests.restore(c.getSharedPreferences("secure",0),secure);}finally{QuickWidgetTests.restore(NativeStore.settings(c),settings);}}
  }
  return checks;
 }

 static final class Probe {
  final Context c;final SheetDialog sheet;final Switch on,off;final CheckBox check,disabledOn,disabledOff;int switchChanges,checkChanges;
  Probe(MainActivity host,boolean large){
   ContextThemeWrapper theme=new ContextThemeWrapper(host,Glass.dark(host)?R.style.GlassSheetDark:R.style.GlassSheet);
   if(large){Configuration config=new Configuration(host.getResources().getConfiguration());config.fontScale=1.5f;theme.applyOverrideConfiguration(config);}c=theme;
   LinearLayout body=Glass.column(c);body.addView(Glass.text(c,"Tus preferencias",23,true));
   on=new Switch(c);on.setText(large?"Registrar automáticamente compras y transferencias detectadas en tus aplicaciones":"Registro automático");on.setContentDescription("Registrar avisos automáticamente");on.setChecked(true);on.setOnCheckedChangeListener((v,value)->switchChanges++);Controls.style(on);body.addView(on,row());
   off=new Switch(c);off.setText("Ocultar importes");Controls.style(off);body.addView(off,row());
   check=new CheckBox(c);check.setText(large?"Incluir BancoEstado y MACHBANK en el resumen de mis cuentas y movimientos":"Incluir en mi dinero");check.setOnCheckedChangeListener((v,value)->checkChanges++);Controls.style(check);body.addView(check,row());
   CheckBox selected=new CheckBox(c);selected.setText("Guardar el historial");selected.setChecked(true);Controls.style(selected);body.addView(selected,row());
   disabledOn=new CheckBox(c);disabledOn.setText("Seleccionada · no disponible");disabledOn.setChecked(true);disabledOn.setEnabled(false);Controls.style(disabledOn);body.addView(disabledOn,row());
   disabledOff=new CheckBox(c);disabledOff.setText("Sin seleccionar · no disponible");disabledOff.setEnabled(false);Controls.style(disabledOff);body.addView(disabledOff,row());
   if(large){body.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);for(int i=0;i<body.getChildCount();i++)body.getChildAt(i).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);}
   sheet=new SheetDialog(host,body);sheet.actions("Listo",()->{});
  }
  LinearLayout.LayoutParams row(){LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(-1,-2);size.topMargin=Glass.dp(c,8);return size;}
 }
 static boolean lastLineFits(CompoundButton view){return view.getLayout()!=null&&view.getLayout().getLineBottom(view.getLineCount()-1)<=view.getHeight()-view.getCompoundPaddingTop()-view.getCompoundPaddingBottom();}
 static void drag(Switch view){Bitmap frame=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);try{view.draw(new Canvas(frame));}finally{frame.recycle();}Rect thumb=view.getThumbDrawable().getBounds(),track=view.getTrackDrawable().getBounds();long time=android.os.SystemClock.uptimeMillis();float x=thumb.centerX(),y=thumb.centerY(),end=track.right-Glass.dp(view.getContext(),10);event(view,time,time,MotionEvent.ACTION_DOWN,x,y);event(view,time,time+100,MotionEvent.ACTION_MOVE,(x+end)/2,y);event(view,time,time+200,MotionEvent.ACTION_MOVE,end,y);event(view,time,time+260,MotionEvent.ACTION_UP,end,y);}
 static void tap(CompoundButton view){long time=android.os.SystemClock.uptimeMillis();float x=view.getWidth()/2f,y=view.getHeight()/2f;event(view,time,time,MotionEvent.ACTION_DOWN,x,y);event(view,time,time+100,MotionEvent.ACTION_UP,x,y);}
 static void event(View view,long start,long time,int action,float x,float y){MotionEvent event=MotionEvent.obtain(start,time,action,x,y,0);try{view.dispatchTouchEvent(event);}finally{event.recycle();}}
 static Bitmap render(Drawable drawable){Rect old=new Rect(drawable.getBounds());int width=drawable.getIntrinsicWidth(),height=drawable.getIntrinsicHeight();Bitmap image=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);try{drawable.setBounds(0,0,width,height);drawable.draw(new Canvas(image));}finally{drawable.setBounds(old);}return image;}
 static int pixels(Drawable drawable,int color){Bitmap image=render(drawable);int count=0;try{for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if(image.getPixel(x,y)==color)count++;return count;}finally{image.recycle();}}
 static int center(Drawable drawable){Bitmap image=render(drawable);try{return image.getPixel(image.getWidth()/2,image.getHeight()/2);}finally{image.recycle();}}
}

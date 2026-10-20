package cl.bolsillo.app;

import android.app.Dialog;
import android.app.Instrumentation;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.view.Window;
import android.view.WindowManager;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Real Android window flags; previews never save a transaction or a widget. */
final class PrivacyChecks {
 static int checks;
 static void yes(boolean condition,String detail)throws Exception{checks++;if(!condition)throw new Exception(detail);}
 static boolean secure(Window window){return window!=null&&(window.getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0;}
 static int run(Instrumentation test,Context c)throws Exception{
  checks=0;NativeSync.EXEC.submit(()->{}).get(10,TimeUnit.SECONDS);
  Map<String,Object> encrypted=VisualChecks.preferences(c.getSharedPreferences("secure",0)),settings=VisualChecks.preferences(NativeStore.settings(c));
  JSONObject finance=LocalData.read(c);MainActivity main=null;QuickEntryActivity quick=null;final Dialog[] current={null};
  try{
   NativeStore.settings(c).edit().putBoolean("capture",false).commit();
   for(boolean enabled:new boolean[]{false,true}){
    NativeStore.settings(c).edit().putBoolean("privateScreen",enabled).commit();
    main=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));test.waitForIdleSync();final MainActivity a=main;
    yes(secure(a.getWindow())==enabled,"Main activity must apply screenshot privacy during creation and resume: "+enabled);
    final SheetDialog[] sheet={null};final QuickEntryDialog.Entry[] entry={null};
    VisualChecks.main(test,()->{
     sheet[0]=new SheetDialog(a,Glass.text(a,"Vista de prueba, sin guardar",16,false));current[0]=sheet[0];
    });
    yes(secure(sheet[0].getWindow())==enabled,"A finance sheet must apply privacy before its first frame: "+enabled);
    VisualChecks.main(test,sheet[0]::show);test.waitForIdleSync();
    yes(secure(sheet[0].getWindow())==enabled,"A shown finance sheet must retain screenshot privacy: "+enabled);
    VisualChecks.main(test,()->{sheet[0].dismiss();current[0]=null;entry[0]=new QuickEntryDialog.Entry(a,0,"",null);current[0]=entry[0].dialog;});
    yes(secure(entry[0].dialog.getWindow())==enabled,"Quick entry must apply privacy before its first frame: "+enabled);
    final int originalType=entry[0].dialog.getWindow().getAttributes().type;final android.os.IBinder originalToken=entry[0].dialog.getWindow().getAttributes().token;
    VisualChecks.main(test,()->{entry[0].dialog.show();Glass.window(entry[0].dialog,a);});test.waitForIdleSync();
    yes(secure(entry[0].dialog.getWindow())==enabled,"Quick entry must retain privacy when the glass window is configured: "+enabled);
    yes(entry[0].dialog.getWindow().getAttributes().type==originalType,"Privacy must not replace the quick window type");
    // Showing an application dialog may assign Android's activity token; the privacy helper itself must preserve it.
    final android.os.IBinder shownToken=entry[0].dialog.getWindow().getAttributes().token;
    final int[] expectedFlags={0};
    VisualChecks.main(test,()->{entry[0].dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);expectedFlags[0]=entry[0].dialog.getWindow().getAttributes().flags;Glass.privacy(entry[0].dialog.getWindow(),a);});
    yes(entry[0].dialog.getWindow().getAttributes().flags==expectedFlags[0]&&entry[0].dialog.getWindow().getAttributes().token==shownToken,"Repeated privacy application must preserve all unrelated flags and the quick window token");
    yes(originalToken==null||originalToken==shownToken,"A preassigned quick-entry token must remain unchanged when shown");
    NativeStore.settings(c).edit().putBoolean("privateScreen",!enabled).commit();
    VisualChecks.main(test,()->{a.applyPrivacy();Glass.privacy(entry[0].dialog.getWindow(),a);});
    yes(secure(a.getWindow())==!enabled&&secure(entry[0].dialog.getWindow())==!enabled,"Changing privacy must add or remove protection on existing activity and dialog windows");
    yes((entry[0].dialog.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)!=0,"Changing privacy must preserve unrelated quick-window flags");
    VisualChecks.main(test,()->{entry[0].dialog.dismiss();current[0]=null;});test.waitForIdleSync();
    NativeStore.settings(c).edit().putBoolean("privateScreen",enabled).commit();
    quick=(QuickEntryActivity)test.startActivitySync(new Intent(c,QuickEntryActivity.class).putExtra("quick","income").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));test.waitForIdleSync();final QuickEntryActivity q=quick;
    yes(secure(q.getWindow())==enabled&&secure(q.panel.getWindow())==enabled,"Widget/shortcut quick-entry activity and its panel must both respect privacy: "+enabled);
    NativeStore.settings(c).edit().putBoolean("privateScreen",!enabled).commit();
    VisualChecks.main(test,q::onResume);test.waitForIdleSync();
    yes(secure(q.getWindow())==!enabled&&secure(q.panel.getWindow())==!enabled,"Quick-entry resume must refresh screenshot protection on the activity and its panel");
    VisualChecks.main(test,q::finish);test.waitForIdleSync();quick=null;
    NativeStore.settings(c).edit().putBoolean("privateScreen",enabled).commit();
    // An invalid id exercises the real exported configuration activity without binding or changing a launcher widget.
    Instrumentation.ActivityMonitor monitor=test.addMonitor(WidgetConfigActivity.class.getName(),null,false);
    try{
     c.startActivity(new Intent(c,WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
     WidgetConfigActivity widget=(WidgetConfigActivity)test.waitForMonitorWithTimeout(monitor,5000);test.waitForIdleSync();
     yes(widget!=null,"The real widget configuration activity must be observed even when it rejects an invalid id");
     yes(secure(widget.getWindow())==enabled,"Widget configuration must protect its window before validating the widget id: "+enabled);
     yes(widget.isFinishing()&&widget.editor==null,"Invalid widget configuration must finish without exposing balances or creating an editor");
    }finally{test.removeMonitor(monitor);}
    VisualChecks.main(test,a::finish);test.waitForIdleSync();main=null;
   }
   yes(LocalData.read(c).toString().equals(finance.toString()),"Privacy toggles and cancelled previews must preserve every finance record");
  }finally{
   try{final MainActivity a=main;final QuickEntryActivity q=quick;VisualChecks.main(test,()->{if(current[0]!=null)current[0].dismiss();if(q!=null)q.finish();if(a!=null)a.finish();});test.waitForIdleSync();NativeSync.EXEC.submit(()->{}).get(10,TimeUnit.SECONDS);}finally{VisualChecks.restore(c.getSharedPreferences("secure",0),encrypted);VisualChecks.restore(NativeStore.settings(c),settings);}
  }
  yes(VisualChecks.preferences(c.getSharedPreferences("secure",0)).equals(encrypted)&&VisualChecks.preferences(NativeStore.settings(c)).equals(settings),"Privacy checks must restore encrypted data and every preference exactly");
  return checks;
 }
}

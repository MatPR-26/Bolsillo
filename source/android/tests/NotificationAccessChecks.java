package cl.bolsillo.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Permission consent cancellation, honest status and reconnect regression; synthetic emulator only. */
final class NotificationAccessChecks {
 static int checks;
 static void yes(boolean condition,String detail)throws Exception{checks++;if(!condition)throw new Exception(detail);}
 static int run(Instrumentation test,Context c)throws Exception{
  checks=0;NativeSync.EXEC.submit(()->{}).get(15,TimeUnit.SECONDS);
  Map<String,Object> secure=VisualChecks.preferences(c.getSharedPreferences("secure",0)),settings=VisualChecks.preferences(NativeStore.settings(c)),updates=VisualChecks.preferences(AppUpdates.prefs(c));
  MainActivity[] host={null};SheetDialog[] disclosure={null};
  try{
   LocalData.restore(c,VisualChecks.fixture());
   NativeStore.settings(c).edit().putBoolean("detectionTutorialSeen",true).putBoolean("privateScreen",false).putBoolean("capture",false).putBoolean("alertChanges",false).putBoolean("alertDetections",false).putBoolean("alertVerifications",false).remove(NotificationAccess.PENDING).commit();
   AppUpdates.prefs(c).edit().putLong("attempt",System.currentTimeMillis()).commit();
   host[0]=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));MainActivity a=host[0];V16Checks.loaded(test,a);
   String ledger=LocalData.read(c).toString();
   for(boolean permission:new boolean[]{false,true})for(boolean capture:new boolean[]{false,true})for(boolean connected:new boolean[]{false,true})for(int apps:new int[]{0,2}){
    NotificationAccess.State state=new NotificationAccess.State(permission,capture,connected,apps);
    yes(state.ready()==(permission&&capture&&connected&&apps>0),"ready requires permission, capture, connection and selected apps");
   }
   yes(new NotificationAccess.State(true,true,false,2).action()==NotificationAccess.Action.RECONNECT,"lost connection offers reconnect, not a capture toggle");
   yes(new NotificationAccess.State(false,true,true,2).action()==NotificationAccess.Action.GRANT,"stale connected flag cannot disguise missing access");
   yes(new NotificationAccess.State(true,true,true,0).action()==NotificationAccess.Action.SELECT_APPS,"no selected apps cannot appear active");

   test.runOnMainSync(()->disclosure[0]=NotificationAccess.disclose(a,null));V15Checks.pause(test,160);
   yes(!NativeStore.settings(c).getBoolean("capture",false)&&!NativeStore.settings(c).getBoolean(NotificationAccess.PENDING,false),"opening explanation neither enables capture nor records consent");
   test.runOnMainSync(()->((Button)disclosure[0].layout.findViewWithTag("notification-access-later")).performClick());V15Checks.pause(test,100);
   yes(!disclosure[0].isShowing()&&!NativeStore.settings(c).getBoolean("capture",false)&&!NativeStore.settings(c).getBoolean(NotificationAccess.PENDING,false),"Ahora no leaves manual operation and consent unchanged");
   test.runOnMainSync(()->disclosure[0]=NotificationAccess.disclose(a,null));V15Checks.pause(test,100);test.runOnMainSync(()->disclosure[0].cancel());
   yes(!NativeStore.settings(c).getBoolean("capture",false)&&!NativeStore.settings(c).getBoolean(NotificationAccess.PENDING,false),"back or outside cancellation cannot enable capture");

   boolean granted=NativeSettings.permission(a);
   NativeStore.settings(c).edit().putBoolean(NotificationAccess.PENDING,true).putBoolean("capture",false).commit();
   test.runOnMainSync(()->NotificationAccess.onResume(a));
   yes(!NativeStore.settings(c).getBoolean(NotificationAccess.PENDING,false),"return consumes pending permission attempt");
   yes(NativeStore.settings(c).getBoolean("capture",false)==granted,"capture follows actual Android grant after explicit consent");

   if(granted){
    NativeStore.settings(c).edit().putString("sources",new JSONArray().put(new JSONObject().put("package","cl.bolsillo.tests").put("kind","mach").put("label","Banco sintético").put("account","bank-mach")).toString()).putBoolean("capture",true).putBoolean("listenerConnected",false).commit();
    test.runOnMainSync(()->NotificationAccess.primary(a));
    yes(NativeStore.settings(c).getBoolean("capture",false),"reconnecting a granted active reader never pauses capture");
   }
   test.runOnMainSync(()->{NativeStore.settings(c).edit().putBoolean("capture",false).apply();a.tutorial=new DetectionTutorial(a,2);a.tutorial.show();});V15Checks.pause(test,200);
   yes(a.tutorial.accessStatus!=null&&a.tutorial.accessAction!=null,"permission tutorial exposes live status and configuration action");
   yes(a.tutorial.accessStatus.getText().toString().contains("Captura: pausada"),"tutorial completion cannot falsely claim capture is active");
   test.runOnMainSync(()->{a.tutorial.step=3;a.tutorial.show();});V15Checks.pause(test,160);
   yes(a.tutorial.ownStatus!=null&&a.tutorial.ownAction!=null,"sending our own notices has a separate tutorial control");
   yes(ledger.equals(LocalData.read(c).toString()),"permission explanations, status and reconnect preserve financial records");
  }finally{
   if(disclosure[0]!=null)test.runOnMainSync(()->disclosure[0].dismiss());
   if(host[0]!=null)test.runOnMainSync(()->{if(host[0].tutorial!=null)host[0].tutorial.close(false);host[0].finish();});
   test.waitForIdleSync();NativeSync.EXEC.submit(()->{}).get(15,TimeUnit.SECONDS);
   VisualChecks.restore(c.getSharedPreferences("secure",0),secure);VisualChecks.restore(NativeStore.settings(c),settings);VisualChecks.restore(AppUpdates.prefs(c),updates);
  }
  return checks;
 }
}

package cl.bolsillo.app;

import android.animation.ValueAnimator;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Exercise the real API34 callback without leaving the root or writing finances. */
final class ModernBackChecks {
 static int checks;
 static void yes(boolean condition,String detail)throws Exception{checks++;if(!condition)throw new Exception(detail);}
 static void same(View target,String detail)throws Exception{yes(Math.abs(target.getScaleX()-1)<.0001f&&Math.abs(target.getScaleY()-1)<.0001f&&Math.abs(target.getTranslationX())<.0001f,detail);}
 static int run(Instrumentation test,Context c)throws Exception{
  checks=0;yes(Build.VERSION.SDK_INT>=34,"Predictive progress tests require a real Android34+ callback");
  NativeSync.EXEC.submit(()->{}).get(10,TimeUnit.SECONDS);
  Map<String,Object> secure=VisualChecks.preferences(c.getSharedPreferences("secure",0)),settings=VisualChecks.preferences(NativeStore.settings(c));
  JSONObject before=LocalData.read(c);MainActivity activity=null;
  try{
   NativeStore.settings(c).edit().putBoolean("capture",false).putBoolean("privateScreen",false).commit();
   activity=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));test.waitForIdleSync();final MainActivity a=activity;
   VisualChecks.main(test,()->{a.history.clear();a.tab="Inicio";a.render();});test.waitForIdleSync();
   yes(a.modernBack.driver instanceof ModernBack.Animated,"Android34+ must use the platform animation callback driver");
   yes(!a.modernBack.driver.registered&&a.history.isEmpty(),"At the home root Android must own back-to-home without an app interception");
   VisualChecks.main(test,()->a.listener.onSharedPreferenceChanged(NativeStore.settings(c),null));yes(LocalData.read(c).toString().equals(before.toString()),"A platform null reset callback must be harmless and preserve finances");
   final OnBackAnimationCallback callback=(OnBackAnimationCallback)a.modernBack.driver.callback;
   VisualChecks.main(test,()->a.navigate("Cuentas"));test.waitForIdleSync();
   yes(a.tab.equals("Cuentas")&&a.history.size()==1&&a.modernBack.driver.registered,"Opening an internal route must register back and remember exactly one previous route");
   final View left=a.root;
   VisualChecks.main(test,()->{callback.onBackStarted(new BackEvent(0,100,0,BackEvent.EDGE_LEFT));callback.onBackProgressed(new BackEvent(70,100,.6f,BackEvent.EDGE_LEFT));});
   if(ValueAnimator.areAnimatorsEnabled())yes(left.getScaleX()<1&&left.getScaleY()<1&&left.getTranslationX()>0,"Left-edge progress must animate the current page towards the gesture without navigating");else same(left,"When system animations are disabled progress must leave page geometry unchanged");
   yes(a.tab.equals("Cuentas")&&a.history.size()==1,"Predictive progress must not consume the navigation history");
   VisualChecks.main(test,callback::onBackCancelled);Thread.sleep(230);test.waitForIdleSync();
   same(left,"Cancelling a predictive gesture must restore scale and translation");
   yes(a.tab.equals("Cuentas")&&a.history.size()==1&&a.modernBack.driver.registered,"Cancellation must keep the internal route and interception registered");
   VisualChecks.main(test,()->{callback.onBackStarted(new BackEvent(400,100,0,BackEvent.EDGE_RIGHT));callback.onBackProgressed(new BackEvent(330,100,.7f,BackEvent.EDGE_RIGHT));});
   if(ValueAnimator.areAnimatorsEnabled())yes(left.getTranslationX()<0,"Right-edge progress must move in the opposite direction");else same(left,"Disabled right-edge animation must keep geometry unchanged");
   VisualChecks.main(test,callback::onBackInvoked);test.waitForIdleSync();
   same(left,"Invoking back must reset the previous page before replacing it");
   yes(a.tab.equals("Inicio")&&a.history.isEmpty()&&!a.modernBack.driver.registered&&!a.isFinishing(),"Invoking internal back must return home once and release the platform callback without finishing");
   VisualChecks.main(test,()->{a.history.clear();a.tab="Cuentas";a.render();});test.waitForIdleSync();
   yes(a.modernBack.driver.registered,"A directly opened internal route must still intercept one back gesture");
   VisualChecks.main(test,callback::onBackInvoked);test.waitForIdleSync();
   yes(a.tab.equals("Inicio")&&a.history.isEmpty()&&!a.modernBack.driver.registered,"An internal route without history must return to the home root");
   VisualChecks.main(test,()->{a.navigate("Cuentas");a.navigate("Personas");});test.waitForIdleSync();
   yes(a.history.size()==2,"Two nested routes must preserve the order of both previous destinations");
   VisualChecks.main(test,callback::onBackInvoked);test.waitForIdleSync();
   yes(a.tab.equals("Cuentas")&&a.history.size()==1&&a.modernBack.driver.registered,"One back invocation must pop only the latest route");
   final View page=a.root;VisualChecks.main(test,()->{callback.onBackStarted(new BackEvent(0,100,0,BackEvent.EDGE_LEFT));callback.onBackProgressed(new BackEvent(50,100,.8f,BackEvent.EDGE_LEFT));a.render(false);});test.waitForIdleSync();
   same(page,"A page refresh during a gesture must restore the old page transform");
   yes(((ModernBack.Animated)a.modernBack.driver).target==null&&a.tab.equals("Cuentas")&&a.history.size()==1,"A refresh must clear the gesture target without changing the route history");
   VisualChecks.main(test,()->{callback.onBackStarted(new BackEvent(0,100,0,BackEvent.EDGE_LEFT));callback.onBackProgressed(new BackEvent(50,100,.8f,BackEvent.EDGE_LEFT));a.modernBack.dispose();});
   same(a.root,"Disposing the callback must reset the current page transform");yes(!a.modernBack.driver.registered,"Disposal must unregister the callback");
   VisualChecks.main(test,a.modernBack::refresh);yes(a.modernBack.driver.registered,"Refreshing an active route after disposal must register exactly once");
   yes(LocalData.read(c).toString().equals(before.toString()),"All predictive navigation must preserve every existing finance record");
  }finally{
   try{if(activity!=null){final MainActivity a=activity;test.runOnMainSync(a::finish);test.waitForIdleSync();}NativeSync.EXEC.submit(()->{}).get(10,TimeUnit.SECONDS);}finally{VisualChecks.restore(c.getSharedPreferences("secure",0),secure);VisualChecks.restore(NativeStore.settings(c),settings);}
  }
  yes(VisualChecks.preferences(c.getSharedPreferences("secure",0)).equals(secure)&&VisualChecks.preferences(NativeStore.settings(c)).equals(settings),"Navigation tests must restore all encrypted data and preferences");
  return checks;
 }
}

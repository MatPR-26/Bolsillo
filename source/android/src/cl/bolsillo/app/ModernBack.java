package cl.bolsillo.app;

import android.animation.ValueAnimator;
import android.os.Build;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/** Intercept internal routes only; Android owns the root back-to-home transition. */
final class ModernBack {
 final MainActivity activity;
 final Plain driver;
 ModernBack(MainActivity activity){this.activity=activity;driver=Build.VERSION.SDK_INT>=34?new Animated(activity):Build.VERSION.SDK_INT>=33?new Plain(activity):null;}
 void refresh(){if(driver!=null)driver.update(!activity.history.isEmpty()||!activity.tab.equals("Inicio"));}
 void reset(){if(driver!=null)driver.reset();}
 void dispose(){if(driver!=null){driver.reset();driver.update(false);}}

 static class Plain {
  final MainActivity activity;
  OnBackInvokedCallback callback;
  boolean registered;
  Plain(MainActivity activity){this.activity=activity;callback=()->{reset();activity.goBack();};}
  void update(boolean enabled){if(registered==enabled)return;OnBackInvokedDispatcher dispatcher=activity.getOnBackInvokedDispatcher();if(enabled)dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,callback);else dispatcher.unregisterOnBackInvokedCallback(callback);registered=enabled;}
  void reset(){}
 }
 static final class Animated extends Plain {
  View target;
  int edge;
  Animated(MainActivity activity){super(activity);callback=new OnBackAnimationCallback(){
   @Override public void onBackStarted(BackEvent event){reset();target=activity.root;edge=event.getSwipeEdge();if(target!=null){target.animate().cancel();target.setPivotX(target.getWidth()/2f);target.setPivotY(target.getHeight()/2f);}}
   @Override public void onBackProgressed(BackEvent event){if(target==null||!ValueAnimator.areAnimatorsEnabled())return;float p=Math.max(0,Math.min(1,event.getProgress())),eased=1-(1-p)*(1-p)*(1-p);target.setScaleX(1-.035f*eased);target.setScaleY(1-.035f*eased);target.setTranslationX(activity.dp(20)*eased*(edge==BackEvent.EDGE_LEFT?1:-1));}
   @Override public void onBackCancelled(){if(target==null)return;View restored=target;if(!ValueAnimator.areAnimatorsEnabled()){reset();return;}restored.animate().scaleX(1).scaleY(1).translationX(0).setDuration(160).setInterpolator(new PathInterpolator(.2f,0,0,1)).withEndAction(()->{if(target==restored)target=null;}).start();}
   @Override public void onBackInvoked(){reset();activity.goBack();}
  };}
  @Override void reset(){if(target!=null){target.animate().cancel();target.setScaleX(1);target.setScaleY(1);target.setTranslationX(0);target=null;}}
 }
}

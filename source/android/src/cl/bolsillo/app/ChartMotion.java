package cl.bolsillo.app;
import android.animation.*;import android.graphics.Rect;import android.view.*;import java.util.function.Consumer;
/** Reveal a chart once it becomes visible and its page transition has settled. */
final class ChartMotion implements ViewTreeObserver.OnPreDrawListener {
 final View view;final Consumer<Float> draw;final Rect visible=new Rect();ViewTreeObserver tree;ValueAnimator animation;
 ChartMotion(View view,boolean meaningful,Consumer<Float> draw){this.view=view;this.draw=draw;if(meaningful&&ValueAnimator.areAnimatorsEnabled()){tree=view.getViewTreeObserver();tree.addOnPreDrawListener(this);}}
 public boolean onPreDraw(){if(!view.isShown()||!view.getGlobalVisibleRect(visible)||visible.width()==0||visible.height()==0)return true;if(view.getContext() instanceof MainActivity){MainActivity a=(MainActivity)view.getContext();if(a.dataLoading||a.scroll!=null&&PageMotion.running(a.scroll))return true;}unwatch();animation=ValueAnimator.ofFloat(0,1);animation.setDuration(320);animation.setInterpolator(PageMotion.EASE);animation.addUpdateListener(v->draw.accept((float)v.getAnimatedValue()));animation.start();return true;}
 void unwatch(){if(tree!=null&&tree.isAlive())tree.removeOnPreDrawListener(this);tree=null;}
 void cancel(){unwatch();if(animation!=null){animation.removeAllUpdateListeners();animation.cancel();animation=null;}}
}

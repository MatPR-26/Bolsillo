package cl.bolsillo.app;
import android.animation.ValueAnimator;
import android.view.*;
/** Small perspective response to an intentional touch; yields immediately to scrolling. */
final class CardTouch {
 static void apply(View card){
  card.setCameraDistance(card.getResources().getDisplayMetrics().density*8000);
  card.setOnTouchListener(new View.OnTouchListener(){float x,y;boolean moving;
   void reset(){card.animate().rotationX(0).rotationY(0).setDuration(ValueAnimator.areAnimatorsEnabled()?170:0).setInterpolator(PageMotion.EASE).start();}
   public boolean onTouch(View v,MotionEvent e){
    if(!ValueAnimator.areAnimatorsEnabled())return false;
    switch(e.getActionMasked()){
     case MotionEvent.ACTION_DOWN:x=e.getX();y=e.getY();moving=false;float nx=x/Math.max(1,card.getWidth())-.5f,ny=y/Math.max(1,card.getHeight())-.5f;card.animate().rotationX(-ny*2.4f).rotationY(nx*2.4f).setDuration(120).setInterpolator(PageMotion.EASE).start();break;
     case MotionEvent.ACTION_MOVE:if(!moving&&Math.hypot(e.getX()-x,e.getY()-y)>ViewConfiguration.get(v.getContext()).getScaledTouchSlop()){moving=true;reset();}break;
     case MotionEvent.ACTION_CANCEL:case MotionEvent.ACTION_UP:reset();break;
    }return false;
   }
  });
  card.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){v.animate().cancel();v.setRotationX(0);v.setRotationY(0);}});
 }
}

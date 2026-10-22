package cl.bolsillo.app;
import android.animation.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.*;import android.view.*;
/** Two quiet water rings from the actual touch point; geometry is clipped to the capsule. */
final class WaterRipple extends android.graphics.drawable.LayerDrawable {
 final Rings rings;
 WaterRipple(Context c,Drawable base){this(base,new Rings(c));}
 private WaterRipple(Drawable base,Rings rings){super(new Drawable[]{base,rings});this.rings=rings;}
 static void apply(View button){if(button.getBackground()!=null&&!(button.getBackground() instanceof WaterRipple))button.setBackground(new WaterRipple(button.getContext(),button.getBackground()));}
 @Override public void setHotspot(float x,float y){super.setHotspot(x,y);rings.x=x;rings.y=y;}
 @Override protected void onBoundsChange(Rect bounds){super.onBoundsChange(bounds);if(rings==null)return;Outline shape=new Outline();getDrawable(0).getOutline(shape);rings.radius=shape.getRadius()<0?Glass.dp(rings.c,18):shape.getRadius();rings.reclip();}
 static final class Rings extends Drawable {
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final Path clip=new Path();final Context c;float radius,x=-1,y=-1,progress=1;boolean pressed;ValueAnimator animation;
  Rings(Context c){this.c=c;}
  @Override public boolean isStateful(){return true;}
  @Override protected boolean onStateChange(int[] states){boolean down=false;for(int state:states)if(state==android.R.attr.state_pressed)down=true;if(down&&!pressed)start();pressed=down;return true;}
  void start(){if(animation!=null){animation.cancel();animation.removeAllUpdateListeners();}if(!ValueAnimator.areAnimatorsEnabled())return;animation=ValueAnimator.ofFloat(0,1);animation.setDuration(620);animation.setInterpolator(new android.view.animation.DecelerateInterpolator(.85f));animation.addUpdateListener(v->{progress=(Float)v.getAnimatedValue();invalidateSelf();});animation.start();}
  void reclip(){clip.reset();clip.addRoundRect(new RectF(getBounds()),radius,radius,Path.Direction.CW);}
  @Override protected void onBoundsChange(Rect bounds){reclip();}
  @Override public void draw(Canvas canvas){if(progress>=1)return;Rect bounds=getBounds();float cx=x<0?bounds.exactCenterX():x,cy=y<0?bounds.exactCenterY():y;int saved=canvas.save();canvas.clipPath(clip);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,c.getResources().getDisplayMetrics().density*.65f));int color=Glass.dark(c)?Glass.mix(Glass.primary(c),Color.WHITE,.65f):Glass.primary(c);float max=(float)Math.hypot(Math.max(cx-bounds.left,bounds.right-cx),Math.max(cy-bounds.top,bounds.bottom-cy));for(int i=0;i<2;i++){float p=(progress-i*.14f)/(1-i*.14f);if(p<0||p>1)continue;paint.setColor(Glass.alpha(color,(int)(50*(1-p)*(1-p))));canvas.drawCircle(cx,cy,Glass.dp(c,5)+max*p,paint);}canvas.restoreToCount(saved);}
  @Override public boolean setVisible(boolean visible,boolean restart){if(!visible&&animation!=null){animation.cancel();animation.removeAllUpdateListeners();progress=1;}return super.setVisible(visible,restart);}
  @Override public void setAlpha(int alpha){}@Override public void setColorFilter(ColorFilter color){}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
}

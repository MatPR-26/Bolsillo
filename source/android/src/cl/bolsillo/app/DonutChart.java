package cl.bolsillo.app;

import android.animation.*;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.function.IntConsumer;

/** Exact annular sectors with cached optical edges, restrained depth and real selection. */
final class DonutChart extends View {
 final long[] values;final int[] colors;final boolean night;final long total;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 final RectF area=new RectF(),outer=new RectF(),inner=new RectF();final float[] starts,sweeps;final Shader[] materials;
 final Path[] sectors,outerEdges,innerEdges;final float[] markerX,markerY,offsetX,offsetY;final Typeface medium,bold;
 final String[] valueLabels,percentLabels;String[] labels,centerLabels,accessibilityLabels;
 Shader centerMaterial,reflection;ChartMotion entrance;ValueAnimator selectionAnimator;
 float reveal=1f,selectionReveal=1f,cx,cy,r,stroke,labelSize,amountSize,percentSize,downX,downY;String amount;String period="3 MESES";
 int selected=-1;boolean dragged;IntConsumer selection;
 DonutChart(Context c,long[] amounts,int[] shades){
  super(c);medium=Glass.font(c,500);bold=Glass.font(c,600);night=Glass.dark(c);values=amounts.clone();colors=shades.clone();starts=new float[values.length];sweeps=new float[values.length];materials=new Shader[values.length];sectors=new Path[values.length];outerEdges=new Path[values.length];innerEdges=new Path[values.length];markerX=new float[values.length];markerY=new float[values.length];offsetX=new float[values.length];offsetY=new float[values.length];labels=new String[values.length];valueLabels=new String[values.length];percentLabels=new String[values.length];
  long sum=0;for(long value:values)if(value>0)sum=Math.addExact(sum,value);total=sum;float angle=-90;
  for(int i=0;i<values.length;i++){starts[i]=angle;sweeps[i]=total>0&&values[i]>0?(float)(360d*values[i]/total):0;angle+=sweeps[i];sectors[i]=new Path();outerEdges[i]=new Path();innerEdges[i]=new Path();labels[i]="Categoría "+(i+1);valueLabels[i]=money(c,values[i]);percentLabels[i]=SpendingCharts.percent(values[i],total);}
  amount=money(c,total);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setClickable(total>0);setFocusable(total>0);setLabels(labels);
 }
 private static String money(Context c,long value){if(c instanceof MainActivity&&((MainActivity)c).hidden)return "••••••";return BolsilloWidget.money(value);}
 void setLabels(String[] names){for(int i=0;i<labels.length;i++)if(names!=null&&i<names.length&&names[i]!=null)labels[i]=names[i];accessibilityLabels=new String[labels.length];for(int i=0;i<labels.length;i++)accessibilityLabels[i]=labels[i]+", "+valueLabels[i]+", "+percentLabels[i];setContentDescription("Distribución de gasto personal. Total "+amount+". "+String.join(". ",accessibilityLabels));if(getWidth()>0)cacheGeometry(getWidth(),getHeight());}
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);cacheGeometry(w,h);}
 private void cacheGeometry(int w,int h){
  cx=w/2f;cy=h/2f;float side=Math.min(w,h),dp=getResources().getDisplayMetrics().density,sp=getResources().getDisplayMetrics().scaledDensity;
  stroke=Math.min(31*dp,side*.17f);r=Math.max(1,side*.35f);area.set(cx-r,cy-r,cx+r,cy+r);float outside=r+stroke/2,inside=Math.max(1,r-stroke/2);outer.set(cx-outside,cy-outside,cx+outside,cy+outside);inner.set(cx-inside,cy-inside,cx+inside,cy+inside);
  for(int i=0;i<values.length;i++){
   int tone=colors.length==0?Glass.accent(getContext()):colors[i%colors.length];if(!night)tone=Glass.mix(tone,Glass.ink(getContext()),.18f);
   materials[i]=new LinearGradient(cx-outside,cy-outside,cx+outside,cy+outside,new int[]{Glass.mix(tone,Color.WHITE,.24f),tone,Glass.mix(tone,Glass.panel(getContext()),.14f)},new float[]{0,.53f,1},Shader.TileMode.CLAMP);
   Path sector=sectors[i];sector.reset();outerEdges[i].reset();innerEdges[i].reset();if(sweeps[i]<=0)continue;
   if(sweeps[i]>=359.999f){sector.addOval(outer,Path.Direction.CW);sector.addOval(inner,Path.Direction.CCW);}else{sector.arcTo(outer,starts[i],sweeps[i],true);sector.arcTo(inner,starts[i]+sweeps[i],-sweeps[i],false);sector.close();}
   outerEdges[i].arcTo(outer,starts[i],sweeps[i],true);innerEdges[i].arcTo(inner,starts[i],sweeps[i],true);
   double mid=Math.toRadians(starts[i]+sweeps[i]/2d);offsetX[i]=(float)Math.cos(mid)*3.5f*dp;offsetY[i]=(float)Math.sin(mid)*3.5f*dp;markerX[i]=cx+(float)Math.cos(mid)*(outside+5*dp);markerY[i]=cy+(float)Math.sin(mid)*(outside+5*dp);
  }
  int surface=Glass.panel(getContext());centerMaterial=new RadialGradient(cx-r*.26f,cy-r*.35f,Math.max(1,r),new int[]{Glass.alpha(Glass.mix(surface,Glass.primary(getContext()),night?.07f:.05f),220),Glass.alpha(surface,140)},null,Shader.TileMode.CLAMP);
  reflection=new LinearGradient(cx-outside,cy-outside,cx+outside,cy+outside,new int[]{night?0x82ffffff:0xccffffff,0x05ffffff,night?0x25ffffff:0x65ffffff},new float[]{0,.62f,1},Shader.TileMode.CLAMP);
  labelSize=Math.min(side>210*dp?12*sp:10*sp,side*.068f);amountSize=Math.min(side>210*dp?28*sp:18*sp,side*.128f);percentSize=Math.min(11*sp,side*.065f);p.setTypeface(bold);p.setTextSize(amountSize);float max=inside*1.78f;
  float widest=p.measureText(total==0?"Sin gastos":amount);for(String value:valueLabels)widest=Math.max(widest,p.measureText(value));if(widest>max)amountSize*=max/widest;
  android.text.TextPaint labelPaint=new android.text.TextPaint(p);labelPaint.setTypeface(medium);labelPaint.setTextSize(labelSize);centerLabels=new String[labels.length];for(int i=0;i<labels.length;i++)centerLabels[i]=android.text.TextUtils.ellipsize(labels[i],labelPaint,max,android.text.TextUtils.TruncateAt.END).toString();
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();entrance=new ChartMotion(this,total>0,value->{reveal=value;invalidate();});}
 @Override protected void onDetachedFromWindow(){if(entrance!=null){entrance.cancel();entrance=null;}if(selectionAnimator!=null){selectionAnimator.cancel();selectionAnimator=null;}reveal=selectionReveal=1;super.onDetachedFromWindow();}
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);if(r<=0)return;float dp=getResources().getDisplayMetrics().density;canvas.save();float scale=.975f+.025f*reveal;canvas.scale(scale,scale,cx,cy);int opacity=(int)(180+75*reveal);
  p.setShader(null);p.setAlpha(255);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(stroke);p.setColor(night?0xff263146:0xffe2e8f0);canvas.drawOval(area,p);
  for(int i=0;i<values.length;i++)if(sweeps[i]>0){canvas.save();if(i==selected)canvas.translate(offsetX[i]*selectionReveal,offsetY[i]*selectionReveal);p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(night?0x44000000:0x1b172238);canvas.save();canvas.translate(0,1.5f*dp);canvas.drawPath(sectors[i],p);canvas.restore();p.setShader(materials[i]);p.setAlpha(selected>=0&&i!=selected?(int)(opacity*.66f):opacity);canvas.drawPath(sectors[i],p);p.setAlpha(255);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,.75f*dp));p.setShader(reflection);canvas.drawPath(outerEdges[i],p);p.setShader(null);p.setColor(night?0x48202b43:0x4267778c);canvas.drawPath(innerEdges[i],p);canvas.restore();}
  // Separators are an optical stroke over shared logical boundaries, capped for tiny slices.
  if(total>0){int positive=0;for(float sweep:sweeps)if(sweep>0)positive++;if(positive>1)for(int i=0;i<sweeps.length;i++)if(sweeps[i]>0){int before=i;do{before=(before+sweeps.length-1)%sweeps.length;}while(sweeps[before]<=0);float adjacent=Math.min(sweeps[i],sweeps[before]);if(adjacent<.18f)continue;float width=Math.min(1.7f*dp,r*(float)Math.toRadians(adjacent)*.14f);double angle=Math.toRadians(starts[i]);float x=(float)Math.cos(angle),y=(float)Math.sin(angle);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.BUTT);p.setStrokeWidth(Math.max(.25f,width));p.setColor(night?0xff151e30:0xffffffff);canvas.drawLine(cx+x*(r-stroke/2+.25f*dp),cy+y*(r-stroke/2+.25f*dp),cx+x*(r+stroke/2-.25f*dp),cy+y*(r+stroke/2-.25f*dp),p);}}
  p.setStyle(Paint.Style.FILL);p.setShader(centerMaterial);p.setAlpha(255);canvas.drawCircle(cx,cy,Math.max(0,r-stroke*.57f),p);p.setShader(null);
  if(selected>=0){p.setColor(night?0xff151e30:Color.WHITE);canvas.drawCircle(markerX[selected],markerY[selected],4.5f*dp,p);p.setColor(colors.length==0?Glass.accent(getContext()):colors[selected%colors.length]);canvas.drawCircle(markerX[selected],markerY[selected],2.5f*dp,p);}
  p.setTextAlign(Paint.Align.CENTER);p.setTypeface(medium);p.setColor(Glass.subtle(getContext()));p.setTextSize(labelSize);String heading=selected>=0&&centerLabels!=null?centerLabels[selected]:period;canvas.drawText(heading,cx,cy-amountSize*.68f,p);p.setTypeface(bold);p.setColor(Glass.ink(getContext()));p.setTextSize(amountSize);canvas.drawText(total==0?"Sin gastos":selected>=0?valueLabels[selected]:amount,cx,cy+amountSize*.36f,p);
  if(selected>=0){p.setTypeface(medium);p.setColor(Glass.subtle(getContext()));p.setTextSize(percentSize);canvas.drawText(percentLabels[selected]+" del total",cx,cy+amountSize*.36f+percentSize*1.7f,p);}canvas.restore();
 }
 int hit(float x,float y){double distance=Math.hypot(x-cx,y-cy);if(distance<r-stroke*.75f||distance>r+stroke*.85f)return -1;double angle=Math.toDegrees(Math.atan2(y-cy,x-cx));while(angle< -90)angle+=360;for(int i=0;i<sweeps.length;i++)if(sweeps[i]>0&&angle>=starts[i]&&angle<starts[i]+sweeps[i])return i;return -1;}
 void select(int index,boolean announce){if(index< -1||index>=values.length||index>=0&&values[index]<=0)return;if(selected==index)return;selected=index;if(selectionAnimator!=null)selectionAnimator.cancel();selectionReveal=1;if(index>=0&&ValueAnimator.areAnimatorsEnabled()&&isAttachedToWindow()){selectionReveal=0;selectionAnimator=ValueAnimator.ofFloat(0,1);selectionAnimator.setDuration(160);selectionAnimator.setInterpolator(PageMotion.EASE);selectionAnimator.addUpdateListener(a->{selectionReveal=(float)a.getAnimatedValue();invalidate();});selectionAnimator.start();}invalidate();if(index>=0&&announce)announceForAccessibility(accessibilityLabels[index]);if(selection!=null)selection.accept(index);}
 @Override public boolean onTouchEvent(MotionEvent e){if(total==0)return super.onTouchEvent(e);switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:downX=e.getX();downY=e.getY();dragged=false;return hit(downX,downY)>=0;case MotionEvent.ACTION_MOVE:if(Math.hypot(e.getX()-downX,e.getY()-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop())dragged=true;return true;case MotionEvent.ACTION_UP:if(!dragged){int part=hit(e.getX(),e.getY());if(part>=0){select(part,true);performClick();}}return true;case MotionEvent.ACTION_CANCEL:dragged=true;return true;default:return true;}}
 @Override public boolean performClick(){super.performClick();return true;}
 private int next(int direction){if(values.length==0)return -1;int index=selected<0?(direction>0?-1:0):selected;for(int count=0;count<values.length;count++){index=(index+direction+values.length)%values.length;if(values[index]>0)return index;}return -1;}
 @Override public boolean onKeyDown(int key,KeyEvent event){if(key==KeyEvent.KEYCODE_DPAD_RIGHT||key==KeyEvent.KEYCODE_DPAD_LEFT){select(next(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1),true);return true;}return super.onKeyDown(key,event);}
 @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);if(total>0){info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}}
 @Override public boolean performAccessibilityAction(int action,Bundle args){if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD||action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){select(next(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD?1:-1),true);return true;}return super.performAccessibilityAction(action,args);}
}

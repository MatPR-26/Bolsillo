package cl.bolsillo.app;
import android.animation.ValueAnimator;import android.content.*;import android.graphics.*;import android.view.*;

/** Exact contiguous shares: lighting changes the material, never the size of a category. */
final class DonutChart extends View {
 final long[] values;final int[] colors;final boolean night;final long total;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 final RectF area=new RectF(),outer=new RectF(),inner=new RectF();final float[] starts,sweeps;final Shader[] materials;
 final Typeface medium,bold;
 Shader centerMaterial,reflection;ValueAnimator animation;float reveal=1f,cx,cy,r,stroke,labelSize,amountSize;String amount;String period="3 MESES";
 DonutChart(Context c,long[] amounts,int[] shades){
  super(c);medium=Glass.font(c,500);bold=Glass.font(c,600);night=Glass.dark(c);values=amounts.clone();colors=shades.clone();starts=new float[values.length];sweeps=new float[values.length];materials=new Shader[values.length];long sum=0;for(long value:values)if(value>0)sum+=value;total=sum;
  float angle=-90;for(int i=0;i<values.length;i++){starts[i]=angle;sweeps[i]=total>0&&values[i]>0?(float)(360d*values[i]/total):0;angle+=sweeps[i];}
  boolean hidden=c instanceof MainActivity&&((MainActivity)c).hidden;amount=hidden?"••••••":total>=1000000?String.format(java.util.Locale.forLanguageTag("es-CL"),"$%.1f M",total/1000000d):total>=100000?"$"+Math.round(total/1000d)+" mil":BolsilloWidget.money(total);
  setContentDescription("Distribución de gastos de tres meses. Total: "+(hidden?"importe oculto":BolsilloWidget.money(total)));setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
 }
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
  super.onSizeChanged(w,h,oldw,oldh);cx=w/2f;cy=h/2f;float side=Math.min(w,h);stroke=Math.min(Glass.dp(getContext(),19),side*.155f);r=Math.max(1,side*.355f);area.set(cx-r,cy-r,cx+r,cy+r);float outside=r+stroke/2,inside=r-stroke/2;outer.set(cx-outside,cy-outside,cx+outside,cy+outside);inner.set(cx-inside,cy-inside,cx+inside,cy+inside);
  for(int i=0;i<values.length;i++){int color=colors.length==0?Glass.accent(getContext()):colors[i%colors.length];int tone=night?color:Glass.mix(color,Glass.ink(getContext()),.12f);materials[i]=new LinearGradient(0,cy-outside,0,cy+outside,new int[]{Glass.mix(tone,Color.WHITE,.36f),tone,Glass.mix(tone,Glass.secondary(getContext()),.12f)},new float[]{0,.48f,1},Shader.TileMode.CLAMP);}
  int surface=Glass.panel(getContext());centerMaterial=new RadialGradient(cx-r*.26f,cy-r*.32f,Math.max(1,r),new int[]{Glass.mix(surface,Glass.primary(getContext()),night?.10f:.14f),Glass.alpha(surface,155)},null,Shader.TileMode.CLAMP);reflection=new LinearGradient(0,cy-outside,0,cy+outside,new int[]{0x95ffffff,0x00ffffff,0x20ffffff},new float[]{0,.56f,1},Shader.TileMode.CLAMP);
  float sp=getResources().getDisplayMetrics().scaledDensity;labelSize=Math.min(9*sp,side*.075f);amountSize=Math.min(15*sp,side*.115f);p.setTypeface(bold);p.setTextSize(amountSize);while(p.measureText(amount)>inside*1.7f&&amountSize>8*sp){amountSize-=.5f;p.setTextSize(amountSize);}
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(total>0&&ValueAnimator.areAnimatorsEnabled()){reveal=0;animation=ValueAnimator.ofFloat(0,1);animation.setDuration(600);animation.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));animation.addUpdateListener(v->{reveal=(float)v.getAnimatedValue();invalidate();});animation.start();}else reveal=1;}
 @Override protected void onDetachedFromWindow(){if(animation!=null){animation.cancel();animation.removeAllUpdateListeners();animation=null;}reveal=1;super.onDetachedFromWindow();}
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);if(r<=0)return;canvas.save();float scale=.93f+.07f*reveal;canvas.scale(scale,scale,cx,cy);int opacity=(int)(130+125*reveal);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.BUTT);p.setStrokeWidth(stroke+Glass.dp(getContext(),3));p.setColor(night?0x38000000:0x12636c91);canvas.save();canvas.translate(0,Glass.dp(getContext(),2));canvas.drawOval(area,p);canvas.restore();
  p.setStrokeWidth(stroke);p.setColor(night?0xff2d3550:0xffdfe2ee);canvas.drawOval(area,p);for(int i=0;i<values.length;i++)if(sweeps[i]>0){p.setShader(materials[i]);p.setAlpha(opacity);canvas.drawArc(area,starts[i],sweeps[i],false,p);}p.setShader(null);p.setAlpha(255);
  p.setStyle(Paint.Style.FILL);p.setShader(centerMaterial);canvas.drawCircle(cx,cy,Math.max(0,r-stroke*.64f),p);p.setShader(null);
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,Glass.dp(getContext(),1)*.7f));p.setShader(reflection);canvas.drawOval(outer,p);p.setShader(null);p.setColor(night?0x60202a45:0x438a94ae);canvas.drawOval(inner,p);
  p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(medium);p.setColor(Glass.subtle(getContext()));p.setTextSize(labelSize);canvas.drawText(period,cx,cy-Glass.dp(getContext(),7),p);p.setTypeface(bold);p.setColor(Glass.ink(getContext()));p.setTextSize(amountSize);canvas.drawText(amount,cx,cy+Glass.dp(getContext(),14),p);canvas.restore();
 }
}
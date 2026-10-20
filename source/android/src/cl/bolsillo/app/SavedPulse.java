package cl.bolsillo.app;
import android.animation.*;import android.content.*;import android.graphics.*;import android.view.*;
/** Short, quiet confirmation with concentric light rings; no text glyph used as an icon. */
final class SavedPulse extends View {
 final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);float progress;ValueAnimator pulse;
 SavedPulse(Context c){super(c);setContentDescription("Movimiento guardado");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(ValueAnimator.areAnimatorsEnabled()){pulse=ValueAnimator.ofFloat(0,1);pulse.setDuration(540);pulse.setInterpolator(PageMotion.EASE);pulse.addUpdateListener(v->{progress=(float)v.getAnimatedValue();invalidate();});pulse.start();}else progress=1;}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float center=getWidth()/2f,cy=getHeight()/2f,r=Glass.dp(getContext(),7);paint.setColor(Glass.accent(getContext()));paint.setStyle(Paint.Style.FILL);canvas.drawCircle(center-Glass.dp(getContext(),58),cy,r*(.75f+.25f*progress),paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Glass.dp(getContext(),1));paint.setAlpha((int)(120*(1-progress)));canvas.drawCircle(center-Glass.dp(getContext(),58),cy,r+Glass.dp(getContext(),16)*progress,paint);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);paint.setTypeface(Glass.font(getContext(),600));paint.setTextSize(16*getResources().getDisplayMetrics().scaledDensity);paint.setColor(Glass.ink(getContext()));canvas.drawText("Guardado",center-Glass.dp(getContext(),39),cy-(paint.ascent()+paint.descent())/2,paint);}
 @Override protected void onDetachedFromWindow(){if(pulse!=null){pulse.cancel();pulse.removeAllUpdateListeners();}super.onDetachedFromWindow();}
}

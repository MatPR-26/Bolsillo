package cl.bolsillo.app;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ReplacementSpan;
import android.widget.EditText;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Localized presentation; the underlying editable remains the existing ISO date. */
final class DateInput {
 static final DateTimeFormatter DISPLAY=DateTimeFormatter.ofPattern("d 'de' MMMM 'de' uuuu",new Locale("es","CL"));
 static String friendly(String raw){try{return LocalDate.parse(raw).format(DISPLAY);}catch(Exception invalid){return raw;}}
 static void apply(MainActivity a,EditText field,String label){Icons.Line icon=new Icons.Line(a,"calendar",Glass.accent(a));icon.setBounds(0,0,a.dp(21),a.dp(21));Icons.Line arrow=new Icons.Line(a,"chevron",Glass.subtle(a));arrow.setBounds(0,0,a.dp(16),a.dp(16));field.setCompoundDrawablesRelative(icon,null,arrow,null);field.setCompoundDrawablePadding(a.dp(10));field.setHint("Elegir fecha");field.setCursorVisible(false);field.setTextIsSelectable(false);field.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){}public void afterTextChanged(Editable text){present(field,label,text);}});present(field,label,field.getText());}
 static void present(EditText field,String label,Editable text){for(DisplayDate span:text.getSpans(0,text.length(),DisplayDate.class))text.removeSpan(span);String raw=text.toString(),display=friendly(raw);if(!raw.isEmpty()&&!display.equals(raw))text.setSpan(new DisplayDate(display),0,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);field.setContentDescription(label+(raw.isEmpty()?". Elegir fecha":". "+display)+(label.toLowerCase(Locale.ROOT).contains("opcional")?". Mantén pulsado para borrar":""));}
 static final class DisplayDate extends ReplacementSpan {
  final String display;
  DisplayDate(String display){this.display=display;}
  @Override public int getSize(Paint paint,CharSequence raw,int start,int end,Paint.FontMetricsInt metrics){if(metrics!=null)paint.getFontMetricsInt(metrics);return (int)Math.ceil(paint.measureText(display));}
  @Override public void draw(Canvas canvas,CharSequence raw,int start,int end,float x,int top,int baseline,int bottom,Paint paint){canvas.drawText(display,x,baseline,paint);}
 }
}

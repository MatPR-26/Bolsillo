package cl.bolsillo.app;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.Locale;

/** Optional personal colours; a cancelled preview never changes the current theme. */
final class CustomPalette {
 static SheetDialog show(MainActivity a){
  int[] colors={NativeStore.settings(a).getInt("customPrimary",0xff4dcfb0),NativeStore.settings(a).getInt("customSecondary",0xffa384e8)};int[] selected={0};boolean[] updating={false};
  LinearLayout box=NativeSettings.panel(a,"Tus colores");SheetDialog dialog=new SheetDialog(a,box);box.setFocusableInTouchMode(true);box.requestFocus();LinearLayout tabs=Modern.row(a);TextView[] pills={Glass.pill(a,"Acento",true),Glass.pill(a,"Reflejos",false)};for(TextView pill:pills)tabs.addView(pill,new LinearLayout.LayoutParams(0,-2,1));Modern.add(box,tabs,12);
  LinearLayout preview=Modern.column(a);preview.setPadding(a.dp(20),a.dp(20),a.dp(20),a.dp(20));AccountGlass material=new AccountGlass(a,colors[0]);preview.setBackground(material);preview.addView(Modern.text(a,"Así se verá tu cristal",18,Glass.ink(a),true));Modern.add(preview,Modern.text(a,"Color con contraste adaptado",13,Glass.subtle(a),false),8);Modern.add(box,preview,16);
  TextView hexLabel=Modern.text(a,"Color",12,Glass.subtle(a),false);Modern.add(box,hexLabel,20);EditText hex=NativeSettings.input(a,box,"HEX · #RRGGBB","");hex.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);hex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
  TextView hueLabel=Modern.text(a,"Tono",12,Glass.subtle(a),false);box.addView(hueLabel);SeekBar hue=new SeekBar(a);hue.setMax(359);hue.setContentDescription("Tono del color");hue.setSplitTrack(false);hue.setMinimumHeight(a.dp(48));GradientDrawable rainbow=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xffff5b5b,0xffffdd5b,0xff60db78,0xff5bd6e5,0xff667de8,0xffcc6ae5,0xffff5b5b});rainbow.setCornerRadius(a.dp(4));hue.setProgressDrawable(rainbow);hue.setPadding(a.dp(12),0,a.dp(12),0);box.addView(hue,new LinearLayout.LayoutParams(-1,a.dp(48)));
  TextView satLabel=Modern.text(a,"Saturación",12,Glass.subtle(a),false);box.addView(satLabel);SeekBar sat=new SeekBar(a);sat.setMax(100);sat.setContentDescription("Saturación del color");sat.setMinimumHeight(a.dp(48));sat.setProgressTintList(ColorStateList.valueOf(Glass.accent(a)));box.addView(sat,new LinearLayout.LayoutParams(-1,a.dp(48)));
  Runnable paint=()->{updating[0]=true;float[] hsv=new float[3];Color.colorToHSV(colors[selected[0]],hsv);hue.setProgress(Math.round(hsv[0]));sat.setProgress(Math.round(hsv[1]*100));hex.setText(String.format(Locale.ROOT,"#%06X",colors[selected[0]]&0xffffff));GradientDrawable thumb=Glass.round(a,colors[selected[0]],12);thumb.setSize(a.dp(24),a.dp(24));thumb.setStroke(a.dp(2),Glass.ink(a));hue.setThumb(thumb);GradientDrawable satThumb=Glass.round(a,colors[selected[0]],12);satThumb.setSize(a.dp(24),a.dp(24));satThumb.setStroke(a.dp(2),Glass.ink(a));sat.setThumb(satThumb);material.palette(colors[0],colors[1]);Glass.selectPill(a,pills[0],selected[0]==0);Glass.selectPill(a,pills[1],selected[0]==1);updating[0]=false;};
  for(int i=0;i<2;i++){final int index=i;Modern.press(pills[i],()->{selected[0]=index;paint.run();});}
  SeekBar.OnSeekBarChangeListener slides=new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int progress,boolean user){if(!user||updating[0])return;float[] hsv=new float[3];Color.colorToHSV(colors[selected[0]],hsv);hsv[0]=hue.getProgress();hsv[1]=sat.getProgress()/100f;hsv[2]=Math.max(.55f,hsv[2]);colors[selected[0]]=Color.HSVToColor(hsv);paint.run();}};hue.setOnSeekBarChangeListener(slides);sat.setOnSeekBarChangeListener(slides);
  hex.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int start,int before,int count){if(updating[0])return;String value=s.toString();if(value.matches("#[0-9A-Fa-f]{6}")){colors[selected[0]]=Color.parseColor(value);paint.run();hex.setSelection(hex.length());hex.setError(null);}}});
  dialog.actions("Aplicar colores",()->{if(!hex.getText().toString().matches("#[0-9A-Fa-f]{6}")){hex.setError("Usa un color como #7D64D8");return;}NativeStore.settings(a).edit().putInt("customPrimary",colors[0]).putInt("customSecondary",colors[1]).putString("palette","custom").apply();dialog.dismiss();a.render(false);BolsilloWidget.updateAll(a);});paint.run();dialog.show();return dialog;
 }
}

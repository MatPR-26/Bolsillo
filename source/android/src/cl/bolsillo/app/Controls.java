package cl.bolsillo.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.Layout;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;

/** Material-sized native controls. Checked state, listeners, keyboard/drag behaviour and
 * accessibility remain owned by Android's Switch and CheckBox; only their skin changes.
 * Android touch-target guidance: developer.android.com/guide/topics/ui/accessibility/apps
 * Material switch tokens: github.com/material-components/material-components-android
 */
final class Controls {
 private Controls(){}
 private static final int ENABLED=android.R.attr.state_enabled;
 private static final int CHECKED=android.R.attr.state_checked;
 private static final int PRESSED=android.R.attr.state_pressed;

 static void style(Switch view){
  Context c=view.getContext();label(view,500,52);
  view.setShowText(false);view.setSplitTrack(false);view.setSwitchMinWidth(Glass.dp(c,52));
  view.setSwitchPadding(Glass.dp(c,16));view.setThumbTextPadding(0);
  // Clear framework/theme tint before attaching independent stateful drawables.
  view.setTrackTintList(null);view.setThumbTintList(null);
  view.setTrackDrawable(tracks(c));view.setThumbDrawable(thumbs(c));
 }

 static void style(CheckBox view){
  Context c=view.getContext();label(view,400,48);
  view.setButtonTintList(null);view.setButtonDrawable(checks(c));
 }

 private static void label(CompoundButton view,int weight,int height){
  Context c=view.getContext();view.setTypeface(Glass.font(c,weight));view.setTextSize(15);
  view.setTextColor(new ColorStateList(new int[][]{{-ENABLED},{}},new int[]{Glass.mix(Glass.panel(c),Glass.ink(c),.54f),Glass.ink(c)}));
  view.setAllCaps(false);view.setIncludeFontPadding(false);view.setLetterSpacing(0);
  view.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);view.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
  view.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);view.setCompoundDrawablePadding(Glass.dp(c,10));
  view.setMinHeight(Math.max(view.getMinHeight(),Glass.dp(c,height)));
  view.setMinimumHeight(Math.max(view.getMinimumHeight(),Glass.dp(c,height)));
  view.setMinWidth(Math.max(view.getMinWidth(),Glass.dp(c,48)));
  view.setMinimumWidth(Math.max(view.getMinimumWidth(),Glass.dp(c,48)));
  // Preserve deliberately larger paddings and explicit dimensions; grow a too-small hit area.
  int start=Math.max(view.getPaddingStart(),Glass.dp(c,4)),end=Math.max(view.getPaddingEnd(),Glass.dp(c,4));
  view.setPaddingRelative(start,Math.max(view.getPaddingTop(),Glass.dp(c,8)),end,Math.max(view.getPaddingBottom(),Glass.dp(c,8)));
  ViewGroup.LayoutParams size=view.getLayoutParams();
  if(size!=null){if(size.height>0&&size.height<Glass.dp(c,48))size.height=Glass.dp(c,48);if(size.width>0&&size.width<Glass.dp(c,48))size.width=Glass.dp(c,48);view.setLayoutParams(size);}
  ColorStateList rippleColor=new ColorStateList(new int[][]{{-ENABLED},{}},new int[]{Color.TRANSPARENT,Glass.alpha(Glass.accent(c),38)});
  Drawable old=view.getBackground();
  if(old instanceof RippleDrawable)((RippleDrawable)old).setColor(rippleColor);
  else view.setBackground(new RippleDrawable(rippleColor,old,Glass.round(c,Color.WHITE,12)));
 }

 private static StateListDrawable tracks(Context c){
  StateListDrawable states=new StateListDrawable();
  states.addState(new int[]{-ENABLED,CHECKED},track(c,Glass.mix(Glass.panel(c),Glass.accent(c),.24f),0));
  states.addState(new int[]{-ENABLED},track(c,Glass.mix(Glass.panel(c),Glass.ink(c),.035f),Glass.mix(Glass.panel(c),Glass.ink(c),.27f)));
  states.addState(new int[]{CHECKED},track(c,Glass.accent(c),0));
  states.addState(new int[]{},track(c,Glass.panel(c),Glass.mix(Glass.panel(c),Glass.subtle(c),.82f)));
  return states;
 }

 private static Drawable track(Context c,int fill,int stroke){
  Track drawable=new Track(c);drawable.setColor(fill);drawable.setCornerRadius(Glass.dp(c,16));
  drawable.setSize(Glass.dp(c,52),Glass.dp(c,32));
  if(stroke!=0)drawable.setStroke(Glass.dp(c,2),stroke);
  return drawable;
 }

 private static StateListDrawable thumbs(Context c){
  StateListDrawable states=new StateListDrawable();
  states.addState(new int[]{-ENABLED,CHECKED},thumb(c,true,false,false));
  states.addState(new int[]{-ENABLED},thumb(c,false,false,false));
  states.addState(new int[]{CHECKED},thumb(c,true,true,false));
  states.addState(new int[]{PRESSED},thumb(c,false,true,true));
  states.addState(new int[]{},thumb(c,false,true,false));
  return states;
 }

 private static Drawable thumb(Context c,boolean checked,boolean enabled,boolean pressed){
  int fill=enabled?(checked?(Glass.dark(c)?0xff203332:Color.WHITE):Glass.subtle(c)):Glass.mix(Glass.panel(c),Glass.ink(c),.43f);
  int diameter=checked?24:pressed?20:16;
  GradientDrawable circle=new GradientDrawable();circle.setShape(GradientDrawable.OVAL);circle.setColor(fill);
  SizedLayers layers=new SizedLayers(c,24,32,checked?new Drawable[]{circle,new Icons.Line(c,"check",enabled?Glass.accent(c):Glass.panel(c))}:new Drawable[]{circle});
  layers.center(0,Glass.dp(c,diameter),Glass.dp(c,diameter));
  if(checked)layers.center(1,Glass.dp(c,16),Glass.dp(c,16));
  return layers;
 }

 private static StateListDrawable checks(Context c){
  StateListDrawable states=new StateListDrawable();
  states.addState(new int[]{-ENABLED,CHECKED},check(c,true,false));
  states.addState(new int[]{-ENABLED},check(c,false,false));
  states.addState(new int[]{CHECKED},check(c,true,true));
  states.addState(new int[]{},check(c,false,true));
  return states;
 }

 private static Drawable check(Context c,boolean checked,boolean enabled){
  GradientDrawable square=new GradientDrawable();square.setCornerRadius(Glass.dp(c,3));
  square.setColor(checked?(enabled?Glass.accent(c):Glass.mix(Glass.panel(c),Glass.accent(c),.24f)):Color.TRANSPARENT);
  if(!checked)square.setStroke(Glass.dp(c,2),enabled?Glass.subtle(c):Glass.mix(Glass.panel(c),Glass.ink(c),.30f));
  int checkColor=enabled?(Glass.dark(c)?0xff203332:Color.WHITE):Glass.mix(Glass.panel(c),Glass.ink(c),.54f);
  SizedLayers layers=new SizedLayers(c,32,24,checked?new Drawable[]{square,new Icons.Line(c,"check",checkColor)}:new Drawable[]{square});
  layers.center(0,Glass.dp(c,18),Glass.dp(c,18));
  if(checked)layers.center(1,Glass.dp(c,15),Glass.dp(c,15));
  return layers;
 }

 /** Optical inset keeps a selected thumb inside the native 52dp track. */
 private static final class Track extends GradientDrawable {
  final int inset;
  Track(Context c){inset=Glass.dp(c,2);}
  @Override public boolean getPadding(Rect padding){padding.set(inset,0,inset,0);return true;}
 }

 /** Fixed transparent slot, so Android can animate the thumb without stretching its circle. */
 private static final class SizedLayers extends LayerDrawable {
  final int width,height;
  SizedLayers(Context c,int width,int height,Drawable[] parts){super(parts);this.width=Glass.dp(c,width);this.height=Glass.dp(c,height);setPaddingMode(PADDING_MODE_STACK);}
  void center(int layer,int width,int height){setLayerSize(layer,width,height);setLayerGravity(layer,Gravity.CENTER);}
  @Override public int getIntrinsicWidth(){return width;}
  @Override public int getIntrinsicHeight(){return height;}
  // Switch/CompoundButton reapply even a cleared theme tint to newly attached drawables.
  // Each layer already owns its palette colour; preserve the contrasting official check.
  @Override public void setTintList(ColorStateList tint){}
 }
}

package cl.bolsillo.app;
import android.app.*;import android.content.*;import android.content.res.Configuration;import android.graphics.*;import android.graphics.drawable.*;import android.os.Build;import android.view.*;import android.widget.*;

/** Native glass tokens: reflections, semantic colours and adaptive contrast. */
public final class Glass {
 static Typeface regularFont;static final java.util.Map<Integer,Typeface> weights=new java.util.HashMap<>();
 static synchronized Typeface font(Context c,int weight){if(regularFont==null)regularFont=c.getResources().getFont(R.font.manrope);Typeface found=weights.get(weight);if(found==null){found=Build.VERSION.SDK_INT>=28?Typeface.create(regularFont,weight,false):Typeface.create(regularFont,weight>=600?Typeface.BOLD:Typeface.NORMAL);weights.put(weight,found);}return found;}
 static final int MINT=0xff70edc4,VIOLET=0xffb1a0ff,CORAL=0xffff929f,BLUE=0xff86bfff,AMBER=0xffffce82;
 static boolean dark(Context c){String mode=NativeStore.settings(c).getString("appearance","dark");return mode.equals("dark")||mode.equals("system")&&(c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;}
 static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 static void editor(EditText field){field.setImeOptions(field.getImeOptions()|android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN);}
 static String palette(Context c){return NativeStore.settings(c).getString("palette","prisma");}
 static int intensity(Context c){return Math.max(0,Math.min(2,NativeStore.settings(c).getInt("glassIntensity",1)));}
 static int primary(Context c){switch(palette(c)){case "ocean":return 0xff7cd4fa;case "amber":return 0xffffcd91;default:return 0xff99e8de;}}
 static int secondary(Context c){switch(palette(c)){case "ocean":return 0xff7399f7;case "amber":return 0xffec929f;default:return 0xffad93fb;}}
 static int ink(Context c){return dark(c)?0xfff6f4ff:0xff20233c;}
 static int subtle(Context c){return dark(c)?0xffb3b6ce:0xff61647d;}
 /** Semantic foregrounds retain contrast on pale glass; brand assets keep their own colours. */
 static int semantic(Context c,int color){if(dark(c))return color;if(color==MINT)return 0xff16785f;if(color==VIOLET)return 0xff6450ac;if(color==CORAL)return 0xffaa3c52;if(color==BLUE)return 0xff306898;if(color==AMBER)return 0xff86560b;return color;}
 static int accent(Context c){if(dark(c))return primary(c);switch(palette(c)){case "ocean":return 0xff086793;case "amber":return 0xff935226;default:return 0xff256f6a;}}
 static int panel(Context c){return dark(c)?0xff1d243c:0xffeef0fa;}
 static int alpha(int color,int a){return (color&0xffffff)|(Math.max(0,Math.min(255,a))<<24);}
 static int mix(int from,int to,float n){return Color.rgb((int)(Color.red(from)*(1-n)+Color.red(to)*n),(int)(Color.green(from)*(1-n)+Color.green(to)*n),(int)(Color.blue(from)*(1-n)+Color.blue(to)*n));}
 static Drawable surface(Context c,int alpha){return new GlassMaterial(c,alpha,28,secondary(c));}
 static Drawable material(Context c,int alpha,int radius,int tint){return new GlassMaterial(c,alpha,radius,tint);}
 static GradientDrawable gradient(Context c,int from,int to,int radius){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{from,to});g.setCornerRadius(dp(c,radius));g.setStroke(dp(c,1),0x65ffffff);return g;}
 static Drawable backdrop(Context c){return new PrismaBackdrop(c);}
 static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(1);l.setPadding(dp(c,20),dp(c,20),dp(c,20),dp(c,20));return l;}
 static TextView text(Context c,String label,int size,boolean bold){TextView v=new TextView(c);v.setText(label);v.setTextSize(size);v.setTextColor(ink(c));v.setFontFeatureSettings("tnum");v.setTypeface(font(c,bold?600:400));v.setLetterSpacing(size>=20?-.025f:0);v.setPadding(0,dp(c,3),0,dp(c,3));v.setIncludeFontPadding(false);v.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);v.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);return v;}
 static Button button(Context c,String label){Button b=new Button(c);b.setText(label);b.setTextSize(15);b.setAllCaps(false);b.setTypeface(font(c,600));boolean secondary=label.equals("Cancelar")||label.equals("Volver")||label.equals("Cerrar")||label.startsWith("Eliminar")||label.startsWith("Descartar")||label.startsWith("Más opciones")||label.startsWith("Menos opciones");b.setTextColor(secondary?ink(c):0xff162c34);Drawable bg=secondary?material(c,238,18,secondary(c)):gradient(c,mix(primary(c),Color.WHITE,.22f),primary(c),18);b.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(alpha(primary(c),55)),bg,null));b.setMinHeight(dp(c,52));b.setMinimumHeight(dp(c,52));b.setPadding(dp(c,16),dp(c,14),dp(c,16),dp(c,14));b.setStateListAnimator(null);b.setTranslationZ(0);b.setElevation(0);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(c,12);b.setLayoutParams(lp);return b;}
 static GradientDrawable round(Context c,int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(c,radius));return g;}
 static TextView pill(Context c,String label,boolean selected){TextView t=text(c,label,13,true);t.setGravity(Gravity.CENTER);t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setAutoSizeTextTypeUniformWithConfiguration(10,13,1,android.util.TypedValue.COMPLEX_UNIT_SP);t.setMinHeight(dp(c,44));t.setPadding(dp(c,12),dp(c,9),dp(c,12),dp(c,9));selectPill(c,t,selected);return t;}
 static void selectPill(Context c,TextView t,boolean selected){t.setTextColor(selected?accent(c):subtle(c));t.setBackground(selected?material(c,238,16,primary(c)):round(c,Color.TRANSPARENT,16));}
 static void privacy(Window w,Context c){if(w==null)return;if(NativeStore.settings(c).getBoolean("privateScreen",false))w.addFlags(WindowManager.LayoutParams.FLAG_SECURE);else w.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);}
 static void window(Dialog d,Context c){Window w=d.getWindow();if(w==null)return;privacy(w,c);w.setBackgroundDrawable(surface(c,242));w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setDimAmount(.42f);w.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);WindowManager.LayoutParams attrs=w.getAttributes();attrs.y=dp(c,12);w.setAttributes(attrs);w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);int width=Math.min(dp(c,480),c.getResources().getDisplayMetrics().widthPixels-dp(c,16));w.setLayout(width,-2);w.getDecorView().setElevation(dp(c,8));if(Build.VERSION.SDK_INT>=31){w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);WindowManager.LayoutParams p=w.getAttributes();p.setBlurBehindRadius(dp(c,intensity(c)==0?0:18));w.setAttributes(p);WindowManager manager=c.getSystemService(WindowManager.class);java.util.function.Consumer<Boolean> changed=enabled->{w.setBackgroundDrawable(surface(c,enabled?232:255));w.setDimAmount(enabled?.36f:.5f);};manager.addCrossWindowBlurEnabledListener(changed);w.getDecorView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){manager.removeCrossWindowBlurEnabledListener(changed);v.removeOnAttachStateChangeListener(this);}});}}
}




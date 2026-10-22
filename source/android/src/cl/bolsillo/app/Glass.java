package cl.bolsillo.app;
import android.app.*;import android.content.*;import android.content.res.Configuration;import android.graphics.*;import android.graphics.drawable.*;import android.os.Build;import android.view.*;import android.widget.*;

/** Native glass tokens: reflections, semantic colours and adaptive contrast. */
public final class Glass {
 static Typeface regularFont;static final java.util.Map<Integer,Typeface> weights=new java.util.HashMap<>();
 static synchronized Typeface font(Context c,int weight){if(regularFont==null)regularFont=c.getResources().getFont(R.font.inter);Typeface found=weights.get(weight);if(found==null){found=Build.VERSION.SDK_INT>=28?Typeface.create(regularFont,weight,false):Typeface.create(regularFont,weight>=600?Typeface.BOLD:Typeface.NORMAL);weights.put(weight,found);}return found;}
 // Keep legacy names for callers while assigning the approved semantic roles.
 static final int MINT=0xff77dfc0,VIOLET=0xffbba9f4,CORAL=0xffff9aab,BLUE=0xff93c5f8,AMBER=0xfff3cb80;
 static boolean dark(Context c){String mode=NativeStore.settings(c).getString("appearance","dark");return mode.equals("dark")||mode.equals("system")&&(c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;}
 static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 static void editor(EditText field){field.setImeOptions(field.getImeOptions()|android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN);}
 static String palette(Context c){return NativeStore.settings(c).getString("palette","prisma");}
 static int intensity(Context c){return Math.max(0,Math.min(2,NativeStore.settings(c).getInt("glassIntensity",1)));}
 static int primary(Context c){boolean night=dark(c);switch(palette(c)){case "custom":return ThemeColors.role(NativeStore.settings(c).getInt("customPrimary",0xff4dcfb0),night);case "ocean":return night?0xff93c5f8:0xff245f99;case "amber":return night?0xfff3cb80:0xff80550b;default:return night?0xff9ce8d5:0xff126a59;}}
 static int secondary(Context c){boolean night=dark(c);switch(palette(c)){case "custom":return ThemeColors.role(NativeStore.settings(c).getInt("customSecondary",0xffa384e8),night);case "ocean":return night?0xffa7bafb:0xff4a5ba0;case "amber":return night?0xffffb8c5:0xffa13f5d;default:return night?0xffbba9f4:0xff6846a8;}}
 static int ink(Context c){return dark(c)?0xfff2f5fa:0xff172238;}
 static int subtle(Context c){return dark(c)?0xffaab7cc:0xff56647b;}
 static int metadata(Context c){return dark(c)?0xff91a1b8:0xff5c6a80;}
 static int canvas(Context c){return dark(c)?0xff0c1220:0xfff4f6fa;}
 static int raised(Context c){return dark(c)?0xff1d2940:0xffe9eef5;}
 static int divider(Context c){return dark(c)?0xff35435a:0xffd4dce8;}
 static int inputOutline(Context c){return dark(c)?0xff788aa4:0xff7887a0;}
 static int onPrimary(Context c){return dark(c)?palette(c).equals("custom")?0xff101827:0xff0b3029:Color.WHITE;}
 static int primaryContainer(Context c){if(palette(c).equals("custom"))return mix(panel(c),primary(c),dark(c)?.2f:.12f);return dark(c)?0xff183d36:0xffd5f4e8;}
 static int secondaryContainer(Context c){if(palette(c).equals("custom"))return mix(panel(c),secondary(c),dark(c)?.2f:.12f);return dark(c)?0xff332a4b:0xffeae2fc;}
 /** Semantic foregrounds retain contrast on pale glass; brand assets keep their own colours. */
 static int semantic(Context c,int color){if(dark(c))return color;if(color==MINT)return 0xff116b4f;if(color==VIOLET)return 0xff6846a8;if(color==CORAL)return 0xffb52b4e;if(color==BLUE)return 0xff245f99;if(color==AMBER)return 0xff80550b;return color;}
 static int accent(Context c){return primary(c);}
 static int panel(Context c){return dark(c)?0xff151e30:Color.WHITE;}
 static int alpha(int color,int a){return (color&0xffffff)|(Math.max(0,Math.min(255,a))<<24);}
 static int mix(int from,int to,float n){return Color.rgb((int)(Color.red(from)*(1-n)+Color.red(to)*n),(int)(Color.green(from)*(1-n)+Color.green(to)*n),(int)(Color.blue(from)*(1-n)+Color.blue(to)*n));}
 static Drawable surface(Context c,int alpha){return new GlassMaterial(c,alpha,24,secondary(c));}
 static Drawable material(Context c,int alpha,int radius,int tint){return new GlassMaterial(c,alpha,radius,tint);}
 static GradientDrawable gradient(Context c,int from,int to,int radius){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{from,to});g.setCornerRadius(dp(c,radius));g.setStroke(Math.max(1,Math.round(dp(c,1)*.75f)),dark(c)?0x30ffffff:0x28ffffff);return g;}
 static Drawable backdrop(Context c){return new PrismaBackdrop(c);}
 static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(1);l.setPadding(dp(c,20),dp(c,20),dp(c,20),dp(c,20));return l;}
 static TextView text(Context c,String label,int size,boolean bold){TextView v=new TextView(c);v.setText(label);v.setTextSize(size);v.setTextColor(ink(c));v.setFontFeatureSettings("tnum");v.setTypeface(font(c,bold?600:400));v.setFontVariationSettings("'opsz' "+(size>=26?32:14));v.setLetterSpacing(size>=24?-.018f:0);v.setLineSpacing(0,1.12f);v.setPadding(0,dp(c,2),0,dp(c,2));v.setIncludeFontPadding(false);v.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);v.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);return v;}
 static Button button(Context c,String label){Button b=new Button(c);b.setText(label);b.setTextSize(15);b.setAllCaps(false);b.setTypeface(font(c,600));b.setFontFeatureSettings("tnum");b.setIncludeFontPadding(false);boolean secondary=label.equals("Cancelar")||label.equals("Volver")||label.equals("Cerrar")||label.equals("Ahora no")||label.equals("Otro servicio")||label.startsWith("Eliminar")||label.startsWith("Descartar")||label.startsWith("Más opciones")||label.startsWith("Menos opciones");b.setTextColor(secondary?ink(c):onPrimary(c));Drawable bg=secondary?material(c,238,16,secondary(c)):gradient(c,mix(primary(c),dark(c)?Color.WHITE:Color.BLACK,dark(c)?.08f:.04f),primary(c),16);b.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(alpha(primary(c),32)),bg,null));b.setMinHeight(dp(c,52));b.setMinimumHeight(dp(c,52));b.setPadding(dp(c,16),dp(c,14),dp(c,16),dp(c,14));MaterialMotion.touch(b);b.setTranslationZ(0);b.setElevation(0);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(c,12);b.setLayoutParams(lp);return b;}
 static GradientDrawable round(Context c,int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(c,radius));return g;}
 static TextView pill(Context c,String label,boolean selected){TextView t=text(c,label,13,true);t.setGravity(Gravity.CENTER);t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setMinHeight(dp(c,48));t.setPadding(dp(c,12),dp(c,9),dp(c,12),dp(c,9));selectPill(c,t,selected);return t;}
 static void selectPill(Context c,TextView t,boolean selected){t.setTextColor(selected?accent(c):subtle(c));t.setBackground(selected?material(c,238,16,primary(c)):round(c,Color.TRANSPARENT,16));if(t.isClickable()){t.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(alpha(primary(c),32)),t.getBackground(),round(c,Color.WHITE,16)));WaterRipple.apply(t);}}
 static void privacy(Window w,Context c){if(w==null)return;if(NativeStore.settings(c).getBoolean("privateScreen",false))w.addFlags(WindowManager.LayoutParams.FLAG_SECURE);else w.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);}
 static void window(Dialog d,Context c){Window w=d.getWindow();if(w==null)return;privacy(w,c);w.setBackgroundDrawable(material(c,255,28,secondary(c)));w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setDimAmount(.46f);w.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);WindowManager.LayoutParams attrs=w.getAttributes();attrs.y=dp(c,12);w.setAttributes(attrs);w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);int width=Math.min(dp(c,480),c.getResources().getDisplayMetrics().widthPixels-dp(c,16));w.setLayout(width,-2);w.getDecorView().setElevation(dp(c,8));w.setWindowAnimations(R.style.SheetMotion);if(Build.VERSION.SDK_INT>=31){w.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);w.setBackgroundBlurRadius(0);}}
}

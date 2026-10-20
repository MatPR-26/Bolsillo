package cl.bolsillo.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/** Official Google Material Symbols Rounded, 24px/400, rendered by Android vectors.
 * Original path data and Apache-2.0 provenance live in android/licenses.
 * Bank and service brand assets remain separate.
 */
final class Icons {
 static ImageView view(Context c,String name,int color,int size){ImageView v=new ImageView(c);v.setImageDrawable(new Line(c,name,color));v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);v.setLayoutParams(new android.view.ViewGroup.LayoutParams(Glass.dp(c,size),Glass.dp(c,size)));v.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);return v;}
 static String brand(String s){s=BankNotificationParser.normalize(s);if(s.contains("netflix"))return "netflix";if(s.contains("spotify"))return "spotify";if(s.contains("bancoestado")||s.contains("banco estado"))return "bancoestado";if(s.contains("mach"))return "mach";String bank=BankCatalog.detect(s);if(!bank.isEmpty())return bank;if(s.contains("wallet")||s.contains("billetera"))return "wallet";if(s.contains("onepay")||s.contains("one pay"))return "onepay";return "";}
 static int color(String name){switch(name){case "netflix":return 0xffff526b;case "spotify":return 0xff52d88b;case "bancoestado":return 0xffffb561;case "mach":return Glass.VIOLET;case "income":return Glass.MINT;case "expense":return Glass.CORAL;case "wallet":return Glass.BLUE;case "onepay":return Glass.CORAL;default:return Glass.VIOLET;}}
 static ImageView badge(Context c,String name,int size){ImageView v=new ImageView(c);int resource=brandResource(name);v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);v.setBackground(Glass.material(c,225,16,Glass.secondary(c)));int pad=Glass.dp(c,Math.max(6,size/4));if(resource!=0){v.setImageResource(resource);if(BankCatalog.known(name)){pad=0;v.setScaleType(ImageView.ScaleType.CENTER_CROP);v.setClipToOutline(true);}else v.setBackground(Glass.round(c,name.equals("netflix")?0xff241d2b:0xff142b27,16));}else v.setImageDrawable(new Line(c,name,color(name)));v.setPadding(pad,pad,pad,pad);v.setLayoutParams(new android.view.ViewGroup.LayoutParams(Glass.dp(c,size),Glass.dp(c,size)));v.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);return v;}

 static int brandResource(String name){switch(name){
  case "mach":return R.drawable.brand_mach;
  case "bancoestado":return R.drawable.brand_bancoestado;
  case "chile":return R.drawable.brand_chile;
  case "santander":return R.drawable.brand_santander;
  case "bci":return R.drawable.brand_bci;
  case "scotiabank":return R.drawable.brand_scotiabank;
  case "itau":return R.drawable.brand_itau;
  case "falabella":return R.drawable.brand_falabella;
  case "bice":return R.drawable.brand_bice;
  case "consorcio":return R.drawable.brand_consorcio;
  case "internacional":return R.drawable.brand_internacional;
  case "security":return R.drawable.brand_security;
  case "ripley":return R.drawable.brand_ripley;
  case "tenpo":return R.drawable.brand_tenpo;
  case "coopeuch":return R.drawable.brand_coopeuch;
  case "mercadopago":return R.drawable.brand_mercadopago;
  case "netflix":return R.drawable.brand_netflix;
  case "spotify":return R.drawable.brand_spotify;
  default:return 0;
 }}

 static int resource(String name){
  if(name==null)return R.drawable.ms_account_balance_wallet;if(BankCatalog.known(name))return R.drawable.ms_account_balance;
  switch(name){
   case "home":return R.drawable.ms_home;
   case "menu":return R.drawable.ms_apps;
   case "mic":return R.drawable.ms_mic;
   case "activity":return R.drawable.ms_receipt_long;
   case "transfer":return R.drawable.ms_swap_horiz;
   case "income":return R.drawable.ms_south_west;
   case "expense":return R.drawable.ms_north_east;
   case "plus":return R.drawable.ms_add;
   case "close":return R.drawable.ms_close;
   case "chevron":return R.drawable.ms_chevron_right;
   case "back":return R.drawable.ms_chevron_left;
   case "check":return R.drawable.ms_check;
   case "card":case "accounts":return R.drawable.ms_credit_card;
   case "onepay":return R.drawable.ms_payments;
   case "wallet":return R.drawable.ms_account_balance_wallet;
   case "bank":return R.drawable.ms_account_balance;
   case "bell":return R.drawable.ms_notifications;
   case "eye":return R.drawable.ms_visibility;
   case "search":return R.drawable.ms_search;
   case "calendar":return R.drawable.ms_calendar_month;
   case "chart":return R.drawable.ms_monitoring;
   case "target":case "savings":return R.drawable.ms_savings;
   case "shield":return R.drawable.ms_verified_user;
   case "auto":case "sparkle":return R.drawable.ms_auto_awesome;
   case "subscription":return R.drawable.ms_subscriptions;
   case "food":return R.drawable.ms_restaurant;
   case "transport":return R.drawable.ms_directions_bus;
   case "gift":return R.drawable.ms_redeem;
   case "pets":return R.drawable.ms_pets;
   case "travel":return R.drawable.ms_flight;
   case "sport":return R.drawable.ms_fitness_center;
   case "donation":return R.drawable.ms_volunteer_activism;
   case "technology":return R.drawable.ms_devices;
   case "care":return R.drawable.ms_spa;
   case "tax":return R.drawable.ms_request_quote;
   case "shopping":return R.drawable.ms_shopping_bag;
   case "education":return R.drawable.ms_school;
   case "health":return R.drawable.ms_medical_services;
   case "leisure":return R.drawable.ms_theater_comedy;
   case "utilities":return R.drawable.ms_lightbulb;
   case "salary":return R.drawable.ms_payments;
   case "people":return R.drawable.ms_group;
   case "settings":return R.drawable.ms_settings;
   case "cloud":return R.drawable.ms_cloud_sync;
   case "tag":return R.drawable.ms_sell;
   case "refresh":return R.drawable.ms_refresh;
   case "work":return R.drawable.ms_work;
   case "delete":return R.drawable.ms_delete;
   case "edit":return R.drawable.ms_edit;
   case "download":return R.drawable.ms_download;
   case "upload":return R.drawable.ms_upload;
   case "more":return R.drawable.ms_more_horiz;
   case "history":return R.drawable.ms_history;
   case "phone":return R.drawable.ms_smartphone;
   case "email":case "mail":return R.drawable.ms_mail;
   case "warning":return R.drawable.ms_warning;
   default:return R.drawable.ms_account_balance_wallet;
  }
 }

 /** Keeps the existing drawable API while delegating all geometry to official assets. */
 static final class Line extends Drawable {
  final String name;
  final Drawable vector;
  private int color;
  Line(Context c,String name,int color){this.name=name;this.color=Glass.semantic(c,color);vector=c.getDrawable(resource(name)).mutate();vector.setTint(this.color);}
  int color(){return color;}
  @Override protected void onBoundsChange(Rect bounds){vector.setBounds(bounds);}
  @Override public void draw(Canvas canvas){vector.draw(canvas);}
  @Override public void setAlpha(int alpha){vector.setAlpha(alpha);invalidateSelf();}
  @Override public int getAlpha(){return vector.getAlpha();}
  @Override public void setColorFilter(ColorFilter filter){vector.setColorFilter(filter);invalidateSelf();}
  @Override public ColorFilter getColorFilter(){return vector.getColorFilter();}
  @Override public void setTint(int tint){color=tint;vector.setTint(tint);invalidateSelf();}
  @Override public void setTintList(ColorStateList tint){vector.setTintList(tint);invalidateSelf();}
  @Override public int getIntrinsicWidth(){return vector.getIntrinsicWidth();}
  @Override public int getIntrinsicHeight(){return vector.getIntrinsicHeight();}
  @Override public boolean isStateful(){return vector.isStateful();}
  @Override protected boolean onStateChange(int[] state){boolean changed=vector.setState(state);if(changed)invalidateSelf();return changed;}
  @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
}

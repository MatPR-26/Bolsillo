package cl.bolsillo.app;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;

/** Direct category access without opening the full transaction editor. */
final class CategoryShortcut {
 static void attach(MainActivity a,LinearLayout row,JSONObject tx,String detail)throws Exception{
  String type=tx.optString("type");if(!type.equals("expense")&&!type.equals("income"))return;
  LinearLayout texts=(LinearLayout)row.getChildAt(1);
  if(texts.getChildCount()<2||!(texts.getChildAt(1) instanceof TextView))return;
  TextView metadata=(TextView)texts.getChildAt(1);
  JSONObject category=Ledger.find(a.data.getJSONArray("categories"),tx.optString("category"));
  String full=category==null?"Asignar categoría":category.optString("name","Asignar categoría");
  LinearLayout shortcut=Modern.column(a);shortcut.setMinimumHeight(a.dp(48));shortcut.setGravity(Gravity.CENTER_VERTICAL);
  shortcut.setPadding(0,a.dp(4),a.dp(3),a.dp(4));
  TextView label=Modern.text(a,full,12,QuickCategory.needsCategory(a.data,tx)?Glass.accent(a):Glass.ink(a),true);
  label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);
  Drawable arrow=new Icons.Line(a,"edit",Glass.subtle(a));arrow.setBounds(0,0,a.dp(14),a.dp(14));
  label.setCompoundDrawablePadding(a.dp(4));label.setCompoundDrawablesRelative(null,null,arrow,null);
  texts.removeView(metadata);metadata.setText(detail);metadata.setMaxLines(2);
  label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);metadata.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  shortcut.addView(label,new LinearLayout.LayoutParams(-1,-2));shortcut.addView(metadata,new LinearLayout.LayoutParams(-1,-2));
  texts.addView(shortcut,1,new LinearLayout.LayoutParams(-1,-2));
  shortcut.setContentDescription("Cambiar categoría: "+full+". "+detail);
  shortcut.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
  Modern.press(shortcut,()->a.attempt(()->QuickCategory.show(a,tx.optString("id"))));
 }
 static boolean open(MainActivity a,Intent intent){
  if(intent==null||!intent.hasExtra("quickCategoryTx"))return false;
  String id=intent.getStringExtra("quickCategoryTx"),alert=intent.getStringExtra("localAlertId");
  for(String key:new String[]{"quickCategoryTx","localAlertId","localAlertTx","localAlertReview","localAlertSection"})intent.removeExtra(key);
  LocalAlerts.markOpened(a,alert);
  a.handler.post(()->a.attempt(()->{
   Ledger.require(id!=null&&!id.isEmpty(),"Este aviso no identifica un movimiento.");
   a.data=LocalData.read(a);JSONObject tx=Ledger.find(a.data.getJSONArray("transactions"),id);
   Ledger.require(tx!=null,"Este movimiento ya no está en tu registro.");
   String date=tx.optString("date",Ledger.today());if(date.length()>=7)a.month=date.substring(0,7);
   a.filter="";a.accountFilter="";a.grouped=false;a.navigate("Movimientos");QuickCategory.show(a,id);
  }));return true;
 }
}

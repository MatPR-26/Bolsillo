package cl.bolsillo.app;

import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Read-only detail first; editing and destructive actions are explicit secondary steps. */
final class TransactionDetail {
 static SheetDialog show(MainActivity a,JSONObject original)throws Exception {
  JSONObject current=Ledger.find(a.data.getJSONArray("transactions"),original.optString("id"));
  Ledger.require(current!=null,"Este movimiento ya no está en tu registro");
  final JSONObject t=current;
  String type=t.optString("type"),note=t.optString("note");
  boolean incoming=type.equals("income")||type.equals("repayment");
  String title=note.isEmpty()?(incoming?"Ingreso":type.equals("transfer")?"Transferencia":"Gasto"):note;
  LinearLayout box=Modern.column(a);box.setPadding(a.dp(20),a.dp(8),a.dp(20),a.dp(16));
  SheetDialog dialog=new SheetDialog(a,box);dialog.layout.setTag("transaction-detail");
  LinearLayout heading=Modern.row(a);
  heading.addView(Modern.text(a,"Movimiento",20,Glass.ink(a),true),new LinearLayout.LayoutParams(0,-2,1));
  heading.addView(Modern.iconButton(a,"more","Más opciones del movimiento",()->{dialog.dismiss();Forms.transactionMenu(a,t);}),new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));
  heading.addView(Modern.iconButton(a,"close","Cerrar detalle",dialog::dismiss),new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));box.addView(heading);
  LinearLayout identity=Modern.row(a);identity.setPadding(0,a.dp(18),0,a.dp(8));
  String icon=Icons.brand(title);if(icon.isEmpty())icon=Modern.iconFor(a.name("categories",t.optString("category")));
  identity.addView(Icons.badge(a,icon,48),new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));
  TextView name=Modern.text(a,title,20,Glass.ink(a),true);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setPadding(a.dp(12),0,0,0);identity.addView(name,new LinearLayout.LayoutParams(0,-2,1));box.addView(identity);
  TextView amount=Modern.text(a,(incoming?"+":type.equals("expense")||type.equals("loan")?"−":"")+a.money(t.optLong("amount")),38,Glass.ink(a),false);
  amount.setTypeface(Glass.font(a,500));Modern.fitAmount(amount,20,38);amount.setGravity(Gravity.CENTER);amount.setTag("transaction-detail-amount");Modern.add(box,amount,12);
  TextView date=Modern.text(a,MainActivity.shortDate(t.optString("date")),13,Glass.subtle(a),false);date.setGravity(Gravity.CENTER);Modern.add(box,date,4);
  boolean pending=t.optString("status").equals("pending");TextView status=Modern.chip(a,pending?"Por confirmar":t.optBoolean("excluded")?"Fuera de informes":"Confirmado",pending?Glass.AMBER:Glass.MINT);
  LinearLayout statusRow=Modern.row(a);statusRow.setGravity(Gravity.CENTER);statusRow.addView(status);Modern.add(box,statusRow,12);
  JSONArray parts=t.optJSONArray("shares");
  if(type.equals("expense")&&parts!=null&&parts.length()>0){
   long outstanding=0;for(int i=0;i<parts.length();i++){JSONObject part=parts.getJSONObject(i);outstanding=Math.addExact(outstanding,Math.max(0,part.optLong("amount")-Finance.paid(a.data,t.optString("id")+":"+part.optString("id"),false)));}
   LinearLayout split=Modern.surface(a),row=Modern.row(a);row.addView(metric(a,"Tu gasto",Finance.personalExpense(t)),new LinearLayout.LayoutParams(0,-2,1));row.addView(metric(a,"Por cobrar",outstanding),new LinearLayout.LayoutParams(0,-2,1));split.addView(row);split.setTag("transaction-detail-shares");Modern.add(box,split,20);
  }
  LinearLayout fields=Modern.surface(a);if(note.length()>60)field(a,fields,"activity","Descripción",note,null);
  field(a,fields,"card","Cuenta",a.name("accounts",t.optString("account")),null);
  if(type.equals("transfer"))field(a,fields,"transfer","Destino",a.name("accounts",t.optString("to")),null);
  if(!t.optString("category").isEmpty())field(a,fields,"tag","Categoría",a.name("categories",t.optString("category")),()->{dialog.dismiss();QuickCategory.show(a,t.optString("id"));});
  if(!t.optString("detectedSource").isEmpty())field(a,fields,"bell","Origen",t.optString("detectedSource"),null);
  if(t.optInt("installments",1)>1)field(a,fields,"calendar","Cuota",t.optInt("installment")+" de "+t.optInt("installments"),null);
  if(t.optDouble("workHours",0)>0)field(a,fields,"work","Horas",String.valueOf(t.optDouble("workHours")),null);
  Modern.add(box,fields,16);
  if(parts!=null&&parts.length()>0){
   TextView section=Modern.text(a,"Compartido con",14,Glass.ink(a),true);Modern.add(box,section,20);
   for(int i=0;i<parts.length();i++){JSONObject part=parts.getJSONObject(i);long unpaid=Math.max(0,part.optLong("amount")-Finance.paid(a.data,t.optString("id")+":"+part.optString("id"),false));
    Modern.add(box,Modern.item(a,"people",part.optString("name","Persona"),unpaid==0?"Pagado":"Pendiente "+a.money(unpaid),a.money(part.optLong("amount")),unpaid==0?Glass.MINT:Glass.AMBER,null),6);
   }
  }
  if(!t.optString("workNote").isEmpty())field(a,box,"activity","Nota",t.optString("workNote"),null);
  if(!t.optString("learnedExplanation").isEmpty()){
   TextView explanation=Modern.text(a,t.optString("learnedExplanation"),12,Glass.subtle(a),false);Modern.add(box,explanation,12);
  }
  JSONArray log=a.data.optJSONArray("changeLog");List<JSONObject> changes=new ArrayList<>();if(log!=null)for(int i=log.length()-1;i>=0;i--){JSONObject entry=log.getJSONObject(i);if(entry.optString("section").equals("transactions")&&entry.optString("recordId").equals(t.optString("id")))changes.add(entry);}
  if(!changes.isEmpty()){
   TextView label=Modern.text(a,"Registro de cambios",14,Glass.ink(a),true);Modern.add(box,label,20);
   for(int i=0;i<Math.min(3,changes.size());i++){JSONObject entry=changes.get(i);field(a,box,"history",stamp(entry.optLong("at")),change(a,entry),null);}
   Button history=Glass.button(a,"Ver historial");box.addView(history);history.setOnClickListener(v->a.attempt(()->{dialog.dismiss();history(a,t.optString("id"),0);}));
  }
  LinearLayout footer=Modern.row(a);Button primary=Glass.button(a,pending?"Confirmar movimiento":type.equals("loan")||type.equals("repayment")?"Ver personas":"Editar movimiento");primary.setTag("transaction-detail-primary");footer.addView(primary,new LinearLayout.LayoutParams(-1,-2));dialog.footer.addView(footer);
  primary.setOnClickListener(v->a.attempt(()->{dialog.dismiss();if(pending){JSONObject fresh=Ledger.find(LocalData.read(a).getJSONArray("transactions"),t.optString("id"));Ledger.require(fresh!=null,"Este movimiento ya no existe");LocalData.upsert(a,"transactions",new JSONObject(fresh.toString()).put("status","posted"));a.render(false);}else if(type.equals("loan")||type.equals("repayment"))a.navigate("Personas");else if(WorkScreen.matches(a.data,t))WorkScreen.edit(a,t);else Forms.transaction(a,t,type);}));
  dialog.show();return dialog;
 }
 private static View metric(MainActivity a,String title,long value){LinearLayout item=Modern.column(a);item.addView(Modern.text(a,title,12,Glass.subtle(a),false));TextView amount=Modern.text(a,a.money(value),23,Glass.ink(a),true);Modern.fitAmount(amount,13,23);Modern.add(item,amount,6);return item;}
 private static void field(MainActivity a,LinearLayout box,String icon,String title,String value,Runnable action){
  LinearLayout row=Modern.row(a);row.setPadding(0,a.dp(10),0,a.dp(10));row.setMinimumHeight(a.dp(52));row.addView(Icons.view(a,icon,Glass.subtle(a),20),new LinearLayout.LayoutParams(a.dp(20),a.dp(20)));
  LinearLayout words=Modern.column(a);words.setPadding(a.dp(10),0,a.dp(8),0);words.addView(Modern.text(a,title,11,Glass.subtle(a),false));TextView content=Modern.text(a,value,14,Glass.ink(a),false);words.addView(content);row.addView(words,new LinearLayout.LayoutParams(0,-2,1));if(action!=null){row.addView(Icons.view(a,"edit",Glass.subtle(a),16),new LinearLayout.LayoutParams(a.dp(16),a.dp(16)));Modern.press(row,action);}box.addView(row);
 }
 private static void history(MainActivity a,String id,int page)throws Exception{
  LinearLayout box=NativeSettings.panel(a,"Historial del movimiento");SheetDialog dialog=new SheetDialog(a,box);dialog.layout.setTag("transaction-history");JSONArray log=a.data.optJSONArray("changeLog");List<JSONObject> records=new ArrayList<>();if(log!=null)for(int i=log.length()-1;i>=0;i--){JSONObject entry=log.getJSONObject(i);if(entry.optString("section").equals("transactions")&&entry.optString("recordId").equals(id))records.add(entry);}
  for(int i=page*10;i<Math.min(records.size(),(page+1)*10);i++){JSONObject entry=records.get(i);LinearLayout card=Modern.surface(a);field(a,card,"history",stamp(entry.optLong("at")),change(a,entry),null);JSONObject before=entry.optJSONObject("before"),after=entry.optJSONObject("after");if(before!=null&&after!=null)for(String[] item:new String[][]{{"amount","Monto"},{"date","Fecha"},{"note","Descripción"},{"workNote","Nota"},{"account","Cuenta"},{"category","Categoría"},{"status","Estado"}}){String key=item[0];if(!before.optString(key).equals(after.optString(key))){String old=value(a,before,key),now=value(a,after,key);field(a,card,"activity",item[1],old+" → "+now,null);}}Modern.add(box,card,8);}
  if(records.isEmpty())Modern.add(box,Modern.text(a,"No hay cambios guardados para este movimiento.",14,Glass.subtle(a),false),12);
  LinearLayout actions=Modern.row(a);if(page>0){Button previous=Glass.button(a,"Más recientes");actions.addView(previous,new LinearLayout.LayoutParams(0,-2,1));previous.setOnClickListener(v->a.attempt(()->{dialog.dismiss();history(a,id,page-1);}));}if((page+1)*10<records.size()){Button next=Glass.button(a,"Más antiguos");actions.addView(next,new LinearLayout.LayoutParams(0,-2,1));next.setOnClickListener(v->a.attempt(()->{dialog.dismiss();history(a,id,page+1);}));}if(actions.getChildCount()>0)box.addView(actions);Button close=Glass.button(a,"Cerrar");dialog.footer.addView(close);close.setOnClickListener(v->dialog.dismiss());dialog.show();
 }
 private static String value(MainActivity a,JSONObject record,String key)throws Exception{if(key.equals("amount"))return a.money(record.optLong(key));if(key.equals("account")||key.equals("category"))return a.name(key.equals("account")?"accounts":"categories",record.optString(key));if(key.equals("status"))return record.optString(key).equals("pending")?"Pendiente":"Confirmado";return record.optString(key).isEmpty()?"Sin dato":record.optString(key);}
 private static String stamp(long millis){return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm",new Locale("es","CL")));}
 private static String change(MainActivity a,JSONObject entry){JSONObject before=entry.optJSONObject("before"),after=entry.optJSONObject("after");if(after==null)return "Movimiento eliminado";if(before!=null&&before.optLong("amount")!=after.optLong("amount"))return a.money(before.optLong("amount"))+" → "+a.money(after.optLong("amount"));if(before!=null&&!before.optString("category").equals(after.optString("category")))return "Categoría actualizada";return "Movimiento actualizado";}
}

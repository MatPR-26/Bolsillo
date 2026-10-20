package cl.bolsillo.app;

import org.json.*;
import java.util.*;

/** Pure descriptions of committed local financial changes. Does not alter the ledger. */
final class AlertEvents {
 static final String[] SECTIONS={"transactions","accounts","receivables","planned","recurring","budgets","goals"};
 private static final String[] TX_FIELDS={"amount","type","account","to","date","status"},ACCOUNT_FIELDS={"opening"},DEBT_FIELDS={"amount","settled"},GOAL_FIELDS={"target"},AMOUNT_FIELDS={"amount"};
 private static String[] fields(String section){return section.equals("transactions")?TX_FIELDS:section.equals("accounts")?ACCOUNT_FIELDS:section.equals("receivables")?DEBT_FIELDS:section.equals("goals")?GOAL_FIELDS:AMOUNT_FIELDS;}
 private static boolean numeric(String key){return key.equals("amount")||key.equals("opening")||key.equals("settled")||key.equals("target");}
 private static boolean same(String section,JSONObject a,JSONObject b){if(a==null||b==null)return a==b;for(String key:fields(section)){if(numeric(key)){if(a.optLong(key,0)!=b.optLong(key,0))return false;}else if(!a.optString(key).equals(b.optString(key)))return false;}return true;}
 static String signature(String section,JSONObject item)throws Exception{return item==null?"deleted":Detection.hash(signatureValue(section,item));}
 private static String signatureValue(String section,JSONObject item){
  if(item==null)return "deleted";
  String[] keys=fields(section);
  StringBuilder value=new StringBuilder(section).append('|').append(item.optString("id"));
  for(String key:keys)value.append('|').append(key).append('=').append(numeric(key)?String.valueOf(item.optLong(key,0)):item.optString(key));
  return value.toString();
 }
 static Map<String,JSONObject> index(JSONArray a){Map<String,JSONObject> result=new LinkedHashMap<>();if(a!=null)for(int i=0;i<a.length();i++){JSONObject item=a.optJSONObject(i);if(item!=null&&!item.optString("id").isEmpty())result.put(item.optString("id"),item);}return result;}
 static List<JSONObject> changes(JSONObject before,JSONObject after)throws Exception{
  List<JSONObject> result=new ArrayList<>();if(before==null||after==null)return result;Map<String,Long> balances=null;
  for(String section:SECTIONS){Map<String,JSONObject> old=index(before.optJSONArray(section)),now=index(after.optJSONArray(section));Set<String> ids=new LinkedHashSet<>(old.keySet());ids.addAll(now.keySet());
   for(String id:ids){JSONObject previous=old.get(id),current=now.get(id);if(same(section,previous,current))continue;
    // A new account with no opening money is setup, rather than a money change.
    if(section.equals("accounts")&&previous==null&&current!=null&&current.optLong("opening")==0)continue;
    if(balances==null)balances=Ledger.balances(after,Ledger.today());result.add(describe(section,id,previous,current,after,balances));
   }
  }
  return result;
 }
 static JSONObject describe(String section,String id,JSONObject previous,JSONObject current,JSONObject data,Map<String,Long> balances)throws Exception{
  JSONObject item=current==null?previous:current;boolean deleted=current==null,created=previous==null;
  String title,body,hidden;long amount=item.optLong(section.equals("goals")?"target":section.equals("accounts")?"opening":"amount");
  String label=item.optString("note",item.optString("name",""));if(label.trim().isEmpty())label=section.equals("transactions")?"Movimiento":"Registro";
  if(section.equals("transactions")){
   String type=item.optString("type"),kind=type.equals("expense")?"Gasto":type.equals("income")?"Ingreso":type.equals("transfer")?"Transferencia":type.equals("repayment")?"Abono":type.equals("loan")?"Préstamo":"Movimiento";
   boolean feminine=type.equals("transfer");
   title=kind+(deleted?(feminine?" eliminada":" eliminado"):created?(feminine?" registrada":" registrado"):(feminine?" actualizada":" actualizado"));
   String account=name(data,"accounts",item.optString("account"));
   body=money(amount)+" · "+label+(account.isEmpty()?"":" · "+account);
   if(type.equals("transfer")){String to=name(data,"accounts",item.optString("to"));if(!to.isEmpty())body+=" → "+to;}
   if("pending".equals(item.optString("status")))body+=" · Pendiente, sin modificar el saldo";
   else if(item.optString("date").compareTo(Ledger.today())>0)body+=" · Fecha futura: "+item.optString("date");
   else body+=" · Registro local";
   if(!deleted&&"posted".equals(item.optString("status"))&&item.optString("date").compareTo(Ledger.today())<=0){Long balance=balances.get(item.optString("account"));if(balance!=null)body+="\nSaldo registrado: "+money(balance);}
   hidden="Consulta el movimiento en tu registro local.";
  }else{
   String kind=section.equals("accounts")?"Saldo inicial":section.equals("receivables")?"Deuda":section.equals("planned")?"Gasto previsto":section.equals("recurring")?"Programación":section.equals("budgets")?"Presupuesto":"Meta";
   boolean feminine=section.equals("receivables")||section.equals("recurring")||section.equals("goals");
   title=kind+(deleted?(feminine?" eliminada":" eliminado"):created?(feminine?" registrada":" registrado"):(feminine?" actualizada":" actualizado"));
   if(section.equals("receivables"))amount=Math.max(0,item.optLong("amount")-item.optLong("settled"));
   if(section.equals("budgets"))label=name(data,"categories",item.optString("category"));
   body=label+" · "+money(amount)+(section.equals("receivables")?" por cobrar":section.equals("accounts")?" de saldo inicial local":"");
   hidden="Consulta el cambio en tus registros de Bolsillo.";
  }
  JSONObject event=new JSONObject().put("entity",section+":"+id).put("signature",signature(section,current)).put("section",section).put("recordId",id).put("title",title).put("body",body).put("hiddenBody",hidden).put("amount",amount).put("deleted",deleted);
  if(previous!=null)event.put("beforeAmount",previous.optLong(section.equals("goals")?"target":section.equals("accounts")?"opening":"amount"));
  if(section.equals("transactions")&&!deleted)event.put("txId",id);
  return event;
 }
 static String name(JSONObject d,String key,String id)throws Exception{JSONArray a=d.optJSONArray(key);if(a==null)return "";JSONObject item=Ledger.find(a,id);return item==null?"":item.optString("name");}
 static String money(long amount){return "$"+java.text.NumberFormat.getIntegerInstance(new java.util.Locale("es","CL")).format(amount);}
}

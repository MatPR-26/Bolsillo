package cl.bolsillo.app;

import android.widget.*;
import org.json.*;
import java.util.*;

/** Device-local examples, never raw emails or notification text. */
final class LearnedRules {
 static JSONArray list(JSONObject d){JSONArray a=d.optJSONArray("learnedRules");return a==null?new JSONArray():a;}
 static int threshold(JSONObject d){JSONObject c=d.optJSONObject("learnedConfig");return Math.max(3,Math.min(10,c==null?3:c.optInt("threshold",3)));}
 static String mode(JSONObject d,boolean legacyAuto){JSONObject c=d.optJSONObject("learnedConfig");String mode=c==null?"":c.optString("mode");return Arrays.asList("review","patterns","automatic").contains(mode)?mode:legacyAuto?"automatic":"review";}
 static String merchant(String text){return BankNotificationParser.normalize(text).replaceAll("\\b(?:com|cl)\\b$","").trim();}
 static boolean usable(JSONObject e){String m=merchant(e.optString("merchant"));return !m.isEmpty()&&!Arrays.asList("compra detectada","transferencia recibida","ingreso detectado","transferencia o retiro","movimiento por revisar").contains(m)&&Arrays.asList("expense","income").contains(e.optString("type"));}
 static String pattern(JSONObject e){return merchant(e.optString("merchant"))+"|"+e.optString("type")+"|"+e.optString("source")+"|"+e.optString("last4");}
 static boolean contains(JSONArray a,String value){if(a!=null)for(int i=0;i<a.length();i++)if(value.equals(a.optString(i)))return true;return false;}
 static final class Match {final JSONObject rule;final boolean ready;final String reason;Match(JSONObject r,boolean ok,String why){rule=r;ready=ok;reason=why;}}
 static Match match(JSONObject d,JSONObject e)throws Exception{
  if(!usable(e))return null;JSONArray all=list(d);String key=pattern(e);int required=threshold(d);
  for(int i=0;i<all.length();i++){JSONObject r=all.optJSONObject(i);if(r==null||!key.equals(r.optString("pattern"))||!r.optBoolean("enabled",true))continue;
   String reason="";if(r.optBoolean("conflict"))reason="Las confirmaciones tienen categorías o cuentas diferentes";
   else if(r.optJSONArray("exampleIds")==null||r.optJSONArray("exampleIds").length()<required)reason="Faltan confirmaciones del patrón";
   else if(e.optString("account").isEmpty())reason="Falta identificar la cuenta o tarjeta";
   else if(!e.optString("account").equals(r.optString("account")))reason="La cuenta cambió respecto al patrón";
   else if(e.optLong("amount")<r.optLong("minAmount",1)||e.optLong("amount")>r.optLong("maxAmount",0))reason="Monto fuera del intervalo confirmado";
   else {JSONObject account=Ledger.find(d.optJSONArray("accounts"),r.optString("account")),cat=Ledger.find(d.optJSONArray("categories"),r.optString("category"));if(account==null||account.optBoolean("archived")||cat==null||cat.optBoolean("archived")||!cat.optString("type").equals(e.optString("type")))reason="La cuenta o categoría del patrón ya no está disponible";}
   return new Match(r,reason.isEmpty(),reason);
  }return null;
 }
 static void confirmed(JSONObject d,JSONObject evidence,JSONObject tx)throws Exception{
  if(tx==null||!"posted".equals(tx.optString("status"))||!evidence.optString("duplicateOf").isEmpty()||evidence.optBoolean("suppressed")||!usable(evidence)||tx.optLong("amount")<=0)return;
  // A correction to the movement's type needs its own repeated evidence.
  if(!evidence.optString("type").equals(tx.optString("type")))return;
  JSONArray all=list(d);String key=pattern(evidence),event=evidence.optString("id");if(event.isEmpty())return;JSONObject rule=null;
  for(int i=0;i<all.length();i++){JSONObject r=all.optJSONObject(i);if(r!=null&&key.equals(r.optString("pattern"))){rule=r;break;}}
  if(rule==null){if(all.length()>=500)return;rule=new JSONObject().put("id",Ledger.id()).put("pattern",key).put("merchant",evidence.optString("merchant")).put("source",evidence.optString("source")).put("label",evidence.optString("label")).put("last4",evidence.optString("last4")).put("type",tx.optString("type")).put("account",tx.optString("account")).put("category",tx.optString("category")).put("minAmount",tx.getLong("amount")).put("maxAmount",tx.getLong("amount")).put("enabled",true).put("exampleIds",new JSONArray()).put("examples",new JSONArray());all.put(rule);}
  JSONArray ids=rule.optJSONArray("exampleIds");if(ids==null){ids=new JSONArray();rule.put("exampleIds",ids);}if(contains(ids,event))return;
  // An Android notification ID is often reused. Only the same key AND time is one example.
  JSONArray examples=rule.optJSONArray("examples");if(examples==null){examples=new JSONArray();rule.put("examples",examples);}for(int i=0;i<examples.length();i++){JSONObject old=examples.optJSONObject(i);if(old!=null&&old.optString("key").equals(evidence.optString("key"))&&old.optLong("at")==evidence.optLong("at"))return;}
  String ref=evidence.optString("reference");if(!ref.isEmpty())for(int i=0;i<examples.length();i++){JSONObject old=examples.optJSONObject(i);if(old!=null&&ref.equals(old.optString("reference")))return;}
  if(!tx.optString("account").equals(rule.optString("account"))||!tx.optString("category").equals(rule.optString("category")))rule.put("conflict",true);
  ids.put(event);examples.put(new JSONObject().put("id",event).put("txId",tx.optString("id")).put("key",evidence.optString("key")).put("reference",ref).put("at",evidence.optLong("at")).put("amount",tx.getLong("amount")).put("account",tx.optString("account")).put("category",tx.optString("category")));
  rule.put("minAmount",Math.min(rule.optLong("minAmount",tx.getLong("amount")),tx.getLong("amount"))).put("maxAmount",Math.max(rule.optLong("maxAmount",tx.getLong("amount")),tx.getLong("amount"))).put("updatedAt",System.currentTimeMillis());
  d.put("learnedRules",all);
 }
 static String explanation(JSONObject r){int n=r.optJSONArray("exampleIds")==null?0:r.optJSONArray("exampleIds").length();return n+" confirmaciones · "+r.optString("label")+(r.optString("last4").isEmpty()?"":" · •• "+r.optString("last4"));}
 static void show(MainActivity a)throws Exception{
  Forms.Form f=new Forms.Form(a,"Aprendizaje local");JSONObject config=a.data.optJSONObject("learnedConfig");String mode=mode(a.data,NativeStore.settings(a).getBoolean("autoPost",true));Spinner modes=f.select("Cuándo registrar",Arrays.asList("Revisar siempre","Solo patrones confirmados","Automático si es seguro"),Arrays.asList("review","patterns","automatic").indexOf(mode));Spinner threshold=f.select("Confirmaciones distintas",Arrays.asList("3","4","5","6","7","8","9","10"),threshold(a.data)-3);f.note("Aprende comercio, app, tarjeta, cuenta, categoría e intervalo de montos. Un aviso duplicado no enseña de nuevo.");
  JSONArray rules=list(a.data);if(rules.length()==0)f.note("Confirma tres avisos distintos del mismo comercio para crear tu primer patrón.");for(int i=0;i<rules.length();i++){JSONObject r=rules.getJSONObject(i);String state=r.optBoolean("conflict")?"Revisar categorías":!r.optBoolean("enabled",true)?"Desactivada":r.optJSONArray("exampleIds").length()>=threshold(a.data)?"Lista":"Aprendiendo";Modern.add(f.box,Modern.item(a,"sparkle",r.optString("merchant"),explanation(r),state,Glass.VIOLET,()->a.attempt(()->edit(a,r))),8);}
  f.save("Guardar preferencias",()->LocalData.change(a,d->d.put("learnedConfig",new JSONObject().put("mode",Arrays.asList("review","patterns","automatic").get(modes.getSelectedItemPosition())).put("threshold",threshold.getSelectedItemPosition()+3))));
 }
 static void edit(MainActivity a,JSONObject original)throws Exception{
  JSONObject r=new JSONObject(original.toString());Forms.Form f=new Forms.Form(a,"Patrón · "+r.optString("merchant"));f.note(explanation(r));f.note("Coincide por comercio, tipo, app y tarjeta. Si no se identifica la cuenta, pide revisión.");Forms.Pick account=new Forms.Pick(f,"Cuenta confirmada","accounts","",r.optString("account")),category=new Forms.Pick(f,"Categoría","categories",r.optString("type"),r.optString("category"));EditText min=f.field("Monto mínimo",r.optString("minAmount"),true),max=f.field("Monto máximo",r.optString("maxAmount"),true);Switch enabled=f.toggle("Aplicar patrón",r.optBoolean("enabled",true));if(r.optBoolean("conflict"))f.note("Se confirmaron cuentas o categorías diferentes. Al guardar eliges la que se aplicará desde ahora.");
  f.action("Ver confirmaciones",()->{Forms.Form details=new Forms.Form(a,"Ejemplos confirmados");JSONArray examples=r.optJSONArray("examples");if(examples!=null)for(int i=examples.length()-1;i>=0;i--){JSONObject e=examples.getJSONObject(i);details.note(Detection.date(e.optLong("at"))+" · "+a.money(e.optLong("amount"))+" · "+a.name("accounts",e.optString("account"))+" · "+a.name("categories",e.optString("category")));}details.action("Cerrar",details.dialog::dismiss);});
  f.action("Borrar patrón",()->a.confirm("Borrar patrón","Conserva todos tus movimientos. Los próximos avisos volverán a aprender desde cero.",()->{LocalData.change(a,d->{JSONArray all=list(d);for(int i=all.length()-1;i>=0;i--)if(r.optString("id").equals(all.getJSONObject(i).optString("id")))all.remove(i);d.put("learnedRules",all);});f.dialog.dismiss();a.render();}));
  f.save("Guardar patrón",()->{long lo=Forms.amount(min,false),hi=Forms.amount(max,false);Ledger.require(hi>=lo,"El monto máximo debe ser mayor o igual al mínimo");r.put("account",account.id()).put("category",category.id()).put("minAmount",lo).put("maxAmount",hi).put("enabled",enabled.isChecked()).put("conflict",false).put("editedAt",System.currentTimeMillis());LocalData.change(a,d->{JSONArray all=list(d);for(int i=0;i<all.length();i++)if(r.optString("id").equals(all.getJSONObject(i).optString("id")))all.put(i,r);d.put("learnedRules",all);});});
 }
}

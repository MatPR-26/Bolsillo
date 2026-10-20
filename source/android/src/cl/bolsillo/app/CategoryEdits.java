package cl.bolsillo.app;

import android.content.Context;
import org.json.*;
import java.util.*;

/** Category-only edits with an explicit, fixed set of record IDs and fresh validation. */
final class CategoryEdits {
 static final class Target {final String id,category;Target(JSONObject tx){id=tx.optString("id");category=tx.optString("category");}}
 static final class Plan {
  final String txId,type,key;final boolean group;final List<Target> targets;
  Plan(JSONObject anchor,boolean group,List<Target> targets){txId=anchor.optString("id");type=anchor.optString("type");key=ConceptCatalog.key(anchor);this.group=group;this.targets=Collections.unmodifiableList(new ArrayList<>(targets));}
  int count(){return targets.size();}
 }
 static final class Result {final String category;final List<String> ids;Result(String category,List<String> ids){this.category=category;this.ids=Collections.unmodifiableList(new ArrayList<>(ids));}int count(){return ids.size();}}
 static boolean editable(JSONObject tx){return tx!=null&&!tx.optBoolean("archived")&&!tx.optBoolean("deleted")&&Arrays.asList("expense","income").contains(tx.optString("type"))&&Arrays.asList("posted","pending").contains(tx.optString("status"));}
 static JSONObject category(JSONObject data,String id){if(id==null)return null;JSONArray cats=data==null?null:data.optJSONArray("categories");if(cats!=null)for(int i=0;i<cats.length();i++){JSONObject cat=cats.optJSONObject(i);if(cat!=null&&id.equals(cat.optString("id")))return cat;}return null;}
 static boolean otherCategory(JSONObject cat){if(cat==null)return false;String name=ConceptCatalog.normalize(cat.optString("name"));return Arrays.asList("otros","otros gastos","otros ingresos","otro gasto","otro ingreso","sin categoria").contains(name);}
 static boolean needsCategory(JSONObject data,JSONObject tx){if(!editable(tx))return false;JSONObject cat=category(data,tx.optString("category"));return cat==null||cat.optBoolean("archived")||cat.optBoolean("deleted")||!cat.optString("type").equals(tx.optString("type"))||otherCategory(cat);}
 static Map<String,JSONObject> transactions(JSONObject data)throws Exception{LinkedHashMap<String,JSONObject> map=new LinkedHashMap<>();JSONArray list=data.getJSONArray("transactions");for(int i=0;i<list.length();i++){JSONObject tx=list.getJSONObject(i);map.put(tx.getString("id"),tx);}return map;}
 static Set<String> otherIds(JSONObject data,String type){Set<String> ids=new HashSet<>();JSONArray cats=data.optJSONArray("categories");if(cats!=null)for(int i=0;i<cats.length();i++){JSONObject cat=cats.optJSONObject(i);if(cat!=null&&!cat.optBoolean("archived")&&!cat.optBoolean("deleted")&&type.equals(cat.optString("type"))&&otherCategory(cat))ids.add(cat.optString("id"));}return ids;}
 static Plan plan(JSONObject data,String txId,boolean group)throws Exception{
  Map<String,JSONObject> all=transactions(data);JSONObject anchor=all.get(txId);Ledger.require(editable(anchor),"Este movimiento ya no está disponible");List<Target> targets=new ArrayList<>();String key=ConceptCatalog.key(anchor),type=anchor.optString("type");
  if(!group)targets.add(new Target(anchor));else{Set<String> others=otherIds(data,type);Ledger.require(others.contains(anchor.optString("category")),"Este movimiento ya tiene categoría");for(JSONObject tx:all.values())if(editable(tx)&&type.equals(tx.optString("type"))&&others.contains(tx.optString("category"))&&key.equals(ConceptCatalog.key(tx)))targets.add(new Target(tx));}
  Ledger.require(!targets.isEmpty(),"No quedan movimientos por clasificar");return new Plan(anchor,group,targets);
 }
 static List<JSONObject> validated(JSONObject data,Plan plan)throws Exception{
  Ledger.require(plan!=null&&!plan.targets.isEmpty(),"Elige un movimiento");Map<String,JSONObject> all=transactions(data);JSONObject anchor=all.get(plan.txId);Ledger.require(editable(anchor)&&plan.type.equals(anchor.optString("type"))&&plan.key.equals(ConceptCatalog.key(anchor)),"El movimiento cambió; vuelve a elegirlo");Set<String> others=plan.group?otherIds(data,plan.type):Collections.emptySet();List<JSONObject> targets=new ArrayList<>();Set<String> unique=new HashSet<>();
  for(Target expected:plan.targets){JSONObject tx=all.get(expected.id);Ledger.require(editable(tx)&&unique.add(expected.id)&&plan.type.equals(tx.optString("type"))&&plan.key.equals(ConceptCatalog.key(tx))&&expected.category.equals(tx.optString("category"))&&(!plan.group||others.contains(tx.optString("category"))),"Los movimientos cambiaron; vuelve a elegir el alcance");targets.add(tx);}
  return targets;
 }
 static JSONObject destination(JSONObject data,String id,String type)throws Exception{JSONObject cat=category(data,id);Ledger.require(cat!=null&&!cat.optBoolean("archived")&&!cat.optBoolean("deleted")&&type.equals(cat.optString("type")),"La categoría ya no está disponible para este movimiento");return cat;}
 static Result edit(JSONObject data,Plan plan,String categoryId)throws Exception{List<JSONObject> targets=validated(data,plan);destination(data,categoryId,plan.type);return put(targets,categoryId);}
 static Result create(JSONObject data,Plan plan,String name)throws Exception{
  List<JSONObject> targets=validated(data,plan);Ledger.require(name!=null,"Escribe una categoría");String normalized=ConceptCatalog.normalize(name);boolean existing=false;JSONArray cats=data.getJSONArray("categories");
  for(int i=0;i<cats.length();i++){JSONObject cat=cats.getJSONObject(i);if(plan.type.equals(cat.optString("type"))&&normalized.equals(ConceptCatalog.normalize(cat.optString("name")))){Ledger.require(!cat.optBoolean("deleted"),"Esa categoría está eliminada; elige otro nombre");existing=true;}}
  Ledger.require(existing||cats.length()<300,"Hay 300 categorías; usa una existente");JSONObject cat=ConceptCatalog.ensureCategory(data,plan.type,name);destination(data,cat.getString("id"),plan.type);return put(targets,cat.getString("id"));
 }
 private static Result put(List<JSONObject> targets,String category)throws Exception{List<String> changed=new ArrayList<>();for(JSONObject tx:targets)if(!category.equals(tx.optString("category"))){tx.put("category",category);changed.add(tx.getString("id"));}return new Result(category,changed);}
 static Result apply(Context c,Plan plan,String category)throws Exception{final Result[] result={null};LocalData.change(c,data->{result[0]=edit(data,plan,category);learn(c,data,plan,result[0]);});return result[0];}
 static Result createAndApply(Context c,Plan plan,String name)throws Exception{final Result[] result={null};LocalData.change(c,data->{result[0]=create(data,plan,name);learn(c,data,plan,result[0]);});return result[0];}
 /** A bulk correction never pretends to be several independent confirmations.
  * It can still suspend an existing contradictory rule without creating examples. */
 static void learn(Context c,JSONObject data,Plan plan,Result result)throws Exception{if(result.count()==0)return;JSONObject state=NativeStore.state(c);Map<String,JSONObject> records=transactions(data),events=new HashMap<>();JSONArray history=state.optJSONArray("detectionEvents");if(history!=null)for(int i=0;i<history.length();i++){JSONObject e=history.optJSONObject(i);if(e!=null)events.put(e.optString("id"),e);}for(String id:result.ids){JSONObject tx=records.get(id);JSONObject evidence=linkedEvidence(state,tx,events.get(tx.optString("detectionId")));if(evidence!=null)correctLearning(data,evidence,tx,!plan.group);}}
 static void correctLearning(JSONObject data,JSONObject evidence,JSONObject tx,boolean allowNew)throws Exception{String pattern=LearnedRules.pattern(evidence);JSONArray all=LearnedRules.list(data);for(int i=0;i<all.length();i++){JSONObject rule=all.optJSONObject(i);if(rule!=null&&pattern.equals(rule.optString("pattern"))&&LearnedRules.contains(rule.optJSONArray("exampleIds"),evidence.optString("id"))&&!rule.optString("category").equals(tx.optString("category"))){rule.put("conflict",true).put("updatedAt",System.currentTimeMillis()).put("lastCorrection",new JSONObject().put("evidenceId",evidence.optString("id")).put("txId",tx.optString("id")).put("from",rule.optString("category")).put("to",tx.optString("category")).put("at",System.currentTimeMillis()));return;}}if(allowNew)LearnedRules.confirmed(data,evidence,tx);}
 static JSONObject linkedEvidence(JSONObject state,JSONObject tx)throws Exception{
  if(tx==null)return null;JSONArray events=state.optJSONArray("detectionEvents");if(events!=null)for(int i=events.length()-1;i>=0;i--){JSONObject e=events.optJSONObject(i);if(e!=null&&tx.optString("detectionId").equals(e.optString("id")))return linkedEvidence(state,tx,e);}
  return null;
 }
 private static JSONObject linkedEvidence(JSONObject state,JSONObject tx,JSONObject e)throws Exception{if(e==null||!editable(tx)||!"posted".equals(tx.optString("status"))||!"notification".equals(tx.optString("source"))||tx.optString("detectionId").isEmpty())return null;JSONObject cache=state.optJSONObject("cache");if(cache==null)return null;String owner=cache.optString("ownerId");JSONObject account=Ledger.find(cache.getJSONObject("data").getJSONArray("accounts"),tx.optString("account"));if(account==null||account.optBoolean("archived"))return null;if(owner.isEmpty()||!owner.equals(e.optString("ownerId"))||!tx.optString("id").equals(e.optString("txId"))||e.optBoolean("suppressed")||!e.optString("duplicateOf").isEmpty()||"discarded".equals(e.optString("resolution"))||e.optString("source").isEmpty()||e.optString("key").isEmpty()||e.optLong("at")<=0||e.optLong("amount")!=tx.optLong("amount")||!e.optString("type").equals(tx.optString("type"))||!e.optString("account").equals(tx.optString("account"))||!ConceptCatalog.concept(e.optString("merchant")).equals(ConceptCatalog.concept(tx.optString("note")))||!LearnedRules.usable(e))return null;return new JSONObject(e.toString());}
}

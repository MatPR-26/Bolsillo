package cl.bolsillo.app;
import android.text.*;import android.view.View;import android.widget.*;import org.json.*;import java.util.*;import java.time.*;
final class Subscriptions {
 static ServiceCatalog.Entry service(JSONObject record){ServiceCatalog.Entry service=ServiceCatalog.byId(record.optString("service"));return service==null?ServiceCatalog.identify(record.optString("name")):service;}
 static List<JSONObject> configured(MainActivity a,ServiceCatalog.Entry service)throws Exception{return configured(LocalData.read(a),service);}
 static List<JSONObject> configured(JSONObject data,ServiceCatalog.Entry service)throws Exception{List<JSONObject> found=new ArrayList<>();if(service==null)return found;JSONArray recurring=data.getJSONArray("recurring");for(int i=0;i<recurring.length();i++){JSONObject record=recurring.getJSONObject(i);if(!record.optString("type").equals("expense"))continue;ServiceCatalog.Entry identified=service(record);if(identified!=null&&service.id.equals(identified.id))found.add(record);}found.sort(Comparator.comparing(record->record.optString("next")));return found;}
 static void row(MainActivity a,JSONObject r)throws Exception{
  long owed=0;JSONArray parts=r.optJSONArray("shares");if(parts!=null)for(int i=0;i<parts.length();i++)owed+=parts.getJSONObject(i).optLong("amount");
  String detail=MainActivity.shortDate(r.optString("next"))+" · "+(r.optBoolean("active")?(r.optBoolean("automatic")?"Programado":r.optBoolean("matchNotifications")?"Aviso":"Manual"):"Pausado");
  if(owed>0)detail+=" · Tú "+a.money(r.optLong("amount")-owed)+" · Otros "+a.money(owed);
  ServiceCatalog.Entry brand=service(r);String icon=brand==null?"subscription":brand.id;
  Modern.add(a.body,Modern.item(a,icon,r.optString("name"),detail,a.money(r.optLong("amount")),Glass.VIOLET,()->Forms.recurringMenu(a,r)),8);
 }
 static void screen(MainActivity a)throws Exception{
  JSONArray all=a.data.getJSONArray("recurring");long monthly=0;
  for(int i=0;i<all.length();i++){JSONObject r=all.getJSONObject(i);if(r.optBoolean("active")&&r.optString("type").equals("expense")){long amount=Finance.personalExpense(r);monthly+=r.optString("frequency").equals("weekly")?Math.round(amount*52d/12):r.optString("frequency").equals("yearly")?Math.round(amount/12d):amount;}}
  Modern.add(a.body,Modern.stat(a,"subscription","Tu parte al mes",a.money(monthly),Glass.VIOLET),8);
  Modern.section(a,"Tus servicios","Añadir",()->ServicePicker.show(a));
  Modern.add(a.body,Modern.item(a,"search","Buscar servicio","Streaming, música, IA, almacenamiento y más","",Glass.VIOLET,()->a.attempt(()->ServicePicker.show(a))),4);
  List<JSONObject> sorted=new ArrayList<>();for(int i=0;i<all.length();i++)if(all.getJSONObject(i).optString("type").equals("expense"))sorted.add(all.getJSONObject(i));sorted.sort(Comparator.comparing(r->r.optString("next")));for(JSONObject r:sorted)row(a,r);
  if(sorted.isEmpty())a.caption("Organiza aquí los próximos cobros de tus servicios.");
  Modern.section(a,"Otros pagos","Crear",()->Forms.recurring(a));for(int i=0;i<all.length();i++)if(!all.getJSONObject(i).optString("type").equals("expense"))row(a,all.getJSONObject(i));
 }
 static List<String> suggestions(JSONObject data)throws Exception{
  LinkedHashMap<String,String> names=new LinkedHashMap<>();
  JSONArray recurring=data.getJSONArray("recurring");for(int i=recurring.length()-1;i>=0;i--){JSONObject record=recurring.getJSONObject(i);if(record.optString("type").equals("expense"))suggest(names,record.optString("name"));}
  JSONArray tx=data.getJSONArray("transactions");for(int i=tx.length()-1;i>=0;i--){JSONObject record=tx.getJSONObject(i);if(record.optString("type").equals("expense")&&!record.optBoolean("deleted")&&!record.optBoolean("archived"))suggest(names,record.optString("note"));}
  for(ServiceCatalog.Entry service:ServiceCatalog.all())suggest(names,service.name);
  return new ArrayList<>(names.values());
 }
 private static void suggest(LinkedHashMap<String,String> names,String raw){String clean=raw.trim(),normal=ConceptCatalog.normalize(clean);if(clean.length()<2||ConceptCatalog.generic(normal))return;ServiceCatalog.Entry service=ServiceCatalog.identify(clean);String key=service==null?normal:"service:"+service.id;if(!names.containsKey(key))names.put(key,clean);}
 static Forms.Form add(MainActivity a,String service)throws Exception{
  // Catalogue data fills identity and category, never the price or an existing financial record.
  Forms.Form f=new Forms.Form(a,"Nueva suscripción");AutoCompleteTextView name=f.conceptField("Servicio",service,"expense");name.setAdapter(NativeSettings.suggestions(a,suggestions(a.data)));name.setTag("subscription-service");
  TextView warning=Modern.text(a,"",12,Glass.subtle(a),false);warning.setVisibility(View.GONE);warning.setTag("subscription-existing-warning");Modern.add(f.box,warning,0);
  Map<String,Integer> existing=new HashMap<>();JSONArray current=a.data.getJSONArray("recurring");for(int i=0;i<current.length();i++){JSONObject record=current.getJSONObject(i);if(!record.optString("type").equals("expense"))continue;ServiceCatalog.Entry identified=service(record);if(identified!=null)existing.put(identified.id,existing.getOrDefault(identified.id,0)+1);}
  Runnable updateWarning=()->{ServiceCatalog.Entry identified=ServiceCatalog.identify(Forms.value(name));int count=identified==null?0:existing.getOrDefault(identified.id,0);warning.setText(count==0?"":"Ya tienes "+count+(count==1?" programación":" programaciones")+" de "+identified.name+". Guardar creará otra.");warning.setVisibility(count==0?View.GONE:View.VISIBLE);};
  name.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){updateWarning.run();}public void afterTextChanged(Editable e){}});updateWarning.run();
  EditText amount=f.field("Monto en CLP","",true),date=f.field("Próximo cobro · fecha",Ledger.today(),false);amount.setTag("subscription-amount");
  Forms.Pick account=new Forms.Pick(f,"Cuenta","accounts","",""),category=new Forms.Pick(f,"Categoría","categories","expense",Detection.category(a,a.data,service));
  name.setOnItemClickListener((parent,view,position,id)->a.attempt(()->{String preferred=Detection.category(a,a.data,Forms.value(name));if(!preferred.isEmpty())category.setId(preferred);}));
  Spinner freq=f.select("Frecuencia",Arrays.asList("Mensual","Semanal","Anual"),0);Switch auto=f.toggle("Registrar desde notificaciones",true);SharesEditor shares=new SharesEditor(f,null);
  f.note("Al reconocer servicio, cuenta y monto cerca de su fecha, el aviso registra el gasto y avanza el próximo cobro. No se crea otro gasto programado.");
  f.save("Guardar suscripción",()->{
   Ledger.require(!Forms.value(name).isEmpty(),"Escribe el servicio");LocalDate next=LocalDate.parse(Forms.value(date));Ledger.require(shares.total()<=Forms.amount(amount,false),"Las partes superan el cobro");ServiceCatalog.Entry brand=ServiceCatalog.identify(Forms.value(name));
   LocalData.upsert(a,"recurring",new JSONObject().put("id",Ledger.id()).put("name",Forms.value(name)).put("type","expense").put("amount",Forms.amount(amount,false)).put("account",account.id()).put("category",category.id()).put("next",next.toString()).put("frequency",new String[]{"monthly","weekly","yearly"}[freq.getSelectedItemPosition()]).put("active",true).put("automatic",false).put("matchNotifications",auto.isChecked()).put("anchorDay",next.getDayOfMonth()).put("shares",shares.value()).put("service",brand==null?"":brand.id));
  });return f;
 }
}

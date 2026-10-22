package cl.bolsillo.app;

import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.os.*;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Standalone upgrade probe: uses only the unchanged v16 financial/storage API. */
public final class UpgradePreservationChecks extends Instrumentation {
 private String phase;
 private int checks;
 private Context c;
 private final String[] settings={"sources","paymentCards","categoryRules","movementRules","appearance","palette","glassIntensity","autoPost","capture","quickAccount","quickKeepOpen","peopleSort","widgetAccount","widgetHidden","alertChanges","alertDetections","alertVerifications","alertHideAmounts","privateScreen","detectionTutorialSeen"};
 @Override public void onCreate(Bundle args){super.onCreate(args);phase=args==null?"verify":args.getString("phase","verify");start();}
 private void yes(boolean ok,String message)throws Exception{checks++;if(!ok)throw new Exception(message);}
 private long version()throws Exception{PackageInfo p=c.getPackageManager().getPackageInfo(c.getPackageName(),0);return Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;}
 private String canonical(Object value)throws Exception{
  if(value instanceof JSONObject){JSONObject o=(JSONObject)value;List<String> keys=new ArrayList<>();Iterator<String> it=o.keys();while(it.hasNext())keys.add(it.next());Collections.sort(keys);StringBuilder b=new StringBuilder("{");for(String key:keys){if(b.length()>1)b.append(',');b.append(JSONObject.quote(key)).append(':').append(canonical(o.get(key)));}return b.append('}').toString();}
  if(value instanceof JSONArray){JSONArray a=(JSONArray)value;StringBuilder b=new StringBuilder("[");for(int i=0;i<a.length();i++){if(i>0)b.append(',');b.append(canonical(a.get(i)));}return b.append(']').toString();}
  if(value==null||value==JSONObject.NULL)return "null";if(value instanceof String)return JSONObject.quote((String)value);return String.valueOf(value);
 }
 private String hash(Object value)throws Exception{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(canonical(value).getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
 private JSONObject selectedSettings()throws Exception{JSONObject out=new JSONObject();Map<String,?> actual=NativeStore.settings(c).getAll();for(String key:settings){Object value=actual.get(key);out.put(key,new JSONObject().put("type",value==null?"missing":value.getClass().getSimpleName()).put("value",value==null?JSONObject.NULL:value));}return out;}
 private JSONObject tx(String id,String type,long amount,String category,String note)throws Exception{return NativeSync.transaction(type,amount,"bank-mach","",category,note,"manual","").put("id",id).put("date","2025-04-15");}
 private JSONObject fixture()throws Exception{
  JSONObject d=Ledger.initial();Ledger.find(d.getJSONArray("accounts"),"bank-mach").put("opening",9000000);Ledger.find(d.getJSONArray("accounts"),"mach-savings").put("opening",2500000);
  d.getJSONArray("accounts").put(Ledger.account("upgrade-archived","Cuenta archivada sintética","bank","other").put("archived",true).put("included",false).put("opening",12345));
  d.getJSONArray("categories").put(new JSONObject().put("id","upgrade-custom").put("name","Instrumentos musicales").put("type","expense").put("archived",false).put("emoji","🎹").put("color","#7568C8"));
  d.getJSONArray("people").put(new JSONObject().put("id","upgrade-person").put("name","Persona sintética").put("note","Perfil anterior a la actualización"));
  JSONArray shares=new JSONArray().put(new JSONObject().put("id","upgrade-share").put("personId","upgrade-person").put("name","Persona sintética").put("amount",5000));
  d.getJSONArray("transactions").put(tx("upgrade-netflix","expense",9990,"expense-7","Netflix compartido").put("shares",shares).put("service","netflix").put("tags","mensual, compartido"));
  d.getJSONArray("transactions").put(tx("upgrade-repayment","repayment",2000,"","Abono con fecha y nota").put("receivable","upgrade-netflix:upgrade-share").put("paymentNote","Primera parte"));
  d.getJSONArray("transactions").put(tx("upgrade-large","income",900000000000L,"income-0","Trabajo de prueba").put("workNote","Nota completa conservada").put("workDays",20).put("customMetadata",new JSONObject().put("nested",new JSONArray().put("conservar").put(42))));
  d.getJSONArray("transactions").put(tx("upgrade-pending","expense",12345,"upgrade-custom","Por revisar").put("status","pending"));
  d.getJSONArray("receivables").put(new JSONObject().put("id","upgrade-prior").put("personId","upgrade-person").put("name","Persona sintética").put("amount",1000000).put("settled",0).put("loan",true).put("date","2024-12-12").put("note","Deuda previa; no descontar nuevamente"));
  Finance.abonoPrior(d,"prior:upgrade-prior",150000,"2025-01-15","Abono histórico por transferencia","Regalo");
  Finance.abonoPrior(d,"prior:upgrade-prior",200000,"2025-02-15","Segundo abono histórico","Regalo");
  d.getJSONArray("planned").put(new JSONObject().put("id","upgrade-planned").put("name","Electricidad futura").put("amount",48000).put("date","2099-05-01").put("account","bank-mach").put("category","expense-2").put("status","planned"));
  d.getJSONArray("recurring").put(tx("upgrade-recurring","expense",9990,"expense-7","Netflix").put("name","Netflix familiar").put("service","netflix").put("next","2099-05-15").put("frequency","monthly").put("anchorDay",15).put("active",true).put("automatic",false).put("shares",new JSONArray(shares.toString())));
  d.getJSONArray("templates").put(tx("upgrade-template","expense",25990,"upgrade-custom","Plantilla musical").put("name","Ensayo"));
  d.getJSONArray("budgets").put(new JSONObject().put("id","upgrade-budget").put("category","upgrade-custom").put("amount",150000));
  JSONArray accounts=new JSONArray().put("mach-savings");d.getJSONArray("goals").put(new JSONObject().put("id","upgrade-goal").put("name","Fondo de emergencia").put("target",5000000).put("deadline","2099-12-31").put("accounts",accounts));
  d.getJSONArray("groups").put(new JSONObject().put("id","upgrade-group").put("name","Fondos personales").put("accounts",new JSONArray().put("bank-mach").put("mach-savings")));
  Map<String,Map<String,String>> before=ChangeHistory.snapshot(d);Ledger.find(d.getJSONArray("transactions"),"upgrade-netflix").put("note","Netflix compartido, nota editada con historial");ChangeHistory.record(d,before);
  d.put("learnedConfig",new JSONObject().put("mode","patterns").put("threshold",4));
  d.put("learnedRules",new JSONArray().put(new JSONObject().put("id","upgrade-learned").put("pattern","instrumentos|expense|synthetic.bank|").put("merchant","Instrumentos").put("type","expense").put("source","synthetic.bank").put("account","bank-mach").put("category","upgrade-custom").put("minAmount",1000).put("maxAmount",90000).put("enabled",true).put("exampleIds",new JSONArray().put("example1").put("example2")).put("examples",new JSONArray())));
  d.put("customUpgradeMetadata",new JSONObject().put("preserve",true));Ledger.validate(d);return d;
 }
 private void seed()throws Exception{
  yes(version()==16,"Seed requires the official v16 package");LocalData.restore(c,fixture());
  JSONArray cards=new JSONArray();for(String[] card:new String[][]{{"upgrade-physical","MACH física","bank-mach","physical","1111"},{"upgrade-digital","MACH digital","bank-mach","digital","2222"},{"upgrade-bank","BancoEstado","bank-bancoestado","physical","3333"}})cards.put(new JSONObject().put("id",card[0]).put("name",card[1]).put("account",card[2]).put("kind",card[3]).put("suffixes",card[4]));PaymentCards.save(c,cards);
  JSONArray sources=new JSONArray();for(String[] source:new String[][]{{"synthetic.bank","Banco sintético","mach","bank-mach"},{"synthetic.wallet","Wallet sintético","wallet",""},{"synthetic.mail","Gmail sintético","email",""}})sources.put(new JSONObject().put("package",source[0]).put("label",source[1]).put("kind",source[2]).put("account",source[3]).put("enabled",true));
  NativeStore.settings(c).edit().putString("sources",sources.toString()).putString("categoryRules",new JSONArray().put(new JSONObject().put("word","instrumentos").put("category","upgrade-custom")).toString()).putString("movementRules",new JSONArray().put(new JSONObject().put("word","abono especial").put("type","income").put("package","synthetic.bank")).toString()).putString("appearance","dark").putString("palette","ocean").putInt("glassIntensity",2).putBoolean("autoPost",false).putBoolean("capture",false).putString("quickAccount","bank-mach").putBoolean("quickKeepOpen",true).putString("peopleSort","amount").putString("widgetAccount","bank-mach").putBoolean("widgetHidden",true).putBoolean("alertChanges",false).putBoolean("alertDetections",false).putBoolean("alertVerifications",false).putBoolean("alertHideAmounts",true).putBoolean("privateScreen",true).putBoolean("detectionTutorialSeen",true).commit();
 }
 private void launch()throws Exception{Activity a=startActivitySync(new Intent().setClassName(c,"cl.bolsillo.app.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));NativeSync.EXEC.submit(()->{}).get(20,TimeUnit.SECONDS);waitForIdleSync();Thread.sleep(800);runOnMainSync(a::finish);waitForIdleSync();}
 private void checkpoint()throws Exception{yes(version()==16,"Checkpoint belongs to v16");JSONObject data=LocalData.read(c);c.getSharedPreferences("upgrade-preservation-qa",0).edit().putString("data",hash(data)).putString("settings",hash(selectedSettings())).putString("cards",hash(PaymentCards.list(c))).putString("balances",hash(new JSONObject(Ledger.balances(data,Ledger.today())))).putString("date",Ledger.today()).commit();}
 private void verify()throws Exception{
  yes(version()==17,"Verification requires installed v17");SharedPreferences prior=c.getSharedPreferences("upgrade-preservation-qa",0);yes(prior.getString("date","").equals(Ledger.today()),"Same-day balances checkpoint exists");JSONObject d=LocalData.read(c);
  yes(hash(d).equals(prior.getString("data","")),"Every financial record, custom field, note, date and history remains exact");yes(hash(selectedSettings()).equals(prior.getString("settings","")),"All selected typed configuration values remain exact");yes(hash(PaymentCards.list(c)).equals(prior.getString("cards","")),"Physical and digital card aliases remain exact");yes(hash(new JSONObject(Ledger.balances(d,Ledger.today()))).equals(prior.getString("balances","")),"Every account balance remains exact");
  yes(d.getJSONArray("transactions").length()==4,"No transaction added or removed");yes(Finance.personalExpense(Ledger.find(d.getJSONArray("transactions"),"upgrade-netflix"))==4990,"Shared expense retains personal amount");yes(Finance.claim(d,"upgrade-netflix:upgrade-share").getLong("amount")==3000,"Shared repayment remains linked");yes(Ledger.find(d.getJSONArray("receivables"),"upgrade-prior").getJSONArray("settlementHistory").length()==2,"Both dated prior-debt installments survive");yes(Finance.claim(d,"prior:upgrade-prior").getLong("amount")==650000,"Historical debt outstanding amount remains exact");yes(PaymentCards.list(c).length()==3,"Three cards still share the original two bank balances");
 }
 @Override public void onStart(){Bundle result=new Bundle();try{c=getTargetContext();String hardware=Build.HARDWARE.toLowerCase(Locale.ROOT);yes(hardware.contains("ranchu")||hardware.contains("goldfish"),"Synthetic emulator required");if(phase.equals("seed")){seed();launch();checkpoint();}else if(phase.equals("checkpoint")){launch();checkpoint();}else if(phase.equals("verify")){launch();verify();}else throw new Exception("Unknown phase");result.putString("result","PASS: "+checks+" official upgrade preservation checks ("+phase+")");result.putString("ledgerHash",hash(LocalData.read(c)));finish(Activity.RESULT_OK,result);}catch(Exception error){result.putString("result","FAIL: "+error.toString());finish(Activity.RESULT_CANCELED,result);}}
}

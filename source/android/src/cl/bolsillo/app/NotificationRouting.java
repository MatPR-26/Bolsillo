package cl.bolsillo.app;
import org.json.*;
/** Deterministic routing. Never select the first of several bank accounts. */
final class NotificationRouting {
 static String resolve(JSONArray accounts,JSONObject source,String last4,JSONObject suffixes)throws Exception{
  String bank=source.optString("kind");if(bank.equals("machbank"))bank="mach";if(bank.equals("other")||bank.isEmpty()){String label=BankNotificationParser.normalize(source.optString("label")+" "+source.optString("package"));if(label.contains("mach"))bank="mach";else if(label.contains("bancoestado")||label.contains("net.veritran.becl.prod"))bank="bancoestado";}if(!BankCatalog.known(bank)&&bank.equals("other")){String detected=BankCatalog.detect(source.optString("label")+" "+source.optString("package"));if(!detected.isEmpty())bank=detected;}if(BankCatalog.known(bank)){String only="";int count=0;for(int i=0;i<accounts.length();i++){JSONObject a=accounts.getJSONObject(i);if(!a.optBoolean("archived")&&a.optString("type").equals("bank")&&a.optString("institution").equals(bank)){if(a.optString("id").equals(source.optString("account")))return a.getString("id");only=a.getString("id");count++;}}return count==1?only:"";}
  String match="";for(int i=0;i<accounts.length();i++){JSONObject a=accounts.getJSONObject(i);if(a.optBoolean("archived"))continue;String id=a.getString("id");if(!last4.isEmpty())for(String suffix:suffixes.optString(id).split("[,; ]+"))if(last4.equals(suffix)){if(!match.isEmpty()&&!match.equals(id))return "";match=id;}}
  if(!match.isEmpty())return match;
  String configured=source.optString("account");if(!configured.isEmpty()){JSONObject a=Ledger.find(accounts,configured);return a!=null&&!a.optBoolean("archived")?configured:"";}
  // Selecting "identify card / ask me" must actually ask; no implicit bank fallback.
  return "";
 }
}

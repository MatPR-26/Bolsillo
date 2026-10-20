package cl.bolsillo.app;
import org.json.*;import java.util.*;
/** Deleting an unused profile never removes money; linked profiles can only be archived. */
final class PersonRecords {
 static boolean linked(JSONObject data,String id)throws Exception{
  JSONArray debts=data.getJSONArray("receivables");for(int i=0;i<debts.length();i++)if(id.equals(debts.getJSONObject(i).optString("personId")))return true;
  for(String key:new String[]{"transactions","recurring"}){JSONArray records=data.getJSONArray(key);for(int i=0;i<records.length();i++){JSONArray shares=records.getJSONObject(i).optJSONArray("shares");if(shares!=null)for(int j=0;j<shares.length();j++)if(id.equals(shares.getJSONObject(j).optString("personId")))return true;}}return false;
 }
 static void remove(JSONObject data,String id)throws Exception{Ledger.require(!linked(data,id),"Este perfil tiene registros vinculados. Puedes archivarlo conservando sus deudas.");JSONArray people=data.getJSONArray("people");for(int i=people.length()-1;i>=0;i--)if(id.equals(people.getJSONObject(i).optString("id")))people.remove(i);}
 static void archive(JSONObject data,String id)throws Exception{JSONObject person=Ledger.find(data.getJSONArray("people"),id);Ledger.require(person!=null,"El perfil ya no existe");person.put("archived",true);}
 static List<JSONObject> sorted(JSONArray list,String mode,Map<String,Long> amounts,Map<String,String> dates)throws Exception{List<JSONObject> out=new ArrayList<>();for(int i=0;i<list.length();i++)out.add(list.getJSONObject(i));out.sort((a,b)->{int order=mode.equals("amount")?Long.compare(amounts.getOrDefault(b.optString("id",b.optString("key")),b.optLong("amount")),amounts.getOrDefault(a.optString("id",a.optString("key")),a.optLong("amount"))):mode.equals("date")?dates.getOrDefault(b.optString("id"),b.optString("date")).compareTo(dates.getOrDefault(a.optString("id"),a.optString("date"))):0;return order!=0?order:java.text.Collator.getInstance(new Locale("es","CL")).compare(a.optString("name"),b.optString("name"));});return out;}
}

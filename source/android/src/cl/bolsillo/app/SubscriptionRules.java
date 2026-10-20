package cl.bolsillo.app;
import org.json.*;import java.time.*;import java.time.temporal.ChronoUnit;
final class SubscriptionRules {
 /** Advance only an unambiguous, opted-in schedule. The notification is the only charge. */
 static void apply(JSONObject data,JSONObject tx)throws Exception{
  if(!tx.optString("source").equals("notification")||!tx.optString("status").equals("posted")||!tx.optString("type").equals("expense")||tx.has("subscriptionId"))return;
  JSONObject match=null;JSONArray rules=data.getJSONArray("recurring");String merchant=BankNotificationParser.normalize(tx.optString("note"));
  for(int i=0;i<rules.length();i++){JSONObject r=rules.getJSONObject(i);if(!r.optBoolean("active")||!r.optBoolean("matchNotifications")||r.optBoolean("automatic")||!r.optString("type").equals("expense")||!r.optString("account").equals(tx.optString("account"))||r.optLong("amount")!=tx.optLong("amount"))continue;String name=BankNotificationParser.normalize(r.optString("name"));if(name.length()<3||!merchant.contains(name))continue;long days=Math.abs(ChronoUnit.DAYS.between(LocalDate.parse(r.getString("next")),LocalDate.parse(tx.getString("date"))));if(days>5)continue;if(match!=null)return;match=r;}
  if(match!=null){tx.put("subscriptionId",match.getString("id")).put("service",match.optString("service"));if(match.optJSONArray("shares")!=null&&tx.optJSONArray("shares").length()==0)tx.put("shares",new JSONArray(match.getJSONArray("shares").toString()));match.put("lastPaid",tx.getString("date")).put("next",Ledger.next(match.getString("next"),match.getString("frequency"),match.optInt("anchorDay")));}
 }
}

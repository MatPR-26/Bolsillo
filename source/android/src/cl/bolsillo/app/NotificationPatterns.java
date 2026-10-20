package cl.bolsillo.app;
import android.content.Context;import org.json.*;
final class NotificationPatterns {
 static BankNotificationParser.Candidate parse(Context c,String text,String title,JSONObject source){String kind=source.optString("kind"),bank=PaymentCards.kind(source.optString("label"),source.optString("package"));boolean dedicated=BankCatalog.known(kind)||BankCatalog.known(bank);String forced="";try{JSONArray rules=new JSONArray(NativeStore.settings(c).getString("movementRules","[]"));String normalized=BankNotificationParser.normalize(text);for(int i=0;i<rules.length();i++){JSONObject r=rules.getJSONObject(i);if(r.optString("package").equals(source.optString("package"))&&normalized.contains(BankNotificationParser.normalize(r.optString("word")))){String type=r.optString("type");if(!forced.isEmpty()&&!forced.equals(type))return null;forced=type;}}}catch(Exception ignored){}return BankNotificationParser.movement(text,title,kind.equals("wallet"),dedicated,forced);}
}

package cl.bolsillo.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.*;
import java.util.*;

/** Optional Android notices; all history and deduplication state use the encrypted local store. */
final class LocalAlerts {
 static final String CHANGES="bolsillo_changes",DETECTIONS="bolsillo_detections",VERIFICATIONS="bolsillo_verifications";
 static final int HISTORY_LIMIT=500,SIGNATURE_LIMIT=24000;
 static void channels(Context c){
  NotificationManager manager=c.getSystemService(NotificationManager.class);if(manager==null)return;
  channel(manager,CHANGES,"Cambios de tus registros","Movimientos y montos guardados en Bolsillo",NotificationManager.IMPORTANCE_LOW);
  channel(manager,DETECTIONS,"Movimientos detectados","Movimientos reconocidos de las apps que elegiste",NotificationManager.IMPORTANCE_DEFAULT);
  channel(manager,VERIFICATIONS,"Revisiones pendientes","Avisos que necesitan tu confirmación",NotificationManager.IMPORTANCE_DEFAULT);
 }
 private static void channel(NotificationManager manager,String id,String name,String description,int importance){NotificationChannel channel=new NotificationChannel(id,name,importance);channel.setDescription(description);channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);channel.setShowBadge(true);manager.createNotificationChannel(channel);}
 static boolean permission(Context c){return Build.VERSION.SDK_INT<33||c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;}
 static boolean available(Context c){NotificationManager manager=c.getSystemService(NotificationManager.class);return permission(c)&&manager!=null&&manager.areNotificationsEnabled();}
 static boolean enabled(Context c,String id){channels(c);NotificationManager m=c.getSystemService(NotificationManager.class);NotificationChannel channel=m==null?null:m.getNotificationChannel(id);return available(c)&&NativeStore.settings(c).getBoolean(preference(id),true)&&channel!=null&&channel.getImportance()!=NotificationManager.IMPORTANCE_NONE;}
 static boolean test(Context c)throws Exception{if(!enabled(c,VERIFICATIONS))return false;JSONObject event=new JSONObject().put("id","test-"+System.currentTimeMillis()).put("channel",VERIFICATIONS).put("title","Bolsillo está listo").put("body","Este es un aviso real del teléfono. Tus movimientos no se han modificado.").put("hiddenBody","Los avisos de Bolsillo están activos.").put("section","alerts");post(c,event);NotificationManager m=c.getSystemService(NotificationManager.class);for(android.service.notification.StatusBarNotification notice:m.getActiveNotifications())if(("bolsillo-local:"+event.optString("id")).equals(notice.getTag()))return true;return false;}
 static String preference(String channel){return channel.equals(DETECTIONS)?"alertDetections":channel.equals(VERIFICATIONS)?"alertVerifications":"alertChanges";}
 static boolean hidden(Context c){return NativeStore.settings(c).getBoolean("alertHideAmounts",false);}
 static JSONArray history(Context c)throws Exception{synchronized(NativeStore.class){JSONArray list=NativeStore.state(c).optJSONArray("localAlertHistory");return list==null?new JSONArray():new JSONArray(list.toString());}}
 static void clearHistory(Context c)throws Exception{synchronized(NativeStore.class){JSONObject state=NativeStore.state(c);state.put("localAlertHistory",new JSONArray());NativeStore.write(c,state);}NotificationManager manager=c.getSystemService(NotificationManager.class);if(manager!=null)manager.cancelAll();}
 static void changed(Context c,JSONObject before,JSONObject after){
  try{List<JSONObject> changes=AlertEvents.changes(before,after);if(changes.isEmpty())return;
   synchronized(NativeStore.class){JSONObject state=NativeStore.state(c),signatures=signatures(state);List<JSONObject> fresh=new ArrayList<>();for(JSONObject change:changes){String key=change.getString("entity"),signature=change.getString("signature");if(!signature.equals(signatures.optString(key))){fresh.add(change);signatures.put(key,signature);}}if(fresh.isEmpty())return;
    JSONObject event;if(fresh.size()==1)event=new JSONObject(fresh.get(0).toString());else{JSONArray entries=new JSONArray();StringBuilder summary=new StringBuilder();for(JSONObject change:fresh){if(entries.length()<40)entries.put(change);if(summary.length()<300){if(summary.length()>0)summary.append('\n');summary.append(change.optString("title")).append(" · ").append(change.optString("body"));}}event=new JSONObject().put("title",fresh.size()+" registros actualizados").put("body",summary.toString()).put("hiddenBody","Consulta los cambios guardados en Bolsillo.").put("section",fresh.get(0).optString("section")).put("entries",entries).put("changeCount",fresh.size());}
    event.put("id",Ledger.id()).put("channel",CHANGES).put("at",System.currentTimeMillis());save(c,state,signatures,event);
   }
  }catch(Exception e){failure(c);}
 }
 static void detected(Context c,JSONObject evidence,JSONObject tx){
  if(evidence==null||evidence.optBoolean("suppressed"))return;
  try{synchronized(NativeStore.class){JSONObject state=NativeStore.state(c),signatures=signatures(state);boolean review=tx==null||!"posted".equals(tx.optString("status"));String id=evidence.optString("id");if(id.isEmpty())return;
   String signature=tx==null?evidence.optString("reason")+"|"+evidence.optString("amount")+"|"+evidence.optString("account"):AlertEvents.signature("transactions",tx);
   String key="detection:"+id;if(signature.equals(signatures.optString(key)))return;signatures.put(key,signature);
   JSONObject event=new JSONObject().put("id",Ledger.id()).put("channel",review?VERIFICATIONS:DETECTIONS).put("at",System.currentTimeMillis()).put("reviewId",review?id:"").put("detectionId",id).put("section","transactions");
   String label=evidence.optString("label","App elegida"),merchant=evidence.optString("merchant","Movimiento");
   String body=(evidence.optLong("amount")>0?AlertEvents.money(evidence.optLong("amount")):"Importe por verificar")+" · "+merchant+" · "+label+(review?"\n"+evidence.optString("reason","Confirma los datos antes de registrar"):" · Guardado en tu registro local");
   if(tx!=null&&!review){JSONObject cache=state.optJSONObject("cache");if(cache!=null){JSONObject data=cache.getJSONObject("data");String account=AlertEvents.name(data,"accounts",tx.optString("account"));Long balance=Ledger.balances(data,Ledger.today()).get(tx.optString("account"));if(!account.isEmpty())body+="\n"+account+": "+(balance==null?"saldo registrado":AlertEvents.money(balance)+" de saldo registrado");}}
   event.put("title",review?"Revisa un movimiento":evidence.optBoolean("confirmedByUser")?"Detección confirmada":"Movimiento detectado").put("body",body).put("hiddenBody",review?"Hay un aviso pendiente de verificar en Bolsillo.":"Un movimiento se guardó en tu registro local.");
   if(tx!=null){event.put("txId",tx.optString("id"));signatures.put("transactions:"+tx.optString("id"),AlertEvents.signature("transactions",tx));if("posted".equals(tx.optString("status")))resolveHistory(c,state,id,tx.optString("id"));}
   save(c,state,signatures,event);
  }}catch(Exception e){failure(c);}
 }
 static void resolved(Context c,String detectionId,JSONObject tx){try{synchronized(NativeStore.class){JSONObject state=NativeStore.state(c);resolveHistory(c,state,detectionId,tx==null?"":tx.optString("id"));NativeStore.write(c,state);} }catch(Exception e){failure(c);}}
 private static void resolveHistory(Context c,JSONObject state,String detectionId,String txId)throws Exception{JSONArray history=state.optJSONArray("localAlertHistory");if(history==null)return;NotificationManager manager=c.getSystemService(NotificationManager.class);for(int i=0;i<history.length();i++){JSONObject event=history.getJSONObject(i);if(detectionId.equals(event.optString("reviewId"))){event.put("resolved",true).put("resolvedAt",System.currentTimeMillis());if(!txId.isEmpty())event.put("txId",txId);if(manager!=null)manager.cancel("bolsillo-local:"+event.optString("id"),event.optString("id").hashCode());}}}
 private static JSONObject signatures(JSONObject state)throws Exception{JSONObject signatures=state.optJSONObject("localAlertSignatures");if(signatures==null)signatures=new JSONObject();return signatures;}
 private static void save(Context c,JSONObject state,JSONObject signatures,JSONObject event)throws Exception{
  while(signatures.length()>SIGNATURE_LIMIT){Iterator<String> keys=signatures.keys();if(!keys.hasNext())break;signatures.remove(keys.next());}
  for(String key:new String[]{"title","body","hiddenBody"}){String value=event.optString(key);int max=key.equals("body")?3000:160;if(value.length()>max)event.put(key,value.substring(0,max-1)+"…");}
  JSONArray history=state.optJSONArray("localAlertHistory");if(history==null)history=new JSONArray();history.put(event);state.put("localAlertHistory",NativeStore.tail(history,HISTORY_LIMIT)).put("localAlertSignatures",signatures);NativeStore.write(c,state);post(c,event);
 }
 private static void post(Context c,JSONObject event){
  try{channels(c);String channel=event.optString("channel",CHANGES);if(!NativeStore.settings(c).getBoolean(preference(channel),true)||!available(c))return;NotificationManager manager=c.getSystemService(NotificationManager.class);NotificationChannel system=manager.getNotificationChannel(channel);if(system==null||system.getImportance()==NotificationManager.IMPORTANCE_NONE)return;
   Intent intent=new Intent(c,MainActivity.class).setData(android.net.Uri.parse("bolsillo://aviso/"+android.net.Uri.encode(event.optString("id")))).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("localAlertId",event.optString("id"));
   if(!event.optString("txId").isEmpty())intent.putExtra("localAlertTx",event.optString("txId"));else if(!event.optString("reviewId").isEmpty())intent.putExtra("localAlertReview",event.optString("reviewId"));else intent.putExtra("localAlertSection",event.optString("section"));
   int request=event.optString("id").hashCode();PendingIntent tap=PendingIntent.getActivity(c,request,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   String body=hidden(c)?event.optString("hiddenBody"):event.optString("body");Notification publicVersion=new Notification.Builder(c,channel).setSmallIcon(R.drawable.ms_account_balance_wallet).setContentTitle("Bolsillo").setContentText("Tienes una actualización en tus registros.").build();
   Notification.Builder builder=new Notification.Builder(c,channel).setSmallIcon(R.drawable.ms_account_balance_wallet).setContentTitle(event.optString("title")).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(tap).setAutoCancel(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(publicVersion).setCategory(channel.equals(VERIFICATIONS)?Notification.CATEGORY_REMINDER:Notification.CATEGORY_STATUS);
   if(channel.equals(VERIFICATIONS)&&!event.optString("reviewId").isEmpty())builder.addAction(new Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(c,R.drawable.ms_check),"Revisar",tap).build());
   Notification.Action category=categoryAction(c,event);if(category!=null)builder.addAction(category);
   Notification notice=builder.build();
   manager.notify("bolsillo-local:"+event.optString("id"),request,notice);
  }catch(SecurityException ignored){}catch(Exception e){failure(c);}
 }
 static Notification.Action categoryAction(Context c,JSONObject event){
  try{
   String id=event.optString("txId");if(id.isEmpty()||!event.optString("reviewId").isEmpty())return null;
   JSONObject state=NativeStore.state(c),cache=state.optJSONObject("cache"),data=cache==null?null:cache.optJSONObject("data");if(data==null)return null;
   JSONObject tx=Ledger.find(data.getJSONArray("transactions"),id);
   if(tx==null||!"posted".equals(tx.optString("status"))||!QuickCategory.needsCategory(data,tx))return null;
   String alert=event.optString("id");Intent intent=new Intent(c,MainActivity.class).setData(android.net.Uri.parse("bolsillo://categoria/"+android.net.Uri.encode(alert)+"/"+android.net.Uri.encode(id))).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("quickCategoryTx",id).putExtra("localAlertId",alert);
   PendingIntent tap=PendingIntent.getActivity(c,("category:"+alert).hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification.Action.Builder action=new Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(c,R.drawable.ms_sell),"Categoría",tap);
   if(Build.VERSION.SDK_INT>=31)action.setAuthenticationRequired(true);
   return action.build();
  }catch(Exception ignored){return null;}
 }
 static void markOpened(Context c,String alertId){if(alertId==null||alertId.isEmpty())return;try{synchronized(NativeStore.class){JSONObject state=NativeStore.state(c);JSONArray history=state.optJSONArray("localAlertHistory");boolean changed=false;if(history!=null)for(int i=0;i<history.length();i++){JSONObject event=history.getJSONObject(i);if(alertId.equals(event.optString("id"))&&!event.optBoolean("opened")){event.put("opened",true);changed=true;}}if(changed)NativeStore.write(c,state);}}catch(Exception e){failure(c);}}
 private static void failure(Context c){NativeStore.settings(c).edit().putString("localAlertStatus","No se pudo actualizar un aviso; tus registros financieros siguen guardados.").apply();}
}

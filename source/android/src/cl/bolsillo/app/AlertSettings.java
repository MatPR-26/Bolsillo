package cl.bolsillo.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.os.Build;
import android.provider.Settings;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Settings and a compact, paged history using the app's shared components. */
final class AlertSettings {
 static void show(MainActivity a){LocalAlerts.channels(a);Forms.Form f=new Forms.Form(a,"Avisos de Bolsillo");
  f.note(LocalAlerts.available(a)?"Avisos del teléfono activos":"Activa el permiso para ver los avisos en tu teléfono.");
  toggle(a,f,"Cambios guardados","alertChanges");toggle(a,f,"Movimientos detectados","alertDetections");toggle(a,f,"Revisiones pendientes","alertVerifications");
  Switch privacy=f.toggle("Ocultar importes",LocalAlerts.hidden(a));privacy.setOnCheckedChangeListener((v,on)->NativeStore.settings(a).edit().putBoolean("alertHideAmounts",on).apply());
  if(!LocalAlerts.available(a))f.action("Activar avisos del teléfono",()->permission(a));else f.action("Sonido y canales de Android",()->systemSettings(a));
  f.action("Enviar notificación de prueba",()->a.attempt(()->{if(!LocalAlerts.enabled(a,LocalAlerts.VERIFICATIONS)){NativeSettings.info(a,"Activa el permiso y el canal Revisiones pendientes para recibir la prueba.");permission(a);return;}NativeSettings.info(a,LocalAlerts.test(a)?"Aviso enviado. Revisa el centro de notificaciones.":"Android no mostró el aviso. Revisa los canales de Bolsillo.");}));
  for(String channel:new String[]{LocalAlerts.CHANGES,LocalAlerts.DETECTIONS,LocalAlerts.VERIFICATIONS})if(LocalAlerts.available(a)&&!LocalAlerts.enabled(a,channel)&&NativeStore.settings(a).getBoolean(LocalAlerts.preference(channel),true)){String label=channel.equals(LocalAlerts.CHANGES)?"Cambios":channel.equals(LocalAlerts.DETECTIONS)?"Detectados":"Revisión";f.action(label+" · canal desactivado",()->a.startActivity(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,a.getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,channel)));}
  f.action("Historial de avisos",()->{f.dialog.dismiss();history(a);});f.note("Se informa sobre tus registros locales. Bolsillo no consulta el saldo del banco.");f.action("Cerrar",f.dialog::dismiss);
 }
 private static void toggle(MainActivity a,Forms.Form f,String label,String key){Switch toggle=f.toggle(label,NativeStore.settings(a).getBoolean(key,true));toggle.setOnCheckedChangeListener((v,on)->{NativeStore.settings(a).edit().putBoolean(key,on).apply();if(on&&!LocalAlerts.permission(a))permission(a);});}
 static void permission(MainActivity a){LocalAlerts.channels(a);if(Build.VERSION.SDK_INT>=33&&!LocalAlerts.permission(a)){if(NativeStore.settings(a).getBoolean("alertPermissionRequested",false)&&!a.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)){systemSettings(a);return;}NativeStore.settings(a).edit().putBoolean("alertPermissionRequested",true).apply();a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},64);}else systemSettings(a);}
 private static void systemSettings(MainActivity a){a.startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,a.getPackageName()));}
 static void history(MainActivity a)throws Exception{history(a,"",0);}
 private static void history(MainActivity a,String filter,int page)throws Exception{
  JSONArray all=LocalAlerts.history(a);List<JSONObject> list=new ArrayList<>();for(int i=all.length()-1;i>=0;i--){JSONObject event=all.getJSONObject(i);if(filter.isEmpty()||filter.equals(event.optString("channel")))list.add(event);}
  Forms.Form f=new Forms.Form(a,"Historial de avisos");LinearLayout filters=Modern.row(a);String[] ids={"",LocalAlerts.CHANGES,LocalAlerts.DETECTIONS,LocalAlerts.VERIFICATIONS},names={"Todos","Cambios","Detectados","Revisión"};for(int i=0;i<ids.length;i++){final String id=ids[i];TextView pill=Glass.pill(a,names[i],filter.equals(id));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(a.dp(2),0,a.dp(2),0);filters.addView(pill,p);Modern.press(pill,()->a.attempt(()->{f.dialog.dismiss();history(a,id,0);}));}f.box.addView(filters);
  int pages=Math.max(1,(list.size()+14)/15),current=Math.max(0,Math.min(page,pages-1));if(list.isEmpty())f.note("Aquí aparecerán los cambios, detecciones y revisiones de Bolsillo.");
  String day="";for(int i=current*15;i<Math.min(list.size(),(current+1)*15);i++){JSONObject event=list.get(i);String date=Detection.date(event.optLong("at"));if(!date.equals(day)){TextView heading=Modern.text(a,date,12,Glass.subtle(a),true);heading.setPadding(a.dp(2),a.dp(16),0,a.dp(5));f.box.addView(heading);day=date;}
   String icon=event.optString("channel").equals(LocalAlerts.VERIFICATIONS)?"check":event.optString("channel").equals(LocalAlerts.DETECTIONS)?"sparkle":"activity";
   String body=LocalAlerts.hidden(a)?event.optString("hiddenBody"):event.optString("body");String state=event.optBoolean("resolved")?"Revisión resuelta":event.optBoolean("opened")?"Consultado":"";
   java.time.ZonedDateTime time=java.time.Instant.ofEpochMilli(event.optLong("at")).atZone(java.time.ZoneId.of("America/Santiago"));String subtitle=String.format(java.util.Locale.ROOT,"%02d:%02d",time.getHour(),time.getMinute())+(state.isEmpty()?"":" · "+state)+" · "+body;
   Modern.add(f.box,Modern.item(a,icon,event.optString("title"),subtitle,"",Glass.accent(a),()->a.attempt(()->{f.dialog.dismiss();detail(a,event);})),6);
  }
  if(pages>1){f.note((current+1)+" / "+pages);if(current>0)f.action("Anteriores",()->{f.dialog.dismiss();history(a,filter,current-1);});if(current<pages-1)f.action("Más avisos",()->{f.dialog.dismiss();history(a,filter,current+1);});}
  if(all.length()>0){f.advanced();f.action("Vaciar historial de avisos",()->a.confirm("Vaciar historial","Los movimientos, deudas y saldos se conservarán.",()->{LocalAlerts.clearHistory(a);f.dialog.dismiss();history(a);}));f.collapseAdvanced();}
  f.action("Cerrar",f.dialog::dismiss);
 }
 private static void detail(MainActivity a,JSONObject event)throws Exception{LocalAlerts.markOpened(a,event.optString("id"));Forms.Form f=new Forms.Form(a,event.optString("title","Aviso de Bolsillo"));f.note(java.time.Instant.ofEpochMilli(event.optLong("at")).atZone(java.time.ZoneId.of("America/Santiago")).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm",new java.util.Locale("es","CL"))));f.note(LocalAlerts.hidden(a)?event.optString("hiddenBody"):event.optString("body"));
  if(event.optBoolean("resolved"))f.note("Esta revisión ya fue resuelta.");f.action("Abrir registro",()->{f.dialog.dismiss();openEvent(a,event);});f.action("Cerrar",f.dialog::dismiss);
 }
 static boolean open(MainActivity a,Intent intent){if(intent==null||!intent.hasExtra("localAlertId"))return false;String id=intent.getStringExtra("localAlertId");if(id==null||id.isEmpty())return false;String tx=intent.getStringExtra("localAlertTx"),review=intent.getStringExtra("localAlertReview"),section=intent.getStringExtra("localAlertSection");for(String key:new String[]{"localAlertId","localAlertTx","localAlertReview","localAlertSection"})intent.removeExtra(key);LocalAlerts.markOpened(a,id);a.handler.post(()->a.attempt(()->{JSONObject saved=Ledger.find(LocalAlerts.history(a),id);JSONObject event=saved==null?new JSONObject().put("txId",tx).put("reviewId",review).put("section",section):saved;openEvent(a,event);}));return true;}
 private static void openEvent(MainActivity a,JSONObject event)throws Exception{if(event.optString("section").equals("alerts")){show(a);return;}a.data=LocalData.read(a);String txId=event.optString("txId");if(!txId.isEmpty()){JSONObject tx=Ledger.find(a.data.getJSONArray("transactions"),txId);if(tx!=null){a.month=tx.optString("date",Ledger.today()).substring(0,7);a.filter="";a.accountFilter="";a.grouped=false;a.navigate("Movimientos");Forms.details(a,tx);return;}}
  if(!event.optString("reviewId").isEmpty()&&!event.optBoolean("resolved")){a.autoTab="Revisión";a.navigate("Automático");JSONArray inbox=NativeStore.state(a).optJSONArray("detectionInbox");JSONObject evidence=inbox==null?null:Ledger.find(inbox,event.optString("reviewId"));if(evidence!=null)AutomationScreen.reviewItem(a,evidence);return;}
  if(event.optBoolean("resolved")&&!event.optString("reviewId").isEmpty()){NativeSettings.info(a,"Este aviso ya fue revisado y no está pendiente.");return;}
  String section=event.optString("section");a.navigate(section.equals("accounts")?"Cuentas":section.equals("receivables")?"Personas":section.equals("planned")?"Previstos":section.equals("recurring")?"Suscripciones":section.equals("budgets")?"Presupuestos":section.equals("goals")?"Metas":"Historial");
 }
}

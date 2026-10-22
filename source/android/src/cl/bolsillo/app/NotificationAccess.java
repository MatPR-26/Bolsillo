package cl.bolsillo.app;

import android.app.*;
import android.content.*;
import android.provider.Settings;
import android.widget.*;

/** Explicit consent and honest status for notification reading, separate from sending our own notices. */
final class NotificationAccess {
 static final String PENDING="notificationAccessPendingCapture";
 enum Action { GRANT, ENABLE, SELECT_APPS, RECONNECT, MANAGE }
 static final class State {
  final boolean permission,capture,connected;final int apps;
  State(boolean permission,boolean capture,boolean connected,int apps){this.permission=permission;this.capture=capture;this.connected=connected;this.apps=Math.max(0,apps);}
  boolean ready(){return permission&&capture&&connected&&apps>0;}
  Action action(){return !permission?Action.GRANT:!capture?Action.ENABLE:apps==0?Action.SELECT_APPS:!connected?Action.RECONNECT:Action.MANAGE;}
  String title(){switch(action()){case GRANT:return "Falta acceso a notificaciones";case ENABLE:return "Detección pausada";case SELECT_APPS:return "Elige tus apps";case RECONNECT:return "Esperando conexión";default:return "Detección activa";}}
  String button(){switch(action()){case GRANT:return "Configurar";case ENABLE:return "Activar";case SELECT_APPS:return "Elegir";case RECONNECT:return "Reconectar";default:return "Gestionar";}}
  String detail(){return apps+(apps==1?" app elegida":" apps elegidas")+" · "+(!permission?"acceso pendiente":!capture?"lectura pausada":!connected?"sin conexión del lector":"acceso permitido");}
 }
 static State state(MainActivity a){return new State(NativeSettings.permission(a),NativeStore.settings(a).getBoolean("capture",false),NativeStore.settings(a).getBoolean("listenerConnected",false),a.activeSources());}

 /** Called when returning from Android settings; consent alone must never be shown as a grant. */
 static void onResume(MainActivity a){
  android.content.SharedPreferences settings=NativeStore.settings(a);
  if(settings.getBoolean(PENDING,false)){
   android.content.SharedPreferences.Editor change=settings.edit().remove(PENDING);
   if(NativeSettings.permission(a))change.putBoolean("capture",true);
   change.apply();
  }
  if(a.tutorial!=null)a.tutorial.refreshAccess();
 }

 static SheetDialog primary(MainActivity a){
  State s=state(a);
  switch(s.action()){
   case GRANT:return disclose(a,null);
   case ENABLE:NativeStore.settings(a).edit().putBoolean("capture",true).apply();BankNotificationListener.reconnect(a);a.render(false);return null;
   case SELECT_APPS:a.attempt(()->AutomationScreen.selectApps(a));return null;
   case RECONNECT:BankNotificationListener.reconnect(a);NativeSettings.info(a,"Conexión solicitada. La lectura sigue activada.");a.render(false);return null;
   default:return manage(a);
  }
 }

 static SheetDialog disclose(MainActivity a,Runnable beforeSettings){
  LinearLayout box=NativeSettings.panel(a,"Leer avisos elegidos");
  Modern.add(box,Modern.text(a,"Android permite que Bolsillo reciba notificaciones de otras apps. Bolsillo procesa solo las que elijas para reconocer montos, comercios y movimientos.",14,Glass.ink(a),false),12);
  Modern.add(box,Modern.text(a,"El análisis se hace en este teléfono. Se guardan datos del movimiento y su revisión. No se solicita tu clave bancaria; Gmail aporta únicamente sus avisos visibles, sin acceso al buzón.",13,Glass.subtle(a),false),12);
  Modern.add(box,Modern.text(a,"Puedes pausar la lectura o retirar el acceso desde Android. Sin este acceso puedes seguir registrando manualmente.",13,Glass.subtle(a),false),12);
  SheetDialog dialog=new SheetDialog(a,box);
  Button consent=Glass.button(a,"Aceptar y abrir ajustes");consent.setTag("notification-access-consent");dialog.footer.addView(consent);
  Button later=secondary(a,"Ahora no");later.setTag("notification-access-later");dialog.footer.addView(later);
  later.setOnClickListener(v->dialog.dismiss());
  consent.setOnClickListener(v->{
   // The app-level choice is applied only after Android reports that access was granted.
   NativeStore.settings(a).edit().putBoolean(PENDING,true).apply();
   dialog.dismiss();if(beforeSettings!=null)beforeSettings.run();
   openSettings(a,true);
  });
  dialog.show();return dialog;
 }

 static void configure(MainActivity a,Runnable beforeSettings){
  if(NativeSettings.permission(a)){if(beforeSettings!=null)beforeSettings.run();openSettings(a,false);}
  else disclose(a,beforeSettings);
 }
 private static void openSettings(MainActivity a,boolean clearPendingOnFailure){
  try{a.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}
  catch(ActivityNotFoundException|SecurityException error){if(clearPendingOnFailure)NativeStore.settings(a).edit().remove(PENDING).apply();NativeSettings.info(a,"Abre Ajustes de Android y busca Acceso a notificaciones.");if(a.tutorial!=null)a.tutorial.resumeAfterSettings();}
 }
 static SheetDialog manage(MainActivity a){
  LinearLayout box=NativeSettings.panel(a,"Detección activa");
  Modern.add(box,Modern.text(a,"Bolsillo procesa avisos de las apps elegidas. Pausar no elimina movimientos ni retira el permiso de Android.",14,Glass.subtle(a),false),12);
  SheetDialog dialog=new SheetDialog(a,box);
  Button pause=Glass.button(a,"Pausar detección"),access=secondary(a,"Gestionar acceso de Android"),close=secondary(a,"Volver");
  dialog.footer.addView(pause);dialog.footer.addView(access);dialog.footer.addView(close);
  pause.setOnClickListener(v->{NativeStore.settings(a).edit().putBoolean("capture",false).remove(PENDING).apply();dialog.dismiss();a.render(false);});
  access.setOnClickListener(v->{dialog.dismiss();configure(a,null);});close.setOnClickListener(v->dialog.dismiss());dialog.show();return dialog;
 }
 private static Button secondary(MainActivity a,String text){Button button=Glass.button(a,"Volver");button.setText(text);return button;}
 static String permissionSummary(MainActivity a){State s=state(a);return "Acceso de lectura: "+(s.permission?"permitido":"pendiente")+" · Captura: "+(s.capture?"activada":"pausada")+"\n"+s.apps+(s.apps==1?" app elegida":" apps elegidas")+" · Lector: "+(s.permission&&s.connected?"conectado":"sin conexión");}
}

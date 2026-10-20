package cl.bolsillo.app;
import android.content.*;import android.content.pm.PackageInstaller;
/** Installer result is accepted only for the session explicitly started by the user. */
public class UpdateInstallReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){if(!"cl.bolsillo.INSTALL_RESULT".equals(i.getAction())||i.getIntExtra(PackageInstaller.EXTRA_SESSION_ID,-2)!=AppUpdates.prefs(c).getInt("installSession",-1))return;int status=i.getIntExtra(PackageInstaller.EXTRA_STATUS,PackageInstaller.STATUS_FAILURE);if(status==PackageInstaller.STATUS_PENDING_USER_ACTION){Intent confirm=i.getParcelableExtra(Intent.EXTRA_INTENT);if(confirm!=null)try{confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(confirm);}catch(Exception error){AppUpdates.prefs(c).edit().putString("error","Abre Actualizaciones para continuar la instalación").apply();}}else{AppUpdates.prefs(c).edit().remove("installSession").putString("error",status==PackageInstaller.STATUS_SUCCESS?"":status==PackageInstaller.STATUS_FAILURE_ABORTED?"Instalación cancelada. Tus registros se conservan.":"Android no pudo instalar esta actualización").apply();}AppUpdates.changed();}
}

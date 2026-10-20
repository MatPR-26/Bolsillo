package cl.bolsillo.app;
import android.app.*;import android.os.*;
/** Synthetic notification source, installed only in the isolated emulator. */
public class Notifier extends Activity {
 @Override public void onCreate(Bundle state){super.onCreate(state);NotificationManager m=getSystemService(NotificationManager.class);m.createNotificationChannel(new NotificationChannel("purchases","Synthetic purchases",NotificationManager.IMPORTANCE_DEFAULT));m.notify(7,new Notification.Builder(this,"purchases").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Banco de pruebas").setContentText("Compra $1777 en CAFETERIA TEST").build());m.notify(8,new Notification.Builder(this,"purchases").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("MACHBANK").setContentText("Has recibido una transferencia por 12600. Ingresa para revisar tu saldo").build());finish();}
}

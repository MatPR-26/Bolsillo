package cl.bolsillo.app;
import android.content.*;import android.os.*;import org.json.*;import java.util.*;import java.util.concurrent.*;
public final class NativeSync {
 static final ExecutorService EXEC=Executors.newSingleThreadExecutor();
 interface Callback {void done(boolean ok,String message);}
 static void run(Context context,Callback callback){Context c=context.getApplicationContext();EXEC.execute(()->{boolean ok=false;String message;try{message=sync(c);ok=true;}catch(Exception e){message=e.getMessage()==null?"No se pudo actualizar el registro local":e.getMessage();}boolean result=ok;String text=message;if(callback!=null)new Handler(Looper.getMainLooper()).post(()->callback.done(result,text));});}
 static String sync(Context c)throws Exception {LocalData.initialize(c);BolsilloWidget.updateAll(c);return "Guardado en este teléfono";}
 static JSONObject transaction(String type,long amount,String account,String to,String category,String note,String source,String sourceId)throws Exception {JSONObject t=new JSONObject();t.put("id",sourceId.isEmpty()?UUID.randomUUID().toString():"detect-"+sourceId);t.put("type",type);t.put("amount",amount);t.put("account",account);t.put("to",to);t.put("category",category);t.put("date",java.time.LocalDate.now(java.time.ZoneId.of("America/Santiago")).toString());t.put("createdAt",System.currentTimeMillis());t.put("note",note);t.put("tags",source.equals("notification")?"detectado":"rápido");t.put("excluded",false);t.put("status",source.equals("notification")?"pending":"posted");t.put("installmentGroup","");t.put("installment",0);t.put("installments",1);t.put("shares",new JSONArray());t.put("receivable","");t.put("service","");t.put("source",source);t.put("sourceId",sourceId);return t;}
}



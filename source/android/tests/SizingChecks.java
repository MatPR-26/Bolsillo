package cl.bolsillo.app;

import android.app.Dialog;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Real 130%-font financial controls. The host sets font_scale and restores it. */
final class SizingChecks {
 static int checks;
 static void yes(boolean ok,String detail)throws Exception{checks++;if(!ok)throw new Exception(detail);}
 static int available(EditText field){return field.getWidth()-field.getCompoundPaddingLeft()-field.getCompoundPaddingRight();}
 static void report(Instrumentation test,String name,EditText field,int rendered){Bundle result=new Bundle();result.putString("sizingBounds",name+": fontScale="+field.getResources().getConfiguration().fontScale+" width="+field.getWidth()+" available="+available(field)+" rendered="+rendered+" textPx="+field.getTextSize()+" scrollX="+field.getScrollX());test.sendStatus(0,result);}
 static void shot(Instrumentation test,Context c,String name)throws Exception{Thread.sleep(600);test.waitForIdleSync();Bitmap image=test.getUiAutomation().takeScreenshot();yes(image!=null,"Sizing screenshot must be capturable");File directory=new File(c.getExternalFilesDir(null),"qa");directory.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(directory,name+".png"))){yes(image.compress(Bitmap.CompressFormat.PNG,100,out),"Sizing PNG must be written");}finally{image.recycle();}}
 static int run(Instrumentation test,Context c)throws Exception{
  checks=0;NativeSync.EXEC.submit(()->{}).get(10,TimeUnit.SECONDS);
  Map<String,Object> encrypted=VisualChecks.preferences(c.getSharedPreferences("secure",0)),settings=VisualChecks.preferences(NativeStore.settings(c));JSONObject finance=LocalData.read(c);
  MainActivity activity=null;final Dialog[] opened={null};List<String> failures=new ArrayList<>();
  try{
   NativeStore.settings(c).edit().putBoolean("capture",false).putBoolean("privateScreen",false).putString("appearance","dark").putString("palette","prisma").commit();
   activity=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));test.waitForIdleSync();final MainActivity a=activity;
   yes(a.getResources().getConfiguration().fontScale>=1.29f,"Run sizing with the real device font_scale set to 1.3 or larger");
   final EditText[] date={null};final int[] dateWidth={0};
   VisualChecks.main(test,()->{Forms.Form form=new Forms.Form(a,"Fecha legible");opened[0]=form.dialog;date[0]=form.field("Fecha del movimiento","2026-09-27",false);form.save("Volver",()->{});});
   Thread.sleep(600);test.waitForIdleSync();VisualChecks.main(test,()->{
    DateInput.DisplayDate[] spans=date[0].getText().getSpans(0,date[0].length(),DateInput.DisplayDate.class);
    yes(spans.length==1&&Forms.value(date[0]).equals("2026-09-27"),"A responsive date must preserve one presentation span and its original ISO value");
    dateWidth[0]=spans[0].getSize(date[0].getPaint(),date[0].getText(),0,date[0].length(),null);report(test,"date-long-month",date[0],dateWidth[0]);
    yes(date[0].getContentDescription().toString().contains("27 de septiembre de 2026"),"The full date must remain accessible even when visual presentation adapts");
    if(dateWidth[0]>available(date[0])+1)failures.add("Localized September date exceeds its field: "+dateWidth[0]+" > "+available(date[0]));
    if(date[0].getScrollX()!=0)failures.add("Localized date is horizontally scrolled: "+date[0].getScrollX());
   });shot(test,c,"sizing-date-font13");VisualChecks.main(test,()->{opened[0].dismiss();opened[0]=null;});test.waitForIdleSync();
   final QuickEntryDialog.Entry[] quick={null};VisualChecks.main(test,()->{quick[0]=new QuickEntryDialog.Entry(a,0,"",null);opened[0]=quick[0].dialog;opened[0].show();Glass.window(opened[0],a);quick[0].amount.setText(String.valueOf(Ledger.MAX));quick[0].amount.clearFocus();((InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(quick[0].root.getWindowToken(),0);});
   Thread.sleep(600);test.waitForIdleSync();VisualChecks.main(test,()->{EditText field=quick[0].amount;int width=(int)Math.ceil(field.getPaint().measureText(field.getText().toString()));report(test,"quick-maximum-valid",field,width);yes(field.getText().toString().replaceAll("[^0-9]","").equals(String.valueOf(Ledger.MAX)),"The maximum valid CLP draft must keep every digit while formatted");if(width>available(field)+1)failures.add("Maximum valid quick-entry amount exceeds its field: "+width+" > "+available(field));if(field.getScrollX()!=0)failures.add("Maximum valid quick-entry amount is horizontally scrolled: "+field.getScrollX());});shot(test,c,"sizing-quick-max-font13");
   // Thirteen input digits also exercise the pre-validation draft, without attempting to save an out-of-range value.
   VisualChecks.main(test,()->quick[0].amount.setText("9999999999999"));Thread.sleep(600);test.waitForIdleSync();VisualChecks.main(test,()->{EditText field=quick[0].amount;int width=(int)Math.ceil(field.getPaint().measureText(field.getText().toString()));report(test,"quick-thirteen-digit-draft",field,width);yes(field.getText().toString().replaceAll("[^0-9]","").equals("9999999999999"),"A thirteen-digit draft must not truncate or silently change its value");if(width>available(field)+1)failures.add("Thirteen-digit quick-entry draft exceeds its field: "+width+" > "+available(field));if(field.getScrollX()!=0)failures.add("Thirteen-digit quick-entry draft is horizontally scrolled: "+field.getScrollX());});shot(test,c,"sizing-quick-draft-font13");
   yes(LocalData.read(c).toString().equals(finance.toString()),"Sizing previews must never save a movement or change any finance record");
  }finally{
   try{final MainActivity a=activity;VisualChecks.main(test,()->{if(opened[0]!=null)opened[0].dismiss();if(a!=null)a.finish();});test.waitForIdleSync();NativeSync.EXEC.submit(()->{}).get(10,TimeUnit.SECONDS);}finally{VisualChecks.restore(c.getSharedPreferences("secure",0),encrypted);VisualChecks.restore(NativeStore.settings(c),settings);}
  }
  yes(VisualChecks.preferences(c.getSharedPreferences("secure",0)).equals(encrypted)&&VisualChecks.preferences(NativeStore.settings(c)).equals(settings),"Sizing checks must restore encrypted data and every app preference exactly");
  yes(failures.isEmpty(),String.join("; ",failures));
  return checks;
 }
}

package cl.bolsillo.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** A catalogue remains read-only until an explicit save, including already configured services. */
final class ServicePickerChecks {
 static int checks;
 static void yes(boolean value,String text)throws Exception{checks++;if(!value)throw new Exception(text);}
 static void main(Instrumentation test,VisualChecks.Step work)throws Exception{VisualChecks.main(test,work);}
 static String shell(Instrumentation test,String command)throws Exception{try(InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(test.getUiAutomation().executeShellCommand(command));ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[1024];int count;while((count=input.read(buffer))!=-1)out.write(buffer,0,count);return new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8).trim();}}
 static void visible(View view,String reason)throws Exception{Rect bounds=new Rect();yes(view!=null&&view.getGlobalVisibleRect(bounds)&&bounds.height()>=view.getHeight()-2&&bounds.width()>=view.getWidth()-2,reason);}
 static void assets(MainActivity a)throws Exception{
  List<String> failures=new ArrayList<>();
  for(ServiceCatalog.Entry entry:ServiceCatalog.all()){
   Bitmap bitmap=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);
   try{
    int id=ServiceAssets.resource(entry.id);yes(id!=0,"bundled asset exists: "+entry.id);
    Drawable drawable=a.getDrawable(id);drawable.setBounds(0,0,96,96);drawable.draw(new Canvas(bitmap));int opaque=0;
    for(int y=0;y<96;y++)for(int x=0;x<96;x++)if(Color.alpha(bitmap.getPixel(x,y))>20)opaque++;
    yes(opaque>8,"brand image contains drawable paths: "+entry.id);bitmap.eraseColor(Color.TRANSPARENT);
    View badge=Icons.badge(a,entry.id,48);badge.measure(View.MeasureSpec.makeMeasureSpec(96,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(96,View.MeasureSpec.EXACTLY));badge.layout(0,0,96,96);badge.draw(new Canvas(bitmap));Set<Integer> colors=new HashSet<>();int nonWhite=0;
    for(int y=0;y<96;y++)for(int x=0;x<96;x++){int color=bitmap.getPixel(x,y);colors.add(color);if(Color.alpha(color)>20&&(color&0xffffff)!=0xffffff)nonWhite++;}
    yes(colors.size()>1&&nonWhite>20,"brand badge is neither blank nor all white: "+entry.id);
   }catch(Exception error){Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();failures.add(entry.id+": "+cause.toString());android.util.Log.e("BolsilloServiceAssets",entry.id,error);}
   finally{bitmap.recycle();}
  }
  yes(failures.isEmpty(),"Service image failures: "+String.join("; ",failures));
 }
 static int run(Instrumentation test,Context c)throws Exception{
  checks=0;NativeSync.EXEC.submit(()->{}).get(15,TimeUnit.SECONDS);
  Map<String,Object> secure=VisualChecks.preferences(c.getSharedPreferences("secure",0)),settings=VisualChecks.preferences(NativeStore.settings(c)),updates=VisualChecks.preferences(AppUpdates.prefs(c));
  MainActivity[] host={null};SheetDialog[] picker={null},existing={null};Forms.Form[] form={null};String previousFont=null;boolean fontChanged=false;
  try{
   JSONObject fixture=VisualChecks.fixture();
   for(int i=0;i<2;i++)fixture.getJSONArray("recurring").put(new JSONObject().put("id","service-netflix-"+i).put("name",i==0?"Netflix familiar":"NETFLIX.COM").put("type","expense").put("amount",i==0?9990:12990).put("account","bank-mach").put("category","expense-7").put("next",Ledger.today()).put("frequency","monthly").put("active",i==0).put("automatic",false).put("matchNotifications",true).put("shares",new JSONArray()).put("service",i==0?"netflix":""));
   LocalData.restore(c,fixture);NativeStore.settings(c).edit().putBoolean("capture",false).putBoolean("detectionTutorialSeen",true).putBoolean("privateScreen",false).putBoolean("alertChanges",false).putBoolean("alertDetections",false).putBoolean("alertVerifications",false).commit();AppUpdates.prefs(c).edit().putLong("attempt",System.currentTimeMillis()).commit();
   host[0]=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));MainActivity a=host[0];V16Checks.loaded(test,a);
   main(test,()->assets(a));
   String original=LocalData.read(c).toString(),originalRules=LocalData.read(c).getJSONArray("recurring").toString(),originalTx=LocalData.read(c).getJSONArray("transactions").toString();
   List<JSONObject> configured=Subscriptions.configured(a,ServiceCatalog.byId("netflix"));yes(configured.size()==2,"legacy alias and explicit service id retain both Netflix plans, including paused");
   List<String> suggestions=Subscriptions.suggestions(a.data);yes(suggestions.get(0).equals("NETFLIX.COM"),"existing subscription names precede generic catalogue suggestions");yes(suggestions.stream().anyMatch(name->name.equals("Claude")),"catalogue supplies services absent from history");
   main(test,()->picker[0]=ServicePicker.show(a));V15Checks.pause(test,250);
   ListView list=picker[0].layout.findViewWithTag("service-picker-list");EditText query=picker[0].layout.findViewWithTag("service-picker-query");
   yes(list.getCount()==ServiceCatalog.all().size()&&list.getChildCount()>0&&list.getChildCount()<list.getCount(),"catalogue only creates the visible portion of its service rows");
   main(test,()->{List<String> groups=new ArrayList<>();groups.add("");groups.addAll(ServiceCatalog.groups());for(String label:groups){TextView chip=picker[0].layout.findViewWithTag(label);yes(chip.getLayout()!=null&&chip.getLayout().getEllipsisCount(0)==0,"horizontal group chip keeps its full label: "+chip.getText());}});
   UiSmoke.shot(test,c,"services-catalogue-start");
   View[] recycled={null};main(test,()->{recycled[0]=list.getAdapter().getView(0,null,list);yes(list.getAdapter().getView(1,recycled[0],list)==recycled[0],"scrolling reuses service row containers");});
   main(test,()->query.setText("claude"));V15Checks.pause(test,120);yes(list.getCount()==1&&((ServiceCatalog.Entry)list.getAdapter().getItem(0)).id.equals("claude"),"search finds Claude without adding a subscription");
   UiSmoke.shot(test,c,"services-claude");
   String group=ServiceCatalog.byId("claude").group;main(test,()->picker[0].layout.findViewWithTag(group).performClick());yes(list.getCount()==1,"group filter combines with search");
   String other=ServiceCatalog.groups().stream().filter(name->!name.equals(group)).findFirst().get();main(test,()->picker[0].layout.findViewWithTag(other).performClick());yes(list.getCount()==0,"unrelated group cannot leak search results");
   main(test,()->{picker[0].layout.findViewWithTag("").performClick();query.setText("netflix");});V15Checks.pause(test,120);
   yes(list.getChildAt(0)!=null&&list.getChildAt(0).getContentDescription().toString().contains("Ya añadido"),"configured service is identified visibly");
   yes(original.equals(LocalData.read(c).toString()),"opening, searching and filtering cannot change any ledger data");
   UiSmoke.shot(test,c,"services-catalogue");
   main(test,()->list.getChildAt(0).performClick());V15Checks.pause(test,150);
   android.view.accessibility.AccessibilityNodeInfo root=test.getUiAutomation().getRootInActiveWindow();yes(root!=null&&!root.findAccessibilityNodeInfosByText("Netflix ya está añadido").isEmpty(),"tapping a configured service opens an explicit existing-plan choice");yes(original.equals(LocalData.read(c).toString()),"existing-plan prompt never creates a duplicate");
   V15Checks.tap(test,"Volver");
   main(test,()->existing[0]=ServicePicker.existing(a,ServiceCatalog.byId("netflix"),configured));V15Checks.pause(test,120);ListView choices=existing[0].layout.findViewWithTag("service-existing-list");yes(choices.getCount()==2,"both legitimate plans remain individually selectable");yes(existing[0].layout.findViewWithTag("service-existing-another")!=null,"creating another plan requires its own explicit action");main(test,()->existing[0].dismiss());
   main(test,()->form[0]=Subscriptions.add(a,"Netflix"));V15Checks.pause(test,150);
   EditText amount=form[0].box.findViewWithTag("subscription-amount");TextView warning=form[0].box.findViewWithTag("subscription-existing-warning");yes(amount.getText().length()==0,"known-service form never copies an existing or catalogue price");yes(warning.getVisibility()==View.VISIBLE&&warning.getText().toString().contains("2 programaciones"),"typed or explicitly repeated service warns without replacing existing plans");main(test,()->form[0].dialog.dismiss());
   main(test,()->{picker[0]=ServicePicker.show(a);});V15Checks.pause(test,120);yes(picker[0].layout.findViewWithTag("service-picker-custom")!=null,"catalogue retains an accessible custom-service action");main(test,()->picker[0].dismiss());
   main(test,()->form[0]=Subscriptions.add(a,""));V15Checks.pause(test,120);AutoCompleteTextView name=form[0].box.findViewWithTag("subscription-service");amount=form[0].box.findViewWithTag("subscription-amount");yes(name.getText().length()==0&&amount.getText().length()==0,"custom service starts with blank identity and price");
   final EditText value=amount;main(test,()->{name.setText("Mi servicio personalizado",false);value.setText("12345678912");});yes(original.equals(LocalData.read(c).toString()),"filling a custom draft still leaves records intact");
   main(test,()->{for(Button button:VisualChecks.views(((SheetDialog)form[0].dialog).footer,Button.class))if(button.getText().toString().equals("Guardar suscripción")){button.performClick();return;}throw new Exception("missing explicit subscription save");});V15Checks.pause(test,200);
   JSONObject saved=LocalData.read(c);JSONArray rules=saved.getJSONArray("recurring");yes(rules.length()==fixture.getJSONArray("recurring").length()+1,"one explicit save creates exactly one custom subscription");JSONObject custom=rules.getJSONObject(rules.length()-1);yes(custom.optString("name").equals("Mi servicio personalizado")&&custom.optLong("amount")==12345678912L&&custom.optString("service").isEmpty(),"custom identity and large CLP amount are preserved without inventing a brand");
   JSONArray preserved=new JSONArray();for(int i=0;i<rules.length()-1;i++)preserved.put(rules.getJSONObject(i));yes(preserved.toString().equals(originalRules),"all previously configured subscriptions remain byte-for-byte unchanged");yes(saved.getJSONArray("transactions").toString().equals(originalTx),"configuring subscriptions never posts a financial movement");
   String afterSave=saved.toString();
   yes(shell(test,"getprop ro.kernel.qemu").equals("1"),"font-scale exercise is restricted to an emulator");previousFont=shell(test,"settings get system font_scale");yes(previousFont.equals("null")||previousFont.matches("[0-9]+(\\.[0-9]+)?"),"existing font scale can be restored safely");
   main(test,()->host[0].finish());test.waitForIdleSync();fontChanged=true;shell(test,"settings put system font_scale 1.3");Thread.sleep(350);
   host[0]=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));MainActivity scaled=host[0];V16Checks.loaded(test,scaled);yes(scaled.getResources().getConfiguration().fontScale>=1.29f,"catalogue is checked with actual Android font scale 1.3");
   main(test,()->picker[0]=ServicePicker.show(scaled));V15Checks.pause(test,200);EditText scaledQuery=picker[0].layout.findViewWithTag("service-picker-query");
   main(test,()->{scaledQuery.setText("claude");scaledQuery.requestFocus();scaledQuery.postDelayed(()->((InputMethodManager)scaled.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(scaledQuery,InputMethodManager.SHOW_IMPLICIT),120);});VisualChecks.waitImeVisible(test,picker[0].layout);
   main(test,()->{yes(VisualChecks.imeVisible(picker[0].layout),"real keyboard is open during catalogue reachability check");visible(scaledQuery,"search stays fully visible with large text and keyboard");visible(VisualChecks.description(picker[0].layout,"Cerrar catálogo"),"catalogue close action stays reachable with keyboard");});VisualChecks.reachable(test,picker[0],"Otro servicio");
   UiSmoke.shot(test,c,"services-font13-keyboard");yes(afterSave.equals(LocalData.read(c).toString()),"large-font keyboard browsing does not mutate subscriptions or balances");
  }finally{
   try{
    try{if(picker[0]!=null)main(test,()->picker[0].dismiss());if(existing[0]!=null)main(test,()->existing[0].dismiss());if(form[0]!=null)main(test,()->form[0].dialog.dismiss());if(host[0]!=null)main(test,()->host[0].finish());test.waitForIdleSync();NativeSync.EXEC.submit(()->{}).get(15,TimeUnit.SECONDS);}
    finally{try{VisualChecks.restore(c.getSharedPreferences("secure",0),secure);}finally{try{VisualChecks.restore(NativeStore.settings(c),settings);}finally{VisualChecks.restore(AppUpdates.prefs(c),updates);}}}
   }finally{if(fontChanged){shell(test,previousFont.equals("null")?"settings delete system font_scale":"settings put system font_scale "+previousFont);Thread.sleep(200);}}
  }
  return checks;
 }
}

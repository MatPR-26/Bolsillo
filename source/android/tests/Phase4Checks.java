package cl.bolsillo.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Functional visual regression checks. Runs only against the isolated synthetic emulator. */
final class Phase4Checks {
 static int checks;
 static void yes(boolean condition,String why)throws Exception{checks++;if(!condition)throw new Exception(why);}
 static void main(Instrumentation test,VisualChecks.Step work)throws Exception{VisualChecks.main(test,work);}
 static void settled(Instrumentation test)throws Exception{V15Checks.pause(test,300);}
 static String shell(Instrumentation test,String command)throws Exception{
  try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(test.getUiAutomation().executeShellCommand(command));ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] buffer=new byte[1024];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8).trim();
  }
 }
 static TextView text(View root,String wanted){for(TextView v:VisualChecks.views(root,TextView.class))if(wanted.contentEquals(v.getText()))return v;return null;}
 static Button button(View root,String prefix){for(Button v:VisualChecks.views(root,Button.class))if(v.getText().toString().startsWith(prefix))return v;return null;}
 static boolean inside(View child,View parent){while(child!=null){if(child==parent)return true;child=child.getParent() instanceof View?(View)child.getParent():null;}return false;}
 static void visible(View v,String why)throws Exception{Rect rect=new Rect();yes(v!=null&&v.isShown()&&v.getGlobalVisibleRect(rect)&&rect.width()>=v.getWidth()-2&&rect.height()>=v.getHeight()-2,why);}
 static boolean accessibilityDescription(AccessibilityNodeInfo node,String wanted){if(node==null)return false;if(node.getContentDescription()!=null&&wanted.contentEquals(node.getContentDescription()))return true;for(int i=0;i<node.getChildCount();i++)if(accessibilityDescription(node.getChild(i),wanted))return true;return false;}
 static AccessibilityNodeInfo described(AccessibilityNodeInfo node,String wanted){if(node==null)return null;if(node.getContentDescription()!=null&&wanted.contentEquals(node.getContentDescription()))return node;for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo found=described(node.getChild(i),wanted);if(found!=null)return found;}return null;}
 static JSONObject fixture()throws Exception{
  JSONObject data=VisualChecks.fixture();
  JSONObject share=Ledger.find(data.getJSONArray("transactions"),"visual-shared");
  Map<String,Map<String,String>> before=ChangeHistory.snapshot(data);
  share.put("workNote","Nota de control: el cargo se pagó completo desde BancoEstado. Diego abonó una parte después. Esta nota conserva todos los detalles y debe poder leerse sin que una altura fija descarte sus últimas líneas. El resto sigue pendiente de cobro.");
  ChangeHistory.record(data,before);
  JSONArray tx=data.getJSONArray("transactions");
  tx.put(NativeSync.transaction("repayment",2500,"bank-bancoestado","","","Abono de Diego","manual","").put("id","phase4-abono").put("receivable","visual-shared:visual-share-diego").put("date",Ledger.today()).put("paymentNote","Primera parte; conservar fecha y nota"));
  tx.put(NativeSync.transaction("expense",1009991,"bank-mach","","expense-4","Compra pendiente QA","manual","").put("id","phase4-pending").put("status","pending").put("date",Ledger.today()));
  tx.put(NativeSync.transaction("income",900000000000L,"bank-mach","","income-3","Importe grande QA","manual","").put("id","phase4-large").put("date",Ledger.today()).put("customMetadata",new JSONObject().put("keep","sin cambios")));
  data.getJSONArray("accounts").put(Ledger.account("phase4-credit","Crédito QA","credit","other").put("opening",-45000).put("limit",1000000));
  Ledger.validate(data);return data;
 }
 static double channel(int c){double n=c/255d;return n<=.04045?n/12.92:Math.pow((n+.055)/1.055,2.4);}
 static double contrast(int a,int b){double x=.2126*channel(Color.red(a))+.7152*channel(Color.green(a))+.0722*channel(Color.blue(a)),y=.2126*channel(Color.red(b))+.7152*channel(Color.green(b))+.0722*channel(Color.blue(b));return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}

 static int run(Instrumentation test,Context c)throws Exception{
  checks=0;yes(shell(test,"getprop ro.kernel.qemu").equals("1"),"phase 4 financial/UI tests are restricted to the dedicated emulator");
  NativeSync.EXEC.submit(()->{}).get(15,TimeUnit.SECONDS);
  Map<String,Object> secure=VisualChecks.preferences(c.getSharedPreferences("secure",0)),settings=VisualChecks.preferences(NativeStore.settings(c)),updates=VisualChecks.preferences(AppUpdates.prefs(c));
  String previousFont=shell(test,"settings get system font_scale");yes(previousFont.equals("null")||previousFont.matches("[0-9]+(\\.[0-9]+)?"),"font scale can be restored without shell interpolation of untrusted text");
  boolean[] fontChanged={false};MainActivity[] host={null};SheetDialog[] detail={null};
  try{
   JSONObject fixture=fixture();
   LocalData.restore(c,fixture);NativeStore.settings(c).edit().putBoolean("capture",false).remove(NotificationAccess.PENDING).remove("paymentCards").putBoolean("detectionTutorialSeen",true).putBoolean("privateScreen",false).putBoolean("alertChanges",false).putBoolean("alertDetections",false).putBoolean("alertVerifications",false).putString("appearance","dark").putString("palette","prisma").putInt("glassIntensity",1).commit();AppUpdates.prefs(c).edit().putLong("attempt",System.currentTimeMillis()).commit();
   host[0]=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));MainActivity a=host[0];V16Checks.loaded(test,a);
   String original=LocalData.read(c).toString(),txBefore=LocalData.read(c).getJSONArray("transactions").toString();Map<String,Long> balances=Ledger.balances(LocalData.read(c),Ledger.today());

   // Native Inter, cached weights and semantic contrast replace font-name/pixel snapshots.
   main(test,()->{
    Typeface regular=Glass.font(a,400);yes(regular!=null&&regular==Glass.font(a,400),"repeated labels reuse the cached native font");
    if(Build.VERSION.SDK_INT>=28){yes(Glass.font(a,500).getWeight()==500&&Glass.font(a,600).getWeight()==600,"native font exposes the intended reading/title weights");}
    try(InputStream font=a.getResources().openRawResource(R.font.inter_variable)){byte[] magic=new byte[4];yes(font.read(magic)==4&&magic[0]==0&&magic[1]==1&&magic[2]==0&&magic[3]==0,"Inter is packaged as real Android-readable TrueType, not renamed WOFF");}
   });
   for(String mode:new String[]{"dark","light"}){
    NativeStore.settings(c).edit().putString("appearance",mode).commit();
    yes(contrast(Glass.ink(c),Glass.raised(c))>=4.5&&contrast(Glass.subtle(c),Glass.raised(c))>=4.5,"main and secondary text retain readable contrast in "+mode);
    yes(contrast(Glass.semantic(c,Glass.CORAL),Glass.panel(c))>=4.5&&contrast(Glass.semantic(c,Glass.MINT),Glass.panel(c))>=4.5,"expense and income semantic text stay legible in "+mode);
   }
   NativeStore.settings(c).edit().putString("appearance","dark").commit();main(test,()->a.render(false));settled(test);

   // All existing financial carousels survive navigation; page state is not reset.
   main(test,()->{
    List<SnapCarousel> carousels=VisualChecks.views(a.body,SnapCarousel.class);yes(carousels.size()>=3,"home retains balances, accounts and distribution carousels");
    SnapCarousel hero=VisualChecks.hero(a);yes(hero.anchors().size()==3,"all three balance panels remain available");hero.go(1);
   });settled(test);
   yes(a.heroPage==1,"swiping to the free-money page updates the remembered page");
   main(test,()->{a.navigate("Cuentas");a.navigate("Inicio");});settled(test);
   yes(a.heroPage==1&&VisualChecks.hero(a).page()==1,"returning home retains the financial carousel selection");
   main(test,()->a.navigate("Explorar"));settled(test);
   boolean organizer=false;for(View v:VisualChecks.views(a.navigationDock,View.class))if(v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("Organizar"))organizer=true;
   yes(organizer&&a.tab.equals("Explorar"),"visible Organizar navigation preserves the existing internal route");

   // Accounts separate usable money, savings and credit without multiplying card balances.
   String cardsBefore=PaymentCards.list(a).toString();main(test,()->{a.accountsPage=0;a.navigate("Cuentas");});settled(test);
   main(test,()->{
    yes(text(a.body,"Disponibles")!=null&&text(a.body,"Ahorro")!=null&&text(a.body,"Crédito")!=null,"account views expose usable money, savings and a credit tab when a credit account exists");
    int mach=0;for(View view:VisualChecks.views(a.body,View.class))if(view.getContentDescription()!=null&&view.getContentDescription().toString().startsWith("MACHBANK.")){mach++;yes(view.getContentDescription().toString().contains("2 tarjetas · un saldo"),"MACH account identifies its two cards as one shared balance");}
    yes(mach==1&&text(a.body,"MACH · física")!=null&&text(a.body,"MACH · digital")!=null,"two MACH cards do not create a second account balance row");
    text(a.body,"Ahorro").performClick();
   });settled(test);
   yes(a.accountsPage==1&&text(a.body,"MACHBANK · Ahorro")!=null&&text(a.body,"Crédito QA")==null,"savings tab shows its own account set without credit accounts");
   main(test,()->text(a.body,"Crédito").performClick());settled(test);
   yes(a.accountsPage==2&&text(a.body,"Crédito QA")!=null&&text(a.body,"MACHBANK")==null,"credit tab contains credit accounts without cloning usable balances");
   main(test,()->{a.navigate("Inicio");a.navigate("Cuentas");});settled(test);yes(a.accountsPage==2,"account tab selection survives leaving and returning");
   main(test,()->{a.accountsPage=0;a.render(false);});settled(test);UiSmoke.shot(test,c,"phase4-accounts");
   yes(cardsBefore.equals(PaymentCards.list(a).toString())&&original.equals(LocalData.read(c).toString()),"switching account views never alters card identifiers, account balances or historic records");

   // The redesigned person profile is read-only and its single debt action is separate from edits/deletion.
   main(test,()->{a.navigate("Personas");PeopleScreen.detail(a,Ledger.find(a.data.getJSONArray("people"),"visual-diego"));});settled(test);
   AccessibilityNodeInfo personRoot=test.getUiAutomation().getRootInActiveWindow();
   yes(personRoot!=null&&!personRoot.findAccessibilityNodeInfosByText("Diego").isEmpty()&&!personRoot.findAccessibilityNodeInfosByText(a.money(6500)).isEmpty(),"person profile retains the exact pending share after an existing partial repayment");
   AccessibilityNodeInfo register=VisualChecks.node(test,"Registrar deuda");Rect registerBounds=new Rect();if(register!=null)register.getBoundsInScreen(registerBounds);yes(register!=null&&register.isClickable()&&register.isVisibleToUser()&&!registerBounds.isEmpty(),"person profile has one reachable explicit debt-registration action");
   AccessibilityNodeInfo personOptions=described(personRoot,"Más opciones de persona");yes(personOptions!=null&&personOptions.performAction(AccessibilityNodeInfo.ACTION_CLICK),"profile editing and deletion live in its secondary options menu");settled(test);
   yes(VisualChecks.node(test,"Editar perfil")!=null&&VisualChecks.node(test,"Eliminar perfil")!=null,"profile menu preserves both edit and delete operations without triggering either");
   test.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settled(test);UiSmoke.shot(test,c,"phase4-person");
   AccessibilityNodeInfo closePerson=described(test.getUiAutomation().getRootInActiveWindow(),"Cerrar persona");yes(closePerson!=null&&closePerson.performAction(AccessibilityNodeInfo.ACTION_CLICK),"person profile closes without editing the debt");settled(test);
   yes(original.equals(LocalData.read(c).toString()),"opening a person, inspecting its menu and closing preserve all abonos, profiles and transaction history");

   // Review is a direct Activity destination and batch actions stay reachable below the list.
   main(test,a::openReview);settled(test);yes(a.tab.equals("Movimientos")&&a.activityReview,"review opens directly inside Activity");
   main(test,()->ReviewSelection.start(a,"pending:phase4-pending"));settled(test);
   main(test,()->{
    Button approve=button(a.navigationDock,"Aprobar"),discard=button(a.navigationDock,"Descartar");yes(approve!=null&&discard!=null&&approve.isEnabled()&&discard.isEnabled(),"selected review records expose enabled approval/discard in the dock");
    yes(!inside(approve,a.scroll)&&!inside(discard,a.scroll),"batch actions stay outside the scrolling content");
    yes(ReviewSelection.requests(a,false).size()==1&&ReviewSelection.requests(a,false).containsKey("pending:phase4-pending"),"review selection contains only the intended pending record");a.scroll.fullScroll(View.FOCUS_DOWN);
   });settled(test);
   main(test,()->{visible(button(a.navigationDock,"Aprobar"),"approval remains fully visible after scrolling");visible(button(a.navigationDock,"Descartar"),"discard remains fully visible after scrolling");});
   UiSmoke.shot(test,c,"phase4-review-selection");
   main(test,()->ReviewSelection.cancel(a));settled(test);yes(a.activityReview&&!a.reviewSelecting&&button(a.navigationDock,"Aprobar")==null,"leaving selection stays in Review and restores normal navigation");
   yes(original.equals(LocalData.read(c).toString()),"review navigation and selection do not post, delete or alter any record");

   // A movement tap enters its useful detail, not an options list.
   main(test,()->{a.filter="";a.accountFilter="";a.grouped=false;a.navigate("Movimientos");});settled(test);
   main(test,()->{View row=null;for(View v:VisualChecks.views(a.body,View.class))if(v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("Cena compartida.")){row=v;break;}yes(row!=null,"the shared purchase remains present in Activity");row.performClick();});settled(test);
   yes(accessibilityDescription(test.getUiAutomation().getRootInActiveWindow(),"Más opciones del movimiento"),"tapping the row opens direct detail with a secondary options action");
   test.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settled(test);
   main(test,()->detail[0]=TransactionDetail.show(a,Ledger.find(a.data.getJSONArray("transactions"),"visual-shared")));settled(test);
   main(test,()->{
    View split=detail[0].layout.findViewWithTag("transaction-detail-shares");yes(split!=null,"shared detail separates personal expense from money owed");
    yes(text(split,a.money(9000))!=null&&text(split,a.money(6500))!=null,"an existing partial repayment reduces only pending collection, not personal expense");
    TextView headline=detail[0].layout.findViewWithTag("transaction-detail-amount");yes(headline!=null&&headline.getText().toString().contains(a.money(18000)),"detail retains the full account debit as its headline");
    yes(text(detail[0].layout,"Registro de cambios")!=null,"prior edits remain discoverable from the movement");
    TextView note=text(detail[0].layout,Ledger.find(a.data.getJSONArray("transactions"),"visual-shared").optString("workNote"));yes(note!=null&&note.getMaxLines()>3,"long financial notes are not silently limited to three lines");
   });
   VisualChecks.reachable(test,detail[0],"Editar movimiento");UiSmoke.shot(test,c,"phase4-shared-detail");
   main(test,()->{TextView category=text(detail[0].layout,"Categoría");yes(category!=null,"movement detail exposes a category");View action=category;while(action!=null&&!action.isClickable())action=action.getParent() instanceof View?(View)action.getParent():null;yes(action!=null,"category can be changed directly without opening the full editor");action.performClick();});settled(test);
   yes(VisualChecks.node(test,"Nueva categoría")!=null,"category shortcut opens the existing quick-category chooser");V15Checks.tap(test,"Cancelar");settled(test);detail[0]=null;
   yes(original.equals(LocalData.read(c).toString()),"opening and cancelling a detail/category editor preserves all historic records and notes");

   // Privacy and hidden balances apply to new details as well as the main window.
   main(test,()->{NativeStore.settings(c).edit().putBoolean("privateScreen",true).commit();a.applyPrivacy();detail[0]=TransactionDetail.show(a,Ledger.find(a.data.getJSONArray("transactions"),"phase4-large"));});settled(test);
   yes((a.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0&&(detail[0].getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"privacy protects both activity and new detail windows");
   main(test,()->{detail[0].dismiss();NativeStore.settings(c).edit().putBoolean("privateScreen",false).commit();a.applyPrivacy();a.hidden=true;detail[0]=TransactionDetail.show(a,Ledger.find(a.data.getJSONArray("transactions"),"phase4-large"));});settled(test);
   main(test,()->{TextView hidden=detail[0].layout.findViewWithTag("transaction-detail-amount");yes(hidden.getText().toString().contains("••••••")&&!hidden.getText().toString().contains("900"),"hidden-balance preference also masks a large movement amount");detail[0].dismiss();a.hidden=false;});detail[0]=null;

   // Actual system text scaling, not a resized screenshot, must keep the sole action reachable.
   main(test,a::finish);test.waitForIdleSync();fontChanged[0]=true;shell(test,"settings put system font_scale 1.3");Thread.sleep(300);
   host[0]=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));MainActivity scaled=host[0];V16Checks.loaded(test,scaled);yes(scaled.getResources().getConfiguration().fontScale>=1.29f,"detail is exercised with real Android text scale 1.3");
   main(test,()->detail[0]=TransactionDetail.show(scaled,Ledger.find(scaled.data.getJSONArray("transactions"),"phase4-large")));settled(test);VisualChecks.reachable(test,detail[0],"Editar movimiento");
   main(test,()->{TextView value=detail[0].layout.findViewWithTag("transaction-detail-amount");yes(value.getText().toString().contains(scaled.money(900000000000L)),"large CLP amounts remain exact rather than abbreviated or rounded in detail");visible(value,"large-font amount remains fully inside its view");});UiSmoke.shot(test,c,"phase4-detail-font13");
   yes(!NativeStore.settings(c).getBoolean("capture",true),"visual navigation never turns on notification capture");
   yes(txBefore.equals(LocalData.read(c).getJSONArray("transactions").toString())&&original.equals(LocalData.read(c).toString()),"all existing movements, subscriptions, abonos, change history and custom metadata stay byte-for-byte unchanged");
   yes(balances.equals(Ledger.balances(LocalData.read(c),Ledger.today())),"full account balances and outstanding debts are unchanged by the redesign's read-only flows");
  }finally{
   try{
    try{if(detail[0]!=null)main(test,()->detail[0].dismiss());if(host[0]!=null)main(test,()->host[0].finish());test.waitForIdleSync();NativeSync.EXEC.submit(()->{}).get(15,TimeUnit.SECONDS);}
    finally{try{VisualChecks.restore(c.getSharedPreferences("secure",0),secure);}finally{try{VisualChecks.restore(NativeStore.settings(c),settings);}finally{VisualChecks.restore(AppUpdates.prefs(c),updates);}}}
   }finally{if(fontChanged[0]){shell(test,previousFont.equals("null")?"settings delete system font_scale":"settings put system font_scale "+previousFont);Thread.sleep(200);}}
  }
  return checks;
 }
}

package cl.bolsillo.app;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/** Synthetic instrumentation only; calendar selection never saves finance data. */
final class CalendarChecks {
 static int checks;
 static void yes(boolean condition,String detail)throws Exception{checks++;if(!condition)throw new Exception(detail);}
 static int run(Instrumentation test,Context c)throws Exception{
  checks=0;math();JSONObject before=LocalData.read(c);Map<String,?> settings=new HashMap<>(NativeStore.settings(c).getAll());MainActivity a=null;final CalendarSheet.Session[] session={null};final EditText[] target={null};
  try{
   NativeStore.settings(c).edit().putString("appearance","dark").putString("palette","prisma").commit();a=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));final MainActivity host=a;test.waitForIdleSync();
   test.runOnMainSync(()->{target[0]=new EditText(host);target[0].setText("2024-02-29");DateInput.apply(host,target[0],"Fecha");session[0]=CalendarSheet.create(host,target[0],LocalDate.of(2024,2,29));session[0].dialog.show();});test.waitForIdleSync();
   yes(DateInput.friendly("2024-02-29").equals("29 de febrero de 2024")&&Forms.value(target[0]).equals("2024-02-29"),"Localized date presentation must keep the original ISO editable and parsing value");yes(target[0].getText().getSpans(0,target[0].length(),DateInput.DisplayDate.class).length==1&&target[0].getContentDescription().toString().contains("29 de febrero de 2024"),"Localized date fields must render one display span and expose their full date to accessibility");
   final boolean[] optional={false};test.runOnMainSync(()->{EditText field=new EditText(host);DateInput.apply(host,field,"Fecha de pago opcional");boolean empty=Forms.value(field).isEmpty()&&field.getText().getSpans(0,field.length(),DateInput.DisplayDate.class).length==0;field.setText("2026-10-06");boolean shown=Forms.value(field).equals("2026-10-06")&&field.getText().getSpans(0,field.length(),DateInput.DisplayDate.class).length==1;field.setText("");optional[0]=empty&&shown&&Forms.value(field).isEmpty()&&field.getText().getSpans(0,field.length(),DateInput.DisplayDate.class).length==0;});yes(optional[0],"An optional localized date must remain empty, retain ISO when set and clear its span when erased");
   yes(session[0].dayCells.size()==29&&session[0].dayCells.get(29).isSelected(),"Leap month must have 29 selectable days and preserve its selection");
   final int[] first={0};test.runOnMainSync(()->first[0]=((ViewGroup)session[0].dayCells.get(1).getParent()).indexOfChild(session[0].dayCells.get(1)));yes(first[0]==3,"Week grid must start on Monday: 1 February 2024 belongs in Thursday column");
   test.runOnMainSync(()->session[0].chooseMonthYear(2025,2));yes(session[0].selected.equals(LocalDate.of(2025,2,28)),"Changing to a non-leap year must safely clamp February 29");yes(target[0].getText().toString().equals("2024-02-29"),"Month and year changes must not edit the parent field before confirmation");
   test.runOnMainSync(()->session[0].chooseMonthYear(2028,2));yes(session[0].selected.getDayOfMonth()==29,"Returning to a leap year must retain the preferred day");
   test.runOnMainSync(()->session[0].monthControl.performClick());test.waitForIdleSync();final View[] october={null};test.runOnMainSync(()->{october[0]=findDescription(session[0].child.layout,"Elegir Octubre");october[0].performClick();});test.waitForIdleSync();yes(session[0].selected.equals(LocalDate.of(2028,10,29))&&session[0].child==null,"The quick month grid must select October and close without altering the target");yes(target[0].getText().toString().equals("2024-02-29"),"The quick month sheet must keep its changes inside the draft");test.runOnMainSync(()->session[0].chooseMonthYear(2028,2));
   test.runOnMainSync(()->session[0].yearControl.performClick());test.waitForIdleSync();test.runOnMainSync(()->{session[0].yearField.setText("1901");positive(session[0].child).performClick();});test.waitForIdleSync();yes(session[0].selected.equals(LocalDate.of(1901,2,28)),"Direct year entry must reach a historical year without month swipes");yes(target[0].getText().toString().equals("2024-02-29"),"The year sheet must only modify the independent calendar draft");
   test.runOnMainSync(()->session[0].yearControl.performClick());test.waitForIdleSync();test.runOnMainSync(()->{session[0].yearField.setText("1880");cancel(session[0].child).performClick();});test.waitForIdleSync();yes(session[0].selected.equals(LocalDate.of(1901,2,28))&&session[0].child==null,"Cancelling year entry must leave the calendar draft itself unchanged");
   test.runOnMainSync(()->session[0].yearControl.performClick());test.waitForIdleSync();test.runOnMainSync(()->{session[0].yearField.setText("-1000000000");positive(session[0].child).performClick();});test.waitForIdleSync();yes(session[0].child!=null&&session[0].child.isShowing()&&session[0].yearField.getError()!=null,"An invalid signed year must display an error without truncating into a different valid year");yes(session[0].selected.equals(LocalDate.of(1901,2,28)),"Invalid year input must preserve the prior draft");
   test.runOnMainSync(()->{session[0].yearField.setText("2026");positive(session[0].child).performClick();});test.waitForIdleSync();shot(test,c,session[0].dialog.layout,"calendar-dark-native");
   test.runOnMainSync(()->cancel(session[0].dialog).performClick());test.waitForIdleSync();yes(target[0].getText().toString().equals("2024-02-29"),"Cancelling after historical and invalid year edits must leave the parent date intact");
   NativeStore.settings(c).edit().putString("appearance","light").putString("palette","ocean").commit();test.runOnMainSync(()->{session[0]=CalendarSheet.create(host,target[0],LocalDate.of(2026,1,31));session[0].dialog.show();session[0].chooseMonthYear(2026,4);});test.waitForIdleSync();yes(session[0].selected.equals(LocalDate.of(2026,4,30)),"January 31 must clamp to April 30");test.runOnMainSync(()->session[0].chooseMonthYear(2026,5));yes(session[0].selected.getDayOfMonth()==31,"Longer months must restore the preferred day after a clamp");
   test.runOnMainSync(()->session[0].dayCells.get(12).performClick());test.waitForIdleSync();yes(session[0].selected.equals(LocalDate.of(2026,5,12))&&session[0].preferredDay==12,"Tapping a day must change the draft and its preferred day");yes(target[0].getText().toString().equals("2024-02-29"),"A day tap must not commit early");
   final boolean[] size={true},inside={false};test.runOnMainSync(()->{for(View cell:session[0].dayCells.values())size[0]&=cell.getWidth()>=host.dp(44)&&cell.getHeight()>=host.dp(44);});yes(size[0],"Calendar days must provide at least 44dp touch targets");shot(test,c,session[0].dialog.layout,"calendar-light-native");
   test.runOnMainSync(()->session[0].dialog.getWindow().setLayout(host.dp(360),host.dp(340)));test.waitForIdleSync();test.runOnMainSync(()->{int[] action=new int[2],surface=new int[2];View footer=positive(session[0].dialog);footer.getLocationOnScreen(action);session[0].dialog.layout.getLocationOnScreen(surface);inside[0]=action[1]>=surface[1]&&action[1]+footer.getHeight()<=surface[1]+session[0].dialog.layout.getHeight();});yes(inside[0],"Confirmation must remain reachable in a constrained landscape-height viewport");
   test.runOnMainSync(()->positive(session[0].dialog).performClick());test.waitForIdleSync();yes(target[0].getText().toString().equals("2026-05-12")&&!session[0].dialog.isShowing(),"Only Elegir fecha must commit the selected ISO date and dismiss the sheet");yes(Forms.value(target[0]).equals("2026-05-12")&&target[0].getText().getSpans(0,target[0].length(),DateInput.DisplayDate.class).length==1&&target[0].getContentDescription().toString().contains("12 de mayo de 2026"),"Confirming a calendar date must update its display and accessibility while preserving ISO storage");
   test.runOnMainSync(()->{session[0]=CalendarSheet.create(host,target[0],LocalDate.of(9999,12,31));session[0].dialog.show();});test.waitForIdleSync();yes(!session[0].canMove(1)&&session[0].canMove(-1),"Last financial ISO year must disable forward navigation safely");test.runOnMainSync(()->{session[0].chooseMonthYear(1,1);session[0].yearControl.performClick();});test.waitForIdleSync();yes(!session[0].canMove(-1)&&session[0].yearField.getText().toString().equals("1"),"First financial ISO year must remain editable and disable backward overflow");test.runOnMainSync(()->session[0].dialog.dismiss());
   yes(LocalData.read(c).toString().equals(before.toString()),"All calendar editing and previews must preserve every finance record");
  }finally{if(a!=null){final MainActivity host=a;test.runOnMainSync(()->{if(session[0]!=null)session[0].dialog.dismiss();host.finish();});}QuickWidgetTests.restore(NativeStore.settings(c),settings);}
  return checks;
 }
 static void math()throws Exception{
  for(int year:new int[]{1,4,100,400,1900,2000,2024,2100,9999})for(int month=1;month<=12;month++){LocalDate date=CalendarSheet.clampDate(year,month,31);yes(date.getYear()==year&&date.getMonthValue()==month&&date.getDayOfMonth()==date.lengthOfMonth()&&date.toString().length()==10,"Month clamp must preserve its valid year/month and exact ten-character financial ISO value");}
  yes(CalendarSheet.clampDate(1900,2,29).getDayOfMonth()==28&&CalendarSheet.clampDate(2000,2,29).getDayOfMonth()==29,"Gregorian century leap rules must remain exact");
  yes(CalendarSheet.shiftedYear(9999,12)==9999&&CalendarSheet.shiftedYear(1,-12)==1,"Year page arithmetic must remain inside the financial ISO range");
  for(String value:new String[]{"2026"," 1901 ","1","9999","+9999"})yes(CalendarSheet.parseYear(value)==Integer.parseInt(value.trim()),"Direct year entry must preserve every valid financial ISO year");
  for(String value:new String[]{"","2026.5","0","-1","10000","1000000000","-1000000000","2x26","--2026"}){boolean failed=false;try{CalendarSheet.parseYear(value);}catch(IllegalArgumentException e){failed=true;}yes(failed,"Invalid or unsupported financial year text must be rejected without constructing a different date");}
  for(int year:new int[]{0,-1,10000}){boolean failed=false;try{CalendarSheet.clampDate(year,2,29);}catch(java.time.DateTimeException e){failed=true;}yes(failed,"Clamping a month must not accept an unsupported financial year");}
 }
 static View positive(SheetDialog dialog){return ((ViewGroup)dialog.footer.getChildAt(0)).getChildAt(1);}
 static View cancel(SheetDialog dialog){return ((ViewGroup)dialog.footer.getChildAt(0)).getChildAt(0);}
 static View findDescription(View view,String label){if(label.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=findDescription(((ViewGroup)view).getChildAt(i),label);if(found!=null)return found;}return null;}
 static void shot(Instrumentation test,Context c,View view,String name)throws Exception{test.waitForIdleSync();final Bitmap[] image={null};test.runOnMainSync(()->{image[0]=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);view.draw(new Canvas(image[0]));});File folder=new File(c.getExternalFilesDir(null),"qa");folder.mkdirs();try(FileOutputStream output=new FileOutputStream(new File(folder,name+".png"))){yes(image[0].compress(Bitmap.CompressFormat.PNG,100,output),"Calendar visual evidence must be capturable");}finally{image[0].recycle();}}
}

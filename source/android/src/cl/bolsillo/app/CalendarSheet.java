package cl.bolsillo.app;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/** Original Views implementation of the Material 3 modal date selection pattern.
 * https://developer.android.com/develop/ui/compose/components/datepickers
 * Uses an independent draft and explicit confirmation; no Compose dependency or
 * upstream implementation is copied. ISO LocalDate values stay timezone-free.
 */
final class CalendarSheet {
 static final int MIN_YEAR=1,MAX_YEAR=9999;
 static final Locale ES=Locale.forLanguageTag("es-CL");
 static final DateTimeFormatter SHORT=DateTimeFormatter.ofPattern("EEE, d MMM",ES);
 static final DateTimeFormatter FULL=DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' uuuu",ES);
 static void show(MainActivity a,EditText target,LocalDate initial){Session session=create(a,target,initial);session.dialog.show();}
 static Session create(MainActivity a,EditText target,LocalDate initial){return new Session(a,target,initial==null?LocalDate.now():initial);}
 static LocalDate clampDate(int year,int month,int day){if(year<MIN_YEAR||year>MAX_YEAR)throw new DateTimeException("Año entre 1 y 9999");if(day<1||day>31)throw new DateTimeException("Día fuera de rango");YearMonth ym=YearMonth.of(year,month);return ym.atDay(Math.min(day,ym.lengthOfMonth()));}
 static int parseYear(String value){String text=value==null?"":value.trim();if(!text.matches("[+]?[0-9]{1,4}"))throw new IllegalArgumentException("Año entre 1 y 9999");int year=Integer.parseInt(text);if(year<MIN_YEAR||year>MAX_YEAR)throw new IllegalArgumentException("Año entre 1 y 9999");return year;}
 static int shiftedYear(int year,int offset){return (int)Math.max(MIN_YEAR,Math.min(MAX_YEAR,(long)year+offset));}
 static String title(String text){return text.isEmpty()?text:text.substring(0,1).toUpperCase(ES)+text.substring(1);}

 static final class Session {
  final MainActivity a;final EditText target;final LocalDate today=LocalDate.now();
  final SheetDialog dialog;final LinearLayout content,grid;final TextView selectedLabel;
  final View monthControl,yearControl,previous,next,todayControl;final TextView monthLabel,yearLabel;
  final Map<Integer,View> dayCells=new LinkedHashMap<>();
  LocalDate selected;int preferredDay;SheetDialog child;EditText yearField;int yearCenter;
  Session(MainActivity host,EditText field,LocalDate initial){
   a=host;target=field;selected=clampDate(Math.max(MIN_YEAR,Math.min(MAX_YEAR,initial.getYear())),initial.getMonthValue(),initial.getDayOfMonth());preferredDay=initial.getDayOfMonth();
   content=Modern.column(a);content.setPadding(a.dp(18),a.dp(15),a.dp(18),a.dp(12));
   LinearLayout heading=Modern.row(a);TextView label=Modern.text(a,"ELEGIR FECHA",10,Glass.accent(a),true);label.setLetterSpacing(.14f);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));heading.addView(Icons.view(a,"calendar",Glass.accent(a),23),new LinearLayout.LayoutParams(a.dp(23),a.dp(23)));content.addView(heading);
   selectedLabel=Modern.text(a,"",29,Glass.ink(a),false);selectedLabel.setTypeface(Glass.font(a,500));selectedLabel.setMaxLines(2);selectedLabel.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);selectedLabel.setPadding(0,a.dp(10),0,a.dp(17));content.addView(selectedLabel,new LinearLayout.LayoutParams(-1,-2));
   LinearLayout period=Modern.row(a);monthLabel=Modern.text(a,"",16,Glass.ink(a),true);yearLabel=Modern.text(a,"",16,Glass.ink(a),true);monthControl=selector(monthLabel,()->months());yearControl=selector(yearLabel,()->years());LinearLayout.LayoutParams ml=new LinearLayout.LayoutParams(0,a.dp(52),.6f);ml.rightMargin=a.dp(8);period.addView(monthControl,ml);period.addView(yearControl,new LinearLayout.LayoutParams(0,a.dp(52),.4f));content.addView(period);
   LinearLayout navigation=Modern.row(a);previous=Modern.iconButton(a,"back","Mes anterior",()->move(-1));next=Modern.iconButton(a,"chevron","Mes siguiente",()->move(1));todayControl=Glass.pill(a,"Hoy",false);Modern.press(todayControl,()->{selected=today;preferredDay=today.getDayOfMonth();draw();});navigation.addView(previous,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,a.dp(44),1);hp.setMargins(a.dp(10),0,a.dp(10),0);navigation.addView(todayControl,hp);navigation.addView(next,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));Modern.add(content,navigation,12);
   HorizontalScrollView smallScreen=new HorizontalScrollView(a);smallScreen.setFillViewport(true);smallScreen.setHorizontalScrollBarEnabled(false);grid=Modern.column(a);grid.setMinimumWidth(a.dp(7*44));smallScreen.addView(grid,new HorizontalScrollView.LayoutParams(-1,-2));Modern.add(content,smallScreen,10);
   dialog=new SheetDialog(a,content);dialog.actions("Elegir fecha",()->{target.setText(selected.toString());dialog.dismiss();});dialog.setOnDismissListener(d->{if(child!=null)child.dismiss();content.animate().cancel();grid.animate().cancel();});draw();
  }
  View selector(TextView words,Runnable action){LinearLayout row=Modern.row(a);row.setBackground(Glass.material(a,234,17,Glass.secondary(a)));row.setPadding(a.dp(12),0,a.dp(9),0);words.setSingleLine(true);words.setEllipsize(android.text.TextUtils.TruncateAt.END);words.setAutoSizeTextTypeUniformWithConfiguration(11,16,1,android.util.TypedValue.COMPLEX_UNIT_SP);row.addView(words,new LinearLayout.LayoutParams(0,-2,1));ImageView arrow=Icons.view(a,"chevron",Glass.subtle(a),18);arrow.setRotation(90);LinearLayout.LayoutParams icon=new LinearLayout.LayoutParams(a.dp(18),a.dp(18));icon.leftMargin=a.dp(5);row.addView(arrow,icon);Modern.press(row,action);return row;}
  void chooseMonthYear(int year,int month){selected=clampDate(year,month,preferredDay);draw();}
  boolean canMove(int delta){try{int year=YearMonth.from(selected).plusMonths(delta).getYear();return year>=MIN_YEAR&&year<=MAX_YEAR;}catch(DateTimeException e){return false;}}
  void move(int delta){if(!canMove(delta))return;YearMonth month=YearMonth.from(selected).plusMonths(delta);chooseMonthYear(month.getYear(),month.getMonthValue());}
  void draw(){
   selectedLabel.setText(title(SHORT.format(selected)));selectedLabel.setContentDescription("Fecha seleccionada: "+FULL.format(selected));monthLabel.setText(title(selected.getMonth().getDisplayName(TextStyle.FULL,ES)));yearLabel.setText(String.valueOf(selected.getYear()));monthControl.setContentDescription("Elegir mes: "+monthLabel.getText());yearControl.setContentDescription("Elegir año: "+selected.getYear());previous.setEnabled(canMove(-1));previous.setAlpha(canMove(-1)?1:.35f);next.setEnabled(canMove(1));next.setAlpha(canMove(1)?1:.35f);todayControl.setContentDescription("Elegir hoy, "+FULL.format(today));
   grid.removeAllViews();dayCells.clear();String[] initials={"L","M","X","J","V","S","D"},names={"lunes","martes","miércoles","jueves","viernes","sábado","domingo"};LinearLayout weekdays=Modern.row(a);for(int i=0;i<7;i++){TextView day=Modern.text(a,initials[i],11,Glass.subtle(a),true);day.setGravity(Gravity.CENTER);day.setContentDescription(names[i]);weekdays.addView(day,new LinearLayout.LayoutParams(0,a.dp(32),1));}grid.addView(weekdays);
   int offset=selected.withDayOfMonth(1).getDayOfWeek().getValue()-1,maximum=selected.lengthOfMonth();
   for(int row=0;row<6;row++){LinearLayout line=Modern.row(a);for(int col=0;col<7;col++){
    int number=row*7+col-offset+1;FrameLayout cell=new FrameLayout(a);cell.setMinimumWidth(a.dp(44));cell.setMinimumHeight(a.dp(48));line.addView(cell,new LinearLayout.LayoutParams(0,a.dp(48),1));
    if(number<1||number>maximum){cell.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);continue;}
    final LocalDate date=selected.withDayOfMonth(number);boolean chosen=date.equals(selected),isToday=date.equals(today);TextView text=Modern.text(a,String.valueOf(number),16,chosen?0xff162c34:col>4?Glass.subtle(a):Glass.ink(a),chosen);text.setGravity(Gravity.CENTER);text.setPadding(0,0,0,0);text.setSelected(chosen);
    if(chosen)text.setBackground(Glass.gradient(a,Glass.mix(Glass.primary(a),Color.WHITE,.16f),Glass.primary(a),22));else if(isToday){GradientDrawable ring=Glass.round(a,Color.TRANSPARENT,22);ring.setStroke(Math.max(1,a.dp(1)),Glass.accent(a));text.setBackground(ring);text.setTextColor(Glass.accent(a));}
    cell.addView(text,new FrameLayout.LayoutParams(a.dp(44),a.dp(44),Gravity.CENTER));cell.setSelected(chosen);cell.setContentDescription(FULL.format(date)+(isToday?", hoy":"")+(chosen?", seleccionado":""));Modern.press(cell,()->{selected=date;preferredDay=date.getDayOfMonth();draw();});dayCells.put(number,cell);
   }grid.addView(line);}
  }
  LinearLayout childContent(String title){LinearLayout box=Modern.column(a);box.setPadding(a.dp(18),a.dp(17),a.dp(18),a.dp(14));TextView label=Modern.text(a,title,25,Glass.ink(a),false);label.setTypeface(Glass.font(a,500));label.setPadding(0,0,0,a.dp(16));box.addView(label);return box;}
  void openChild(SheetDialog sheet){if(child!=null)child.dismiss();child=sheet;sheet.setOnDismissListener(d->{if(child==sheet)child=null;});sheet.show();}
  void months(){LinearLayout box=childContent("Mes");SheetDialog sheet=new SheetDialog(a,box);for(int row=0;row<4;row++){LinearLayout line=Modern.row(a);for(int column=0;column<3;column++){final int month=row*3+column+1;TextView item=Glass.pill(a,title(java.time.Month.of(month).getDisplayName(TextStyle.FULL,ES)),selected.getMonthValue()==month);item.setContentDescription("Elegir "+item.getText());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,a.dp(52),1);if(column<2)p.rightMargin=a.dp(5);line.addView(item,p);Modern.press(item,()->{chooseMonthYear(selected.getYear(),month);sheet.dismiss();});}Modern.add(box,line,row==0?0:6);}Button cancel=Glass.button(a,"Cancelar");sheet.footer.addView(cancel);cancel.setOnClickListener(v->sheet.dismiss());openChild(sheet);}
  void years(){LinearLayout box=childContent("Año");yearField=new EditText(a);yearField.setText(String.valueOf(selected.getYear()));yearField.setContentDescription("Año, puedes escribirlo directamente");yearField.setHint("Escribir año");yearField.setTextSize(26);yearField.setTypeface(Glass.font(a,400));yearField.setFontFeatureSettings("tnum");yearField.setTextColor(Glass.ink(a));yearField.setHintTextColor(Glass.subtle(a));yearField.setSingleLine(true);Glass.editor(yearField);yearField.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);yearField.setPadding(a.dp(16),a.dp(12),a.dp(16),a.dp(12));yearField.setMinHeight(a.dp(60));yearField.setBackground(Glass.material(a,241,17,Glass.secondary(a)));box.addView(yearField,new LinearLayout.LayoutParams(-1,-2));
   yearCenter=selected.getYear();LinearLayout navigation=Modern.row(a),years=Modern.column(a);TextView range=Modern.text(a,"",12,Glass.subtle(a),false);range.setGravity(Gravity.CENTER);View earlier=Modern.iconButton(a,"back","Doce años anteriores",()->{yearCenter=shiftedYear(yearCenter,-12);yearGrid(years,range);}),later=Modern.iconButton(a,"chevron","Doce años siguientes",()->{yearCenter=shiftedYear(yearCenter,12);yearGrid(years,range);});navigation.addView(earlier,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));navigation.addView(range,new LinearLayout.LayoutParams(0,a.dp(44),1));navigation.addView(later,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));Modern.add(box,navigation,12);Modern.add(box,years,4);yearGrid(years,range);
   SheetDialog sheet=new SheetDialog(a,box);sheet.actions("Aplicar año",()->{try{int year=parseYear(yearField.getText().toString());chooseMonthYear(year,selected.getMonthValue());sheet.dismiss();}catch(IllegalArgumentException e){yearField.setError("Año entre 1 y 9999");yearField.requestFocus();}});openChild(sheet);
  }
  void yearGrid(LinearLayout box,TextView range){box.removeAllViews();int first=(int)Math.max(MIN_YEAR,Math.min((long)MAX_YEAR-11,(long)yearCenter-5));range.setText(first+" — "+(first+11));for(int row=0;row<4;row++){LinearLayout line=Modern.row(a);for(int column=0;column<3;column++){final int year=first+row*3+column;TextView item=Glass.pill(a,String.valueOf(year),year==selected.getYear());item.setContentDescription("Año "+year);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,a.dp(48),1);if(column<2)p.rightMargin=a.dp(5);line.addView(item,p);Modern.press(item,()->{yearField.setError(null);yearField.setText(String.valueOf(year));yearField.setSelection(yearField.length());});}Modern.add(box,line,row==0?0:6);}}
 }
}

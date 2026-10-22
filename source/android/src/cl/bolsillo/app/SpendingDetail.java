package cl.bolsillo.app;

import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** One sheet keeps the chart, selected IDs and dated personal-expense rows in sync. */
final class SpendingDetail {
 static SheetDialog show(MainActivity a,int mode)throws Exception{return show(a,mode,Ledger.today().substring(0,7),-1);}
 static SheetDialog show(MainActivity a,int mode,int selected)throws Exception{return show(a,mode,Ledger.today().substring(0,7),selected);}
 static SheetDialog show(MainActivity a,int mode,String month,int selected)throws Exception{return new Session(a,mode,month,selected).dialog;}
 static final int[] COLORS={Glass.MINT,Glass.VIOLET,Glass.CORAL,Glass.BLUE,Glass.AMBER};
 static final class Session {
  final MainActivity a;final JSONObject data;final SheetDialog dialog;final LinearLayout content,rows,header,modes;final TextView period,range;final View next;final Button recordsAction;
  final List<View> legend=new ArrayList<>();SpendingCharts.Snapshot snapshot;DonutChart chart;LinearLayout records;TextView selectionText;int mode,selected,page;String month;boolean expanded;long generation;
  Session(MainActivity a,int mode,String month,int selected)throws Exception{
   this.a=a;this.data=a.data;this.mode=mode;this.month=month;this.selected=selected;
   LinearLayout box=Modern.column(a);box.setPadding(a.dp(18),a.dp(8),a.dp(18),a.dp(14));dialog=new SheetDialog(a,box);dialog.layout.setTag("spending-detail");
   header=Modern.row(a);header.addView(Modern.text(a,"En qué se va",23,Glass.ink(a),true),new LinearLayout.LayoutParams(0,-2,1));header.addView(Modern.iconButton(a,"close","Cerrar distribución",dialog::dismiss),new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));box.addView(header);
   LinearLayout periodRow=Modern.row(a);periodRow.addView(Modern.iconButton(a,"back","Período anterior",()->shift(-1)),new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));period=Modern.text(a,"",14,Glass.ink(a),true);period.setGravity(Gravity.CENTER);periodRow.addView(period,new LinearLayout.LayoutParams(0,-2,1));next=Modern.iconButton(a,"chevron","Período siguiente",()->shift(1));periodRow.addView(next,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));Modern.add(box,periodRow,8);
   range=Modern.text(a,"",11,Glass.subtle(a),false);range.setGravity(Gravity.CENTER);Modern.add(box,range,0);
   modes=Modern.row(a);String[] titles={"Mes","3 meses","Cuentas"};for(int i=0;i<titles.length;i++){final int value=i;TextView pill=Glass.pill(a,titles[i],mode==i);pill.setTag("spending-mode:"+i);modes.addView(pill,new LinearLayout.LayoutParams(0,-2,1));Modern.press(pill,()->{this.mode=value;this.selected=-1;load();});}Modern.add(box,modes,12);
   content=Modern.column(a);Modern.add(box,content,12);rows=box;recordsAction=Glass.button(a,"Ver movimientos");recordsAction.setTag("spending-records-action");recordsAction.setEnabled(false);dialog.footer.addView(recordsAction);recordsAction.setOnClickListener(v->a.attempt(()->{if(snapshot==null)return;expanded=!expanded;page=0;drawRecords();if(!expanded)dialog.scroll.smoothScrollTo(0,0);else records.post(()->dialog.scroll.smoothScrollTo(0,records.getTop()+content.getTop()));}));
   dialog.setOnDismissListener(d->generation++);dialog.show();load();
  }
  void shift(int offset){String candidate=LocalDate.parse(month+"-01").plusMonths(offset).toString().substring(0,7);if(candidate.compareTo(Ledger.today().substring(0,7))>0)return;month=candidate;selected=-1;load();}
  void load(){
   long request=++generation;expanded=false;page=0;snapshot=null;recordsAction.setEnabled(false);period.setText(LocalDate.parse(month+"-01").format(DateTimeFormatter.ofPattern("MMMM yyyy",new Locale("es","CL"))));next.setEnabled(month.compareTo(Ledger.today().substring(0,7))<0);next.setAlpha(next.isEnabled()?1:.35f);
   for(int i=0;i<modes.getChildCount();i++)Glass.selectPill(a,(TextView)modes.getChildAt(i),i==mode);content.removeAllViews();ProgressBar busy=new ProgressBar(a);LinearLayout.LayoutParams busyLayout=new LinearLayout.LayoutParams(a.dp(28),a.dp(28));busyLayout.gravity=Gravity.CENTER;busyLayout.topMargin=a.dp(20);busyLayout.bottomMargin=a.dp(20);content.addView(busy,busyLayout);final int wantedMode=mode;final String wantedMonth=month;
   NativeSync.EXEC.execute(()->{try{SpendingCharts.Snapshot result=SpendingCharts.snapshot(data,wantedMode,wantedMonth);a.handler.post(()->{if(request!=generation||!dialog.isShowing()||a.isFinishing()||a.isDestroyed())return;snapshot=result;a.attempt(this::paint);});}catch(Exception error){a.handler.post(()->{if(request!=generation||!dialog.isShowing())return;content.removeAllViews();Modern.add(content,Modern.text(a,"No se pudo mostrar este período",14,Glass.ink(a),false),12);});}});
  }
  void paint()throws Exception {
   content.removeAllViews();legend.clear();range.setText(snapshot.from.compareTo(snapshot.until)>0?"Sin gastos confirmados en este período":MainActivity.shortDate(snapshot.from)+" — "+MainActivity.shortDate(snapshot.until)+" · Gasto personal");
   if(selected>=snapshot.slices.size())selected=-1;
   long[] values=new long[snapshot.slices.size()];String[] names=new String[values.length];for(int i=0;i<values.length;i++){values[i]=snapshot.slices.get(i).value;names[i]=snapshot.slices.get(i).label;}
   chart=new DonutChart(a,values,COLORS);chart.period=mode==1?"TRES MESES":"GASTO PERSONAL";chart.setLabels(names);chart.setTag("spending-chart");content.addView(chart,new LinearLayout.LayoutParams(-1,a.dp(248)));
   selectionText=Modern.text(a,"",12,Glass.subtle(a),false);selectionText.setGravity(Gravity.CENTER);selectionText.setTag("spending-selection");selectionText.setMinHeight(a.dp(44));Modern.press(selectionText,()->chart.select(-1,true));Modern.add(content,selectionText,0);
   LinearLayout list=Modern.column(a);list.setPadding(0,a.dp(10),0,0);for(int i=0;i<snapshot.slices.size();i++){final int index=i;View item=legendRow(snapshot.slices.get(i),i);legend.add(item);Modern.add(list,item,6);Modern.press(item,()->chart.select(index,true));}content.addView(list);
   if(snapshot.slices.isEmpty()){TextView empty=Modern.text(a,"No hay gastos confirmados en este período",14,Glass.subtle(a),false);empty.setGravity(Gravity.CENTER);Modern.add(content,empty,12);}
   records=Modern.column(a);records.setTag("spending-records");Modern.add(content,records,20);chart.select(selected,false);chart.selection=index->{selected=index;expanded=false;page=0;updateSelection();a.attempt(this::drawRecords);};updateSelection();drawRecords();
  }
  View legendRow(SpendingCharts.Slice slice,int index){
   int color=COLORS[index%COLORS.length];LinearLayout card=Modern.column(a);card.setPadding(a.dp(10),a.dp(8),a.dp(10),a.dp(8));card.setMinimumHeight(a.dp(56));card.setTag("spending-category:"+index);LinearLayout line=Modern.row(a);
   FrameLayout icon=new FrameLayout(a);icon.setBackground(Glass.round(a,Glass.alpha(color,26),10));icon.addView(Icons.view(a,Modern.iconFor(slice.label),color,18),new FrameLayout.LayoutParams(a.dp(18),a.dp(18),Gravity.CENTER));line.addView(icon,new LinearLayout.LayoutParams(a.dp(32),a.dp(32)));
   LinearLayout names=Modern.column(a);names.setPadding(a.dp(10),0,a.dp(8),0);TextView name=Modern.text(a,slice.label,13,Glass.ink(a),true);name.setMaxLines(2);names.addView(name);line.addView(names,new LinearLayout.LayoutParams(0,-2,1));String full=a.money(slice.value);boolean stacked=full.length()>13||a.getResources().getConfiguration().fontScale>1.15f;
   TextView amount=Modern.text(a,full,14,Glass.ink(a),true);amount.setGravity(Gravity.END);if(stacked){Modern.add(names,amount,4);Modern.fitAmount(amount,12,14);}else{line.addView(amount,new LinearLayout.LayoutParams(-2,-2));}
   TextView share=Modern.text(a,SpendingCharts.percent(slice.value,snapshot.total),12,color,true);share.setGravity(Gravity.END);share.setPadding(a.dp(8),0,0,0);line.addView(share,new LinearLayout.LayoutParams(a.dp(44),-2));card.addView(line);
   ShareTrack track=new ShareTrack(a,color,snapshot.total>0?(float)((double)slice.value/snapshot.total):0);LinearLayout.LayoutParams trackLayout=new LinearLayout.LayoutParams(-1,a.dp(3));trackLayout.leftMargin=a.dp(42);trackLayout.topMargin=a.dp(7);card.addView(track,trackLayout);card.setContentDescription(slice.label+", "+full+", "+SpendingCharts.percent(slice.value,snapshot.total));return card;
  }
  void updateSelection(){for(int i=0;i<legend.size();i++)legend.get(i).setBackground(i==selected?Glass.material(a,220,16,COLORS[i%COLORS.length]):Glass.round(a,android.graphics.Color.TRANSPARENT,16));selectionText.setEnabled(selected>=0);selectionText.setContentDescription(selected<0?"Total de gasto personal":"Quitar filtro y ver todos los gastos");selectionText.setText(selected<0?"Total · "+a.money(snapshot.total):snapshot.slices.get(selected).label+" · "+SpendingCharts.percent(snapshot.slices.get(selected).value,snapshot.total)+"  ×");}
  void drawRecords()throws Exception {
   if(snapshot==null||records==null)return;records.removeAllViews();SpendingCharts.Slice slice=selected>=0?snapshot.slices.get(selected):null;List<JSONObject> entries=snapshot.records(slice);LinearLayout heading=Modern.row(a);heading.addView(Modern.text(a,slice==null?"Movimientos":slice.label,16,Glass.ink(a),true),new LinearLayout.LayoutParams(0,-2,1));heading.addView(Modern.text(a,entries.size()+"",13,Glass.subtle(a),false));records.addView(heading);int limit=expanded?20:3,start=expanded?page*20:0;
   for(int i=start;i<Math.min(entries.size(),start+limit);i++){JSONObject tx=entries.get(i);String note=tx.optString("note");if(note.isEmpty())note="Gasto";long personal=Finance.personalExpense(tx);String subtitle=MainActivity.shortDate(tx.optString("date"))+" · "+a.name("accounts",tx.optString("account"));if(personal<tx.optLong("amount"))subtitle+=" · Tu parte";String icon=Icons.brand(note);if(icon.isEmpty())icon=Modern.iconFor(a.name("categories",tx.optString("category")));Modern.add(records,Modern.item(a,icon,note,subtitle,"−"+a.money(personal),Glass.CORAL,()->{dialog.dismiss();a.attempt(()->Forms.details(a,tx));}),6);}
   if(expanded&&entries.size()>20){LinearLayout paging=Modern.row(a);Button before=Glass.button(a,"Anteriores"),after=Glass.button(a,"Siguientes");before.setEnabled(page>0);after.setEnabled((page+1)*20<entries.size());paging.addView(before,new LinearLayout.LayoutParams(0,-2,1));paging.addView(after,new LinearLayout.LayoutParams(0,-2,1));before.setOnClickListener(v->{page--;a.attempt(this::drawRecords);});after.setOnClickListener(v->{page++;a.attempt(this::drawRecords);});Modern.add(records,paging,8);}
   recordsAction.setEnabled(!entries.isEmpty());recordsAction.setText(expanded?"Volver al gráfico":"Ver "+entries.size()+(entries.size()==1?" movimiento":" movimientos"));
  }
 }
 static final class ShareTrack extends View {
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final RectF track=new RectF(),fill=new RectF();final int color;final float fraction;Shader material;
  ShareTrack(MainActivity a,int color,float fraction){super(a);this.color=color;this.fraction=fraction;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
  protected void onSizeChanged(int w,int h,int oldw,int oldh){track.set(0,0,w,h);fill.set(0,0,w*fraction,h);material=new LinearGradient(0,0,Math.max(1,w),0,new int[]{Glass.mix(color,android.graphics.Color.WHITE,.16f),color},null,Shader.TileMode.CLAMP);}
  protected void onDraw(Canvas c){paint.setShader(null);paint.setColor(Glass.alpha(Glass.subtle(getContext()),35));c.drawRoundRect(track,getHeight()/2f,getHeight()/2f,paint);paint.setShader(material);c.drawRoundRect(fill,getHeight()/2f,getHeight()/2f,paint);paint.setShader(null);}
 }
}

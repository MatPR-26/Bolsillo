package cl.bolsillo.app;

import android.graphics.Color;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Read-only service catalogue. Opening or filtering it never creates subscriptions. */
final class ServicePicker {
 static SheetDialog show(MainActivity a)throws Exception{return new Picker(a).dialog;}

 static final class Picker {
  final MainActivity a;final SheetDialog dialog;final EditText search;final ListView list;
  final List<ServiceCatalog.Entry> visible=new ArrayList<>();final Set<String> configured=new HashSet<>();
  final List<TextView> chips=new ArrayList<>();final BaseAdapter adapter;String group="";
  Picker(MainActivity a)throws Exception{
   this.a=a;
   JSONArray recurring=LocalData.read(a).getJSONArray("recurring");
   for(int i=0;i<recurring.length();i++){JSONObject record=recurring.getJSONObject(i);if(!record.optString("type").equals("expense"))continue;ServiceCatalog.Entry service=Subscriptions.service(record);if(service!=null)configured.add(service.id);}
   LinearLayout header=Modern.column(a);header.setPadding(a.dp(18),a.dp(8),a.dp(18),a.dp(6));
   dialog=new SheetDialog(a,header);dialog.layout.setTag("service-picker");
   LinearLayout heading=Modern.row(a);TextView title=Modern.text(a,"Elige tu servicio",23,Glass.ink(a),true);heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));heading.addView(Modern.iconButton(a,"close","Cerrar catálogo",dialog::dismiss),new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));header.addView(heading);
   search=NativeSettings.input(a,header,"Buscar servicio","");search.setTag("service-picker-query");search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH|android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN);
   LinearLayout.LayoutParams searchLayout=(LinearLayout.LayoutParams)search.getLayoutParams();searchLayout.bottomMargin=a.dp(4);search.setLayoutParams(searchLayout);
   HorizontalScrollView groups=new HorizontalScrollView(a);groups.setHorizontalScrollBarEnabled(false);groups.setFillViewport(false);LinearLayout groupRow=Modern.row(a);groups.addView(groupRow);header.addView(groups,new LinearLayout.LayoutParams(-1,a.dp(48)));
   addChip(groupRow,"Todos","");for(String name:ServiceCatalog.groups())addChip(groupRow,name,name);
   // A direct ListView keeps the search/chips and footer fixed. No nested scrolling containers.
   dialog.scroll.removeView(header);dialog.layout.removeView(dialog.scroll);dialog.layout.addView(header,1,new LinearLayout.LayoutParams(-1,-2));
   FrameLayout results=new FrameLayout(a);results.setPadding(a.dp(18),0,a.dp(18),0);
   list=new ListView(a);list.setTag("service-picker-list");list.setDivider(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));list.setDividerHeight(a.dp(6));list.setCacheColorHint(Color.TRANSPARENT);list.setVerticalScrollBarEnabled(false);list.setChoiceMode(ListView.CHOICE_MODE_NONE);results.addView(list,new FrameLayout.LayoutParams(-1,-1));
   TextView empty=Modern.text(a,"No hay coincidencias. Puedes añadir otro servicio.",14,Glass.subtle(a),false);empty.setGravity(Gravity.CENTER);empty.setPadding(a.dp(16),a.dp(16),a.dp(16),a.dp(16));results.addView(empty,new FrameLayout.LayoutParams(-1,-1));list.setEmptyView(empty);
   dialog.layout.addView(results,2,new LinearLayout.LayoutParams(-1,a.dp(380),1));
   adapter=new BaseAdapter(){
    public int getCount(){return visible.size();}public Object getItem(int position){return visible.get(position);}public long getItemId(int position){return visible.get(position).id.hashCode();}
    public View getView(int position,View recycled,ViewGroup parent){ServiceRow row=recycled instanceof ServiceRow?(ServiceRow)recycled:new ServiceRow(Picker.this);row.bind(visible.get(position));return row;}
   };list.setAdapter(adapter);
   search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){filter();}public void afterTextChanged(Editable e){}});
   Button other=Glass.button(a,"Volver");other.setText("Otro servicio");other.setTag("service-picker-custom");other.setOnClickListener(v->{dialog.dismiss();a.attempt(()->Subscriptions.add(a,""));});dialog.footer.addView(other);
   header.setFocusableInTouchMode(true);header.requestFocus();filter();dialog.show();
  }
  void addChip(LinearLayout row,String label,String value){TextView chip=Glass.pill(a,label,value.equals(group));chip.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,13);chip.setTag(value);chip.setContentDescription("Filtrar por "+label);chip.setMinHeight(a.dp(48));LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-1);params.rightMargin=a.dp(4);row.addView(chip,params);chips.add(chip);Modern.press(chip,()->{group=value;for(TextView item:chips)Glass.selectPill(a,item,group.equals(item.getTag()));filter();});}
  void filter(){visible.clear();visible.addAll(ServiceCatalog.search(search.getText().toString(),group));adapter.notifyDataSetChanged();list.setSelection(0);}
  void select(ServiceCatalog.Entry entry){a.attempt(()->{List<JSONObject> existing=Subscriptions.configured(a,entry);dialog.dismiss();if(existing.isEmpty())Subscriptions.add(a,entry.name);else existing(a,entry,existing);});}
 }

 static final class ServiceRow extends LinearLayout {
  final Picker picker;final FrameLayout icon;final TextView name,detail;ServiceCatalog.Entry entry;String bound="";
  ServiceRow(Picker picker){super(picker.a);this.picker=picker;MainActivity a=picker.a;setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);setPadding(a.dp(10),a.dp(10),a.dp(10),a.dp(10));setMinimumHeight(a.dp(72));setBackground(Glass.surface(a,222));icon=new FrameLayout(a);addView(icon,new LinearLayout.LayoutParams(a.dp(42),a.dp(42)));LinearLayout texts=Modern.column(a);texts.setPadding(a.dp(12),0,a.dp(8),0);name=Modern.text(a,"",15,Glass.ink(a),true);name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);detail=Modern.text(a,"",12,Glass.subtle(a),false);detail.setSingleLine(true);detail.setEllipsize(TextUtils.TruncateAt.END);texts.addView(name);texts.addView(detail);addView(texts,new LinearLayout.LayoutParams(0,-2,1));Modern.press(this,()->{if(entry!=null)picker.select(entry);});}
  void bind(ServiceCatalog.Entry service){entry=service;MainActivity a=picker.a;if(!bound.equals(service.id)){AccountGlass pane=new AccountGlass(a,Glass.secondary(a),24);setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(Glass.alpha(Glass.accent(a),45)),pane,null));WaterRipple.apply(this);BrandPalette.bind(a,service.id,pane);icon.removeAllViews();icon.addView(Icons.badge(a,service.id,42),new FrameLayout.LayoutParams(-1,-1));bound=service.id;}boolean added=picker.configured.contains(service.id);name.setText(service.name);detail.setText(added?"Ya añadido · "+service.group:service.group);setContentDescription(service.name+". "+(added?"Ya añadido. ":"")+service.group);setTag("service:"+service.id);}
 }

 static SheetDialog existing(MainActivity a,ServiceCatalog.Entry entry,List<JSONObject> records){
  LinearLayout box=NativeSettings.panel(a,entry.name+" ya está añadido");
  Modern.add(box,Modern.text(a,"Elige una programación o crea otra independiente.",14,Glass.subtle(a),false),8);
  SheetDialog dialog=new SheetDialog(a,box);dialog.layout.setTag("service-existing");
  ListView list=new ListView(a);list.setTag("service-existing-list");list.setDivider(null);list.setVerticalScrollBarEnabled(false);list.setCacheColorHint(Color.TRANSPARENT);
  list.setAdapter(new BaseAdapter(){public int getCount(){return records.size();}public Object getItem(int position){return records.get(position);}public long getItemId(int position){return records.get(position).optString("id").hashCode();}public View getView(int position,View recycled,ViewGroup parent){JSONObject record=records.get(position);String account="";try{account=a.name("accounts",record.optString("account"))+" · ";}catch(Exception ignored){}String detail=account+MainActivity.shortDate(record.optString("next"))+(record.optBoolean("active")?"":" · Pausada");return Modern.item(a,entry.id,record.optString("name",entry.name),detail,a.money(record.optLong("amount")),Glass.VIOLET,()->{dialog.dismiss();Forms.recurringMenu(a,record);});}});
  dialog.scroll.removeView(box);dialog.layout.removeView(dialog.scroll);dialog.layout.addView(box,1,new LinearLayout.LayoutParams(-1,-2));LinearLayout.LayoutParams listLayout=new LinearLayout.LayoutParams(-1,Math.min(a.dp(320),records.size()*a.dp(84)),1);listLayout.setMargins(a.dp(18),0,a.dp(18),0);dialog.layout.addView(list,2,listLayout);
  Button another=Glass.button(a,"Crear otra suscripción");another.setTag("service-existing-another");dialog.footer.addView(another);another.setOnClickListener(v->{dialog.dismiss();a.attempt(()->Subscriptions.add(a,entry.name));});
  Button close=Glass.button(a,"Volver");dialog.footer.addView(close);close.setOnClickListener(v->dialog.dismiss());dialog.show();return dialog;
 }
}

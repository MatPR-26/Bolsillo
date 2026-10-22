package cl.bolsillo.app;
import org.json.*;import java.time.*;import java.util.*;

/** A read-only chart and its drill-down share one exact period, filter and ID set. */
final class SpendingCharts {
 static final class Slice {
  final String id,label;final long value;final Set<String> members;
  Slice(String id,String label,long value){this(id,label,value,Collections.singleton(id));}
  Slice(String id,String label,long value,Set<String> members){this.id=id;this.label=label;this.value=value;this.members=Collections.unmodifiableSet(new LinkedHashSet<>(members));}
 }
 static final class Snapshot {
  final int mode;final String month,from,until;final List<Slice> slices;final List<JSONObject> rows;final long total;
  Snapshot(int mode,String month,String from,String until,List<Slice> slices,List<JSONObject> rows,long total){this.mode=mode;this.month=month;this.from=from;this.until=until;this.slices=Collections.unmodifiableList(slices);this.rows=Collections.unmodifiableList(rows);this.total=total;}
  List<JSONObject> records(Slice slice){List<JSONObject> result=new ArrayList<>();for(JSONObject row:rows)if(slice==null||slice.members.contains(row.optString(mode==2?"account":"category")))result.add(row);return result;}
 }
 static List<Slice> slices(JSONObject data,int mode)throws Exception{return snapshot(data,mode,Ledger.today().substring(0,7)).slices;}
 static Snapshot snapshot(JSONObject data,int mode,String month)throws Exception {
  if(mode<0||mode>2)throw new IllegalArgumentException("Vista de gastos inválida");
  LocalDate first=LocalDate.parse(month+"-01"),today=LocalDate.parse(Ledger.today());String from=(mode==1?first.minusMonths(2):first).toString(),until=first.withDayOfMonth(first.lengthOfMonth()).isBefore(today)?first.withDayOfMonth(first.lengthOfMonth()).toString():today.toString();
  Map<String,Long> sums=new LinkedHashMap<>();List<JSONObject> rows=new ArrayList<>();long total=0;JSONArray transactions=data.getJSONArray("transactions");
  for(int i=0;i<transactions.length();i++){
   JSONObject t=transactions.getJSONObject(i);String date=t.optString("date");
   if(!t.optString("type").equals("expense")||!t.optString("status").equals("posted")||t.optBoolean("excluded")||date.compareTo(from)<0||date.compareTo(until)>0)continue;
   long personal=Finance.personalExpense(t);if(personal<=0)continue;String id=t.optString(mode==2?"account":"category");sums.put(id,Math.addExact(sums.getOrDefault(id,0L),personal));total=Math.addExact(total,personal);rows.add(t);
  }
  List<Slice> all=new ArrayList<>();JSONArray definitions=data.getJSONArray(mode==2?"accounts":"categories");
  for(Map.Entry<String,Long> e:sums.entrySet()){JSONObject item=Ledger.find(definitions,e.getKey());all.add(new Slice(e.getKey(),item==null?(mode==2?"Sin cuenta":"Sin categoría"):item.optString("name"),e.getValue()));}
  all.sort((a,b)->{int amount=Long.compare(b.value,a.value);return amount!=0?amount:a.label.compareToIgnoreCase(b.label);});
  if(all.size()>5){long other=0;Set<String> members=new LinkedHashSet<>();for(int i=4;i<all.size();i++){other=Math.addExact(other,all.get(i).value);members.addAll(all.get(i).members);}all=new ArrayList<>(all.subList(0,4));String remainder="Otros";for(Slice top:all)if(top.label.trim().equalsIgnoreCase(remainder)){remainder=mode==2?"Más cuentas":"Más categorías";break;}all.add(new Slice("",remainder,other,members));}
  rows.sort((a,b)->{int date=b.optString("date").compareTo(a.optString("date"));return date!=0?date:a.optString("id").compareTo(b.optString("id"));});
  return new Snapshot(mode,month,from,until,all,rows,total);
 }
 static String percent(long value,long total){if(total<=0)return "0%";double share=100d*value/total;if(share>0&&share<1)return "<1%";return Math.round(share)+"%";}
}

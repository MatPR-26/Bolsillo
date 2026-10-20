package cl.bolsillo.app;
import org.json.*;import java.time.*;import java.util.*;
/** One accounting filter for all swipeable spending summaries. */
final class SpendingCharts {
 static final class Slice {String id,label;long value;Slice(String id,String label,long value){this.id=id;this.label=label;this.value=value;}}
 static List<Slice> slices(JSONObject data,int mode)throws Exception{
  String month=Ledger.today().substring(0,7),from=mode==1?LocalDate.parse(month+"-01").minusMonths(2).toString():month+"-01",until=Ledger.today();
  Map<String,Long> sums=new LinkedHashMap<>();JSONArray transactions=data.getJSONArray("transactions");for(int i=0;i<transactions.length();i++){JSONObject t=transactions.getJSONObject(i);String date=t.optString("date");if(!t.optString("type").equals("expense")||!t.optString("status").equals("posted")||t.optBoolean("excluded")||date.compareTo(from)<0||date.compareTo(until)>0)continue;String id=t.optString(mode==2?"account":"category");long personal=Finance.personalExpense(t);if(personal>0)sums.put(id,Math.addExact(sums.getOrDefault(id,0L),personal));}
  List<Slice> all=new ArrayList<>();JSONArray definitions=data.getJSONArray(mode==2?"accounts":"categories");for(Map.Entry<String,Long> e:sums.entrySet()){JSONObject item=Ledger.find(definitions,e.getKey());all.add(new Slice(e.getKey(),item==null?(mode==2?"Sin cuenta":"Sin categoría"):item.optString("name"),e.getValue()));}all.sort((a,b)->Long.compare(b.value,a.value));
  if(all.size()>5){long other=0;for(int i=4;i<all.size();i++)other=Math.addExact(other,all.get(i).value);List<Slice> top=new ArrayList<>(all.subList(0,4));top.add(new Slice("","Otros",other));return top;}return all;
 }
}

package cl.bolsillo.app;

import org.json.*;
import java.util.*;

/** Template recognition is read-only: historic JSON, custom categories and amounts are retained. */
public final class ServiceCatalogTest {
 static int checks;
 static void yes(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static void is(String raw,String expected){ServiceCatalog.Entry found=ServiceCatalog.identify(raw);yes(found!=null&&found.id.equals(expected),"Expected "+expected+" for ["+raw+"], got "+(found==null?"null":found.id));}
 static void unknown(String raw){yes(ServiceCatalog.identify(raw)==null,"Ambiguous/unknown label must stay unassigned: "+raw);}
 static Set<String> ids(List<ServiceCatalog.Entry> entries){Set<String> ids=new LinkedHashSet<>();for(ServiceCatalog.Entry entry:entries)ids.add(entry.id);return ids;}
 static JSONObject movement(String id,String type,String note,long amount,String date,String category)throws Exception{return new JSONObject().put("id",id).put("type",type).put("note",note).put("amount",amount).put("date",date).put("account","bank-mach").put("category",category).put("status","posted").put("source","manual").put("personalNote","Conservar texto original y fecha");}
 static ConceptCatalog.Group group(List<ConceptCatalog.Group> groups,String key){for(ConceptCatalog.Group group:groups)if(group.key.equals(key))return group;throw new AssertionError("Missing group: "+key);}
 public static void main(String[] args)throws Exception{
  List<ServiceCatalog.Entry> all=ServiceCatalog.all();Set<String> unique=new HashSet<>(),declaredGroups=new LinkedHashSet<>();
  yes(all.size()==60,"There must be exactly 60 selectable service templates");
  for(ServiceCatalog.Entry entry:all){
   yes(entry.id.matches("[a-z0-9]+")&&unique.add(entry.id),"Stable service IDs must be unique: "+entry.id);
   yes(!entry.name.trim().isEmpty()&&!entry.group.trim().isEmpty(),"Every service must have a display name and group");
   yes(ServiceCatalog.byId(entry.id)==entry,"IDs resolve the same read-only template");
   is(entry.id,entry.id);is(entry.name,entry.id);is(entry.name.toUpperCase(Locale.ROOT),entry.id);
   yes((entry.color>>>24)==255&&(entry.plate>>>24)==255,"Brand and plate colours must be opaque");
   declaredGroups.add(entry.group);
  }
  yes(ServiceCatalog.byId(null)==null&&ServiceCatalog.byId("unlisted")==null,"Unknown IDs have no fallback service");
  yes(new LinkedHashSet<>(ServiceCatalog.groups()).equals(declaredGroups),"Each declared group is present once in catalog order");
  Set<String> partition=new HashSet<>();
  for(String category:ServiceCatalog.groups()){
   List<ServiceCatalog.Entry> matches=ServiceCatalog.search("",category);yes(!matches.isEmpty(),"Every group has selectable services");
   for(ServiceCatalog.Entry entry:matches){yes(entry.group.equals(category),"Group filters cannot leak unrelated services");yes(partition.add(entry.id),"A service belongs to only one group");}
  }
  yes(partition.equals(unique),"Browsing every group exposes all 60 templates");
  yes(ids(ServiceCatalog.search("",null)).equals(unique)&&ids(ServiceCatalog.search("   ","Todos")).equals(unique),"Empty query and Todos expose every template");
  yes(ServiceCatalog.search("", "Grupo inexistente").isEmpty(),"Unknown group does not silently show all services");
  yes(ids(ServiceCatalog.search("MÚSICA","Todos")).containsAll(Arrays.asList("spotify","deezer","tidal")),"Search ignores accents and matches group names");
  yes(ids(ServiceCatalog.search("  YouTUBE   MUSIC  ","Todos")).equals(Collections.singleton("youtubemusic")),"Search normalizes spaces and case without mixing YouTube plans");
  yes(ids(ServiceCatalog.search("psplus","Videojuegos")).equals(Collections.singleton("playstation")),"Search includes familiar aliases");
  yes(ids(ServiceCatalog.search("copilot","Todos")).equals(new HashSet<>(Arrays.asList("copilot","githubcopilot"))),"Search offers both Copilot products rather than assuming one");
  yes(ids(ServiceCatalog.search("MICROSOFT","Inteligencia artificial")).equals(Collections.singleton("copilot")),"Text and group filters combine");
  yes(ServiceCatalog.search("servicio-inexistente-49872","Todos").isEmpty(),"Unknown searches have no invented match");
  boolean immutable=false;try{all.clear();}catch(UnsupportedOperationException expected){immutable=true;}yes(immutable&&ServiceCatalog.all().size()==60,"Callers cannot accidentally remove public templates");

  String[][] variants={
   {"Compra NETFLIX.COM CL","netflix"},{"Spotify AB Stockholm","spotify"},{"Pago Crunchyroll mensual","crunchyroll"},
   {"Compra Disney+ mensual","disneyplus"},{"Disney Plus anual","disneyplus"},{"Pago Paramount+","paramountplus"},
   {"Amazon Prime Video Chile","primevideo"},{"Pago HBO Max mensual","hbomax"},{"Compra Apple TV+","appletv"},
   {"YouTube Music Premium","youtubemusic"},{"YOUTUBEMUSIC","youtubemusic"},{"Google YouTube Premium","youtubepremium"},
   {"Chat GPT Plus mensual","chatgpt"},{"Google AI Pro","gemini"},{"Pago Microsoft Copilot Pro","copilot"},
   {"GitHub Copilot","githubcopilot"},{"Pago GitHub Copilot Pro","githubcopilot"},{"GitHub Pro anual","github"},
   {"PlayStation Plus Deluxe","playstation"},{"Xbox Game Pass Ultimate","xbox"},{"Pago Nintendo Switch Online","nintendo"},
   {"Office 365 mensual","microsoft365"},{"Creative Cloud","adobe"},{"Almacenamiento iCloud+","icloud"},
   {"MEGA Cloud mensual","mega"},{"Nord VPN mensual","nordvpn"},{"Compra 1Password","1password"},
   {"Discord Nitro anual","discord"},{"Pago Uber One mensual","uberone"},{"Apple One familiar","appleone"}
  };
  for(String[] variant:variants)is(variant[0],variant[1]);
  is("  MAX  ","hbomax");is("MÉGA","mega");is("GpT","chatgpt");
  for(String raw:new String[]{null,"","   ","Netflix + Spotify","Spotify y Netflix","Microsoft Copilot y GitHub Copilot","YouTube Premium y YouTube Music","Pago max mensual","Supermercado MEGA","Compra GPT mensual","MAXIMUM","Megalodon","SpotifyAB","Netflixable","OpenAI API","Servicio personal","GitHub factura"})unknown(raw);

  JSONObject data=Ledger.initial();String personal="category-personal-services";
  JSONObject personalCategory=new JSONObject().put("id",personal).put("name","Mis servicios y ocio").put("type","expense").put("emoji","🎧").put("color","#5B438F").put("budget",987654321).put("archived",false).put("custom",new JSONObject().put("retain",true));
  data.getJSONArray("categories").put(personalCategory);
  JSONArray transactions=data.getJSONArray("transactions");
  transactions.put(movement("nf-old","expense","NETFLIX.COM",500000000000L,"2026-01-03",personal));
  transactions.put(movement("nf-mid","expense","Netflix",400000000000L,"2026-02-04",personal));
  transactions.put(movement("nf-new","expense","Pago Netflix mensual",300000000000L,"2026-03-05",personal));
  transactions.put(movement("yt-one","expense","YouTube Music Premium",4990,"2026-03-06",personal));
  transactions.put(movement("yt-two","expense","YOUTUBEMUSIC",5990,"2026-04-07",personal));
  transactions.put(movement("yt-premium","expense","YouTube Premium",7990,"2026-04-07",personal));
  transactions.put(movement("nf-income","income","Netflix",1990,"2026-04-07","income-0"));
  transactions.put(movement("generic-one","expense","Compra",2000,"2026-04-07",personal));
  transactions.put(movement("generic-two","expense","Compra",3000,"2026-04-07",personal));
  transactions.put(movement("unknown","expense","Servicio personal de Julia",12990,"2026-04-07",personal));
  transactions.put(movement("combined","expense","Netflix + Spotify",14990,"2026-04-08",personal));
  JSONObject recurring=new JSONObject().put("id","my-yt-plan").put("name","YouTube Music").put("type","expense").put("category",personal).put("account","bank-mach").put("amount",4990).put("next","2026-05-07").put("frequency","monthly").put("active",true).put("automatic",false).put("shares",new JSONArray().put(new JSONObject().put("personId","family").put("amount",2500)));
  data.getJSONArray("recurring").put(recurring);
  data.getJSONArray("changeLog").put(new JSONObject().put("id","historic-edit").put("action","edited").put("at",1770000000000L).put("before",new JSONObject(transactions.getJSONObject(0).toString())).put("after",new JSONObject(transactions.getJSONObject(1).toString())));
  String original=data.toString(),categoryBefore=personalCategory.toString();
  List<ConceptCatalog.Group> groups=ConceptCatalog.groups(transactions);ConceptCatalog.Group netflix=group(groups,"expense:netflix"),music=group(groups,"expense:youtubemusic");
  yes(netflix.count()==3&&netflix.total==1200000000000L,"Historical Netflix variants group with exact large CLP totals");
  yes(netflix.items.get(0).getString("id").equals("nf-new")&&netflix.items.get(2).getString("id").equals("nf-old"),"The group opens original records in date order");
  yes(netflix.label.equals("Netflix")&&netflix.category.equals(personal),"Canonical display name retains the user's category");
  yes(music.count()==2&&music.total==10980&&music.label.equals("YouTube Music"),"YouTube Music aliases group together");
  yes(group(groups,"expense:youtubepremium").count()==1&&group(groups,"income:netflix").count()==1,"Premium and Music, and income and expense, remain distinct");
  yes(!ConceptCatalog.key(transactions.getJSONObject(7)).equals(ConceptCatalog.key(transactions.getJSONObject(8))),"Generic purchases never collapse into one unrelated concept");
  yes(group(groups,"expense:servicio personal de julia").label.equals("Servicio personal de Julia"),"Unknown personal concepts retain their display name");
  yes(group(groups,"expense:netflix spotify").count()==1&&group(groups,"expense:netflix spotify").label.equals("Netflix + Spotify"),"A historical combined label remains separate from either service");
  List<String> suggested=ConceptCatalog.suggestions(data,"expense");
  yes(Collections.frequency(suggested,"YouTube Music")==1&&Collections.frequency(suggested,"Netflix")==1,"Autocomplete merges templates with existing aliases without duplicates");
  yes(suggested.contains("Servicio personal de Julia")&&!suggested.contains("Compra"),"Autocomplete preserves personal concepts and excludes generic purchase text");
  yes(ConceptCatalog.preferredCategory(data,"expense","Pago Netflix").equals(personal)&&ConceptCatalog.preferredCategory(data,"expense","YouTubeMusic").equals(personal),"Personal category choices win when a service template is recognized");
  yes(ConceptCatalog.preferredCategory(data,"expense","Servicio sin registros").isEmpty(),"A template lookup cannot invent a category assignment");
  yes(data.toString().equals(original)&&personalCategory.toString().equals(categoryBefore),"Grouping, autocomplete and category suggestions preserve every historical JSON field and personal category exactly");
  int recurringCount=data.getJSONArray("recurring").length();
  for(ServiceCatalog.Entry entry:all){ServiceCatalog.search(entry.name,"Todos");ConceptCatalog.concept(entry.name);ConceptCatalog.label(entry.name);ConceptCatalog.preferredCategory(data,"expense",entry.name);}
  yes(data.toString().equals(original)&&data.getJSONArray("recurring").length()==recurringCount,"Browsing all templates never creates charges, recurring plans, categories or ledger edits");
  System.out.println("PASS: "+checks+" service template, safe recognition, grouping and historical preservation checks");
 }
}

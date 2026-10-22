package cl.bolsillo.app;

import java.util.*;

/** Read-only service templates. Looking up a brand never creates a recurring charge. */
final class ServiceCatalog {
 static final class Entry {
  final String id,name,group;final int color,plate;
  private final String normalizedName,searchText;
  private final String[] aliases;
  Entry(String id,String name,String group,String color,String plate,String aliases){this.id=id;this.name=name;this.group=group;this.normalizedName=normalize(name);this.searchText=normalize(name+" "+group+" "+id);this.color=(int)(0xff000000L|Long.parseLong(color,16));this.plate=(int)(0xff000000L|Long.parseLong(plate,16));String[] raw=aliases.split(";");this.aliases=new String[raw.length];for(int i=0;i<raw.length;i++)this.aliases[i]=(raw[i].startsWith("=")?"=":"")+normalize(raw[i]);}
 }
 private static Entry entry(String id,String name,String group,String color,String plate,String aliases){return new Entry(id,name,group,color,plate,aliases);}
 private static final List<Entry> ENTRIES=Collections.unmodifiableList(Arrays.asList(
  entry("netflix","Netflix","Streaming","E50914","FFFFFF","netflix"),
  entry("spotify","Spotify","Música y audio","1ED760","FFFFFF","spotify"),
  entry("crunchyroll","Crunchyroll","Streaming","FF5E00","FFFFFF","crunchyroll"),
  entry("disneyplus","Disney+","Streaming","000000","FFFFFF","disney plus;disneyplus;disney +;disney"),
  entry("paramountplus","Paramount+","Streaming","0064FF","FFFFFF","paramount plus;paramountplus;paramount"),
  entry("primevideo","Prime Video","Streaming","000000","FFFFFF","prime video;primevideo;amazon prime video"),
  entry("hbomax","HBO Max","Streaming","000000","FFFFFF","hbo max;hbomax;=max;hbo"),
  entry("appletv","Apple TV+","Streaming","000000","FFFFFF","apple tv;appletv"),
  entry("youtubepremium","YouTube Premium","Streaming","FF0000","FFFFFF","youtube premium;=youtube"),
  entry("applemusic","Apple Music","Música y audio","FA243C","FFFFFF","apple music;applemusic"),
  entry("youtubemusic","YouTube Music","Música y audio","FF0000","FFFFFF","youtube music;youtubemusic"),
  entry("amazonmusic","Amazon Music","Música y audio","000000","FFFFFF","amazon music;amazonmusic"),
  entry("deezer","Deezer","Música y audio","A238FF","FFFFFF","deezer"),
  entry("tidal","TIDAL","Música y audio","000000","FFFFFF","tidal"),
  entry("soundcloud","SoundCloud Go+","Música y audio","FF5500","FFFFFF","soundcloud go;soundcloud"),
  entry("audible","Audible","Música y audio","F8991C","FFFFFF","audible"),
  entry("chatgpt","ChatGPT","Inteligencia artificial","000000","FFFFFF","chatgpt;chat gpt;=gpt;openai chatgpt"),
  entry("claude","Claude","Inteligencia artificial","D97757","FFFFFF","claude;anthropic claude"),
  entry("gemini","Google Gemini","Inteligencia artificial","8E75B2","FFFFFF","google gemini;gemini advanced;google ai pro;google ai ultra;=gemini"),
  entry("perplexity","Perplexity","Inteligencia artificial","1FB8CD","FFFFFF","perplexity"),
  entry("copilot","Microsoft Copilot","Inteligencia artificial","000000","FFFFFF","microsoft copilot;copilot pro;=copilot"),
  entry("githubcopilot","GitHub Copilot","Inteligencia artificial","000000","FFFFFF","github copilot;githubcopilot"),
  entry("playstation","PlayStation Plus","Videojuegos","0070D1","FFFFFF","playstation plus;ps plus;psplus"),
  entry("xbox","Xbox Game Pass","Videojuegos","000000","FFFFFF","xbox game pass;game pass;gamepass"),
  entry("nintendo","Nintendo Switch Online","Videojuegos","000000","FFFFFF","nintendo switch online;nintendo online"),
  entry("applearcade","Apple Arcade","Videojuegos","000000","FFFFFF","apple arcade;applearcade"),
  entry("ubisoft","Ubisoft+","Videojuegos","000000","FFFFFF","ubisoft plus;ubisoft+"),
  entry("eaplay","EA Play","Videojuegos","000000","FFFFFF","ea play;eaplay"),
  entry("geforcenow","GeForce NOW","Videojuegos","76B900","FFFFFF","geforce now;geforcenow"),
  entry("microsoft365","Microsoft 365","Productividad","5E5E5E","FFFFFF","microsoft 365;office 365;microsoft365;office365"),
  entry("adobe","Adobe Creative Cloud","Productividad","000000","FFFFFF","adobe creative cloud;creative cloud;adobe cc"),
  entry("canva","Canva Pro","Productividad","000000","FFFFFF","canva pro;canva"),
  entry("notion","Notion","Productividad","000000","FFFFFF","notion"),
  entry("figma","Figma","Productividad","F24E1E","FFFFFF","figma"),
  entry("zoom","Zoom Pro","Productividad","0B5CFF","FFFFFF","zoom pro;zoom workplace;=zoom"),
  entry("grammarly","Grammarly","Productividad","027E6F","FFFFFF","grammarly"),
  entry("github","GitHub Pro","Productividad","181717","FFFFFF","github pro;github team"),
  entry("linear","Linear","Productividad","5E6AD2","FFFFFF","linear app;linear business;=linear"),
  entry("todoist","Todoist","Productividad","E44332","FFFFFF","todoist"),
  entry("evernote","Evernote","Productividad","00A82D","FFFFFF","evernote"),
  entry("googleone","Google One","Almacenamiento","000000","FFFFFF","google one;googleone"),
  entry("icloud","iCloud+","Almacenamiento","3693F3","FFFFFF","icloud;icloud plus"),
  entry("dropbox","Dropbox","Almacenamiento","0061FF","FFFFFF","dropbox"),
  entry("protondrive","Proton Drive","Almacenamiento","EB508D","FFFFFF","proton drive;protondrive"),
  entry("mega","MEGA","Almacenamiento","D9272E","FFFFFF","mega cloud;mega storage;=mega"),
  entry("duolingo","Super Duolingo","Aprendizaje","58CC02","FFFFFF","duolingo;super duolingo"),
  entry("coursera","Coursera Plus","Aprendizaje","0056D2","FFFFFF","coursera"),
  entry("udemy","Udemy Personal","Aprendizaje","A435F0","FFFFFF","udemy"),
  entry("nordvpn","NordVPN","Seguridad y privacidad","4687FF","FFFFFF","nordvpn;nord vpn"),
  entry("surfshark","Surfshark","Seguridad y privacidad","1EBFBF","FFFFFF","surfshark"),
  entry("protonvpn","Proton VPN","Seguridad y privacidad","66DEB1","FFFFFF","proton vpn;protonvpn"),
  entry("1password","1Password","Seguridad y privacidad","145FE4","FFFFFF","1password"),
  entry("bitwarden","Bitwarden","Seguridad y privacidad","175DDC","FFFFFF","bitwarden"),
  entry("discord","Discord Nitro","Comunidad","5865F2","FFFFFF","discord nitro;nitro discord"),
  entry("twitch","Twitch","Comunidad","9146FF","FFFFFF","twitch"),
  entry("patreon","Patreon","Comunidad","000000","FFFFFF","patreon"),
  entry("strava","Strava","Deporte","FC4C02","FFFFFF","strava"),
  entry("uberone","Uber One","Membresías","000000","FFFFFF","uber one;uberone"),
  entry("appleone","Apple One","Membresías","000000","FFFFFF","apple one;appleone"),
  entry("dazn","DAZN","Deporte","F8F8F5","151E30","dazn")
  ));
 private static final Map<String,Entry> IDS;
 private static final List<String> GROUPS;
 static {Map<String,Entry> ids=new LinkedHashMap<>();Set<String> groups=new LinkedHashSet<>();for(Entry e:ENTRIES){if(ids.put(e.id,e)!=null)throw new IllegalStateException("Duplicate service id");groups.add(e.group);}IDS=Collections.unmodifiableMap(ids);GROUPS=Collections.unmodifiableList(new ArrayList<>(groups));}
 static String normalize(String text){return BankNotificationParser.normalize(text);}
 static List<Entry> all(){return ENTRIES;}
 static List<String> groups(){return GROUPS;}
 static Entry byId(String id){return IDS.get(id==null?"":id);}
 static List<Entry> search(String query,String group){String q=normalize(query);List<Entry> result=new ArrayList<>();for(Entry e:ENTRIES){if(group!=null&&!group.isEmpty()&&!group.equals("Todos")&&!e.group.equals(group))continue;boolean found=q.isEmpty()||e.searchText.contains(q);if(!found)for(String alias:e.aliases)if(alias.replace("=","").contains(q)){found=true;break;}if(found)result.add(e);}return result;}
 static final class Hit {final Entry entry;final int start,end;Hit(Entry e,int start,int end){entry=e;this.start=start;this.end=end;}}
 /** Exact generic names are accepted; embedded generic words such as "max" are not. */
 static Entry identify(String raw){Entry id=byId(raw);if(id!=null)return id;String n=normalize(raw);if(n.isEmpty())return null;List<Hit> hits=new ArrayList<>();String padded=" "+n+" ";for(Entry e:ENTRIES){if(n.equals(e.normalizedName))hits.add(new Hit(e,0,n.length()));for(String alias:e.aliases){boolean exact=alias.startsWith("=");String word=exact?alias.substring(1):alias;if(exact){if(n.equals(word))hits.add(new Hit(e,0,n.length()));continue;}int from=0;while(from<padded.length()){int at=padded.indexOf(" "+word+" ",from);if(at<0)break;hits.add(new Hit(e,at,at+word.length()));from=at+word.length()+1;}}}if(hits.isEmpty())return null;Hit best=hits.get(0);for(Hit hit:hits)if(hit.end-hit.start>best.end-best.start)best=hit;for(Hit hit:hits)if(hit.entry!=best.entry&&(hit.end<best.start||hit.start>best.end))return null;return best.entry;}
}

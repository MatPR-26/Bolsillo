package cl.bolsillo.app;
import org.json.*;import java.util.*;
/** Additive catalog upgrade: existing IDs, names, budgets and movements remain intact. */
final class CategoryCatalog {
 static final String[][] EXTRA={
  {"expense","gifts","Regalos","regalo|regalos"},
  {"expense","pets","Mascotas","mascota|mascotas"},
  {"expense","travel","Viajes","viaje|viajes"},
  {"expense","sport","Deporte","deporte|deportes|gimnasio"},
  {"expense","care","Cuidado personal","cuidado personal|belleza"},
  {"expense","tech","Tecnología","tecnologia"},
  {"expense","services","Servicios","servicio|servicios|servicios basicos"},
  {"expense","donations","Donaciones","donacion|donaciones"},
  {"expense","tax","Impuestos","impuesto|impuestos"},
  {"income","refunds","Reembolsos","reembolso|reembolsos"}
 };
 static String normalized(String name){return java.text.Normalizer.normalize(name,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();}
 static void migrate(JSONObject data)throws Exception{
  if(data.optInt("categoryCatalogVersion")>=14)return;JSONArray categories=data.getJSONArray("categories");
  for(String[] entry:EXTRA){boolean exists=false;for(int i=0;i<categories.length();i++){JSONObject category=categories.getJSONObject(i);if(!entry[0].equals(category.optString("type")))continue;String name=normalized(category.optString("name"));for(String alias:entry[3].split("\\|"))if(name.equals(alias)){exists=true;break;}if(exists)break;}
   if(!exists&&categories.length()<300){String id=entry[0]+"-catalog-"+entry[1];if(Ledger.find(categories,id)==null)categories.put(new JSONObject().put("id",id).put("name",entry[2]).put("type",entry[0]));}
  }data.put("categoryCatalogVersion",14);
 }
}

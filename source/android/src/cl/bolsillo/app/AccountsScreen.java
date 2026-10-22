package cl.bolsillo.app;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Account views share the ledger; selecting a tab never creates another balance. */
final class AccountsScreen {
 static int section(JSONObject account){String type=account.optString("type");return type.equals("savings")||type.equals("investment")?1:type.equals("credit")?2:0;}
 static void show(MainActivity a)throws Exception{
  JSONArray accounts=a.data.getJSONArray("accounts"),cards=PaymentCards.list(a);Map<String,Long> balances=Ledger.balances(a.data,Ledger.today());boolean credit=false;
  for(int i=0;i<accounts.length();i++)if(!accounts.getJSONObject(i).optBoolean("archived")&&section(accounts.getJSONObject(i))==2)credit=true;
  final int pages=credit?3:2;a.accountsPage=Math.max(0,Math.min(pages-1,a.accountsPage));String[] labels={"Disponibles","Ahorro","Crédito"};LinearLayout tabs=Modern.row(a);
  for(int i=0;i<pages;i++){final int page=i;TextView tab=Glass.pill(a,labels[i],a.accountsPage==i);tabs.addView(tab,new LinearLayout.LayoutParams(0,-2,1));Modern.press(tab,()->{a.accountsPage=page;a.render(false);});}Modern.add(a.body,tabs,0);
  a.swipePages(()->{if(a.accountsPage<pages-1){a.accountsPage++;a.render();}},()->{if(a.accountsPage>0){a.accountsPage--;a.render();}});
  long total=0;int visible=0;Set<String> active=new HashSet<>();for(int i=0;i<accounts.length();i++){JSONObject ac=accounts.getJSONObject(i);if(ac.optBoolean("archived")||section(ac)!=a.accountsPage)continue;visible++;total=Math.addExact(total,balances.get(ac.optString("id")));active.add(ac.optString("id"));}
  LinearLayout summary=Modern.row(a);summary.setPadding(a.dp(6),a.dp(16),a.dp(6),0);summary.addView(Modern.text(a,"Saldo registrado",12,Glass.subtle(a),false),new LinearLayout.LayoutParams(0,-2,1));TextView sum=Modern.text(a,a.money(total),23,Glass.ink(a),false);sum.setTypeface(Glass.font(a,500));sum.setGravity(Gravity.END);Modern.fitAmount(sum,12,23);summary.addView(sum,new LinearLayout.LayoutParams(0,-2,1));Modern.add(a.body,summary,0);
  Modern.section(a,"Tus cuentas","Añadir",()->BankTemplates.show(a,false,null));
  for(int i=0;i<accounts.length();i++){
   JSONObject account=accounts.getJSONObject(i);if(!active.contains(account.optString("id")))continue;int linked=0;for(int j=0;j<cards.length();j++)if(cards.getJSONObject(j).optString("account").equals(account.optString("id")))linked++;
   String subtitle=linked>1?linked+" tarjetas · un saldo":linked==1?"1 tarjeta":Forms.accountType(account.optString("type"));if(!account.optBoolean("included",true))subtitle+=" · Fuera del patrimonio";
   Modern.add(a.body,card(a,account,balances.get(account.optString("id")),linked,subtitle),12);
  }
  if(visible==0)Modern.empty(a,a.accountsPage==1?"savings":"bank",a.accountsPage==1?"Tu espacio de ahorro":"Sin cuentas en esta vista","", "Añadir cuenta",()->Forms.account(a,null));
  int linked=0;for(int i=0;i<cards.length();i++)if(active.contains(cards.getJSONObject(i).optString("account")))linked++;
  if(linked>0){Modern.section(a,"Tus tarjetas","Gestionar",()->{a.autoTab="Tarjetas";a.navigate("Automático");});for(int i=0;i<cards.length();i++){JSONObject card=cards.getJSONObject(i);if(!active.contains(card.optString("account")))continue;String suffix=card.optString("suffixes");String subtitle=a.name("accounts",card.optString("account"))+" · "+(suffix.isEmpty()?"Saldo compartido":"•• "+suffix);Modern.add(a.body,Modern.item(a,"card",card.optString("name"),subtitle,card.optString("kind").equals("digital")?"Digital":"Física",Glass.subtle(a),()->a.attempt(()->AutomationScreen.editCard(a,card))),6);}}
  Modern.section(a,"Más opciones","",()->{});
  Modern.add(a.body,Modern.item(a,"plus","Otra cuenta","Efectivo, ahorro o una institución diferente","",Glass.accent(a),()->a.attempt(()->Forms.account(a,null))),6);
  List<String> archivedNames=new ArrayList<>();List<JSONObject> archived=new ArrayList<>();for(int i=0;i<accounts.length();i++){JSONObject account=accounts.getJSONObject(i);if(account.optBoolean("archived")){archived.add(account);archivedNames.add(account.optString("name"));}}
  if(!archived.isEmpty())Modern.add(a.body,Modern.item(a,"history","Cuentas archivadas",archived.size()+" cuentas","",Glass.subtle(a),()->NativeSettings.choose(a,"Cuentas archivadas",archivedNames,n->a.attempt(()->Forms.account(a,archived.get(n))))),8);
 }
 private static View card(MainActivity a,JSONObject account,long balance,int linked,String description){
  String icon=a.accountIcon(account),name=account.optString("name");int tint=Glass.secondary(a);
  LinearLayout card=Modern.column(a);card.setPadding(a.dp(20),a.dp(20),a.dp(20),a.dp(18));AccountGlass material=new AccountGlass(a,tint);card.setBackground(material);BrandPalette.bind(a,icon,material);card.setElevation(a.dp(2));if(android.os.Build.VERSION.SDK_INT>=28){card.setOutlineAmbientShadowColor(Glass.alpha(Glass.canvas(a),28));card.setOutlineSpotShadowColor(Glass.alpha(Glass.canvas(a),48));}
  LinearLayout heading=Modern.row(a);heading.addView(Icons.badge(a,icon,48),new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));LinearLayout title=Modern.column(a);title.setPadding(a.dp(12),0,a.dp(8),0);TextView label=Modern.text(a,name,18,Glass.ink(a),true);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);title.addView(label);title.addView(Modern.text(a,Forms.accountType(account.optString("type")),12,Glass.subtle(a),false));heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));card.addView(heading);
  TextView value=Modern.text(a,a.money(balance),36,Glass.ink(a),false);value.setTypeface(Glass.font(a,500));Modern.fitAmount(value,16,36);Modern.add(card,value,20);
  String detail=linked>0?linked+(linked==1?" tarjeta":" tarjetas")+" · un saldo":section(account)==1?"Reservado para tus metas":"Saldo de la cuenta";if(!account.optBoolean("included",true))detail+=" · Fuera del patrimonio";
  LinearLayout foot=Modern.row(a);foot.addView(Icons.view(a,section(account)==1?"savings":linked>0?"card":"wallet",Glass.subtle(a),15),new LinearLayout.LayoutParams(a.dp(15),a.dp(15)));TextView hint=Modern.text(a,detail,12,Glass.subtle(a),false);hint.setPadding(a.dp(7),0,0,0);foot.addView(hint,new LinearLayout.LayoutParams(0,-2,1));Modern.add(card,foot,10);
  card.setTag("account-glass-pane");card.setContentDescription(name+". "+description+". "+a.money(balance));Modern.press(card,()->a.attempt(()->Forms.accountMenu(a,account)));CardTouch.apply(card);return card;
 }
}

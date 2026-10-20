package cl.bolsillo.app;
import java.text.Normalizer;import java.util.regex.*;
/** Conservative CLP parser. Raw notification text is not persisted. */
public final class BankNotificationParser {
 public static final class Candidate{public final long amount;public final String merchant,last4,type,reference,reason;public final boolean review;Candidate(long a,String m,String card){this(a,m,card,"expense",false);}Candidate(long a,String m,String card,String kind,boolean held){this(a,m,card,kind,held,"","");}Candidate(long a,String m,String card,String kind,boolean held,String ref,String why){amount=a;merchant=m;last4=card;type=kind;review=held;reference=ref;reason=why;}Candidate(Candidate p,boolean held,String why){this(p.amount,p.merchant,p.last4,p.type,held,p.reference,why.isEmpty()?p.reason:why);}}
 public static String normalize(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();}
 public static Candidate parse(String input){return parse(input,"",false);}
 public static Candidate parse(String input,String title,boolean wallet){return movement(input,title,wallet,false,"");}
 static boolean has(String text,String pattern){return Pattern.compile(pattern).matcher(text).find();}
 /** Accept whole pesos and decimal suffixes only when their value is exactly zero. */
 public static Long value(String digits){try{String s=digits.trim().replace('\u00a0',' ').replace('\u202f',' ');if(s.matches(".*[.,][0-9]{2}$")){char decimal=s.charAt(s.length()-3);if(!s.endsWith("00"))return null;String integer=s.substring(0,s.length()-3);if(integer.indexOf(decimal)>=0)return null;s=integer;}if(!s.matches("[0-9]+")&&!s.matches("[0-9]{1,3}(?:\\.[0-9]{3})+")&&!s.matches("[0-9]{1,3}(?:,[0-9]{3})+")&&!s.matches("[0-9]{1,3}(?: [0-9]{3})+"))return null;long n=Long.parseLong(s.replaceAll("[., ]",""));return n>0&&n<=1000000000000L?n:null;}catch(Exception e){return null;}}
 public static Candidate movement(String input,String title,boolean wallet,boolean bank,String forced){
  if(input==null||input.length()>5000)return null;String full=normalize(input);input=AlertLanguage.core(input);String s=normalize(input);
  if(AlertLanguage.unsafe(full)||AlertLanguage.request(s)||input.contains("US$")||input.contains("¿"))return null;
  boolean incoming=AlertLanguage.incoming(s),outgoing=AlertLanguage.outgoing(s),purchase=AlertLanguage.purchase(s),debit=AlertLanguage.debit(s);
  if(!incoming&&!outgoing&&!purchase)purchase=has(s,"\\bcompra\\b");
  String type=incoming?"income":purchase||outgoing||debit?"expense":"";boolean review=outgoing||debit&&!purchase;
  if(incoming&&(purchase||outgoing))return null;
  if(type.isEmpty()&&!forced.isEmpty()){type=forced;review=true;}
  if(type.isEmpty()&&wallet&&!has(s,"\\b(saldo|balance|cashback|disponible|recarga|transferencia)\\b"))type="expense";
  if(type.isEmpty()&&!bank)return null;if(type.isEmpty()){type="unknown";review=true;}
  String number="([0-9](?:[0-9., \\u00a0\\u202f]*[0-9])?)(?![0-9]|[.,/:-][0-9])";
  Matcher currency=Pattern.compile("(?:clp\\s*\\$?|\\$)\\s*"+number+"|"+number+"\\s*(?:pesos|clp)\\b",Pattern.CASE_INSENSITIVE).matcher(input);
  Long amount=null;int amounts=0;boolean ambiguous=false;while(currency.find()){String before=normalize(input.substring(Math.max(0,currency.start()-45),currency.start()));if(before.matches("(?s).*\\b(saldo|balance|disponible|ahorro|cashback)\\b(?:\\s+\\w+){0,3}$"))continue;String digits=currency.group(1)==null?currency.group(2):currency.group(1);Long found=value(digits);if(digits.trim().matches("0(?:[.,]00)?"))return null;amounts++;if(found==null)ambiguous=true;else amount=found;}
  if(amounts==0&&bank){Matcher raw=Pattern.compile("(?:\\bpor\\s+(?:un\\s+(?:monto|valor)\\s+de\\s+)?|\\bmonto\\s*[:=]?\\s*|\\bde\\s+)"+number,Pattern.CASE_INSENSITIVE).matcher(input);while(raw.find()){amounts++;Long found=value(raw.group(1));if(found==null)ambiguous=true;else amount=found;}}
  if(amounts==0)return null;if(amounts>1)ambiguous=true;String reason=ambiguous?amounts>1?"Hay varios importes: elige el monto del movimiento":"Formato de monto ambiguo: escribe pesos enteros":"";if(ambiguous){amount=0L;review=true;}if(amount==null)return null;if(has(s,"\\b(?:ahorro|inversion|rentabilidad|rendimientos?|intereses)\\b")){review=true;if(reason.isEmpty())reason="Aviso de ahorro o inversión: confirma la cuenta y si fue un abono real";}
  Matcher merchant=Pattern.compile("(?:\\ben\\s+(?!tu\\b|su\\b)|\\bat\\s+|comercio\\s*:\\s*|establecimiento\\s*:\\s*)([^\\n$]{2,90})",Pattern.CASE_INSENSITIVE).matcher(input);String name=merchant.find()?merchant.group(1).replaceAll("(?i)(?:\\s+(?:por|con|el|a las|using|tarjeta)\\s+|[.,;·]?\\s+(?:saldo|balance|fecha|disponible|referencia|comprobante|folio|operaci[oó]n|transacci[oó]n)\\b).*$","").replaceAll("[.,;· ]+$","").trim():"";
  if(incoming){Matcher sender=Pattern.compile("(?:\\bde\\s+|remitente\\s*:\\s*)([\\p{L}][\\p{L} .]{2,55})(?=\\s+por\\b|\\s+el\\b|\\n|$)",Pattern.CASE_INSENSITIVE).matcher(input);name=sender.find()?"Transferencia de "+sender.group(1).trim():"Transferencia recibida";}
  if(name.isEmpty()&&wallet){String t=title.replaceAll("(?:CLP\\s*)?\\$[0-9., ]+","").trim();if(!normalize(t).matches(".*\\b(google|wallet|compra|pago|pagaste|tarjeta|notificacion)\\b.*")&&t.length()>=2)name=t;}
  if(name.isEmpty())name=incoming?"Ingreso detectado":purchase?"Compra detectada":outgoing?"Transferencia o retiro":"Movimiento por revisar";if(name.length()>100)name=name.substring(0,100);
  Matcher card=Pattern.compile("(?:[*•●xX]{2,}\\s*|(?:terminada?|finaliza(?:da)?|ending|ultimos|últimos)(?:\\s+(?:en|with|dígitos|digitos))?\\s*[:.]?\\s*)([0-9]{4})(?![0-9])",Pattern.CASE_INSENSITIVE).matcher(input);
  Matcher reference=Pattern.compile("(?:comprobante|operaci[oó]n|transacci[oó]n|referencia|folio|transaction(?:\\s+id)?|reference)(?:\\s+(?:n[uú]mero|n[°ºo]|id))?\\s*[:#=-]?\\s*([A-Za-z0-9-]{6,40})(?![A-Za-z0-9-])",Pattern.CASE_INSENSITIVE).matcher(input);String ref=reference.find()?reference.group(1).toLowerCase(java.util.Locale.ROOT):"";if(!ref.matches(".*[0-9].*"))ref="";
  return new Candidate(amount,name,card.find()?card.group(1):"",type,review,ref,reason);
 }
 public static int duplicate(long amount,String account,String merchant,long at,String source,long oldAmount,String oldAccount,String oldMerchant,long oldAt,String oldSource){if(amount!=oldAmount||!account.equals(oldAccount)||Math.abs(at-oldAt)>600000)return 0;if(source.equals(oldSource))return 0;String a=normalize(merchant),b=normalize(oldMerchant);boolean named=!a.equals("compra detectada")&&!b.equals("compra detectada")&&!a.isEmpty();return named&&a.equals(b)&&Math.abs(at-oldAt)<=120000?2:1;}
 public static boolean safeDuplicate(String reference,String oldReference,long amount,long oldAmount,String account,String oldAccount,String type,String oldType,long at,long oldAt){return !reference.isEmpty()&&reference.equals(oldReference)&&amount>0&&amount==oldAmount&&!account.isEmpty()&&account.equals(oldAccount)&&type.equals(oldType)&&Math.abs(at-oldAt)<=86400000L;}
}

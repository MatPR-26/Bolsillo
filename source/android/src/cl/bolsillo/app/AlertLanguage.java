package cl.bolsillo.app;

import java.util.regex.Pattern;

/** Included language patterns, not claimed to be official bank message templates. */
public final class AlertLanguage {
 public static final String[] INCOME={
  "has recibido", "recibiste", "te transfirieron", "te transferimos", "te depositaron",
  "depositaron en tu", "hemos recibido", "hemos abonado", "se abono", "se ha abonado",
  "se acredito", "se ha acreditado", "dinero recibido", "pago recibido", "sueldo abonado",
  "transferencia recibida", "transferencia entrante", "abono recibido", "abono realizado",
  "deposito recibido", "deposito realizado", "deposito exitoso", "received", "credited"
 };
 public static final String[] PURCHASE={
  "compra aprobada", "compra realizada", "compra exitosa", "compra con tu tarjeta",
  "compraste", "has comprado", "pagaste", "has pagado", "pago realizado", "pago exitoso",
  "pago aprobado", "pago efectuado", "pago de", "pago con", "pagado", "cargo por compra",
  "utilizaste tu tarjeta", "usaste tu tarjeta", "purchase", "paid", "spent"
 };
 public static final String[] OUTGOING={
  "transferiste", "enviaste", "has enviado", "transferencia enviada", "transferencia realizada",
  "transferencia exitosa", "transferencia efectuada", "dinero enviado", "pago enviado",
  "retiro realizado", "retiro exitoso", "retiraste", "giro realizado", "giraste", "sent"
 };
 public static final String[] DEBIT={
  "cargo realizado", "cargo efectuado", "cobro realizado", "cobro efectuado", "debito realizado",
  "debito efectuado", "se desconto", "se ha descontado", "cargo", "cobro", "debito", "debitado"
 };
 private static Pattern phrases(String[] values){return Pattern.compile("\\b(?:"+String.join("|",values)+")\\b");}
 private static final Pattern IN=phrases(INCOME),BUY=phrases(PURCHASE),OUT=phrases(OUTGOING),DEB=phrases(DEBIT);
 private static final Pattern CREDIT=Pattern.compile("\\b(?:abono|deposito|transferencia|pago|saldo|cuenta)\\b.{0,45}\\b(?:recibid[ao]|acreditad[ao]|a tu favor|abonad[ao])\\b|\\b(?:recibid[ao]|acreditad[ao]|abonad[ao])\\b.{0,30}\\b(?:cuenta|transferencia|deposito|pago)\\b");
 private static final Pattern COMPLETE_BUY=Pattern.compile("\\b(?:se realizo|se ha realizado|realizaste|realizada|efectuaste)\\s+(?:una?\\s+)?(?:compra|pago)\\b");
 private static final Pattern UNSAFE=Pattern.compile("\\b(?:clave|codigo|otp|rechazad[ao]|fallid[ao]|anulad[ao]|revers[ao]|devolucion|reembolso|intento|declined|refunded|refund|failed|verification)\\b|\\b(?:no se|no has|no fue|no recibiste|no realizado|no pudimos|no pudiste|no pudo)\\b");
 private static final Pattern CURRENCY=Pattern.compile("\\b(?:usd|dolar|dolares|uf|eur|euro|euros)\\b");
 private static final Pattern REQUEST=Pattern.compile("\\b(?:autoriza|autorizar|autorizacion|solicitud|confirma|confirmar|pendiente|procesando|programad[ao]|recibiras|recibira|recibir)\\b");
 private static final Pattern PROMO=Pattern.compile("^(?:.*?\\b(?:promocion|oferta|ganate)\\b|(?:compra con descuento|compra desde|recibe|gana|puedes|podras|hasta)\\b)");
 public static boolean incoming(String s){return IN.matcher(s).find()||CREDIT.matcher(s).find();}
 public static boolean purchase(String s){return BUY.matcher(s).find()||COMPLETE_BUY.matcher(s).find();}
 public static boolean outgoing(String s){return OUT.matcher(s).find();}
 public static boolean debit(String s){return DEB.matcher(s).find();}
 public static boolean unsafe(String s){return UNSAFE.matcher(s).find()||CURRENCY.matcher(s).find();}
 public static boolean request(String s){return REQUEST.matcher(s).find()||PROMO.matcher(s).find();}
 public static String core(String raw){return raw.split("(?i)(?:\\b(?:ingresa|consulta|revisa|puedes|podras)\\b|si no reconoces|para m[aá]s informaci[oó]n)",2)[0];}
 public static int count(){return INCOME.length+PURCHASE.length+OUTGOING.length+DEBIT.length;}
}

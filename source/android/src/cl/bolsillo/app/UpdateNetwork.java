package cl.bolsillo.app;
import java.net.*;import java.io.*;import javax.net.ssl.HttpsURLConnection;
/** Bounded HTTPS operations, no authentication headers, cookies or financial data. */
final class UpdateNetwork {
 static HttpsURLConnection open(URL url,boolean api)throws Exception{
  for(int redirects=0;redirects<5;redirects++){
   Ledger.require(api?url.getProtocol().equals("https")&&url.getHost().equals("api.github.com")&&url.getUserInfo()==null&&(url.getPort()==-1||url.getPort()==443):ReleaseFeed.downloadHost(url),"Servidor de actualización no permitido");HttpsURLConnection connection=(HttpsURLConnection)url.openConnection();connection.setConnectTimeout(15000);connection.setReadTimeout(20000);connection.setInstanceFollowRedirects(false);connection.setRequestProperty("User-Agent","Bolsillo-Android-Updater");connection.setRequestProperty("Accept",api?"application/vnd.github+json":"application/octet-stream");if(api)connection.setRequestProperty("X-GitHub-Api-Version","2022-11-28");int status=connection.getResponseCode();
   if(status>=300&&status<400){String next=connection.getHeaderField("Location");connection.disconnect();Ledger.require(next!=null,"Redirección sin destino");url=new URL(url,next);continue;}
   if(status!=200){connection.disconnect();throw new IOException(status==404?"Todavía no hay una versión estable publicada":status==403||status==429?"GitHub limitó las consultas; prueba más tarde":"No se pudo consultar GitHub ("+status+")");}return connection;
  }throw new IOException("Demasiadas redirecciones de actualización");
 }
 static String metadata(String repo)throws Exception{Ledger.require(ReleaseFeed.repository(repo),"Repositorio no configurado");HttpsURLConnection connection=open(new URL("https://api.github.com/repos/"+repo+"/releases/latest"),true);try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("Respuesta de actualización demasiado grande");out.write(buffer,0,n);}return new String(out.toByteArray(),"UTF-8");}finally{connection.disconnect();}}
}

package cl.bolsillo.app;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;

/** Image-derived colours, calculated once per resource away from the UI thread. */
final class BrandPalette {
 static final class Seeds {final int main,accent;Seeds(int main,int accent){this.main=main;this.accent=accent;}}
 static final ConcurrentHashMap<Integer,CompletableFuture<Seeds>> cache=new ConcurrentHashMap<>();
 static final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Bolsillo-brand-colours");t.setDaemon(true);return t;});
 static final Handler main=new Handler(Looper.getMainLooper());
 static CompletableFuture<Seeds> request(Context c,int resource){Context app=c.getApplicationContext();return cache.computeIfAbsent(resource,key->CompletableFuture.supplyAsync(()->extract(app,key),worker));}
 static void warmBanks(Context c){for(String[] bank:BankCatalog.BANKS){int resource=Icons.brandResource(bank[0]);if(resource!=0)request(c,resource);}}
 static void bind(Context c,String brand,AccountGlass glass){int resource=Icons.brandResource(brand);if(resource==0)return;WeakReference<AccountGlass> target=new WeakReference<>(glass);request(c,resource).thenAccept(seeds->main.post(()->{AccountGlass pane=target.get();if(pane!=null&&seeds!=null)pane.palette(seeds.main,seeds.accent);}));}
 static Seeds extract(Context c,int resource){
  Bitmap bitmap=Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888);
  try{Drawable image=c.getDrawable(resource);if(image==null)return null;image.setBounds(0,0,48,48);image.draw(new Canvas(bitmap));int[] pixels=new int[48*48];bitmap.getPixels(pixels,0,48,0,0,48,48);return analyse(pixels);}catch(Exception ignored){return null;}finally{bitmap.recycle();}
 }
 static Seeds analyse(int[] pixels){
  double[] weight=new double[36];double[][] sum=new double[36][3];float[] hsv=new float[3];double grey=0,greyWeight=0;
  for(int color:pixels){float a=Color.alpha(color)/255f;if(a<.5f)continue;Color.colorToHSV(color,hsv);grey+=hsv[2]*a;greyWeight+=a;if(hsv[1]<.16f||hsv[2]<.12f||hsv[2]>.97f&&hsv[1]<.3f)continue;int bucket=Math.min(35,(int)(hsv[0]/10));double score=a*(.3+.7*hsv[1])*(.3+.7*hsv[2]);weight[bucket]+=score;sum[bucket][0]+=Color.red(color)*score;sum[bucket][1]+=Color.green(color)*score;sum[bucket][2]+=Color.blue(color)*score;}
  int best=-1;for(int i=0;i<36;i++)if(weight[i]>0&&(best<0||weight[i]>weight[best]))best=i;
  if(best<0){int neutral=(int)(255*(greyWeight==0?.5:grey/greyWeight));return new Seeds(Color.rgb(neutral,neutral,neutral),Color.rgb(neutral,neutral,neutral));}
  int primary=average(sum[best],weight[best]),second=-1;double secondaryScore=0;
  for(int i=0;i<36;i++){int distance=Math.min(Math.abs(i-best),36-Math.abs(i-best));double score=weight[i]*Math.min(1,distance/9d);if(distance>=4&&weight[i]>=weight[best]*.12&&score>secondaryScore){second=i;secondaryScore=score;}}
  int accent;if(second>=0)accent=average(sum[second],weight[second]);else{Color.colorToHSV(primary,hsv);hsv[0]=(hsv[0]+18)%360;hsv[1]*=.72f;accent=Color.HSVToColor(hsv);}return new Seeds(primary,accent);
 }
 private static int average(double[] values,double weight){return Color.rgb((int)Math.round(values[0]/weight),(int)Math.round(values[1]/weight),(int)Math.round(values[2]/weight));}
}

package cl.bolsillo.app;
import android.graphics.Color;
/** Seed colours are adapted to readable roles; raw logo colours never become body text. */
final class ThemeColors {
 static final android.util.LruCache<Long,Integer> roles=new android.util.LruCache<>(64);
 static double channel(int n){double v=n/255d;return v<=.04045?v/12.92:Math.pow((v+.055)/1.055,2.4);}
 static double luminance(int color){return .2126*channel(Color.red(color))+.7152*channel(Color.green(color))+.0722*channel(Color.blue(color));}
 static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
 static int role(int seed,boolean dark){long key=(seed&0xffffffffL)|(dark?1L<<32:0);Integer cached=roles.get(key);if(cached!=null)return cached;int target=dark?Color.WHITE:Color.BLACK,surface=dark?0xff151e30:Color.WHITE,label=dark?0xff101827:Color.WHITE,result=target;for(int i=0;i<=100;i++){int color=Glass.mix(seed,target,i/100f);if(contrast(color,surface)>=(dark?5.5:7)&&contrast(color,label)>=(dark?5.5:7)){result=color;break;}}roles.put(key,result);return result;}
}

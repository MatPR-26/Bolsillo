package cl.bolsillo.app;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Logo extraction, actual compositing, colour personalisation and gesture cancellation. */
final class BrandDesignChecks {
 static int count;static void yes(boolean ok,String why)throws Exception{count++;if(!ok)throw new Exception(why);}
 static int run(Instrumentation test,Context c)throws Exception{
  count=0;yes(Phase4Checks.shell(test,"getprop ro.kernel.qemu").equals("1"),"dedicated synthetic emulator only");
  NativeSync.EXEC.submit(()->{}).get(15,TimeUnit.SECONDS);
  Map<String,Object> secure=VisualChecks.preferences(c.getSharedPreferences("secure",0)),settings=VisualChecks.preferences(NativeStore.settings(c)),updates=VisualChecks.preferences(AppUpdates.prefs(c));MainActivity[] host={null};SheetDialog[] sheet={null};
  try{
   int[] pixels=new int[240];Arrays.fill(pixels,Color.WHITE);Arrays.fill(pixels,0,100,0xffd52632);Arrays.fill(pixels,100,160,0xff176bce);BrandPalette.Seeds synthetic=BrandPalette.analyse(pixels);yes(Color.red(synthetic.main)>Color.blue(synthetic.main)*2,"population and chroma retain a red logo instead of its white padding");yes(Color.blue(synthetic.accent)>Color.red(synthetic.accent)*2,"second distinct hue is derived from image pixels");
   Set<Integer> colours=new HashSet<>();for(String[] bank:BankCatalog.BANKS){int res=Icons.brandResource(bank[0]);CompletableFuture<BrandPalette.Seeds> future=BrandPalette.request(c,res);BrandPalette.Seeds seeds=future.get(20,TimeUnit.SECONDS);yes(seeds!=null,"real bundled logo produces colours: "+bank[0]);yes(BrandPalette.request(c,res)==future,"each logo calculation is cached: "+bank[0]);colours.add(seeds.main);}yes(colours.size()>=6,"institutions produce distinct palettes without per-bank colour templates");
   LocalData.restore(c,VisualChecks.fixture());NativeStore.settings(c).edit().putBoolean("capture",false).putBoolean("privateScreen",false).putBoolean("alertChanges",false).putBoolean("detectionTutorialSeen",true).putInt("glassIntensity",1).commit();AppUpdates.prefs(c).edit().putLong("attempt",System.currentTimeMillis()).commit();String financial=LocalData.read(c).toString();
   for(String mode:new String[]{"dark","light"}){
    NativeStore.settings(c).edit().putString("appearance",mode).putString("palette","custom").putInt("customPrimary",0xffff9400).putInt("customSecondary",0xff6236c7).commit();
    host[0]=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));MainActivity a=host[0];V16Checks.loaded(test,a);VisualChecks.main(test,()->a.navigate("Cuentas"));V15Checks.pause(test,350);
    for(int raw:new int[]{Color.BLACK,Color.WHITE,0xffff0000,0xff00ff00,0xff0000ff,0xffffee00,0xffff00ff,0xff00ffff,0xff777777}){int role=ThemeColors.role(raw,mode.equals("dark"));yes(ThemeColors.contrast(role,Glass.panel(a))>=4.5,"custom accent adapts to "+mode+" surfaces");yes(ThemeColors.contrast(role,Glass.onPrimary(a))>=4.5,"custom button text remains legible in "+mode);}
    Bitmap surface=Bitmap.createBitmap(360,220,Bitmap.Config.ARGB_8888);try{for(String[] bank:BankCatalog.BANKS){BrandPalette.Seeds seeds=BrandPalette.request(c,Icons.brandResource(bank[0])).get();AccountGlass pane=new AccountGlass(a,Glass.secondary(a));VisualChecks.main(test,()->{surface.eraseColor(Glass.canvas(a));Canvas canvas=new Canvas(surface);pane.palette(seeds.main,seeds.accent);pane.setBounds(0,0,360,220);pane.draw(canvas);});double worst=100;for(int y=50;y<180;y+=20)for(int x=25;x<330;x+=25)worst=Math.min(worst,ThemeColors.contrast(Glass.ink(a),surface.getPixel(x,y)));yes(worst>=4.5,"actual logo glass retains text contrast: "+mode+"/"+bank[0]+" "+worst);}}finally{surface.recycle();}
    List<View> cards=new ArrayList<>();for(View v:VisualChecks.views(a.body,View.class))if("account-glass-pane".equals(v.getTag()))cards.add(v);yes(cards.size()>=2,"accounts keep broad bank panels");View card=cards.get(0);VisualChecks.main(test,()->{long now=android.os.SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,card.getWidth()*.2f,card.getHeight()*.2f,0);card.dispatchTouchEvent(event);event.recycle();});V15Checks.pause(test,150);
    if(android.animation.ValueAnimator.areAnimatorsEnabled())yes(Math.abs(card.getRotationX())+Math.abs(card.getRotationY())>.1f,"intentional card touch responds with subtle perspective");
    VisualChecks.main(test,()->{long now=android.os.SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(now,now,MotionEvent.ACTION_CANCEL,0,0,0);card.dispatchTouchEvent(event);event.recycle();});V15Checks.pause(test,250);yes(card.getRotationX()==0&&card.getRotationY()==0,"cancelled swipe releases all card perspective");UiSmoke.shot(test,c,"brand-accounts-"+mode);
    VisualChecks.main(test,()->{a.accountsPage=1;a.render(false);});V15Checks.pause(test,250);UiSmoke.shot(test,c,"brand-savings-"+mode);
    String before=NativeStore.settings(c).getString("palette","");int primary=NativeStore.settings(c).getInt("customPrimary",0);VisualChecks.main(test,()->sheet[0]=CustomPalette.show(a));V15Checks.pause(test,250);VisualChecks.main(test,()->{Phase4Checks.button(sheet[0].footer,"Cancelar").performClick();sheet[0]=null;});yes(NativeStore.settings(c).getString("palette","").equals(before)&&NativeStore.settings(c).getInt("customPrimary",0)==primary,"cancelled colour preview preserves the selected theme");
    VisualChecks.main(test,()->sheet[0]=CustomPalette.show(a));V15Checks.pause(test,250);UiSmoke.shot(test,c,"brand-custom-colours-"+mode);VisualChecks.main(test,()->{List<EditText> inputs=VisualChecks.views(sheet[0].layout,EditText.class);inputs.get(0).setText("#187BC4");Phase4Checks.button(sheet[0].footer,"Aplicar").performClick();sheet[0]=null;});V15Checks.pause(test,250);yes(NativeStore.settings(c).getInt("customPrimary",0)==0xff187bc4&&Glass.palette(a).equals("custom"),"custom colours persist only on explicit apply");yes(financial.equals(LocalData.read(c).toString()),"colours, gestures and account navigation never change financial records");
    VisualChecks.main(test,a::finish);test.waitForIdleSync();host[0]=null;
   }
   org.json.JSONObject backup=LocalData.backup(c);int chosen=NativeStore.settings(c).getInt("customPrimary",0);NativeStore.settings(c).edit().remove("customPrimary").remove("customSecondary").commit();LocalData.restore(c,backup);yes(NativeStore.settings(c).getInt("customPrimary",0)==chosen&&NativeStore.settings(c).getAll().get("customSecondary") instanceof Integer,"backup restores personal colour values with their original integer type");yes(financial.equals(LocalData.read(c).toString()),"colour backup round trip preserves every financial record");
  }finally{if(sheet[0]!=null)VisualChecks.main(test,sheet[0]::dismiss);if(host[0]!=null)VisualChecks.main(test,host[0]::finish);NativeSync.EXEC.submit(()->{}).get(15,TimeUnit.SECONDS);VisualChecks.restore(c.getSharedPreferences("secure",0),secure);VisualChecks.restore(NativeStore.settings(c),settings);VisualChecks.restore(AppUpdates.prefs(c),updates);}
  return count;
 }
}

package cl.bolsillo.app;
import android.app.*;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;
/** Optional, resumable spotlight guide. A finished guide never implies granted permissions. */
final class DetectionTutorial {
 static final String[] TITLES={"Elige todas tus apps","Una cuenta, varias tarjetas","Permite leer los avisos","Decide cuánto automatizar","Revisa con un gesto"};
 static final String[] TEXT={"Bolsillo puede procesar avisos del banco, Google Wallet, Onepay y Gmail a la vez. Elige solo las apps que usas.","La tarjeta física y la digital pueden descontar del mismo saldo. Añade sus últimos cuatro dígitos si Wallet los cambia.","Android te permite decidir si Bolsillo puede leer notificaciones. Solo se procesan las apps elegidas; Gmail aporta sus avisos visibles.","Empieza revisando los avisos. Con tus confirmaciones, Bolsillo puede reconocer patrones seguros. Los avisos que envía Bolsillo se autorizan por separado.","Mantén pulsado un aviso para seleccionar varios. Aprueba los correctos o descártalos; los dudosos se completan uno a uno."};
 static final String[] TABS={"Apps","Tarjetas","Apps","Apps","Revisión"},TARGETS={"tutorial-apps","tutorial-cards","tutorial-capture","tutorial-learning","tutorial-review"};
 final MainActivity a;Dialog dialog;int step;boolean closed,awaitingSettings;TextView accessStatus,ownStatus;Button accessAction,ownAction;
 DetectionTutorial(MainActivity a,int step){this.a=a;this.step=Math.max(0,Math.min(TITLES.length-1,step));}
 static void automatic(MainActivity a){if(a.tutorialConsidered||a.data==null||a.dataLoading)return;a.tutorialConsidered=true;if(!NativeStore.settings(a).getBoolean("detectionTutorialSeen",false)&&!a.isFinishing()){if(a.data!=null&&a.data.optBoolean("bankSetupPending"))BankTemplates.show(a,true,()->start(a));else start(a);}}
 static void start(MainActivity a){if(a.tutorial!=null)a.tutorial.close(false);a.tutorialConsidered=true;a.tutorial=new DetectionTutorial(a,NativeStore.settings(a).getInt("detectionTutorialStep",0));a.tutorial.show();}
 static View target(View v,String tag){if(v==null)return null;if(tag.equals(v.getTag()))return v;if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++){View found=target(group.getChildAt(i),tag);if(found!=null)return found;}}return null;}
 void show(){
  if(closed||a.isFinishing())return;
  if(dialog!=null){dialog.setOnDismissListener(null);dialog.dismiss();}
  accessStatus=null;ownStatus=null;accessAction=null;ownAction=null;
  NativeStore.settings(a).edit().putBoolean("detectionTutorialSeen",true).putInt("detectionTutorialStep",step).apply();
  a.autoTab=TABS[step];a.tab="Automático";a.render(false);
  View focus=target(a.root,TARGETS[step]);if(focus!=null)focus.requestRectangleOnScreen(new Rect(0,0,focus.getWidth(),focus.getHeight()),true);
  dialog=new Dialog(a){@Override protected void onStart(){super.onStart();getWindow().setLayout(-1,-1);}@Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)refreshAccess();}};
  dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);Glass.privacy(dialog.getWindow(),a);
  FrameLayout frame=new FrameLayout(a);Spotlight shade=new Spotlight(a);frame.addView(shade,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout card=new SheetDialog.BoundedColumn(a);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(a.dp(22),a.dp(18),a.dp(22),a.dp(18));
  card.setBackground(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{Glass.gradient(a,Glass.dark(a)?0xff242c46:0xfff8fbff,Glass.dark(a)?0xff1b2438:0xffedf1ff,28),Glass.material(a,235,28,Glass.VIOLET)}));
  LinearLayout heading=Modern.row(a);heading.addView(Modern.text(a,"AUTOMATIZACIÓN  ·  "+(step+1)+" / "+TITLES.length,10,Glass.accent(a),true),new LinearLayout.LayoutParams(0,-2,1));
  heading.addView(Modern.iconButton(a,"close","Saltar tutorial",()->close(false)),new LinearLayout.LayoutParams(a.dp(40),a.dp(40)));card.addView(heading);
  ScrollView scroll=new ScrollView(a);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);LinearLayout content=Modern.column(a);scroll.addView(content);card.addView(scroll,new LinearLayout.LayoutParams(-1,-2,1));
  Modern.add(content,Modern.text(a,TITLES[step],22,Glass.ink(a),true),12);Modern.add(content,Modern.text(a,TEXT[step],14,Glass.subtle(a),false),10);
  if(step==2){
   accessStatus=Modern.text(a,"",12,Glass.subtle(a),false);accessStatus.setTag("tutorial-access-status");Modern.add(content,accessStatus,12);
   accessAction=Glass.button(a,"Configurar acceso");accessAction.setTag("tutorial-configure-access");Modern.add(content,accessAction,8);
   accessAction.setOnClickListener(v->{
    NotificationAccess.State state=NotificationAccess.state(a);
    if(state.permission&&!state.capture){NotificationAccess.primary(a);refreshAccess();}
    else NotificationAccess.configure(a,()->{awaitingSettings=true;if(dialog!=null)dialog.hide();});
   });
  }
  if(step==3){
   ownStatus=Modern.text(a,"",12,Glass.subtle(a),false);ownStatus.setTag("tutorial-own-notifications-status");Modern.add(content,ownStatus,12);
   ownAction=Glass.button(a,"Activar avisos de Bolsillo");Modern.add(content,ownAction,8);ownAction.setOnClickListener(v->AlertSettings.permission(a));
  }
  refreshAccess();
  LinearLayout actions=Modern.row(a);
  if(step>0){Button previous=Glass.button(a,"Volver");actions.addView(previous,new LinearLayout.LayoutParams(0,a.dp(52),1));WaterRipple.apply(previous);previous.setOnClickListener(v->{step--;show();});}
  Button next=Glass.button(a,step==2||step==3?"Volver":step==TITLES.length-1?"Empezar":"Siguiente");if(step==2||step==3)next.setText("Siguiente");LinearLayout.LayoutParams nextP=new LinearLayout.LayoutParams(0,a.dp(52),1);if(step>0)nextP.leftMargin=a.dp(8);actions.addView(next,nextP);WaterRipple.apply(next);
  next.setOnClickListener(v->{if(step<TITLES.length-1){step++;show();}else{close(true);a.autoTab="Apps";a.render(false);a.attempt(()->AutomationScreen.selectApps(a));}});Modern.add(card,actions,18);
  FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);cp.setMargins(a.dp(20),a.dp(20),a.dp(20),a.dp(44));frame.addView(card,cp);dialog.setContentView(frame);
  Window window=dialog.getWindow();window.setBackgroundDrawableResource(android.R.color.transparent);window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);dialog.setOnCancelListener(v->close(false));dialog.show();
  frame.post(()->{
   if(closed)return;View selected=target(a.root,TARGETS[step]);if(selected!=null)selected.requestRectangleOnScreen(new Rect(0,0,selected.getWidth(),selected.getHeight()),true);
   int[] origin=new int[2];frame.getLocationOnScreen(origin);Rect bounds=new Rect();
   if(selected!=null&&selected.getGlobalVisibleRect(bounds)){
    bounds.offset(-origin[0],-origin[1]);bounds.inset(-a.dp(5),-a.dp(5));shade.hole.set(bounds);int top=bounds.bottom+a.dp(20);
    if(top+card.getHeight()+a.dp(24)<frame.getHeight()){cp.gravity=Gravity.TOP;cp.topMargin=top;cp.bottomMargin=a.dp(24);}
    else{cp.gravity=Gravity.BOTTOM;cp.bottomMargin=Math.max(a.dp(30),frame.getHeight()-bounds.top+a.dp(18));if(frame.getHeight()-cp.bottomMargin-card.getHeight()<a.dp(20))cp.bottomMargin=a.dp(30);}
    card.setLayoutParams(cp);shade.invalidate();
   }
   if(android.animation.ValueAnimator.areAnimatorsEnabled()){card.setAlpha(0);card.setTranslationY(a.dp(8));card.animate().alpha(1).translationY(0).setDuration(180).setInterpolator(PageMotion.EASE).start();}
  });
 }
 void refreshAccess(){
  if(closed)return;
  if(awaitingSettings){resumeAfterSettings();return;}
  if(accessStatus!=null){NotificationAccess.State state=NotificationAccess.state(a);accessStatus.setText(NotificationAccess.permissionSummary(a));accessAction.setText(!state.permission?"Configurar acceso":!state.capture?"Activar lectura":"Gestionar acceso");}
  if(ownStatus!=null){boolean available=LocalAlerts.available(a);ownStatus.setText(available?"Avisos de Bolsillo permitidos. Puedes ajustar sus canales en Ajustes.":"Avisos de Bolsillo pendientes. Esto no impide leer los avisos elegidos.");ownAction.setText(available?"Configurar avisos de Bolsillo":"Activar avisos de Bolsillo");}
 }
 void resumeAfterSettings(){if(closed)return;awaitingSettings=false;refreshAccess();if(dialog!=null&&!a.isFinishing())dialog.show();}
 void close(boolean complete){if(closed)return;closed=true;awaitingSettings=false;NativeStore.settings(a).edit().putBoolean("detectionTutorialSeen",true).putInt("detectionTutorialStep",complete?0:step).putBoolean("detectionTutorialFinished",complete).apply();if(dialog!=null){dialog.setOnCancelListener(null);dialog.dismiss();}if(a.tutorial==this)a.tutorial=null;}
 static final class Spotlight extends View {final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final RectF hole=new RectF();Spotlight(MainActivity a){super(a);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setClickable(true);}@Override protected void onDraw(Canvas canvas){super.onDraw(canvas);Path mask=new Path();mask.setFillType(Path.FillType.EVEN_ODD);mask.addRect(0,0,getWidth(),getHeight(),Path.Direction.CW);if(!hole.isEmpty())mask.addRoundRect(hole,Glass.dp(getContext(),20),Glass.dp(getContext(),20),Path.Direction.CW);paint.setColor(0xb0101426);paint.setStyle(Paint.Style.FILL);canvas.drawPath(mask,paint);canvas.save();canvas.clipPath(mask);paint.setShader(new RadialGradient(getWidth()*.5f,getHeight()*.45f,Math.max(1,getHeight()*.7f),new int[]{0x00000000,0x88000012},null,Shader.TileMode.CLAMP));canvas.drawRect(0,0,getWidth(),getHeight(),paint);paint.setShader(null);canvas.restore();if(!hole.isEmpty()){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Glass.dp(getContext(),1));paint.setColor(Glass.alpha(Glass.accent(getContext()),210));canvas.drawRoundRect(hole,Glass.dp(getContext(),20),Glass.dp(getContext(),20),paint);paint.setStyle(Paint.Style.FILL);}}}
}

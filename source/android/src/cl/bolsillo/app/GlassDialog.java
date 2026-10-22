package cl.bolsillo.app;

import android.app.Dialog;
import android.content.Context;

/** Configure the window before Dialog.show() attaches its first frame to WindowManager. */
class GlassDialog extends Dialog {
 GlassDialog(Context context){super(context);}
 GlassDialog(Context context,int theme){super(context,theme);}
 @Override protected void onStart(){super.onStart();configureWindow();}
 protected void configureWindow(){Glass.window(this,getContext());}
}

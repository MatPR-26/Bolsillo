package cl.bolsillo.app;
import android.app.*;import android.os.Bundle;
public class QuickEntryActivity extends Activity {
 Dialog panel;
 @Override public void onCreate(Bundle state){super.onCreate(state);Glass.privacy(getWindow(),this);panel=QuickEntryDialog.create(this,"income".equals(getIntent().getStringExtra("quick"))?1:"transfer".equals(getIntent().getStringExtra("quick"))?2:0,getIntent().getStringExtra("quickAccount")==null?"":getIntent().getStringExtra("quickAccount"),state==null?null:state.getBundle("entry"));Glass.privacy(panel.getWindow(),this);panel.setOnDismissListener(x->finish());panel.show();}
 @Override protected void onResume(){super.onResume();Glass.privacy(getWindow(),this);if(panel!=null)Glass.privacy(panel.getWindow(),this);}
 @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);Bundle entry=QuickEntryDialog.draft(panel);if(entry!=null)state.putBundle("entry",entry);}
 @Override protected void onDestroy(){if(panel!=null){panel.setOnDismissListener(null);panel.dismiss();}super.onDestroy();}
}

package cl.bolsillo.app;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.*;
import android.view.*;
import org.json.JSONObject;

/** Launcher configuration also opens from the widget's gear action. */
public class WidgetConfigActivity extends Activity {
 WidgetPreferences.Editor editor;int widgetId;
 @Override public void onCreate(Bundle state){setTheme(Glass.dark(this)?R.style.AppThemeDark:R.style.AppTheme);super.onCreate(state);Glass.privacy(getWindow(),this);setResult(RESULT_CANCELED);widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);if(!WidgetPreferences.belongs(this,widgetId)){finish();return;}JSONObject draft=null;if(state!=null)try{draft=new JSONObject(state.getString("widgetOptions","{}"));}catch(Exception ignored){}editor=new WidgetPreferences.Editor(this,widgetId,draft,()->{setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId));finish();});setContentView(editor.root);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);editor.root.setOnApplyWindowInsetsListener((v,insets)->{int left=insets.getSystemWindowInsetLeft(),right=insets.getSystemWindowInsetRight(),top=insets.getSystemWindowInsetTop(),bottom=insets.getSystemWindowInsetBottom();v.setPadding(left+Glass.dp(this,20),top+Glass.dp(this,14),right+Glass.dp(this,20),bottom+Glass.dp(this,14));return insets;});editor.root.requestApplyInsets();}
 @Override protected void onResume(){super.onResume();Glass.privacy(getWindow(),this);}
 @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);if(editor!=null)state.putString("widgetOptions",editor.options.toString());}
}

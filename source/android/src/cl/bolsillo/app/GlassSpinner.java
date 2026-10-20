package cl.bolsillo.app;
import android.app.Activity;import android.content.Context;import android.view.*;import android.widget.*;import java.util.*;
/** Retains Spinner's selection contract with an accessible shared choice sheet. */
final class GlassSpinner extends Spinner {
 float downX,downY;boolean tap;
 GlassSpinner(Context c){super(c);setBackground(Glass.material(c,232,17,Glass.secondary(c)));setMinimumHeight(Glass.dp(c,56));setPadding(Glass.dp(c,12),Glass.dp(c,4),Glass.dp(c,12),Glass.dp(c,4));}
 @Override public void setAdapter(SpinnerAdapter adapter){if(adapter instanceof ArrayAdapter){super.setAdapter(new Tinted(getContext(),adapter,getContentDescription()==null?"":getContentDescription().toString()));}else super.setAdapter(adapter);}
 @Override public boolean performClick(){Context c=getContext();if(!(c instanceof Activity)||getAdapter()==null)return super.performClick();List<String> names=new ArrayList<>();for(int i=0;i<getAdapter().getCount();i++)names.add(String.valueOf(getAdapter().getItem(i)));NativeSettings.choose((Activity)c,getContentDescription()==null?"Elegir":getContentDescription().toString(),names,n->setSelection(n));return true;}
 /** Never forward a drag into Spinner's stock popup, including at a scroll boundary. */
 @Override public boolean onTouchEvent(MotionEvent event){if(!isEnabled())return false;switch(event.getActionMasked()){
  case MotionEvent.ACTION_DOWN:downX=event.getX();downY=event.getY();tap=true;setPressed(true);return true;
  case MotionEvent.ACTION_MOVE:if(Math.hypot(event.getX()-downX,event.getY()-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop()){tap=false;setPressed(false);}return true;
  case MotionEvent.ACTION_UP:boolean clicked=tap&&event.getX()>=0&&event.getY()>=0&&event.getX()<getWidth()&&event.getY()<getHeight();tap=false;setPressed(false);if(clicked)performClick();return true;
  case MotionEvent.ACTION_CANCEL:tap=false;setPressed(false);return true;
 }return true;}
 static final class Tinted extends android.widget.BaseAdapter implements SpinnerAdapter {final Context c;final SpinnerAdapter original;final String title;Tinted(Context c,SpinnerAdapter a,String title){this.c=c;original=a;this.title=title;}public int getCount(){return original.getCount();}public Object getItem(int p){return original.getItem(p);}public long getItemId(int p){return original.getItemId(p);}public View getView(int p,View recycled,ViewGroup parent){LinearLayout row=Modern.row(c);TextView text=Glass.text(c,String.valueOf(getItem(p)),15,true);text.setMaxLines(2);text.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout words=Modern.column(c);if(!title.isEmpty()){TextView heading=Glass.text(c,title,11,false);heading.setTextColor(Glass.subtle(c));words.addView(heading);}words.addView(text);row.addView(words,new LinearLayout.LayoutParams(0,-2,1));row.addView(Icons.view(c,"chevron",Glass.subtle(c),18),new LinearLayout.LayoutParams(Glass.dp(c,18),Glass.dp(c,18)));return row;}public View getDropDownView(int p,View r,ViewGroup g){return getView(p,r,g);}}
}

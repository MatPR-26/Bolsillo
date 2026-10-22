package cl.bolsillo.app;
import android.content.Context;import android.graphics.*;import android.graphics.drawable.Drawable;
/** Hero and bank panes share one optical surface rather than nested ornamental frames. */
final class LiquidArtwork extends Drawable {
 final float radius;final GlassMaterial material;
 LiquidArtwork(Context c,int tint,int radius){this.radius=Glass.dp(c,radius);material=new GlassMaterial(c,219,radius,tint);}
 @Override protected void onBoundsChange(Rect bounds){material.setBounds(bounds);}
 @Override public void draw(Canvas canvas){material.draw(canvas);}
 @Override public void getOutline(Outline outline){material.getOutline(outline);}
 @Override public void setAlpha(int alpha){material.setAlpha(alpha);invalidateSelf();}
 @Override public int getAlpha(){return material.getAlpha();}
 @Override public void setColorFilter(ColorFilter filter){material.setColorFilter(filter);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}

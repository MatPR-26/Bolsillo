package cl.bolsillo.app;

import android.content.Context;
import android.graphics.Bitmap;

/**
 * Software lens for a small, already downsampled backdrop (capture scale 1/5).
 * The rounded-box distance and quartic bevel profile below are an original
 * implementation using elementary geometry; no upstream shader or asset is used.
 * Text/icons must be drawn above this bitmap, so foreground content stays sharp.
 */
public final class DockLens {
    private static final int MAX_PIXELS = 262144;
    private static final float CAPTURE_SCALE = 5f;
    private final Context context;
    private final float cornerRadius;
    private int width, height, intensity = -1;
    private Bitmap output;
    private int[] input, horizontal, softened, result, sampleBase;
    private float[] sampleX, sampleY, coverage;

    public DockLens(Context c) {
        context = c.getApplicationContext();
        cornerRadius = 28f * c.getResources().getDisplayMetrics().density / CAPTURE_SCALE;
    }

    /**
     * Returns a cached ARGB bitmap owned by this lens. Reuse on the UI thread;
     * do not recycle or retain it as an immutable frame. Null source returns null.
     * Only small software bitmaps are accepted, avoiding GPU readback and large
     * per-frame allocations. Source pixels are never changed.
     */
    public synchronized Bitmap render(Bitmap source) {
        if (source == null) return null;
        if (source.isRecycled()) throw new IllegalArgumentException("Backdrop was recycled");
        if (source.getConfig() == Bitmap.Config.HARDWARE)
            throw new IllegalArgumentException("DockLens requires a software backdrop");
        int w = source.getWidth(), h = source.getHeight();
        if ((long) w * h > MAX_PIXELS)
            throw new IllegalArgumentException("Downsample the dock backdrop before rendering");
        int setting = Glass.intensity(context);
        ensure(w, h, setting);
        source.getPixels(input, 0, w, 0, 0, w, h);
        output.setDensity(source.getDensity());
        if (setting == 0) {
            output.setPixels(input, 0, w, 0, 0, w, h);
            return output;
        }

        // Filter/interpolate premultiplied colours to avoid dark transparent rims.
        for (int i = 0; i < input.length; i++) input[i] = premultiply(input[i]);
        int blurRadius = Math.min(setting == 2 ? 2 : 1, Math.max(w, h) - 1);
        box(input, horizontal, w, h, blurRadius, true);
        box(horizontal, softened, w, h, blurRadius, false);
        int right = w > 1 ? 1 : 0, below = h > 1 ? w : 0;
        for (int i = 0; i < result.length; i++) {
            if (coverage[i] == 0f) { result[i] = 0; continue; }
            int at = sampleBase[i];
            int a = softened[at], b = softened[at + right];
            int d = softened[at + below], e = softened[at + below + right];
            float x = sampleX[i], y = sampleY[i];
            int alpha = channel(a, b, d, e, 24, x, y);
            if (alpha == 0) { result[i] = 0; continue; }
            int r = unpremultiply(channel(a, b, d, e, 16, x, y), alpha);
            int g = unpremultiply(channel(a, b, d, e, 8, x, y), alpha);
            int blue = unpremultiply(channel(a, b, d, e, 0, x, y), alpha);
            int covered = Math.round(alpha * coverage[i]);
            result[i] = covered == 0 ? 0 : covered << 24 | r << 16 | g << 8 | blue;
        }
        output.setPixels(result, 0, w, 0, 0, w, h);
        return output;
    }

    /** Convenience for callers that need an independent bitmap instead of a cache. */
    public static Bitmap render(Context c, Bitmap source) {
        DockLens lens = new DockLens(c);
        Bitmap frame = lens.render(source);
        Bitmap copy = frame == null ? null : frame.copy(Bitmap.Config.ARGB_8888, false);
        lens.release();
        return copy;
    }

    /** Drop references after clearing the ImageView; never recycle a displayed frame. */
    public synchronized void release() {
        output = null;
        input = horizontal = softened = result = sampleBase = null;
        sampleX = sampleY = coverage = null;
        width = height = 0;
        intensity = -1;
    }

    private void ensure(int w, int h, int setting) {
        boolean resized = output == null || width != w || height != h;
        if (resized) {
            width = w; height = h;
            int count = w * h;
            input = new int[count]; horizontal = new int[count]; softened = new int[count];
            result = new int[count]; sampleBase = new int[count];
            sampleX = new float[count]; sampleY = new float[count]; coverage = new float[count];
            // Previous bitmap may still belong to RenderThread: let GC reclaim it safely.
            output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        }
        if (!resized && intensity == setting) return;
        intensity = setting;
        float halfW = w * .5f, halfH = h * .5f;
        float radius = Math.min(cornerRadius, Math.min(halfW, halfH));
        float bevel = Math.max(1f, Math.min(Math.min(w, h) * .24f, cornerRadius * .58f));
        float amplitude = bevel * (setting == 2 ? .19f : .13f);
        for (int row = 0, at = 0; row < h; row++) {
            for (int col = 0; col < w; col++, at++) {
                float px = col + .5f - halfW, py = row + .5f - halfH;
                float qx = Math.abs(px) - (halfW - radius);
                float qy = Math.abs(py) - (halfH - radius);
                float ox = Math.max(qx, 0f), oy = Math.max(qy, 0f);
                float length = (float) Math.sqrt(ox * ox + oy * oy);
                float distance = length + Math.min(Math.max(qx, qy), 0f) - radius;
                coverage[at] = clamp(.5f - distance, 0f, 1f);
                float nx, ny;
                if (length > .0001f) {
                    nx = sign(px) * ox / length;
                    ny = sign(py) * oy / length;
                } else if (qx >= qy) { nx = sign(px); ny = 0f; }
                else { nx = 0f; ny = sign(py); }
                float displacement = amplitude * bevelProfile(Math.max(0f, -distance) / bevel);
                float sx = clamp(col - nx * displacement, 0f, w - 1f);
                float sy = clamp(row - ny * displacement, 0f, h - 1f);
                int ix = w > 1 ? Math.min(w - 2, (int) sx) : 0;
                int iy = h > 1 ? Math.min(h - 2, (int) sy) : 0;
                sampleBase[at] = iy * w + ix;
                sampleX[at] = w > 1 ? sx - ix : 0f;
                sampleY[at] = h > 1 ? sy - iy : 0f;
            }
        }
    }

    // Zero value/slope at either end; max derivative < 3.08. Amplitude <= .19
    // of the bevel keeps the one-dimensional sampling map monotonic (no folds).
    static float bevelProfile(float t) {
        if (t <= 0f || t >= 1f) return 0f;
        float shape = t * (1f - t);
        return 16f * shape * shape;
    }

    static float roundedDistance(float x, float y, float halfW, float halfH, float radius) {
        float r = Math.max(0f, Math.min(radius, Math.min(halfW, halfH)));
        float qx = Math.abs(x) - (halfW - r), qy = Math.abs(y) - (halfH - r);
        float ox = Math.max(qx, 0f), oy = Math.max(qy, 0f);
        return (float) Math.sqrt(ox * ox + oy * oy) + Math.min(Math.max(qx, qy), 0f) - r;
    }

    private static float sign(float value) { return value < 0f ? -1f : 1f; }
    private static float clamp(float value, float lo, float hi) { return Math.max(lo, Math.min(hi, value)); }
    private static int premultiply(int color) {
        int a = color >>> 24;
        if (a == 255) return color;
        if (a == 0) return 0;
        return a << 24 | ((color >>> 16 & 255) * a + 127) / 255 << 16
                | ((color >>> 8 & 255) * a + 127) / 255 << 8 | ((color & 255) * a + 127) / 255;
    }
    private static int unpremultiply(int value, int alpha) { return Math.min(255, (value * 255 + alpha / 2) / alpha); }
    private static int channel(int a, int b, int d, int e, int shift, float x, float y) {
        float top = (a >>> shift & 255) * (1f - x) + (b >>> shift & 255) * x;
        float bottom = (d >>> shift & 255) * (1f - x) + (e >>> shift & 255) * x;
        return Math.round(top * (1f - y) + bottom * y);
    }

    /** Separable sliding box: two linear passes, with clamped boundary samples. */
    private static void box(int[] from, int[] to, int w, int h, int radius, boolean across) {
        int lines = across ? h : w, length = across ? w : h;
        int stride = across ? 1 : w, window = radius * 2 + 1;
        for (int line = 0; line < lines; line++) {
            int start = across ? line * w : line;
            int a = 0, r = 0, g = 0, b = 0;
            for (int j = -radius; j <= radius; j++) {
                int color = from[start + Math.max(0, Math.min(length - 1, j)) * stride];
                a += color >>> 24; r += color >>> 16 & 255; g += color >>> 8 & 255; b += color & 255;
            }
            for (int pos = 0; pos < length; pos++) {
                to[start + pos * stride] = (a + window / 2) / window << 24 | (r + window / 2) / window << 16
                        | (g + window / 2) / window << 8 | (b + window / 2) / window;
                int old = from[start + Math.max(0, pos - radius) * stride];
                int next = from[start + Math.min(length - 1, pos + radius + 1) * stride];
                a += (next >>> 24) - (old >>> 24);
                r += (next >>> 16 & 255) - (old >>> 16 & 255);
                g += (next >>> 8 & 255) - (old >>> 8 & 255);
                b += (next & 255) - (old & 255);
            }
        }
    }
}

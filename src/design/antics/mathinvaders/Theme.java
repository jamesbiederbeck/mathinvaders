package design.antics.mathinvaders;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.util.Random;

/**
 * "Candy Arcade" look shared by every kid game in this tree (see DESIGN.md).
 * The repo has no shared code, so this file is copied into each app; only the
 * package line differs. Keep it app-agnostic: colours, type, and paint helpers.
 */
final class Theme {
    static final int NAVY = 0xFF1F2154, SURFACE = 0xFF2E3170;
    static final int CORAL = 0xFFFF6B4A, SUN = 0xFFFFC31F, VIOLET = 0xFFA96CF5;
    static final int MINT = 0xFF5BF7C0, CREAM = 0xFFFFF6E5, WHITE = 0xFFFFFFFF;
    static final int[] TILES = {CORAL, SUN, VIOLET};
    static final Typeface FONT = Typeface.create("sans-serif-black", Typeface.NORMAL);

    /** Hamburger hit area, in 1%-of-width units. */
    static final float MENU_W = 14, MENU_H = 12;

    static void init(Paint p) { p.setAntiAlias(true); p.setTypeface(FONT); }

    static int mix(int c, int to, float t) {
        int a = c >>> 24;
        int r = Math.round(((c >> 16) & 255) * (1 - t) + ((to >> 16) & 255) * t);
        int g = Math.round(((c >> 8) & 255) * (1 - t) + ((to >> 8) & 255) * t);
        int b = Math.round((c & 255) * (1 - t) + (to & 255) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    static int alpha(int c, float a) { return ((int) (255 * a) << 24) | (c & 0xFFFFFF); }

    /** Flat fully-rounded tile with the glossy top-left arc from the icon. */
    static void tile(Canvas cv, Paint p, RectF r, int color) { tile(cv, p, r, color, true); }

    static void tile(Canvas cv, Paint p, RectF r, int color, boolean gloss) {
        float s = Math.min(r.width(), r.height());
        float rad = s * 0.3f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        cv.drawRoundRect(r, rad, rad, p);
        if (!gloss || s < 18) return;
        float gr = rad * 0.62f, cx = r.left + rad, cy = r.top + rad;
        sOval.set(cx - gr, cy - gr, cx + gr, cy + gr);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(Math.max(2, s * 0.07f));
        p.setColor(mix(color, WHITE, 0.4f));
        cv.drawArc(sOval, 180, 90, false, p);
        p.setStyle(Paint.Style.FILL);
    }
    private static final RectF sOval = new RectF();

    /** Text centred on (cx, cy), vertically as well. */
    static void text(Canvas cv, Paint p, String s, float cx, float cy, float size, int color) {
        text(cv, p, s, cx, cy, size, color, Paint.Align.CENTER);
    }

    static void text(Canvas cv, Paint p, String s, float x, float cy, float size, int color, Paint.Align a) {
        p.setStyle(Paint.Style.FILL);
        p.setTypeface(FONT);
        p.setTextSize(size);
        p.setColor(color);
        p.setTextAlign(a);
        cv.drawText(s, x, cy - (p.ascent() + p.descent()) / 2, p);
    }

    /**
     * Question text for the quiz screens. Split on '\n' into left-aligned rows; if
     * the text ends in "\t= ?" (or similar) that tail is drawn well to the right of
     * the rows, vertically centred, so a dot picture reads as ONE group and the
     * answer is clearly for the whole thing. Shrunk to fit maxW x maxH around (cx, cy).
     */
    static void textLines(Canvas cv, Paint p, String s, float cx, float cy, float size, float maxW, float maxH, int color) {
        String tail = "";
        int tab = s.indexOf('\t');
        if (tab >= 0) { tail = s.substring(tab + 1); s = s.substring(0, tab); }
        String[] lines = s.split("\n");
        p.setTypeface(FONT);
        p.setTextSize(size);
        float block = 0;
        for (String l : lines) block = Math.max(block, p.measureText(l));
        float tailW = tail.isEmpty() ? 0 : p.measureText(tail), gapW = tail.isEmpty() ? 0 : size * 1.1f;
        float total = block + gapW + tailW;
        float sz = total > maxW ? size * maxW / total : size;
        sz = Math.min(sz, maxH / (lines.length * 1.15f));
        float k = sz / size, lh = sz * 1.15f;
        float left = cx - total * k / 2, y = cy - lh * (lines.length - 1) / 2;
        for (String l : lines) { text(cv, p, l, left, y, sz, color, Paint.Align.LEFT); y += lh; }
        if (!tail.isEmpty()) text(cv, p, tail, left + (block + gapW) * k, cy, sz, color, Paint.Align.LEFT);
    }

    /** Detects two quick three-finger taps. swallow() is true for the whole gesture once a third finger lands. */
    static final class ThreeTap {
        private boolean multi;
        private long last;
        boolean swallow(MotionEvent e, Runnable onDouble) {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) multi = false;
            if (a == MotionEvent.ACTION_POINTER_DOWN && e.getPointerCount() == 3) {
                multi = true;
                long t = e.getEventTime();
                if (last != 0 && t - last < 700) { last = 0; onDouble.run(); } else last = t;
            }
            return multi;
        }
    }

    /** Same HUD everywhere: hamburger, then score; a centre label; a right label. */
    static void hud(Canvas cv, Paint p, float w, float unit, String left, String center, String right) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(CREAM);
        for (int i = 0; i < 3; i++) {
            sOval.set(3 * unit, 3.4f * unit + i * 2 * unit, 9 * unit, 4.5f * unit + i * 2 * unit);
            cv.drawRoundRect(sOval, unit * 0.55f, unit * 0.55f, p);
        }
        text(cv, p, left, 14 * unit, 6 * unit, 4 * unit, WHITE, Paint.Align.LEFT);
        text(cv, p, center, w / 2 + 4 * unit, 6 * unit, 4 * unit, WHITE, Paint.Align.CENTER);
        text(cv, p, right, w - 3 * unit, 6 * unit, 4 * unit, WHITE, Paint.Align.RIGHT);
    }

    /** Small squares that burst out of a popped tile. */
    static final class Pops {
        private static final int MAX = 96;
        private final float[] x = new float[MAX], y = new float[MAX], vx = new float[MAX], vy = new float[MAX], life = new float[MAX];
        private final int[] col = new int[MAX];
        private int n;
        private final Random rng = new Random();

        void burst(float cx, float cy, int color, float speed, int count) {
            for (int i = 0; i < count && n < MAX; i++, n++) {
                double a = rng.nextDouble() * Math.PI * 2;
                float v = speed * (0.4f + rng.nextFloat() * 0.6f);
                x[n] = cx; y[n] = cy;
                vx[n] = (float) Math.cos(a) * v; vy[n] = (float) Math.sin(a) * v;
                life[n] = 0.5f; col[n] = color;
            }
        }

        void update(float dt) {
            for (int i = 0; i < n; i++) {
                life[i] -= dt;
                x[i] += vx[i] * dt; y[i] += vy[i] * dt; vy[i] += 120 * dt * 10;
                if (life[i] <= 0) {                      // swap-remove
                    n--;
                    x[i] = x[n]; y[i] = y[n]; vx[i] = vx[n]; vy[i] = vy[n]; life[i] = life[n]; col[i] = col[n];
                    i--;
                }
            }
        }

        void draw(Canvas cv, Paint p, float size) {
            p.setStyle(Paint.Style.FILL);
            for (int i = 0; i < n; i++) {
                p.setColor(alpha(col[i], Math.min(1f, life[i] / 0.5f)));
                cv.drawRect(x[i] - size / 2, y[i] - size / 2, x[i] + size / 2, y[i] + size / 2, p);
            }
        }
    }

    /** Fading dots behind a moving ball, like the streak in the icon. */
    static final class Trail {
        private static final int N = 10;
        private final float[] x = new float[N], y = new float[N];
        private int count, head;

        void add(float px, float py) { x[head] = px; y[head] = py; head = (head + 1) % N; if (count < N) count++; }
        void clear() { count = 0; }

        void draw(Canvas cv, Paint p, float radius, int color) {
            p.setStyle(Paint.Style.FILL);
            for (int i = 0; i < count; i++) {                      // oldest first
                int idx = (head - count + i + N) % N;
                float t = (i + 1f) / (count + 1f);
                p.setColor(alpha(color, 0.35f * t));
                cv.drawCircle(x[idx], y[idx], radius * (0.3f + 0.6f * t), p);
            }
        }
    }

    private Theme() {}
}

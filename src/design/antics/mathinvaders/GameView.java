package design.antics.mathinvaders;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

/**
 * One question at the top, four invaders each carrying a candidate answer.
 * Drag to aim, release to fire a single shot. Invaders descend toward the red
 * line at a parent-set speed (a percentage of the fall per second). Of the three
 * wrong ones, one parks at 25%, one at 50% and one at 75% of the fall; the right
 * one keeps going, and if it reaches the line a life is lost. Shooting it clears
 * the wave; shooting a wrong one just pops it.
 */
public class GameView extends View {

    private static final int N = 4;
    private static final float STEP = 1f / 120f;
    static final int DEFAULT_SPEED = 10;   // percent of the fall per second

    private enum State { READY, PLAYING, PAUSED, OVER }

    private final Paint paint = new Paint();
    private final RectF rect = new RectF();
    private final SharedPreferences prefs;
    private final Random rng = new Random();
    private final Theme.Pops pops = new Theme.Pops();
    private boolean menuGesture;
    private final Theme.ThreeTap threeTap = new Theme.ThreeTap();
    private final Theme.Trail trail = new Theme.Trail();
    private final RectF menuBtn = new RectF();

    private State state = State.READY;
    private float w, h, unit;
    private float cannonX, cannonY, cannonW, cannonH;
    private float tileW, tileH, gap, rowTop, fall, dangerY;
    private final float[] frac = new float[N], stop = new float[N];
    private int speed;
    private boolean bulletLive;
    private float bx, by, bulletV;
    private final int[] cand = new int[N];
    private final boolean[] gone = new boolean[N];
    private int answer, score, best, lives, streak, packs;
    private String question = "", feedback = "";
    private long lastNs;
    private float acc;
    private boolean running;
    Runnable onMenu;

    public GameView(Context c) {
        super(c);
        Theme.init(paint);
        prefs = c.getSharedPreferences("game", Context.MODE_PRIVATE);
        best = prefs.getInt("best", 0);
        packs = prefs.getInt("packs2", Packs.DEFAULT_MASK);
        speed = prefs.getInt("speed", DEFAULT_SPEED);
        setKeepScreenOn(true);
    }

    @Override
    protected void onSizeChanged(int nw, int nh, int ow, int oh) {
        w = nw; h = nh; unit = w / 100f;
        menuBtn.set(0, 0, Theme.MENU_W * unit, Theme.MENU_H * unit);
        gap = 2.5f * unit;
        tileW = (w - gap * (N + 1)) / N;
        tileH = 12 * unit;
        rowTop = 28 * unit;
        cannonW = 16 * unit; cannonH = 6 * unit;
        cannonY = h - 14 * unit;
        dangerY = cannonY - 12 * unit;
        fall = dangerY - tileH - rowTop;      // top-of-tile travel from start to the red line
        bulletV = 150 * unit;
        cannonX = w / 2;
        newGame();
    }

    private void newGame() {
        score = 0; lives = 3; streak = 0; feedback = "";
        newQuestion();
        state = State.READY;
    }

    // ---- questions -----------------------------------------------------

    private void newQuestion() {
        Object[] p = Packs.generate(packs, rng);
        question = (String) p[0];
        answer = (Integer) p[1];
        fillCandidates(answer, rng, cand);
        for (int i = 0; i < N; i++) gone[i] = false;
        // Wrong answers park at 25/50/75% in a random order; the right one never parks.
        float[] stops = {0.25f, 0.5f, 0.75f};
        for (int i = stops.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            float t = stops[i]; stops[i] = stops[j]; stops[j] = t;
        }
        int k = 0;
        for (int i = 0; i < N; i++) { frac[i] = 0; stop[i] = cand[i] == answer ? 1f : stops[k++]; }
        bulletLive = false;
    }

    /** One right answer in a random slot, plus distinct, positive, nearby decoys. */
    static void fillCandidates(int answer, Random rng, int[] out) {
        int spread = Math.max(3, answer / 8);
        out[0] = answer;
        for (int i = 1; i < out.length; i++) {
            int v;
            boolean dup;
            do {
                v = answer + (rng.nextBoolean() ? 1 : -1) * (1 + rng.nextInt(spread));
                dup = v < 1;
                for (int j = 0; j < i; j++) if (out[j] == v) dup = true;
            } while (dup);
            out[i] = v;
        }
        int right = rng.nextInt(out.length);       // move the answer to a random slot
        int t = out[right]; out[right] = out[0]; out[0] = t;
    }

    // ---- loop ----------------------------------------------------------

    void resume() { if (running) return; running = true; lastNs = 0; postOnAnimation(tick); }
    void pause() { running = false; if (state == State.PLAYING) state = State.PAUSED; save(); }
    private void save() { if (score > best) { best = score; prefs.edit().putInt("best", best).apply(); } }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long now = System.nanoTime();
            if (lastNs != 0) {
                acc += Math.min((now - lastNs) / 1e9f, 0.05f);
                while (acc >= STEP) { step(STEP); acc -= STEP; }
            }
            lastNs = now;
            invalidate();
            postOnAnimation(this);
        }
    };

    private float tileX(int i) { return gap + i * (tileW + gap); }
    private float tileY(int i) { return rowTop + frac[i] * fall; }

    private void step(float dt) {
        pops.update(dt);
        if (state != State.PLAYING) return;
        for (int i = 0; i < N; i++) {
            if (gone[i] || frac[i] >= stop[i]) continue;
            frac[i] = Math.min(stop[i], frac[i] + speed / 100f * dt);
            if (frac[i] >= 1f) {                 // the right answer reached the line
                feedback = "Too slow! It was " + answer;
                loseLife();
                return;
            }
        }
        if (!bulletLive) return;
        trail.add(bx, by);
        by -= bulletV * dt;
        if (by < 0) { bulletLive = false; return; }
        for (int i = 0; i < N; i++) {
            if (gone[i]) continue;
            float x = tileX(i);
            float y = tileY(i);
            if (bx < x || bx > x + tileW || by > y + tileH || by < y) continue;
            bulletLive = false;
            int color = Theme.TILES[i % 3];
            pops.burst(x + tileW / 2, y + tileH / 2, color, 45 * unit, 10);
            if (cand[i] == answer) {
                streak++;
                score += 10 + 2 * Math.min(streak, 5);
                feedback = "Yes! " + question.replace("?", String.valueOf(answer)).replaceAll("[\\n\\t]", " ");
                save();
                newQuestion();
            } else {
                streak = 0;
                gone[i] = true;
                feedback = "Not " + cand[i] + " - try again";
            }
            return;
        }
    }

    private void loseLife() {
        lives--;
        pops.burst(cannonX, cannonY, Theme.CORAL, 60 * unit, 14);
        if (lives <= 0) { state = State.OVER; save(); }
        else { newQuestion(); state = State.READY; }
    }

    // ---- parents menu hooks --------------------------------------------

    int packs() { return packs; }
    void setPacks(int m) { packs = m; prefs.edit().putInt("packs2", m).apply(); }
    void resetBest() { best = 0; prefs.edit().putInt("best", 0).apply(); }
    int speed() { return speed; }
    void setSpeed(int p) { speed = p; prefs.edit().putInt("speed", p).apply(); }
    void menuOpened() { if (state == State.PLAYING) state = State.PAUSED; }

    // ---- input ---------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (threeTap.swallow(e, () -> { if (state == State.PLAYING) state = State.PAUSED; })) return true;   // double three-finger tap pauses
        if (menuGesture) {                       // the tap that opened the menu must not also act on the game
            if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) menuGesture = false;
            return true;
        }
        if (a == MotionEvent.ACTION_DOWN && menuBtn.contains(e.getX(), e.getY())) {
            menuGesture = true;
            if (onMenu != null) onMenu.run();
            return true;
        }
        if (state == State.PLAYING || state == State.READY)
            cannonX = Math.max(cannonW / 2, Math.min(w - cannonW / 2, e.getX()));
        if (a == MotionEvent.ACTION_UP) {
            switch (state) {
                case READY:  state = State.PLAYING; break;       // first release only starts
                case PAUSED: state = State.PLAYING; break;
                case OVER:   newGame(); break;
                case PLAYING:
                    if (!bulletLive) { bulletLive = true; bx = cannonX; by = cannonY; trail.clear(); }
                    break;
            }
        }
        return true;
    }

    // ---- drawing -------------------------------------------------------

    @Override
    protected void onDraw(Canvas cv) {
        cv.drawColor(Theme.NAVY);
        Theme.hud(cv, paint, w, unit, "Score " + score, "Lives " + Math.max(lives, 0), "Best " + Math.max(best, score));

        Theme.textLines(cv, paint, question, w / 2, 17 * unit, 8 * unit, w * 0.9f, 16 * unit, Theme.WHITE);

        // danger line
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Theme.alpha(Theme.CORAL, 0.35f));
        cv.drawRect(gap, dangerY, w - gap, dangerY + 0.7f * unit, paint);

        for (int i = 0; i < N; i++) {
            if (gone[i]) continue;
            float x = tileX(i), rowY = tileY(i);
            int color = Theme.TILES[i % 3];
            // ears, so the tiles read as invaders
            rect.set(x + tileW * 0.12f, rowY - 2.6f * unit, x + tileW * 0.30f, rowY + 1f * unit);
            Theme.tile(cv, paint, rect, color, false);
            rect.set(x + tileW * 0.70f, rowY - 2.6f * unit, x + tileW * 0.88f, rowY + 1f * unit);
            Theme.tile(cv, paint, rect, color, false);
            rect.set(x, rowY, x + tileW, rowY + tileH);
            Theme.tile(cv, paint, rect, color);
            String s = String.valueOf(cand[i]);
            Theme.text(cv, paint, s, rect.centerX(), rect.centerY(), Math.min(8 * unit, tileW * 0.95f / (s.length() * 0.62f)), Theme.NAVY);
        }

        // cannon
        rect.set(cannonX - 1.8f * unit, cannonY - 4 * unit, cannonX + 1.8f * unit, cannonY + 1 * unit);
        Theme.tile(cv, paint, rect, Theme.MINT, false);
        rect.set(cannonX - cannonW / 2, cannonY, cannonX + cannonW / 2, cannonY + cannonH);
        Theme.tile(cv, paint, rect, Theme.MINT);

        if (bulletLive) {
            trail.draw(cv, paint, 1.3f * unit, Theme.CREAM);
            paint.setColor(Theme.CREAM);
            cv.drawCircle(bx, by, 1.3f * unit, paint);
        }
        pops.draw(cv, paint, unit * 1.1f);

        Theme.text(cv, paint, feedback, w / 2, h - 5 * unit, 3.6f * unit, Theme.SUN);
        String msg = null;
        if (state == State.READY)  msg = "Drag to aim • release to fire";
        if (state == State.PAUSED) msg = "Paused • tap to resume";
        if (state == State.OVER)   msg = "Game over • tap to play again";
        if (msg != null) Theme.text(cv, paint, msg, w / 2, h * 0.58f, 4.2f * unit, Theme.WHITE);
    }
}

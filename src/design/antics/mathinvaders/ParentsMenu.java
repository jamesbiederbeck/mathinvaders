package design.antics.mathinvaders;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * PIN-gated parents menu built from plain framework views. The PIN is a speed
 * bump against a child's tap, not security (stored in plain SharedPreferences).
 */
final class ParentsMenu {
    static final String DEFAULT_PIN = "1234";
    private static final int PIN_LEN = 4;

    interface Host {
        String[] packNames();
        String[] packExamples();
        int packs();
        void setPacks(int mask);
        void resetBest();
        void onMenuClosed();
        /** Descent speed in percent of the fall per second; negative hides the control. */
        default int speed() { return -1; }
        /** {min, afterBalls, growthPerBall, max} problems-per-lost-ball settings, or null to hide them. */
        default int[] growth() { return null; }
        default void setGrowth(int[] g) {}
        default void setSpeed(int percent) {}
    }

    /**
     * Problems needed for the drop-th lost ball (1-based): min for the first
     * `after` balls, then +rate per further ball, never above max.
     */
    static int problems(int[] g, int drop) {
        int n = g[0] + (drop > g[1] ? g[2] * (drop - g[1]) : 0);
        return Math.max(g[0], Math.min(Math.max(g[0], g[3]), n));
    }

    private static LinearLayout stepper(Context c, String label, int lo, int hi, int[] g, int idx, Runnable changed) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = new TextView(c);
        name.setText(label);
        name.setTextSize(15);
        row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button minus = new Button(c), plus = new Button(c);
        minus.setText("-"); plus.setText("+");
        TextView val = new TextView(c);
        val.setTextSize(20);
        val.setGravity(Gravity.CENTER);
        Runnable show = () -> val.setText(String.valueOf(g[idx]));
        show.run();
        minus.setOnClickListener(v -> { if (g[idx] > lo) { g[idx]--; show.run(); changed.run(); } });
        plus.setOnClickListener(v -> { if (g[idx] < hi) { g[idx]++; show.run(); changed.run(); } });
        row.addView(minus, new LinearLayout.LayoutParams(dp(c, 56), dp(c, 48)));
        row.addView(val, new LinearLayout.LayoutParams(dp(c, 44), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(plus, new LinearLayout.LayoutParams(dp(c, 56), dp(c, 48)));
        return row;
    }

    /** Called with the entered digits; return an error message or null to accept. */
    private interface PinListener { String onPin(String pin); }

    static void open(Context c, Host host) {
        SharedPreferences prefs = c.getSharedPreferences("game", Context.MODE_PRIVATE);
        String pin = prefs.getString("pin", DEFAULT_PIN);
        pad(c, "Parents: enter PIN", host, entered -> {
            if (!entered.equals(pin)) return "Wrong PIN";
            settings(c, host, prefs);
            return null;
        });
    }

    private static Dialog dialog(Context c, Host host) {
        Dialog d = new Dialog(c, android.R.style.Theme_DeviceDefault_Dialog);
        d.setCancelable(true);
        d.setOnDismissListener(x -> host.onMenuClosed());
        return d;
    }

    private static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    private static void pad(Context c, String title, Host host, PinListener l) {
        Dialog d = dialog(c, host);
        d.setTitle(title);
        StringBuilder entered = new StringBuilder();
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 16));
        TextView dots = new TextView(c);
        dots.setTextSize(28);
        dots.setGravity(Gravity.CENTER);
        TextView msg = new TextView(c);
        msg.setTextColor(0xFFFFB74D);
        msg.setGravity(Gravity.CENTER);
        col.addView(dots);
        col.addView(msg);
        Runnable refresh = () -> {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < PIN_LEN; i++) s.append(i < entered.length() ? "● " : "○ ");
            dots.setText(s.toString().trim());
        };
        refresh.run();
        String[] labels = {"1","2","3","4","5","6","7","8","9","<","0","X"};
        for (int r = 0; r < 4; r++) {
            LinearLayout row = new LinearLayout(c);
            for (int k = 0; k < 3; k++) {
                String t = labels[r * 3 + k];
                Button b = new Button(c);
                b.setText(t);
                b.setTextSize(22);
                row.addView(b, new LinearLayout.LayoutParams(dp(c, 76), dp(c, 64)));
                b.setOnClickListener(v -> {
                    msg.setText("");
                    if (t.equals("X")) { d.dismiss(); return; }
                    if (t.equals("<")) { if (entered.length() > 0) entered.setLength(entered.length() - 1); }
                    else if (entered.length() < PIN_LEN) entered.append(t);
                    refresh.run();
                    if (entered.length() == PIN_LEN) {
                        String err = l.onPin(entered.toString());
                        if (err == null) { d.setOnDismissListener(null); d.dismiss(); }
                        else { entered.setLength(0); refresh.run(); msg.setText(err); }
                    }
                });
            }
            col.addView(row);
        }
        d.setContentView(col);
        d.show();
    }

    private static void settings(Context c, Host host, SharedPreferences prefs) {
        Dialog d = dialog(c, host);
        d.setTitle("Parents menu");
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(c, 20), dp(c, 8), dp(c, 20), dp(c, 16));

        TextView h = new TextView(c);
        h.setText("Problem packs (at least one)");
        h.setTextSize(16);
        col.addView(h);
        CheckBox[] boxes = new CheckBox[host.packNames().length];
        for (int i = 0; i < boxes.length; i++) {
            CheckBox cb = new CheckBox(c);
            cb.setText(host.packNames()[i] + "  (" + host.packExamples()[i] + ")");
            cb.setChecked((host.packs() & (1 << i)) != 0);
            boxes[i] = cb;
            final int bit = 1 << i;
            cb.setOnCheckedChangeListener((v, on) -> {
                int m = on ? host.packs() | bit : host.packs() & ~bit;
                if (m == 0) { v.setChecked(true); return; }   // keep one on
                host.setPacks(m);
            });
            col.addView(cb);
        }

        if (host.speed() >= 0) {
            final int min = 2;
            TextView sh = new TextView(c);
            sh.setTextSize(16);
            sh.setPadding(0, dp(c, 12), 0, 0);
            SeekBar sb = new SeekBar(c);
            sb.setMax(30 - min);
            sb.setProgress(Math.max(min, Math.min(30, host.speed())) - min);
            Runnable label = () -> sh.setText("Descent speed: " + (sb.getProgress() + min) + "% of the fall per second");
            label.run();
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s, int v, boolean user) { host.setSpeed(v + min); label.run(); }
                public void onStartTrackingTouch(SeekBar s) {}
                public void onStopTrackingTouch(SeekBar s) {}
            });
            col.addView(sh);
            col.addView(sb);
        }

        if (host.growth() != null) {
            int[] g = host.growth().clone();
            TextView gh = new TextView(c);
            gh.setText("Problems per lost ball");
            gh.setTextSize(16);
            gh.setPadding(0, dp(c, 12), 0, 0);
            col.addView(gh);
            TextView preview = new TextView(c);
            preview.setTextSize(13);
            Runnable changed = () -> {
                g[3] = Math.max(g[3], g[0]);                       // max never below min
                StringBuilder sb = new StringBuilder("Balls 1, 2, 3...: ");
                for (int i = 1; i <= 10; i++) sb.append(problems(g, i)).append(i < 10 ? ", " : " ...");
                preview.setText(sb);
                host.setGrowth(g.clone());
            };
            col.addView(stepper(c, "Minimum per ball", 1, 10, g, 0, changed));
            col.addView(stepper(c, "Start growing after ball", 0, 20, g, 1, changed));
            col.addView(stepper(c, "Extra problems per ball after that", 0, 5, g, 2, changed));
            col.addView(stepper(c, "Maximum per ball", 1, 30, g, 3, changed));
            col.addView(preview);
            preview.setText("");
            changed.run();
        }

        Button reset = new Button(c);
        reset.setText("Reset best score");
        reset.setOnClickListener(v -> { host.resetBest(); reset.setText("Best score reset"); });
        col.addView(reset);

        Button chpin = new Button(c);
        chpin.setText("Change PIN");
        chpin.setOnClickListener(v -> {
            d.setOnDismissListener(null);
            d.dismiss();
            pad(c, "New PIN", host, first -> {
                pad(c, "Repeat new PIN", host, second -> {
                    if (!second.equals(first)) return "PINs did not match";
                    prefs.edit().putString("pin", second).apply();
                    return null;
                });
                return null;
            });
        });
        col.addView(chpin);

        Button done = new Button(c);
        done.setText("Done");
        done.setOnClickListener(v -> d.dismiss());
        col.addView(done);

        ScrollView sv = new ScrollView(c);
        sv.addView(col);
        d.setContentView(sv, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        d.show();
    }

    private ParentsMenu() {}
}

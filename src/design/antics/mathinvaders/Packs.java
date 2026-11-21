package design.antics.mathinvaders;

import java.util.Random;

/**
 * Problem packs, ordered roughly by difficulty and organised around strategy
 * (partners, make-ten, doubles, crossing ten, place value, times tables) rather
 * than just bigger numbers. Parents toggle them; each problem comes from a
 * random enabled pack.
 *
 * Every number shown or asked for is a positive integer (>= 1), including
 * addends, sums, differences, factors and products.
 *
 * A pack makes an equation {a, b, c, op} meaning a op b = c, and a blank mode
 * picks which of the three numbers the child has to supply.
 */
final class Packs {
    static final String[] NAMES = {
        "Tiny addition", "Tiny subtraction", "One or two more/less", "Partners within 10",
        "Make ten", "Small doubles", "Teen numbers", "Larger doubles", "Near doubles",
        "Cross ten: addition", "Cross ten: subtraction", "Mixed within 20",
        "Tens and ones", "Two-digit, no regrouping", "Two-digit, regrouping", "Within 1000",
        "Multiply by 2", "Multiply by 5 and 10", "Multiply by 3", "Multiply by 4",
        "Multiply by 6 and 9", "Multiply by 7 and 8", "Mixed multiply and divide",
        "Subitizing: 1 to 5", "Subitizing: 5 to 10", "Subitizing: addition"};
    static final String[] EXAMPLES = {
        "2 + 1", "4 - 1", "6 + 1, 9 - 2", "3 + 4, 7 - 3, 3 + _ = 7",
        "6 + _ = 10, 10 - 6", "3 + 3, 5 + 5", "10 + 7, 17 - 7, 17 - 10", "6 + 6, 9 + 9", "6 + 7, 8 + 9",
        "8 + 5", "13 - 5", "8 + _ = 14, 15 - 7, _ - 3 = 9",
        "30 + 20, 64 - 10 style (multiples of ten)", "23 + 14, 47 - 25", "28 + 5, 42 - 7, 38 + 27", "sums and differences up to 1000",
        "2 x 7", "5 x 6, 10 x 4", "3 x 7", "4 x 6",
        "6 x 7, 9 x 7", "8 x 6, 7 x 6", "7 x _ = 42, 42 / 7",
        "how many dots? (1-5)", "how many dots? (5-10, ten-frame)", "dots + dots (each 1-5)"};
    static final int COUNT = NAMES.length;
    static final int ALL = (1 << COUNT) - 1;
    static final int DEFAULT_MASK = (1 << 3) | (1 << 11);   // partners within 10, mixed within 20

    private static final int SUBITIZE = 23;   // first subitizing pack
    private static final int ADD = 0, SUB = 1, MUL = 2, DIV = 3;
    private static final String[] OP = {"+", "-", "x", "÷"};

    // blank modes
    private static final int RESULT = 0, ANY = 1, TEN = 2;

    private static int rnd(Random r, int lo, int hi) { return lo + r.nextInt(hi - lo + 1); }
    private static int[] add(int a, int b) { return new int[] {a, b, a + b, ADD}; }
    private static int[] sub(int a, int b) { return new int[] {a, b, a - b, SUB}; }
    private static int[] mul(Random r, int a, int b) {
        return r.nextBoolean() ? new int[] {a, b, a * b, MUL} : new int[] {b, a, a * b, MUL};
    }

    /** Returns {question text, answer}. */
    static Object[] generate(int mask, Random r) {
        if ((mask & ALL) == 0) mask = DEFAULT_MASK;
        int p;
        do { p = r.nextInt(COUNT); } while ((mask & (1 << p)) == 0);

        if (p >= SUBITIZE) return subitize(p - SUBITIZE, r);

        int[] e;
        int mode = RESULT;
        boolean plus = r.nextBoolean();
        int a, b, n;
        switch (p) {
            case 0:  a = rnd(r, 1, 4); e = add(a, rnd(r, 1, 5 - a)); break;
            case 1:  a = rnd(r, 2, 5); e = sub(a, rnd(r, 1, a - 1)); break;
            case 2:  n = rnd(r, 1, 2);
                     e = plus ? add(rnd(r, 1, 10 - n), n) : sub(rnd(r, n + 1, 10), n); break;
            case 3:  mode = ANY; n = rnd(r, 2, 10); a = rnd(r, 1, n - 1);
                     e = plus ? add(a, n - a) : sub(n, a); break;
            case 4:  mode = TEN; a = rnd(r, 1, 9);
                     e = plus ? add(a, 10 - a) : sub(10, a); break;
            case 5:  n = rnd(r, 1, 5); e = add(n, n); break;
            case 6:  n = rnd(r, 1, 9);
                     switch (r.nextInt(3)) {
                         case 0:  e = add(10, n); break;
                         case 1:  e = sub(10 + n, n); break;
                         default: e = sub(10 + n, 10); break;
                     } break;
            case 7:  n = rnd(r, 1, 9); e = add(n, n); break;
            case 8:  n = rnd(r, 1, 8); e = add(n, Math.min(9, n + rnd(r, 1, 2))); break;
            case 9:  a = rnd(r, 2, 9); e = add(a, rnd(r, Math.max(2, 11 - a), 9)); break;
            case 10: a = rnd(r, 11, 18); e = sub(a, rnd(r, a - 9, 9)); break;
            case 11: mode = ANY;
                     if (plus) { a = rnd(r, 1, 19); e = add(a, rnd(r, 1, 20 - a)); }
                     else      { a = rnd(r, 2, 20); e = sub(a, rnd(r, 1, a - 1)); }
                     break;
            case 12: if (plus) { a = rnd(r, 1, 9); e = add(10 * a, 10 * rnd(r, 1, 10 - a)); }
                     else      { a = rnd(r, 2, 10); e = sub(10 * a, 10 * rnd(r, 1, a - 1)); }
                     break;
            case 13: if (plus) {
                         int o = rnd(r, 1, 8), t = rnd(r, 1, 8);
                         e = add(10 * t + o, 10 * rnd(r, 1, 9 - t) + rnd(r, 1, 9 - o));
                     } else {
                         int o = rnd(r, 1, 9), t = rnd(r, 2, 9);
                         e = sub(10 * t + o, 10 * rnd(r, 1, t - 1) + rnd(r, 1, o));
                     } break;
            case 14: if (plus) {          // ones sum >= 10, total <= 99
                         int o = rnd(r, 2, 9), t = rnd(r, 1, 7);
                         e = add(10 * t + o, 10 * rnd(r, 0, 8 - t) + rnd(r, 10 - o, 9));
                     } else {             // needs a borrow
                         int o = rnd(r, 0, 8), t = rnd(r, 2, 9);
                         e = sub(10 * t + o, 10 * rnd(r, 0, t - 1) + rnd(r, o + 1, 9));
                     } break;
            case 15: if (plus) { a = rnd(r, 1, 999); e = add(a, rnd(r, 1, 1000 - a)); }
                     else      { a = rnd(r, 2, 1000); e = sub(a, rnd(r, 1, a - 1)); }
                     break;
            case 16: e = mul(r, 2, rnd(r, 1, 10)); break;
            case 17: e = mul(r, plus ? 5 : 10, rnd(r, 1, 10)); break;
            case 18: e = mul(r, 3, rnd(r, 1, 10)); break;
            case 19: e = mul(r, 4, rnd(r, 1, 10)); break;
            case 20: e = mul(r, plus ? 6 : 9, rnd(r, 1, 10)); break;
            case 21: e = mul(r, plus ? 7 : 8, rnd(r, 1, 10)); break;
            default: mode = ANY; a = rnd(r, 2, 10); b = rnd(r, 2, 10);
                     e = plus ? mul(r, a, b) : new int[] {a * b, a, b, DIV}; break;
        }

        int blank = 2;                       // result by default
        if (mode == ANY && r.nextInt(5) >= 3) blank = r.nextInt(2);
        if (mode == TEN && e[3] == ADD) blank = 1;
        String[] t = {String.valueOf(e[0]), String.valueOf(e[1]), String.valueOf(e[2])};
        int answer = e[blank];
        t[blank] = "?";
        return new Object[] {t[0] + " " + OP[e[3]] + " " + t[1] + " = " + t[2], answer};
    }

    private static final String DOT = "\u25CF";

    private static String dots(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(DOT);
        return b.toString();
    }

    /**
     * Dot pictures for subitizing, laid out in rows (a ten-frame style: the top
     * row holds up to 5) so the quantity can be seen at a glance rather than counted.
     * Rows are separated by '\n'; a '\t' sets the "= ?" apart from the picture.
     */
    private static Object[] subitize(int kind, Random r) {
        if (kind == 2) {                                     // dots + dots, each 1-5, total <= 10
            int a = rnd(r, 1, 5), b = rnd(r, 1, Math.min(5, 10 - a));
            return new Object[] {dots(a) + " + " + dots(b) + "\t= ?", a + b};
        }
        int n = kind == 0 ? rnd(r, 1, 5) : rnd(r, 5, 10);
        int top = kind == 0 ? (n <= 3 ? n : (n + 1) / 2) : Math.min(5, n);
        String q = top == n ? dots(n) + "\t= ?" : dots(top) + "\n" + dots(n - top) + "\t= ?";
        return new Object[] {q, n};
    }

    private Packs() {}
}

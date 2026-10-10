package com.estiuk.studyxp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

/** Animated study pet: an egg first, then a fox / panda / cat / bunny that grows with the level. */
final class PetView extends View {
    private static final int DARK = 0xFF1E1B2E, WHITE = 0xFFFFFFFF, BLUSH = 0xFFFF8C96, NOSE = 0xFF4A3036;
    private static final int FOX_B = 0xFFF08A3C, FOX_BELLY = 0xFFFFF1DC, FOX_IN = 0xFFFFC9A0, FOX_TIP = 0xFFFFF6EA;
    private static final int PAN_B = 0xFFF4F4F0, PAN_K = 0xFF2A2A2E, PAN_BELLY = 0xFFFFFFFF;
    private static final int CAT_B = 0xFF9DA7B5, CAT_BELLY = 0xFFF1F4F8, CAT_IN = 0xFFF4B4BE, CAT_STR = 0xFF7C8798, CAT_WHISK = 0xFF6B7585;
    private static final int BUN_B = 0xFFF6E7D8, BUN_BELLY = 0xFFFFFAF4, BUN_IN = 0xFFFFB8C4;
    private static final int SCARF = 0xFFE2574C, LEAF = 0xFF5BD38A, LEAF2 = 0xFF2E9E62, GOLD = 0xFFF5C84C, GOLD2 = 0xFFD99A1F, GLASS = 0xFF3A3A4A;
    private static final int EGG = 0xFFFFF3D6, EGG_SP = 0xFF8FD6A6, BOOK = 0xFFFDF6E3, BOOK2 = 0xFF3F8F63, HEART = 0xFFFF6B8A;

    private static final int PEX_B = 0xFFB4EACD, PEX_LIGHT = 0xFFDDF8EA, PEX_SHADE = 0xFF86CFA8;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rf = new RectF();
    private final float d;
    private final long t0 = SystemClock.uptimeMillis();
    private final ArrayList<float[]> hearts = new ArrayList<>();   // {xUnits, startMs}
    private final Random rnd = new Random();
    private Canvas cv;

    private int type = 0, level = 0, mood = 1;
    private boolean egg = true, focusing = false;
    private float eggProg = 0f;
    private String say = "";
    private long tapAt = -100000;
    private Runnable onTap;
    private int bubbleBg = 0xFFFFFFFF, bubbleFg = 0xFF12261A;

    PetView(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        tp.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tp.setTextAlign(Paint.Align.LEFT);
    }

    void configure(int type, int level, boolean egg, float eggProg, int mood, boolean focusing, String say) {
        this.type = type; this.level = level; this.egg = egg; this.eggProg = eggProg;
        this.mood = mood; this.focusing = focusing; this.say = say == null ? "" : say;
        invalidate();
    }

    void setBubbleColors(int bg, int fg) { bubbleBg = bg; bubbleFg = fg; }
    void setSay(String s) { say = s == null ? "" : s; invalidate(); }
    void setOnPetTap(Runnable r) { onTap = r; }

    // ------------------------------------------------------------ drawing helpers (units; origin = ground centre)
    private void fillColor(int color, int alpha) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        p.setAlpha(alpha * Color.alpha(color) / 255);
    }

    private void oval(float cx, float cy, float rx, float ry, int color) { oval(cx, cy, rx, ry, color, 255); }

    private void oval(float cx, float cy, float rx, float ry, int color, int alpha) {
        fillColor(color, alpha);
        rf.set(cx - rx, cy - ry, cx + rx, cy + ry);
        cv.drawOval(rf, p);
    }

    private void circle(float cx, float cy, float r, int color) { oval(cx, cy, r, r, color, 255); }
    private void circle(float cx, float cy, float r, int color, int alpha) { oval(cx, cy, r, r, color, alpha); }

    private void rotOval(float cx, float cy, float rx, float ry, float deg, int color) {
        cv.save();
        cv.rotate(deg, cx, cy);
        oval(cx, cy, rx, ry, color, 255);
        cv.restore();
    }

    private void poly(int color, float... pts) {
        fillColor(color, 255);
        path.reset();
        path.moveTo(pts[0], pts[1]);
        for (int i = 2; i < pts.length; i += 2) path.lineTo(pts[i], pts[i + 1]);
        path.close();
        cv.drawPath(path, p);
    }

    private void line(float x1, float y1, float x2, float y2, float w, int color) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(w);
        p.setColor(color);
        p.setAlpha(Color.alpha(color));
        cv.drawLine(x1, y1, x2, y2, p);
    }

    private void arc(float cx, float cy, float r, float start, float sweep, float w, int color) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(w);
        p.setColor(color);
        p.setAlpha(Color.alpha(color));
        rf.set(cx - r, cy - r, cx + r, cy + r);
        cv.drawArc(rf, start, sweep, false, p);
    }

    private void ring(float cx, float cy, float r, float w, int color) { arc(cx, cy, r, 0, 360, w, color); }

    // ------------------------------------------------------------ pet body
    /** The app mascot: a soft mint blob with a leaf sprout, big shiny eyes and rosy cheeks. */
    private void drawPexumi(float bob, float wag, float blink) {
        float hy = -40 + bob;
        float ex = 11.5f;
        if (level >= 5) for (int i = 0; i < 6; i++) circle(0, -40 + bob, 72 - i * 9, GOLD, 14);
        oval(0, 0, 31, 5, 0xFF000000, 55);

        // feet + body
        oval(-14, -3, 10.5f, 6, PEX_SHADE);
        oval(14, -3, 10.5f, 6, PEX_SHADE);
        oval(0, -30 + bob * 0.5f, 31, 30, PEX_SHADE);
        oval(0, -31 + bob * 0.5f, 30, 29.5f, PEX_B);
        oval(0, -20 + bob * 0.5f, 19, 15, PEX_LIGHT, 170);
        oval(-14, -52 + bob, 9, 5.5f, WHITE, 110);

        // little arms
        rotOval(-29, -26 + bob * 0.6f, 6.5f, 10, 22, PEX_B);
        rotOval(29, -26 + bob * 0.6f, 6.5f, 10, -22, PEX_B);

        // eyes
        boolean closed = mood == 0 && !focusing;
        boolean happy = mood == 3 && !focusing;
        if (closed) {
            arc(-ex, hy - 1, 4.6f, 20, 140, 2.2f, DARK);
            arc(ex, hy - 1, 4.6f, 20, 140, 2.2f, DARK);
        } else if (happy) {
            arc(-ex, hy + 1, 4.8f, 200, 140, 2.4f, DARK);
            arc(ex, hy + 1, 4.8f, 200, 140, 2.4f, DARK);
        } else {
            float hh = Math.max(0.7f, 6.4f * blink);
            oval(-ex, hy, 5.4f, hh, DARK);
            oval(ex, hy, 5.4f, hh, DARK);
            if (blink > 0.5f) {
                circle(-ex - 1.8f, hy - 2.6f, 1.9f, WHITE);
                circle(ex - 1.8f, hy - 2.6f, 1.9f, WHITE);
                circle(-ex + 1.8f, hy + 2.2f, 0.9f, WHITE);
                circle(ex + 1.8f, hy + 2.2f, 0.9f, WHITE);
            }
        }
        oval(-21, hy + 9, 6.4f, 3.8f, BLUSH, 135);
        oval(21, hy + 9, 6.4f, 3.8f, BLUSH, 135);
        if (happy || mood >= 2) {
            oval(0, hy + 10, 3.6f, 3f, 0xFF7A2E3A);
            oval(0, hy + 11.2f, 2.1f, 1.3f, 0xFFFF8FA0);
        } else if (closed) {
            arc(0, hy + 8.5f, 2.4f, 20, 140, 1.7f, DARK);
        } else {
            oval(0, hy + 9.5f, 2.2f, 1.9f, 0xFF7A2E3A);
        }

        // sprout
        float sway = wag * 0.6f;
        line(0, hy - 19, 0, hy - 27, 2.2f, LEAF2);
        rotOval(-8, hy - 30, 9.5f, 4.6f, -22 - sway, LEAF);
        rotOval(8, hy - 31, 9.5f, 4.6f, 22 + sway, LEAF2);

        // graduation cap (level 4+) like on the mascot
        if (level >= 4) {
            poly(GOLD2, 3, hy - 22, 16, hy - 27, 30, hy - 22, 17, hy - 17);
            poly(GOLD, 3, hy - 24, 16, hy - 29, 30, hy - 24, 17, hy - 19);
            oval(16.5f, hy - 18.5f, 8, 3.3f, GOLD2);
            circle(16.5f, hy - 25, 1.4f, 0xFFFFF4C2);
            line(30, hy - 24, 31, hy - 14, 1.3f, GOLD2);
            circle(31, hy - 13, 1.8f, GOLD);
        }
        if (level >= 3) {
            poly(0xFFFFE08A, -34, hy - 8, -31, hy - 14, -28, hy - 8, -31, hy - 2);
        }
        if (level >= 6) {
            poly(0xFFFFE08A, 36, hy - 34, 39, hy - 41, 42, hy - 34, 39, hy - 27);
        }
        if (focusing) {
            poly(BOOK2, -17, -25 + bob, 0, -21 + bob, 17, -25 + bob, 17, -9 + bob, 0, -5 + bob, -17, -9 + bob);
            poly(BOOK, -15, -24 + bob, 0, -20.5f + bob, 15, -24 + bob, 15, -11 + bob, 0, -7.5f + bob, -15, -11 + bob);
            line(0, -20.5f + bob, 0, -7.5f + bob, 1.2f, BOOK2);
        }
    }

    private void drawPet(float bob, float wag, float blink) {
        if (type == 4) { drawPexumi(bob, wag, blink); return; }
        int body, belly, inner;
        switch (type) {
            case 0: body = FOX_B; belly = FOX_BELLY; inner = FOX_IN; break;
            case 1: body = PAN_B; belly = PAN_BELLY; inner = PAN_K; break;
            case 2: body = CAT_B; belly = CAT_BELLY; inner = CAT_IN; break;
            default: body = BUN_B; belly = BUN_BELLY; inner = BUN_IN; break;
        }
        int limb = type == 1 ? PAN_K : body;
        float hy = -58 + bob;
        float ex = 12.5f;

        if (level >= 5) for (int i = 0; i < 6; i++) circle(0, -48 + bob, 70 - i * 9, GOLD, 14);
        oval(0, 0, 30, 5, 0xFF000000, 55);

        // tail
        if (type == 0) {
            float ang = -35 + wag;
            rotOval(31, -26 + bob, 18, 9, ang, body);
            double r = Math.toRadians(ang);
            circle((float) (31 + 13 * Math.cos(r)), (float) (-26 + bob + 13 * Math.sin(r)), 6.5f, FOX_TIP);
        } else if (type == 2) {
            float ang = -62 + wag;
            rotOval(30, -26 + bob, 17, 5.5f, ang, body);
            double r = Math.toRadians(ang);
            circle((float) (30 + 13 * Math.cos(r)), (float) (-26 + bob + 13 * Math.sin(r)), 5.5f, CAT_STR);
        } else if (type == 3) {
            circle(25, -14 + bob, 8, WHITE);
        }

        // ears
        if (type == 0) {
            poly(body, -31, hy + 2, -27, hy - 44, -6, hy - 24);
            poly(body, 31, hy + 2, 27, hy - 44, 6, hy - 24);
            poly(inner, -26, hy - 6, -25, hy - 36, -12, hy - 24);
            poly(inner, 26, hy - 6, 25, hy - 36, 12, hy - 24);
        } else if (type == 2) {
            poly(body, -31, hy, -27, hy - 40, -5, hy - 22);
            poly(body, 31, hy, 27, hy - 40, 5, hy - 22);
            poly(inner, -26, hy - 6, -25, hy - 31, -11, hy - 21);
            poly(inner, 26, hy - 6, 25, hy - 31, 11, hy - 21);
        } else if (type == 1) {
            circle(-25, hy - 23, 10, PAN_K);
            circle(25, hy - 23, 10, PAN_K);
        } else {
            rotOval(-15, hy - 34, 8.5f, 23, -8, body);
            rotOval(15, hy - 34, 8.5f, 23, 8, body);
            rotOval(-15, hy - 33, 4.4f, 17, -8, inner);
            rotOval(15, hy - 33, 4.4f, 17, 8, inner);
        }

        // body, feet
        oval(0, -24 + bob * 0.5f, 26, 23, body);
        oval(0, -22 + bob * 0.5f, 16, 16, belly);
        oval(-13, -3, 10, 5.5f, limb);
        oval(13, -3, 10, 5.5f, limb);

        if (level >= 3) {
            oval(0, -33 + bob, 23, 6.5f, SCARF);
            rotOval(15, -26 + bob, 4.5f, 10, -12, SCARF);
        }
        rotOval(-25, -30 + bob * 0.6f, 6, 11, 20, limb);
        rotOval(25, -30 + bob * 0.6f, 6, 11, -20, limb);

        // head
        oval(0, hy, 33, 29, body);
        if (type == 0) oval(0, hy + 10, 23, 13, FOX_TIP);
        if (type == 2) {
            line(-3, hy - 27, -3, hy - 19, 2.2f, CAT_STR);
            line(3, hy - 27, 3, hy - 19, 2.2f, CAT_STR);
            line(-10, hy - 26, -9, hy - 20, 2.0f, CAT_STR);
            line(10, hy - 26, 9, hy - 20, 2.0f, CAT_STR);
        }
        if (type == 1) {
            rotOval(-ex, hy, 8.6f, 10.8f, -22, PAN_K);
            rotOval(ex, hy, 8.6f, 10.8f, 22, PAN_K);
        }

        // eyes
        boolean closed = mood == 0 && !focusing;
        boolean happy = mood == 3 && !focusing;
        int lid = type == 1 ? WHITE : DARK;
        if (closed) {
            arc(-ex, hy - 1, 4.2f, 20, 140, 2.0f, lid);
            arc(ex, hy - 1, 4.2f, 20, 140, 2.0f, lid);
        } else if (happy) {
            arc(-ex, hy + 1, 4.5f, 200, 140, 2.2f, lid);
            arc(ex, hy + 1, 4.5f, 200, 140, 2.2f, lid);
        } else {
            float hh = Math.max(0.6f, 5.2f * blink);
            if (type == 1) {
                oval(-ex, hy, 4.3f, hh, WHITE);
                oval(ex, hy, 4.3f, hh, WHITE);
                oval(-ex, hy + 0.5f, 2.5f, Math.max(0.5f, hh * 0.55f), DARK);
                oval(ex, hy + 0.5f, 2.5f, Math.max(0.5f, hh * 0.55f), DARK);
            } else {
                oval(-ex, hy, 4.3f, hh, DARK);
                oval(ex, hy, 4.3f, hh, DARK);
            }
            if (blink > 0.5f) {
                circle(-ex - 1.3f, hy - 2.1f, 1.5f, WHITE);
                circle(ex - 1.3f, hy - 2.1f, 1.5f, WHITE);
            }
        }

        // cheeks, nose, mouth
        oval(-23, hy + 9, 6.2f, 3.6f, BLUSH, 115);
        oval(23, hy + 9, 6.2f, 3.6f, BLUSH, 115);
        oval(0, hy + 7, 3.3f, 2.4f, type == 1 ? PAN_K : NOSE);
        if (happy) {
            oval(0, hy + 13.5f, 4.6f, 3.6f, 0xFF7A2E3A);
            oval(0, hy + 15, 2.6f, 1.6f, 0xFFFF8FA0);
        } else {
            int mc = type == 1 ? PAN_K : DARK;
            arc(-3.2f, hy + 9.5f, 3.4f, 10, 150, 1.7f, mc);
            arc(3.2f, hy + 9.5f, 3.4f, 20, 150, 1.7f, mc);
        }
        if (type == 2) {
            line(-27, hy + 6, -38, hy + 3, 1.1f, CAT_WHISK);
            line(-27, hy + 9, -38, hy + 10, 1.1f, CAT_WHISK);
            line(27, hy + 6, 38, hy + 3, 1.1f, CAT_WHISK);
            line(27, hy + 9, 38, hy + 10, 1.1f, CAT_WHISK);
        }

        // accessories
        if (level >= 4 || focusing) {
            ring(-ex, hy, 8.6f, 1.7f, GLASS);
            ring(ex, hy, 8.6f, 1.7f, GLASS);
            line(-4, hy - 1, 4, hy - 1, 1.6f, GLASS);
        }
        if (level >= 2) {
            float top = hy - 28;
            rotOval(0, top - 4, 4, 9, -20, LEAF);
            rotOval(6, top - 2, 3, 7, 40, LEAF2);
        }
        if (level >= 6) {
            poly(GOLD, -12, hy - 25, -13, hy - 39, -6.5f, hy - 32, 0, hy - 42, 6.5f, hy - 32, 13, hy - 39, 12, hy - 25);
            poly(GOLD2, -12, hy - 25, 12, hy - 25, 12, hy - 28, -12, hy - 28);
            circle(0, hy - 42, 2, 0xFFFFF4C2);
            circle(-13, hy - 39, 1.8f, 0xFFFFF4C2);
            circle(13, hy - 39, 1.8f, 0xFFFFF4C2);
        }
        if (focusing) {
            poly(BOOK2, -17, -25 + bob, 0, -21 + bob, 17, -25 + bob, 17, -9 + bob, 0, -5 + bob, -17, -9 + bob);
            poly(BOOK, -15, -24 + bob, 0, -20.5f + bob, 15, -24 + bob, 15, -11 + bob, 0, -7.5f + bob, -15, -11 + bob);
            line(0, -20.5f + bob, 0, -7.5f + bob, 1.2f, BOOK2);
        }
    }

    private void drawEgg(float wob) {
        oval(0, 0, 26, 5, 0xFF000000, 55);
        if (eggProg > 0.5f) for (int i = 0; i < 4; i++) circle(0, -33, 46 - i * 8, GOLD, 12);
        rotOval(0, -33, 25, 33, wob, EGG);
        circle(-9, -36, 4, EGG_SP);
        circle(9, -26, 3.4f, EGG_SP);
        circle(2, -52, 3, EGG_SP);
        circle(-3, -18, 2.6f, EGG_SP);
        oval(-9, -46, 4.5f, 9, WHITE, 150);
        float[][] pts = {{-14, -44}, {-8, -38}, {-3, -45}, {3, -37}, {9, -44}, {14, -38}};
        int n = Math.min(5, (int) (eggProg * 6));
        for (int i = 0; i < n; i++) line(pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], 1.8f, 0xFF7A5A3A);
    }

    // ------------------------------------------------------------ frame
    @Override protected void onDraw(Canvas c) {
        try { draw0(c); } catch (Throwable t) { App.report(t); }
    }

    private void draw0(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        long now = SystemClock.uptimeMillis();
        float t = (now - t0) / 1000f;
        boolean hasBubble = !say.isEmpty();
        float bubbleH = hasBubble ? 50 * d : 0;
        float areaH = h - bubbleH - 4 * d;
        float grow = egg ? 1.15f : 0.68f + 0.053f * Math.max(0, Math.min(6, level));
        float u = Math.min(w / 96f, areaH / 128f) * grow;
        float ox = w / 2f, oy = h - 4 * d;

        // jump / tap reaction
        float k = (now - tapAt) / 650f;
        boolean tapping = k >= 0 && k < 1f;
        float jump = tapping ? (float) (-24 * Math.sin(Math.PI * k)) : 0f;

        cv = c;
        c.save();
        c.translate(ox, oy);
        c.scale(u, u);
        if (egg) {
            float wob = (float) (4 * Math.sin(t * 2.4) * (0.4 + eggProg));
            if (tapping) wob += (float) (11 * Math.sin(t * 28) * (1f - k));
            drawEgg(wob);
        } else {
            boolean lively = mood >= 2 && !focusing;
            float bob = lively ? (float) (-2.4 * Math.abs(Math.sin(t * 3.2))) : (float) (1.1 * Math.sin(t * 1.7));
            if (mood == 0) bob = (float) (1.6 * Math.sin(t * 1.1));
            bob += jump;
            float wag = (float) ((type == 0 ? 13 : 8) * Math.sin(t * (lively ? 7 : 3)));
            float ph = t % 3.7f;
            float blink = ph < 0.16f ? Math.abs(ph - 0.08f) / 0.08f : 1f;
            drawPet(bob, wag, blink);

            if (mood == 0 && !focusing) {
                tp.setTextSize(11);
                for (int i = 0; i < 3; i++) {
                    float ph2 = ((t * 0.5f) + i / 3f) % 1f;
                    tp.setColor(Color.WHITE);
                    tp.setAlpha((int) (200 * (1f - ph2)));
                    tp.setTextSize(7 + 5 * ph2);
                    c.drawText(i == 0 ? "z" : "Z", 24 + ph2 * 12, -92 - ph2 * 26, tp);
                }
            }
            if (mood == 3 && !focusing) {
                for (int i = 0; i < 4; i++) {
                    float tw = (float) (0.5 + 0.5 * Math.sin(t * 3 + i * 1.7));
                    float sx = new float[]{-44, 46, -36, 40}[i], sy = new float[]{-96, -86, -34, -48}[i];
                    float r = 2.5f + 3.5f * tw;
                    poly(0xFFFFE9A0, sx, sy - r, sx + r * 0.25f, sy - r * 0.25f, sx + r, sy, sx + r * 0.25f, sy + r * 0.25f,
                            sx, sy + r, sx - r * 0.25f, sy + r * 0.25f, sx - r, sy, sx - r * 0.25f, sy - r * 0.25f);
                }
            }
        }
        // hearts from taps
        for (int i = hearts.size() - 1; i >= 0; i--) {
            float[] hv = hearts.get(i);
            float age = (now - (long) hv[1]) / 1200f;
            if (age >= 1f) { hearts.remove(i); continue; }
            float hx = hv[0] + (float) (4 * Math.sin(age * 9));
            float hyy = -92 - age * 44;
            int a = (int) (255 * (1f - age));
            fillColor(HEART, a);
            circle(hx - 3.2f, hyy, 3.6f, HEART, a);
            circle(hx + 3.2f, hyy, 3.6f, HEART, a);
            fillColor(HEART, a);
            path.reset();
            path.moveTo(hx - 6.6f, hyy + 1.2f);
            path.lineTo(hx + 6.6f, hyy + 1.2f);
            path.lineTo(hx, hyy + 9f);
            path.close();
            c.drawPath(path, p);
        }
        c.restore();

        if (hasBubble) drawBubble(c, w, bubbleH);
        postInvalidateDelayed(40);
    }

    private void drawBubble(Canvas c, float w, float bh) {
        tp.setTextSize(10.5f * d);
        tp.setColor(bubbleFg);
        tp.setAlpha(255);
        float maxW = w - 8 * d - 16 * d;
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : say.split(" ")) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (tp.measureText(test) > maxW && cur.length() > 0) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(test);
            }
            if (lines.size() >= 3) break;
        }
        if (cur.length() > 0 && lines.size() < 3) lines.add(cur.toString());
        float lh = 13.5f * d;
        float boxH = lines.size() * lh + 12 * d;
        float top = Math.max(0, bh - 8 * d - boxH);
        rf.set(4 * d, top, w - 4 * d, top + boxH);
        p.setStyle(Paint.Style.FILL);
        p.setColor(bubbleBg);
        p.setAlpha(240);
        c.drawRoundRect(rf, 14 * d, 14 * d, p);
        path.reset();
        float tx = w / 2f;
        path.moveTo(tx - 6 * d, top + boxH - 1);
        path.lineTo(tx + 6 * d, top + boxH - 1);
        path.lineTo(tx, top + boxH + 7 * d);
        path.close();
        c.drawPath(path, p);
        tp.setColor(bubbleFg);
        for (int i = 0; i < lines.size(); i++) {
            c.drawText(lines.get(i), 4 * d + 8 * d, top + 6 * d + lh * (i + 0.8f), tp);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (e.getAction() == MotionEvent.ACTION_UP) {
            long now = SystemClock.uptimeMillis();
            tapAt = now;
            hearts.add(new float[]{(rnd.nextFloat() - 0.5f) * 30f, now});
            if (hearts.size() > 8) hearts.remove(0);
            if (onTap != null) onTap.run();
            invalidate();
            performClick();
            return true;
        }
        return super.onTouchEvent(e);
    }

    @Override public boolean performClick() { return super.performClick(); }
}

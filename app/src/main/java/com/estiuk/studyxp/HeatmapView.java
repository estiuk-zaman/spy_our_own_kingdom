package com.estiuk.studyxp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;

/** GitHub-style calendar heatmap: columns are weeks (newest on the right), rows are weekdays. */
final class HeatmapView extends View {
    interface Cb { void on(int dayOffset); }

    static final int WEEKS = 15;
    private final Paint cell = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private int[] net = new int[WEEKS * 7 + 7];   // index = offset + (len-1) where offset <= 0
    private int todayRow;
    private int cTrack, cAccent, cGold;
    private Cb cb;
    private final float d;
    private float cs, gap, lx;

    HeatmapView(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(1.6f * d);
        todayRow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
    }

    /** nets[i] = net XP for the day (i - (len-1)) days from today. */
    void setData(int[] nets, int track, int accent, int gold, Cb cb) {
        this.net = nets; cTrack = track; cAccent = accent; cGold = gold; this.cb = cb;
        invalidate();
    }

    private int offsetFor(int col, int row) { return (col - (WEEKS - 1)) * 7 + (row - todayRow); }

    @Override protected void onDraw(Canvas c) {
        try { draw0(c); } catch (Throwable t) { App.report(t); }
    }

    private void draw0(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        gap = 3 * d;
        cs = Math.min((w - gap * (WEEKS - 1)) / WEEKS, (h - gap * 6) / 7f);
        lx = (w - (cs * WEEKS + gap * (WEEKS - 1))) / 2f;
        for (int col = 0; col < WEEKS; col++) {
            for (int row = 0; row < 7; row++) {
                int off = offsetFor(col, row);
                if (off > 0) continue;
                int idx = net.length - 1 + off;
                int v = idx >= 0 && idx < net.length ? net[idx] : 0;
                float x = lx + col * (cs + gap), y = row * (cs + gap);
                cell.setColor(v <= 0 ? cTrack : cAccent);
                cell.setAlpha(v <= 0 ? 255 : v < 20 ? 80 : v < 50 ? 140 : v < 100 ? 205 : 255);
                rf.set(x, y, x + cs, y + cs);
                c.drawRoundRect(rf, cs * 0.28f, cs * 0.28f, cell);
                if (off == 0) {
                    ring.setColor(cGold);
                    c.drawRoundRect(rf, cs * 0.28f, cs * 0.28f, ring);
                }
            }
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP && cb != null && cs > 0) {
            int col = (int) ((e.getX() - lx) / (cs + gap));
            int row = (int) (e.getY() / (cs + gap));
            if (col >= 0 && col < WEEKS && row >= 0 && row < 7) {
                int off = offsetFor(col, row);
                if (off <= 0) cb.on(off);
            }
            performClick();
        }
        return true;
    }

    @Override public boolean performClick() { return super.performClick(); }
}

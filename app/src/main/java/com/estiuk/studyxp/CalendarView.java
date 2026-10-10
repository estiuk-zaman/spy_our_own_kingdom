package com.estiuk.studyxp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/** One month as a grid. Each day is shaded by how much XP was earned; tap a day to inspect it. */
final class CalendarView extends View {
    interface Cb { void on(int day); }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private final float d;
    private String[] heads = {"S", "M", "T", "W", "T", "F", "S"};
    private int days = 30, firstDow = 0, sel = -1, today = -1, future = 99;
    private int[] net = new int[31];
    private int cTrack, cAccent, cGold, cText, cDim, cOnAccent;
    private Cb cb;
    private float cs, headH;

    CalendarView(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        txt.setTextAlign(Paint.Align.CENTER);
        txt.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    /** nets[i] = net XP of day i+1. futureFrom = first day number that lies in the future (use 99 for none). */
    void setData(String[] heads, int daysInMonth, int firstDayOfWeek, int[] nets, int selDay, int todayDay, int futureFrom,
                 int track, int accent, int gold, int text, int dim, int onAccent, Cb cb) {
        this.heads = heads; days = daysInMonth; firstDow = firstDayOfWeek; net = nets; sel = selDay; today = todayDay; future = futureFrom;
        cTrack = track; cAccent = accent; cGold = gold; cText = text; cDim = dim; cOnAccent = onAccent; this.cb = cb;
        invalidate();
    }

    @Override protected void onMeasure(int wm, int hm) {
        int w = MeasureSpec.getSize(wm);
        float cell = w / 7f;
        setMeasuredDimension(w, (int) (26 * d + 6 * cell));
    }

    @Override protected void onDraw(Canvas c) {
        try { draw0(c); } catch (Throwable t) { App.report(t); }
    }

    private void draw0(Canvas c) {
        float w = getWidth();
        if (w <= 0) return;
        cs = w / 7f;
        headH = 26 * d;
        txt.setTextSize(11 * d);
        txt.setColor(cDim);
        for (int i = 0; i < 7; i++) c.drawText(heads[i], cs * (i + 0.5f), headH * 0.7f, txt);
        for (int day = 1; day <= days; day++) {
            int idx = firstDow + day - 1;
            int col = idx % 7, row = idx / 7;
            float x = col * cs, y = headH + row * cs;
            rf.set(x + 2 * d, y + 2 * d, x + cs - 2 * d, y + cs - 2 * d);
            int n = net[day - 1];
            boolean fut = day >= future;
            int color, a;
            boolean strong = false;
            if (fut) { color = cTrack; a = 28; }
            else if (n <= 0) { color = cTrack; a = 100; }
            else if (n < 20) { color = cAccent; a = 70; }
            else if (n < 50) { color = cAccent; a = 130; }
            else if (n < 100) { color = cAccent; a = 205; strong = true; }
            else { color = cGold; a = 235; strong = true; }
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(color);
            fill.setAlpha(a);
            c.drawRoundRect(rf, 9 * d, 9 * d, fill);
            if (day == today) {
                fill.setStyle(Paint.Style.STROKE);
                fill.setStrokeWidth(1.6f * d);
                fill.setColor(cGold);
                fill.setAlpha(255);
                c.drawRoundRect(rf, 9 * d, 9 * d, fill);
            }
            if (day == sel) {
                fill.setStyle(Paint.Style.STROKE);
                fill.setStrokeWidth(2.2f * d);
                fill.setColor(cText);
                fill.setAlpha(255);
                rf.inset(-0.5f * d, -0.5f * d);
                c.drawRoundRect(rf, 9 * d, 9 * d, fill);
            }
            int tc = strong ? cOnAccent : cText;
            txt.setColor(tc);
            txt.setAlpha(fut ? 90 : 255);
            txt.setTextSize(13 * d);
            c.drawText(String.valueOf(day), x + cs / 2f, y + cs * (n > 0 && !fut ? 0.47f : 0.60f), txt);
            if (n > 0 && !fut) {
                txt.setTextSize(8.5f * d);
                txt.setAlpha(210);
                c.drawText(String.valueOf(n), x + cs / 2f, y + cs * 0.78f, txt);
            }
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (e.getAction() == MotionEvent.ACTION_UP && cb != null && cs > 0) {
            int col = (int) (e.getX() / cs);
            int row = (int) ((e.getY() - headH) / cs);
            int day = row * 7 + col - firstDow + 1;
            if (e.getY() >= headH && col >= 0 && col < 7 && day >= 1 && day <= days) cb.on(day);
            performClick();
            return true;
        }
        return super.onTouchEvent(e);
    }

    @Override public boolean performClick() { return super.performClick(); }
}

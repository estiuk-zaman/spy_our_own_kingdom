package com.estiuk.studyxp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/** Smooth area line chart of recent daily XP (oldest on the left, today on the right). */
final class TrendView extends View {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint area = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path fillPath = new Path();
    private final float d;
    private int[] v = new int[30];
    private String[] xl = {"", "", ""};
    private int cAccent, cTrack, cDim, cText, cGold;

    TrendView(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        txt.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    void setData(int[] values, String[] xLabels, int accent, int track, int dim, int text, int gold) {
        v = values; xl = xLabels; cAccent = accent; cTrack = track; cDim = dim; cText = text; cGold = gold;
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        try { draw0(c); } catch (Throwable t) { App.report(t); }
    }

    private void draw0(Canvas c) {
        float w = getWidth(), h = getHeight();
        int n = v.length;
        if (w <= 0 || h <= 0 || n < 2) return;
        float l = 6 * d, r = w - 10 * d, top = 22 * d, bot = h - 24 * d;
        int max = 20;
        for (int x : v) max = Math.max(max, x);
        // grid
        line.setStrokeWidth(1 * d);
        line.setColor(cTrack);
        for (int i = 0; i <= 2; i++) {
            float y = bot - (bot - top) * i / 2f;
            c.drawLine(l, y, r, y, line);
        }
        float[] xs = new float[n], ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = l + (r - l) * i / (n - 1f);
            ys[i] = bot - (bot - top) * Math.max(0, v[i]) / (float) max;
        }
        path.reset();
        path.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            float mx = (xs[i - 1] + xs[i]) / 2f;
            path.cubicTo(mx, ys[i - 1], mx, ys[i], xs[i], ys[i]);
        }
        fillPath.set(path);
        fillPath.lineTo(xs[n - 1], bot);
        fillPath.lineTo(xs[0], bot);
        fillPath.close();
        area.setStyle(Paint.Style.FILL);
        area.setShader(new LinearGradient(0, top, 0, bot, (cAccent & 0x00FFFFFF) | 0x80000000, cAccent & 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawPath(fillPath, area);
        line.setColor(cAccent);
        line.setStrokeWidth(2.4f * d);
        c.drawPath(path, line);
        // dots for active days, big dot for today
        dot.setStyle(Paint.Style.FILL);
        for (int i = 0; i < n; i++) {
            if (v[i] <= 0 && i != n - 1) continue;
            dot.setColor(i == n - 1 ? cGold : cAccent);
            c.drawCircle(xs[i], ys[i], (i == n - 1 ? 4.2f : 2.2f) * d, dot);
        }
        txt.setTextSize(11 * d);
        txt.setColor(cDim);
        txt.setTextAlign(Paint.Align.LEFT);
        c.drawText("max " + max, l, 13 * d, txt);
        txt.setColor(cText);
        txt.setTextAlign(Paint.Align.RIGHT);
        c.drawText(String.valueOf(v[n - 1]), r, Math.max(13 * d, ys[n - 1] - 9 * d), txt);
        txt.setColor(cDim);
        txt.setTextAlign(Paint.Align.LEFT);
        c.drawText(xl[0], l, h - 6 * d, txt);
        txt.setTextAlign(Paint.Align.CENTER);
        c.drawText(xl[1], (l + r) / 2f, h - 6 * d, txt);
        txt.setTextAlign(Paint.Align.RIGHT);
        c.drawText(xl[2], r, h - 6 * d, txt);
    }
}

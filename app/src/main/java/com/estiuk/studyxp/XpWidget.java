package com.estiuk.studyxp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.Locale;

/** Home-screen widget: level, XP, streak and today's progress. */
public class XpWidget extends AppWidgetProvider {

    @Override public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        try {
            for (int id : ids) mgr.updateAppWidget(id, build(ctx));
        } catch (Throwable t) { App.report(t); }
    }

    static RemoteViews build(Context c) {
        Store s = new Store(c);
        Lang.bn = s.bn();
        s.rollDay();
        s.sync();
        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_xp);
        int lv = s.level();
        rv.setTextViewText(R.id.w_level, Lang.t("Level ", "লেভেল ") + (lv + 1) + " · " + Store.levelName(lv));
        rv.setTextViewText(R.id.w_xp, String.format(Locale.US, "%,d XP   🔥 %d", s.totalXp(), s.streak()));
        rv.setProgressBar(R.id.w_bar, 100, Math.round(s.levelProgress() * 100), false);
        rv.setTextViewText(R.id.w_today, Lang.t("Today ", "আজ ") + Math.max(0, s.net()) + " / " + Store.STUDY_DAY_XP + " XP");
        Intent i = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        rv.setOnClickPendingIntent(R.id.w_root, PendingIntent.getActivity(c, 0, i, (android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0) | PendingIntent.FLAG_UPDATE_CURRENT));
        return rv;
    }

    static void refresh(Context c) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] ids = m.getAppWidgetIds(new ComponentName(c, XpWidget.class));
            if (ids != null && ids.length > 0) {
                RemoteViews rv = build(c);
                for (int id : ids) m.updateAppWidget(id, rv);
            }
        } catch (Throwable ignored) {
        }
    }
}

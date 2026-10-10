package com.estiuk.studyxp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/**
 * Runs only while a focus session is running. Fires pomodoro / goal alerts and the
 * Focus Guard warning when a social app is opened. Everything stays on the phone.
 */
public class FocusService extends Service {
    static final int ID = 71;
    static final String CH_FOCUS = "focus", CH_ALERT = "alerts";

    private Store st;
    private final Handler h = new Handler(Looper.getMainLooper());
    private long lastStrikeAt = 0, lastNotifAt = 0;
    private String lastKey = "";
    private int alertId = 100;

    private final Runnable loop = new Runnable() {
        @Override public void run() {
            try { tick(); } catch (Throwable t) { App.report(t); }
        }
    };

    static void start(Context c) {
        try {
            Intent i = new Intent(c, FocusService.class);
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
        } catch (Throwable t) { App.report(t); }
    }

    static void stop(Context c) {
        try { c.stopService(new Intent(c, FocusService.class)); } catch (Throwable ignored) { }
    }

    @Override public void onCreate() {
        super.onCreate();
        st = new Store(this);
        Lang.bn = st.bn();
        if (Build.VERSION.SDK_INT < 26) return;
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(CH_FOCUS, Lang.t("Focus session", "ফোকাস সেশন"), NotificationManager.IMPORTANCE_LOW));
            NotificationChannel a = new NotificationChannel(CH_ALERT, Lang.t("Alerts", "অ্যালার্ট"), NotificationManager.IMPORTANCE_HIGH);
            a.enableVibration(true);
            nm.createNotificationChannel(a);
        } catch (Throwable t) { App.report(t); }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Notification n = ongoing();
            if (Build.VERSION.SDK_INT >= 34) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(ID, n);
        } catch (Throwable t) {
            App.report(t);
            stopSelf();
            return START_NOT_STICKY;
        }
        h.removeCallbacks(loop);
        h.post(loop);
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        h.removeCallbacks(loop);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    /** Notification.Builder that also works before Android 8 (no channels there). */
    @SuppressWarnings("deprecation")
    private Notification.Builder builder(String channel, boolean loud) {
        if (Build.VERSION.SDK_INT >= 26) return new Notification.Builder(this, channel);
        Notification.Builder b = new Notification.Builder(this);
        b.setPriority(loud ? Notification.PRIORITY_HIGH : Notification.PRIORITY_LOW);
        return b;
    }

    private PendingIntent open() {
        Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(this, 0, i, (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0) | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private String clock(long s) {
        return String.format(java.util.Locale.US, "%02d:%02d", s / 60, s % 60);
    }

    private Notification ongoing() {
        boolean open = st.sessionOpen(), run = st.sessionRunning();
        long now = System.currentTimeMillis();
        String subj = st.sessionSubject();
        String title = Lang.t("Focusing", "ফোকাস চলছে") + (subj.isEmpty() ? "" : " · " + subj);
        String text;
        Notification.Builder b = builder(CH_FOCUS, false)
                .setSmallIcon(R.drawable.ic_stat)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(open());
        if (!open) {
            text = Lang.t("Focus session", "ফোকাস সেশন");
        } else if (!run) {
            title = Lang.t("Paused", "পজ করা") + (subj.isEmpty() ? "" : " · " + subj);
            text = Lang.t("Tap to resume · ", "চালু করতে ট্যাপ করো · ") + clock(st.sessionSeconds());
            b.setShowWhen(false);
        } else if (st.sessionMode() == 1) {
            long[] ps = st.pomoState();
            boolean br = ps[0] == 1;
            title = (br ? Lang.t("Break", "বিরতি") : "🍅 " + Lang.t("Pomodoro focus", "পোমোডোরো ফোকাস")) + (subj.isEmpty() || br ? "" : " · " + subj);
            text = br ? Lang.t("Relax - back to focus soon", "একটু জিরিয়ে নাও") : Lang.t("Stay with it, you're doing great", "চালিয়ে যাও, দারুণ হচ্ছে");
            // live countdown drawn by the system itself, so it never freezes
            if (Build.VERSION.SDK_INT >= 24) b.setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true).setWhen(now + ps[1] * 1000L);
            else b.setShowWhen(true).setUsesChronometer(true).setWhen(now - (ps[2] - ps[1]) * 1000L);
        } else {
            int g = st.sessionGoal();
            text = g > 0 ? Lang.t("Goal: ", "লক্ষ্য: ") + g + Lang.t(" min", " মিনিট") : Lang.t("Free focus", "ফ্রি ফোকাস");
            b.setShowWhen(true).setUsesChronometer(true).setWhen(now - st.sessionSeconds() * 1000L);
        }
        return b.setContentTitle(title).setContentText(text).build();
    }

    private String stateKey() {
        if (!st.sessionOpen()) return "closed";
        if (!st.sessionRunning()) return "paused";
        if (st.sessionMode() == 1) return "pomo" + st.pomoState()[0] + ":" + st.pomoState()[3];
        return "run";
    }

    private void alert(String title, String text, boolean sound) {
        try {
            Notification n = builder(CH_ALERT, true)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setAutoCancel(true)
                    .setContentIntent(open())
                    .build();
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(alertId++, n);
            if (st.hap()) Sfx.vibrate(this, 0, 200, 100, 200);
            if (sound && st.sfx()) Sfx.alarm(st, this);
        } catch (Throwable t) { App.report(t); }
    }

    private String label(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return String.valueOf(pm.getApplicationLabel(ai));
        } catch (Throwable t) {
            return pkg;
        }
    }

    private void tick() {
        if (!st.sessionOpen()) {
            stopSelf();
            return;
        }
        long now = System.currentTimeMillis();
        if (st.sessionRunning() && !MainActivity.visible) {
            int b = st.pollBoundary();
            if (b == 1) alert(Lang.t("Pomodoro done 🍅", "পোমোডোরো শেষ 🍅"), Lang.t("Take a ", "নাও ") + st.sessionBreak() + Lang.t(" min break.", " মিনিটের বিরতি।"), true);
            else if (b == 2) alert(Lang.t("Break over", "বিরতি শেষ"), Lang.t("Back to focus!", "আবার ফোকাসে ফেরো!"), true);
            else if (b == 3) alert(Lang.t("Goal reached 🎯", "লক্ষ্য পূরণ 🎯"), Lang.t("Open the app and finish to bank your XP.", "XP পেতে অ্যাপ খুলে Finish চাপো।"), true);

            if (st.guard() && st.hasUsageAccess() && now - lastStrikeAt > 45000) {
                String pkg = st.foregroundPackage();
                if (pkg != null) {
                    for (String d : Store.DISTRACT) {
                        if (d.equals(pkg)) {
                            lastStrikeAt = now;
                            st.addStrike();
                            alert(Lang.t("Stay focused! 🌲", "ফোকাসে থাকো! 🌲"),
                                    label(pkg) + Lang.t(" opened during your focus session.", " ফোকাস সেশনের সময় খোলা হয়েছে।"), false);
                            break;
                        }
                    }
                }
            }
        }
        String key = stateKey();
        if (!key.equals(lastKey) || now - lastNotifAt > 60000) {
            lastKey = key;
            lastNotifAt = now;
            try { ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(ID, ongoing()); } catch (Throwable ignored) { }
        }
        h.postDelayed(loop, 3000);
    }
}

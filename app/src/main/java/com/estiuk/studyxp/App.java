package com.estiuk.studyxp;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;

import java.io.PrintWriter;
import java.io.StringWriter;

/** Application: records crashes so the next launch can show what went wrong. */
public class App extends Application {
    static App inst;

    @Override public void onCreate() {
        super.onCreate();
        inst = this;
        try { Brand.NAME = getString(R.string.app_name); } catch (Throwable ignored) { }
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            report(e);
            if (prev != null) prev.uncaughtException(t, e);
        });
    }

    static SharedPreferences cp() { return inst.getSharedPreferences("crash", 0); }

    static String trace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    static void report(Throwable e) {
        try {
            if (inst == null) return;
            String s = "Android " + Build.VERSION.SDK_INT + " · " + Build.MANUFACTURER + " " + Build.MODEL + "\n\n" + trace(e);
            if (s.length() > 6000) s = s.substring(0, 6000);
            cp().edit().putString("last", s).commit();
        } catch (Throwable ignored) {
        }
    }

    static String pending() {
        try { return inst == null ? null : cp().getString("last", null); } catch (Throwable t) { return null; }
    }

    static void clear() {
        try { cp().edit().remove("last").commit(); } catch (Throwable ignored) { }
    }
}

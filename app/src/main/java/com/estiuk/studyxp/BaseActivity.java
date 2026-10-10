package com.estiuk.studyxp;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

/** Shell, navigation, dialogs and helpers shared by every screen. */
abstract class BaseActivity extends Activity {
    static final int M = Ui.M, W = Ui.W;
    static final int TAB_HOME = 0, TAB_FOCUS = 1, TAB_BOOST = 2, TAB_PLAN = 3, TAB_STATS = 4, TAB_REWARDS = 5, TAB_SETTINGS = 6;

    interface StrCb { void on(String s); }

    Store st;
    Pal pal;
    Ui ui;
    FrameLayout frame, content;
    LinearLayout main, navWrap, navRow;
    FrameLayout[] navPills = new FrameLayout[6];
    NavIconView[] navIcons = new NavIconView[6];
    TextView[] navLabels = new TextView[6];
    ScrollView sv;
    int tab = TAB_HOME, topInset = 0, bottomInset = 0;
    boolean started = false;
    final Handler h = new Handler(Looper.getMainLooper());
    private final ArrayList<Runnable> dq = new ArrayList<>();
    private boolean dqShowing = false;

    abstract void buildTab(int t, LinearLayout col);
    abstract void onTabChanging();
    abstract void afterChange();

    String t(String en, String bn) { return Lang.t(en, bn); }

    // ------------------------------------------------------------ shell
    void buildShell() {
        pal = Pal.get(st.dark());
        ui = new Ui(this, pal);
        getWindow().setBackgroundDrawable(new ColorDrawable(pal.bg));
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        frame = new FrameLayout(this);
        frame.setBackgroundColor(pal.bg);
        main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        content = new FrameLayout(this);
        main.addView(content, new LinearLayout.LayoutParams(M, 0, 1f));
        main.addView(buildNav(), new LinearLayout.LayoutParams(M, W));
        frame.addView(main, new FrameLayout.LayoutParams(M, M));
        setContentView(frame);
        applyBarIcons();

        frame.setOnApplyWindowInsetsListener((v, in) -> {
            int tp, bt, ime = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets sb = in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                tp = sb.top;
                bt = sb.bottom;
                ime = in.getInsets(WindowInsets.Type.ime()).bottom;
            } else {
                tp = in.getSystemWindowInsetTop();
                bt = in.getSystemWindowInsetBottom();
            }
            boolean kb = ime > bt;
            main.setPadding(0, 0, 0, kb ? ime : 0);
            navWrap.setVisibility(kb ? View.GONE : View.VISIBLE);
            if (tp != topInset || bt != bottomInset) {
                topInset = tp;
                bottomInset = bt;
                navRow.setPadding(0, ui.dp(8), 0, ui.dp(8) + bottomInset);
                rebuild();
            }
            return in;
        });
        frame.requestApplyInsets();
    }

    void applyBarIcons() {
        boolean light = !pal.dark;
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = frame.getWindowInsetsController();
            if (c != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                c.setSystemBarsAppearance(light ? mask : 0, mask);
            }
        } else {
            int f = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (light) {
                f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                f |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(f);
        }
    }

    View buildNav() {
        String[] labels = {t("Home", "হোম"), t("Focus", "ফোকাস"), t("Boost", "বুস্ট"), t("Plan", "প্ল্যান"), t("Stats", "স্ট্যাটস"), t("Rewards", "পুরস্কার")};
        navWrap = ui.vbox();
        navWrap.setBackgroundColor(pal.nav);
        View line = new View(this);
        line.setBackgroundColor(pal.stroke);
        navWrap.addView(line, new LinearLayout.LayoutParams(M, Math.max(1, ui.dp(1))));
        navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setPadding(0, ui.dp(8), 0, ui.dp(8) + bottomInset);
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            LinearLayout item = ui.vbox();
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            FrameLayout pill = new FrameLayout(this);
            NavIconView icon = new NavIconView(this, i);
            pill.addView(icon, new FrameLayout.LayoutParams(ui.dp(24), ui.dp(24), Gravity.CENTER));
            item.addView(pill, new LinearLayout.LayoutParams(ui.dp(50), ui.dp(30)));
            TextView lab = ui.tvM(labels[i], 10.5f, pal.dim);
            lab.setGravity(Gravity.CENTER);
            lab.setSingleLine(true);
            item.addView(lab, ui.lpm(W, W, 0, 3, 0, 0));
            item.setOnClickListener(v -> { hideKeyboard(); Sfx.tap(st, this); showTab(idx, true); });
            navPills[i] = pill; navIcons[i] = icon; navLabels[i] = lab;
            navRow.addView(item, new LinearLayout.LayoutParams(0, W, 1f));
        }
        navWrap.addView(navRow, new LinearLayout.LayoutParams(M, W));
        return navWrap;
    }

    void updateNav() {
        for (int i = 0; i < 6; i++) {
            boolean on = i == tab;
            navPills[i].setBackground(on ? ui.shape(Ui.alpha(pal.accent, 45), 15) : null);
            navIcons[i].setColor(on ? pal.accent : pal.dim);
            navLabels[i].setTextColor(on ? pal.accent : pal.dim);
        }
    }

    void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(frame.getWindowToken(), 0);
        } catch (Exception ignored) {
        }
    }

    void showTab(int tb, boolean animate) {
        onTabChanging();
        tab = tb;
        content.removeAllViews();
        ScrollView s = new ScrollView(this);
        s.setVerticalScrollBarEnabled(false);
        s.setOverScrollMode(View.OVER_SCROLL_NEVER);
        s.setFillViewport(false);
        LinearLayout col = ui.vbox();
        buildTab(tb, col);
        s.addView(col, new FrameLayout.LayoutParams(M, W));
        content.addView(s, new FrameLayout.LayoutParams(M, M));
        sv = s;
        updateNav();
        if (animate) {
            s.setAlpha(0f);
            s.animate().alpha(1f).setDuration(220).start();
        }
    }

    void rebuild() {
        if (content == null || !started) return;
        final int y = sv != null ? sv.getScrollY() : 0;
        showTab(tab, false);
        final ScrollView cur = sv;
        cur.post(() -> cur.scrollTo(0, y));
    }

    // ------------------------------------------------------------ helpers
    String num(int n) { return String.format(Locale.US, "%,d", n); }

    String dur(int mins) {
        return mins < 60 ? mins + t("m", " মি.") : (mins / 60) + t("h ", " ঘ. ") + (mins % 60) + t("m", " মি.");
    }

    String clock(long s) {
        long hh = s / 3600, mm = (s % 3600) / 60, ss = s % 60;
        return hh > 0 ? String.format(Locale.US, "%d:%02d:%02d", hh, mm, ss) : String.format(Locale.US, "%02d:%02d", mm, ss);
    }

    View header(String title, String sub) {
        LinearLayout l = ui.vbox();
        l.addView(ui.tvM(title, 30, pal.text));
        l.addView(ui.tv(sub, 14, pal.dim), ui.lpm(W, W, 0, 6, 0, 0));
        l.setPadding(0, 0, 0, ui.dp(18));
        return l;
    }

    TextView section(String s) {
        TextView tv = ui.caps(s, pal.dim);
        tv.setLayoutParams(ui.lpm(W, W, 4, 22, 0, 10));
        return tv;
    }

    LinearLayout bar(float frac, int fill, float heightDp) {
        LinearLayout b = new LinearLayout(this);
        b.setBackground(ui.shape(pal.track, 8));
        View f = new View(this);
        f.setBackground(ui.shape(fill, 8));
        frac = Math.max(0f, Math.min(1f, frac));
        b.addView(f, new LinearLayout.LayoutParams(0, M, frac));
        View r = new View(this);
        b.addView(r, new LinearLayout.LayoutParams(0, M, 1f - frac));
        b.setLayoutParams(ui.lp(M, ui.dp(heightDp)));
        return b;
    }

    static void weight(View v) { ((LinearLayout.LayoutParams) v.getLayoutParams()).weight = 1f; }

    View statRow(int dot, String label, String value) {
        LinearLayout r = ui.hbox();
        View d = new View(this);
        d.setBackground(ui.oval(dot));
        r.addView(d, new LinearLayout.LayoutParams(ui.dp(9), ui.dp(9)));
        TextView l = ui.tv(label, 14, pal.dim);
        r.addView(l, ui.lpm(0, W, 10, 0, 0, 0));
        weight(l);
        r.addView(ui.tvM(value, 15, pal.text));
        r.setPadding(0, ui.dp(7), 0, ui.dp(7));
        return r;
    }

    void snack(String msg) {
        if (frame == null) return;
        final TextView tv = ui.tvM(msg, 14, pal.bg);
        tv.setPadding(ui.dp(18), ui.dp(12), ui.dp(18), ui.dp(12));
        tv.setBackground(ui.shape(pal.text, 24));
        tv.setMaxWidth(ui.dp(330));
        tv.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(W, W, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = ui.dp(92) + bottomInset;
        frame.addView(tv, lp);
        tv.setAlpha(0f);
        tv.setTranslationY(ui.dp(10));
        tv.animate().alpha(1f).translationY(0).setDuration(220).start();
        h.postDelayed(() -> tv.animate().alpha(0f).setDuration(260).withEndAction(() -> {
            if (tv.getParent() != null) ((ViewGroup) tv.getParent()).removeView(tv);
        }).start(), 2600);
    }

    void askNotifPermission() {
        try {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED
                    && !st.p.getBoolean("askedNotif", false)) {
                st.p.edit().putBoolean("askedNotif", true).apply();
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 11);
            }
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------ dialogs
    void enqueue(Runnable show) {
        dq.add(show);
        if (!dqShowing) nextDialog();
    }

    void nextDialog() {
        if (dq.isEmpty() || isFinishing()) { dqShowing = false; return; }
        dqShowing = true;
        try { dq.remove(0).run(); } catch (Throwable e) { App.report(e); dqShowing = false; }
    }

    void promptText(String title, String hint, String initial, int type, final StrCb cb) {
        LinearLayout d = ui.dialogCard();
        d.addView(ui.tvM(title, 20, pal.text));
        final EditText e = ui.input(hint, initial, type);
        d.addView(e, ui.lpm(M, W, 0, 16, 0, 0));
        final Dialog dlg = ui.dialog(d);
        LinearLayout btns = ui.hbox();
        btns.addView(ui.button(t("Cancel", "বাতিল"), false, v -> dlg.dismiss()), ui.lpm(0, W, 0, 0, 6, 0));
        btns.addView(ui.button(t("Save", "সেভ"), true, v -> { cb.on(e.getText().toString()); dlg.dismiss(); }), ui.lpm(0, W, 6, 0, 0, 0));
        weight(btns.getChildAt(0));
        weight(btns.getChildAt(1));
        d.addView(btns, ui.lpm(M, W, 0, 18, 0, 0));
        dlg.show();
    }

    void confirm(String title, String msg, String okLabel, boolean danger, final Runnable ok) {
        LinearLayout d = ui.dialogCard();
        d.addView(ui.tvM(title, 20, pal.text));
        d.addView(ui.tv(msg, 14, pal.dim), ui.lpm(W, W, 0, 8, 0, 0));
        final Dialog dlg = ui.dialog(d);
        LinearLayout btns = ui.hbox();
        btns.addView(ui.button(t("Cancel", "বাতিল"), false, v -> dlg.dismiss()), ui.lpm(0, W, 0, 0, 6, 0));
        View okBtn = danger ? ui.buttonColor(okLabel, pal.danger, Color.WHITE, false, v -> { dlg.dismiss(); ok.run(); })
                : ui.button(okLabel, true, v -> { dlg.dismiss(); ok.run(); });
        btns.addView(okBtn, ui.lpm(0, W, 6, 0, 0, 0));
        weight(btns.getChildAt(0));
        weight(btns.getChildAt(1));
        d.addView(btns, ui.lpm(M, W, 0, 18, 0, 0));
        dlg.show();
    }

    /** Big centred celebration card. Queued so several can show one after another. */
    void celebrate(String caps, View visual, String title, String sub, String chip, int chipColor) {
        LinearLayout d = ui.dialogCard();
        d.setGravity(Gravity.CENTER_HORIZONTAL);
        d.addView(ui.caps(caps, pal.accent));
        if (visual != null) d.addView(visual);
        TextView tt = ui.tvM(title, 24, pal.text);
        tt.setGravity(Gravity.CENTER);
        d.addView(tt, ui.lpm(M, W, 0, 10, 0, 0));
        if (sub != null) {
            TextView s = ui.tv(sub, 14, pal.dim);
            s.setGravity(Gravity.CENTER);
            d.addView(s, ui.lpm(M, W, 0, 6, 0, 0));
        }
        if (chip != null) {
            TextView r = ui.tvM(chip, 15, chipColor);
            r.setGravity(Gravity.CENTER);
            r.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
            r.setBackground(ui.shape(Ui.alpha(chipColor, 36), 14));
            d.addView(r, ui.lpm(M, W, 0, 16, 0, 0));
        }
        final Dialog dlg = ui.dialog(d);
        d.addView(ui.button(t("Awesome", "দারুণ"), true, v -> dlg.dismiss()), ui.lpm(M, W, 0, 16, 0, 0));
        dlg.setOnDismissListener(x -> nextDialog());
        dlg.show();
    }

    // ------------------------------------------------------------ crash screen
    void showCrash(final String text, final Runnable openAnyway) {
        ScrollView sc = new ScrollView(this);
        sc.setBackgroundColor(0xFF0B1A13);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        int pd = (int) (20 * getResources().getDisplayMetrics().density);
        l.setPadding(pd, pd * 2, pd, pd);
        TextView title = new TextView(this);
        title.setText(Brand.NAME + " — error report");
        title.setTextColor(0xFFF2C14E);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        l.addView(title);
        TextView info = new TextView(this);
        info.setText("The app hit a problem last time. Please send a screenshot of this screen (or tap Copy) to Claude so it can be fixed.");
        info.setTextColor(0xFFE7F3EA);
        info.setTextSize(14);
        info.setPadding(0, pd / 2, 0, pd / 2);
        l.addView(info);
        TextView body = new TextView(this);
        body.setText(text);
        body.setTextColor(0xFF9FE8B8);
        body.setTextSize(11);
        body.setTypeface(Typeface.MONOSPACE);
        body.setTextIsSelectable(true);
        l.addView(body);
        TextView copy = new TextView(this);
        copy.setText("Copy report");
        copy.setTextColor(0xFF05210F);
        copy.setBackgroundColor(0xFF5BD38A);
        copy.setGravity(Gravity.CENTER);
        copy.setPadding(pd, pd / 2, pd, pd / 2);
        copy.setOnClickListener(v -> {
            try {
                ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("report", text));
                copy.setText("Copied ✓");
            } catch (Throwable ignored) { }
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(M, W);
        cp.topMargin = pd;
        l.addView(copy, cp);
        if (openAnyway != null) {
            TextView go = new TextView(this);
            go.setText("Clear report & open app");
            go.setTextColor(0xFFE7F3EA);
            go.setBackgroundColor(0xFF234734);
            go.setGravity(Gravity.CENTER);
            go.setPadding(pd, pd / 2, pd, pd / 2);
            go.setOnClickListener(v -> { App.clear(); openAnyway.run(); });
            LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(M, W);
            gp.topMargin = pd / 2;
            l.addView(go, gp);
        }
        sc.addView(l);
        setContentView(sc);
    }
}

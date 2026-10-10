package com.estiuk.studyxp;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Home, Focus and Boost screens. */
abstract class TabsA extends BaseActivity {
    int selMode = 0, selGoal = 45, selWork = 25, selBreak = 5;
    String selSubject = "";
    boolean focusInited = false;
    Runnable ticker;
    RingView liveRing;
    TextView liveInfo, liveStrikes;

    void pad(LinearLayout col) {
        col.setPadding(ui.dp(20), topInset + ui.dp(14), ui.dp(20), ui.dp(28));
    }

    @Override void onTabChanging() { stopTicker(); }

    void stopTicker() {
        if (ticker != null) { h.removeCallbacks(ticker); ticker = null; }
    }

    static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    View divider() {
        View v = new View(this);
        v.setBackgroundColor(pal.stroke);
        v.setLayoutParams(new LinearLayout.LayoutParams(Ui.M, Math.max(1, ui.dp(1))));
        return v;
    }

    void openUsageSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } catch (Throwable e) {
            snack(t("Open Settings → Usage access manually", "Settings → Usage access নিজে খোলো"));
        }
    }

    TextView smallBtn(String label, int bg, int fg, View.OnClickListener l) {
        TextView b = ui.tvM(label, 18, fg);
        b.setGravity(Gravity.CENTER);
        b.setBackground(ui.oval(bg));
        b.setClickable(true);
        b.setOnClickListener(l);
        return b;
    }

    String dayLabel(String day) {
        if (day.equals(st.date())) return t("Today", "আজ");
        if (day.equals(st.dayOffset(-1))) return t("Yesterday", "গতকাল");
        return day;
    }

    void openAppInfo() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
        } catch (Throwable e) {
            snack(t("Open Settings → Apps → ", "Settings → Apps → ") + Brand.NAME);
        }
    }

    void usageGuide() {
        LinearLayout d = ui.dialogCard();
        d.addView(ui.tvM(t("Allow usage access", "Usage access দাও"), 20, pal.text));
        d.addView(ui.tv(t("Safe & private: this app has no internet permission at all, so nothing can ever leave your phone. It only reads how many minutes you spent on Instagram / Facebook / TikTok.",
                "নিরাপদ ও প্রাইভেট: এই অ্যাপের ইন্টারনেট পারমিশনই নেই, তাই কিছুই ফোনের বাইরে যেতে পারে না। শুধু Instagram / Facebook / TikTok এ কত মিনিট কাটিয়েছ সেটা পড়ে।"),
                13, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 8, 0, 0));
        d.addView(ui.tvM(t("1.  Tap “Open usage access” and switch " + Brand.NAME + " ON.",
                "১.  “Open usage access” চেপে " + Brand.NAME + " ON করো।"), 14, pal.text), ui.lpm(Ui.W, Ui.W, 0, 14, 0, 0));
        d.addView(ui.tvM(t("2.  Switch greyed out? Android blocks special permissions for apps installed outside the Play Store. Tap “App info”, then ⋮ (top-right) → “Allow restricted settings”, then repeat step 1.",
                "২.  সুইচ ধূসর? Play Store-এর বাইরে থেকে ইনস্টল করা অ্যাপের স্পেশাল পারমিশন Android আটকে রাখে। “App info” চেপে ⋮ (উপরে ডানে) → “Allow restricted settings” দাও, তারপর আবার ১ নম্বর করো।"),
                14, pal.text), ui.lpm(Ui.W, Ui.W, 0, 10, 0, 0));
        final Dialog dlg = ui.dialog(d);
        LinearLayout btns = ui.hbox();
        btns.addView(ui.button(t("App info", "App info"), false, v -> { dlg.dismiss(); openAppInfo(); }), ui.lpm(0, Ui.W, 0, 0, 6, 0));
        btns.addView(ui.button(t("Open usage access", "Open usage access"), true, v -> { dlg.dismiss(); openUsageSettings(); }), ui.lpm(0, Ui.W, 6, 0, 0, 0));
        weight(btns.getChildAt(0));
        weight(btns.getChildAt(1));
        d.addView(btns, ui.lpm(Ui.M, Ui.W, 0, 18, 0, 0));
        dlg.show();
    }

    // ============================================================ pet
    String petLine() {
        if (!st.hatched()) return t("Study a little and I'll hatch! 🥚", "একটু পড়ো, আমি ডিম থেকে বেরোব! 🥚");
        switch (st.petMood()) {
            case 0: return t("Wake me up with a focus session 💤", "ফোকাস সেশন দিয়ে আমাকে জাগাও 💤");
            case 1: return t("Nice start! Keep going 🌱", "শুরুটা ভালো! চালিয়ে যাও 🌱");
            case 2: return t("Over halfway there! 🔥", "অর্ধেকের বেশি হয়ে গেছে! 🔥");
            default: return t("Goal done! So proud of you ⭐", "লক্ষ্য পূরণ! গর্বিত তোমাকে নিয়ে ⭐");
        }
    }

    private int cheerIdx = 0;

    String petCheer() {
        String[] en = {"You've got this! 💪", "One more minute? 😄", "I believe in you 🌟", "Water break? 💧", "Tiny steps count 🐾", "Study buddy ready! 📚"};
        String[] bn = {"তুমি পারবে! 💪", "আরেকটা মিনিট? 😄", "আমি তোমার ওপর বিশ্বাস রাখি 🌟", "একটু পানি খাবে? 💧", "ছোট ছোট পদক্ষেপও দামি 🐾", "পড়ার সাথী রেডি! 📚"};
        cheerIdx = (cheerIdx + 1) % en.length;
        return t(en[cheerIdx], bn[cheerIdx]);
    }

    PetView makePet(final boolean focusing, final boolean bubble) {
        final PetView pv = new PetView(this);
        pv.setBubbleColors(pal.surface, pal.text);
        pv.configure(st.petType(), st.level(), !st.hatched(), Math.min(1f, st.totalXp() / (float) Store.HATCH_XP), st.petMood(), focusing,
                bubble ? petLine() : "");
        pv.setOnPetTap(() -> {
            Sfx.tap(st, this);
            if (bubble) pv.setSay(st.hatched() ? petCheer() : t("Keep studying, I'm almost out! 🐣", "পড়তে থাকো, আমি প্রায় বেরিয়ে এসেছি! 🐣"));
        });
        return pv;
    }

    // ============================================================ dialogs shared by several tabs
    void addSubjectDialog(final StrCb cb) {
        LinearLayout d = ui.dialogCard();
        d.addView(ui.tvM(t("New subject", "নতুন সাবজেক্ট"), 20, pal.text));
        final EditText e = ui.input(t("e.g. Data Structures", "যেমন: ডেটা স্ট্রাকচার"), "",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        d.addView(e, ui.lpm(Ui.M, Ui.W, 0, 14, 0, 0));
        d.addView(ui.caps(t("Colour", "রং"), pal.dim), ui.lpm(Ui.W, Ui.W, 2, 16, 0, 8));
        final int[] sel = {st.subjects().size() % Pal.SUBJ.length};
        final View[] dots = new View[Pal.SUBJ.length];
        LinearLayout row = ui.hbox();
        for (int i = 0; i < Pal.SUBJ.length; i++) {
            final int idx = i;
            FrameLayout cell = new FrameLayout(this);
            View dot = new View(this);
            dots[i] = dot;
            cell.addView(dot, new FrameLayout.LayoutParams(ui.dp(28), ui.dp(28), Gravity.CENTER));
            cell.setOnClickListener(v -> { sel[0] = idx; paintDots(dots, sel[0]); });
            row.addView(cell, new LinearLayout.LayoutParams(0, ui.dp(36), 1f));
        }
        paintDots(dots, sel[0]);
        d.addView(row, ui.lpm(Ui.M, Ui.W, 0, 0, 0, 0));
        final Dialog dlg = ui.dialog(d);
        LinearLayout btns = ui.hbox();
        btns.addView(ui.button(t("Cancel", "বাতিল"), false, v -> dlg.dismiss()), ui.lpm(0, Ui.W, 0, 0, 6, 0));
        btns.addView(ui.button(t("Add", "যোগ করো"), true, v -> {
            String name = e.getText().toString();
            if (st.addSubject(name, sel[0])) {
                dlg.dismiss();
                cb.on(Store.clean(name));
            } else {
                snack(t("Enter a new, unique name", "একটা নতুন নাম দাও"));
            }
        }), ui.lpm(0, Ui.W, 6, 0, 0, 0));
        weight(btns.getChildAt(0));
        weight(btns.getChildAt(1));
        d.addView(btns, ui.lpm(Ui.M, Ui.W, 0, 18, 0, 0));
        dlg.show();
    }

    private void paintDots(View[] dots, int sel) {
        for (int i = 0; i < dots.length; i++) {
            GradientDrawable g = ui.oval(Pal.SUBJ[i]);
            if (i == sel) g.setStroke(ui.dp(3), pal.text);
            dots[i].setBackground(g);
        }
    }

    View sessionRow(final String[] s) {
        LinearLayout r = ui.hbox();
        String subj = s[3].isEmpty() ? t("General", "সাধারণ") : s[3];
        View d = new View(this);
        d.setBackground(ui.oval(s[3].isEmpty() ? pal.dim : st.subjectColor(s[3])));
        r.addView(d, new LinearLayout.LayoutParams(ui.dp(10), ui.dp(10)));
        LinearLayout mid = ui.vbox();
        mid.addView(ui.tvM(subj, 15, pal.text));
        mid.addView(ui.tv(dayLabel(s[0]) + " · " + s[1], 12, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 2, 0, 0));
        r.addView(mid, ui.lpm(0, Ui.W, 12, 0, 0, 0));
        weight(mid);
        r.addView(ui.tvM("+" + s[2] + " XP", 15, pal.accent));
        r.setPadding(0, ui.dp(10), 0, ui.dp(10));
        r.setOnClickListener(v -> confirm(t("Delete session?", "সেশন মুছবে?"),
                t("This takes back its " + s[2] + " XP.", "এর " + s[2] + " XP ফেরত নেওয়া হবে।"),
                t("Delete", "মুছো"), true, () -> {
                    st.deleteSession(parseInt(s[4], -1));
                    afterChange();
                }));
        return r;
    }

    // ============================================================ HOME
    void buildHome(LinearLayout col) {
        pad(col);
        int lv = st.level();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greet = hour >= 5 && hour < 12 ? t("Good morning", "শুভ সকাল")
                : hour < 17 && hour >= 12 ? t("Good afternoon", "শুভ বিকেল")
                : hour < 21 && hour >= 17 ? t("Good evening", "শুভ সন্ধ্যা") : t("Good night", "শুভ রাত্রি");

        // hero
        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(ui.shape(pal.surface, 26));
        hero.setClipToOutline(true);
        ForestView fv = new ForestView(this);
        fv.setScene(pal.dark, pal.bg);
        hero.addView(fv, new FrameLayout.LayoutParams(Ui.M, Ui.M));
        LinearLayout ov = ui.vbox();
        ov.setPadding(ui.dp(20), ui.dp(18), ui.dp(150), 0);
        ov.addView(ui.caps(Brand.NAME, pal.accent));
        ov.addView(ui.tvM(greet + ",", 15, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 12, 0, 0));
        TextView nameTv = ui.tvM(st.name(), 28, pal.text);
        nameTv.setSingleLine(true);
        nameTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        ov.addView(nameTv, ui.lpm(Ui.M, Ui.W, 0, 2, 0, 0));
        ov.addView(ui.tvM("🐾 " + st.petName() + " · " + (st.hatched() ? Store.stageName(lv) : t("Egg", "ডিম")), 12.5f, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 8, 0, 0));
        PetView pet = makePet(false, true);
        hero.addView(pet, new FrameLayout.LayoutParams(ui.dp(165), ui.dp(165), Gravity.BOTTOM | Gravity.END));
        hero.addView(ov, new FrameLayout.LayoutParams(Ui.M, Ui.W));
        TextView gear = ui.tvM("⚙", 20, pal.text);
        gear.setGravity(Gravity.CENTER);
        gear.setBackground(ui.oval(Ui.alpha(pal.surface, 200)));
        gear.setClickable(true);
        gear.setOnClickListener(v -> { Sfx.tap(st, this); showTab(TAB_SETTINGS, true); });
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(ui.dp(42), ui.dp(42), Gravity.TOP | Gravity.END);
        gp.setMargins(0, ui.dp(14), ui.dp(14), 0);
        hero.addView(gear, gp);
        col.addView(hero, new LinearLayout.LayoutParams(Ui.M, ui.dp(225)));

        // level card
        LinearLayout lc = ui.card();
        LinearLayout lr = ui.hbox();
        lr.addView(new TreeView(this, pal, lv), new LinearLayout.LayoutParams(ui.dp(96), ui.dp(108)));
        LinearLayout rt = ui.vbox();
        rt.addView(ui.caps(t("Level ", "লেভেল ") + (lv + 1), pal.accent));
        rt.addView(ui.tvM(Store.levelName(lv), 22, pal.text), ui.lpm(Ui.W, Ui.W, 0, 4, 0, 0));
        rt.addView(ui.tv(Store.stageName(lv), 13, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 2, 0, 10));
        rt.addView(bar(st.levelProgress(), pal.accent, 8));
        String lvl = st.maxLevel() ? num(st.totalXp()) + " XP · " + t("Max level 👑", "সর্বোচ্চ লেভেল 👑")
                : num(st.totalXp()) + " / " + num(Store.LEVELS[lv + 1]) + " XP";
        rt.addView(ui.tvM(lvl, 13, pal.text), ui.lpm(Ui.W, Ui.W, 0, 8, 0, 0));
        lr.addView(rt, ui.lpm(0, Ui.W, 14, 0, 0, 0));
        weight(rt);
        lc.addView(lr);
        col.addView(lc, ui.lpm(Ui.M, Ui.W, 0, 14, 0, 0));

        // today
        LinearLayout tc = ui.card();
        tc.addView(ui.caps(t("Today", "আজ"), pal.dim));
        LinearLayout tr = ui.hbox();
        RingView ring = new RingView(this);
        ring.setColors(pal.track, pal.accent, pal.text, pal.dim, pal.track);
        int goal = st.goalMin();
        ring.setTexts(dur(st.focus()), t("of ", "/ ") + dur(goal));
        ring.setProgress(st.focus() / (float) goal, true);
        tr.addView(ring, new LinearLayout.LayoutParams(ui.dp(132), ui.dp(132)));
        LinearLayout stats = ui.vbox();
        stats.addView(statRow(pal.accent, t("Focus", "ফোকাস"), "+" + st.focus()));
        stats.addView(statRow(pal.gold, t("Bonus", "বোনাস"), "+" + st.bonus()));
        stats.addView(statRow(pal.danger, t("Penalty", "পেনাল্টি"), "−" + st.penalty()));
        stats.addView(divider());
        LinearLayout net = ui.hbox();
        TextView nl = ui.tv(t("Net today", "আজকের নেট"), 14, pal.dim);
        net.addView(nl, new LinearLayout.LayoutParams(0, Ui.W, 1f));
        net.addView(ui.tvM((st.net() >= 0 ? "+" : "") + st.net() + " XP", 18, st.net() >= Store.STUDY_DAY_XP ? pal.accent : pal.text));
        net.setPadding(0, ui.dp(8), 0, 0);
        stats.addView(net);
        tr.addView(stats, ui.lpm(0, Ui.W, 14, 0, 0, 0));
        weight(stats);
        tc.addView(tr, ui.lpm(Ui.M, Ui.W, 0, 10, 0, 0));
        col.addView(tc, ui.lpm(Ui.M, Ui.W, 0, 14, 0, 0));

        // streak
        LinearLayout sc = ui.card();
        LinearLayout sh = ui.hbox();
        sh.addView(ui.tvM("🔥 " + st.streak() + t(" day streak", " দিনের স্ট্রিক"), 17, pal.text), new LinearLayout.LayoutParams(0, Ui.W, 1f));
        sh.addView(ui.tv(t("Best ", "সেরা ") + st.bestStreak(), 13, pal.dim));
        sc.addView(sh);
        sc.addView(ui.tv(st.studyDayDone() ? t("Study day complete ✓", "আজকের স্টাডি ডে সম্পূর্ণ ✓")
                        : t("Earn " + Store.STUDY_DAY_XP + " XP today to keep it alive.", "স্ট্রিক ধরে রাখতে আজ " + Store.STUDY_DAY_XP + " XP অর্জন করো।"),
                13, st.studyDayDone() ? pal.accent : pal.dim), ui.lpm(Ui.W, Ui.W, 0, 6, 0, 12));
        int[] wn = st.weekNet();
        String[] wl = st.weekLabels();
        LinearLayout dots = ui.hbox();
        for (int i = 0; i < 7; i++) {
            LinearLayout c = ui.vbox();
            c.setGravity(Gravity.CENTER_HORIZONTAL);
            boolean on = wn[i] >= Store.STUDY_DAY_XP;
            TextView dot = ui.tvM(on ? "✓" : "", 14, pal.onAccent);
            dot.setGravity(Gravity.CENTER);
            GradientDrawable g = ui.oval(on ? pal.accent : pal.track);
            if (i == 6) g.setStroke(ui.dp(2), pal.gold);
            dot.setBackground(g);
            c.addView(dot, new LinearLayout.LayoutParams(ui.dp(30), ui.dp(30)));
            c.addView(ui.tv(wl[i], 11, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 5, 0, 0));
            dots.addView(c, new LinearLayout.LayoutParams(0, Ui.W, 1f));
        }
        sc.addView(dots);
        col.addView(sc, ui.lpm(Ui.M, Ui.W, 0, 14, 0, 0));

        // tasks summary
        List<Store.Task> tasks = st.tasks();
        if (!tasks.isEmpty()) {
            int done = 0;
            for (Store.Task k : tasks) if (k.done) done++;
            LinearLayout kc = ui.card();
            LinearLayout kh = ui.hbox();
            kh.addView(ui.tvM(t("Today's tasks", "আজকের টাস্ক"), 16, pal.text), new LinearLayout.LayoutParams(0, Ui.W, 1f));
            kh.addView(ui.tvM(done + " / " + tasks.size(), 15, pal.accent));
            kc.addView(kh);
            kc.addView(bar(done / (float) tasks.size(), pal.gold, 8), ui.lpm(Ui.M, ui.dp(8), 0, 10, 0, 0));
            kc.setOnClickListener(v -> showTab(TAB_PLAN, true));
            col.addView(kc, ui.lpm(Ui.M, Ui.W, 0, 14, 0, 0));
        }

        // reward
        LinearLayout rc = ui.card();
        int ready = st.claimableCount();
        Store.Reward nx = st.nextReward();
        if (ready > 0) {
            rc.addView(ui.caps(t("Reward ready", "পুরস্কার প্রস্তুত"), pal.gold));
            rc.addView(ui.tvM("🎁 " + ready + t(ready == 1 ? " reward to claim!" : " rewards to claim!", "টি পুরস্কার নিতে পারো!"), 18, pal.text),
                    ui.lpm(Ui.W, Ui.W, 0, 8, 0, 0));
        } else if (nx != null) {
            rc.addView(ui.caps(t("Next reward", "পরের পুরস্কার"), pal.dim));
            rc.addView(ui.tvM(nx.name, 18, pal.text), ui.lpm(Ui.W, Ui.W, 0, 8, 0, 10));
            rc.addView(bar(st.totalXp() / (float) nx.xp, pal.gold, 8));
            rc.addView(ui.tv(num(nx.xp - st.totalXp()) + t(" XP to go", " XP বাকি"), 13, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 8, 0, 0));
        } else {
            rc.addView(ui.caps(t("Rewards", "পুরস্কার"), pal.dim));
            rc.addView(ui.tv(t("Set your own rewards in the Rewards tab 🎁", "Rewards ট্যাবে নিজের পুরস্কার ঠিক করো 🎁"), 14, pal.text),
                    ui.lpm(Ui.W, Ui.W, 0, 8, 0, 0));
        }
        rc.setOnClickListener(v -> showTab(TAB_REWARDS, true));
        col.addView(rc, ui.lpm(Ui.M, Ui.W, 0, 14, 0, 0));

        col.addView(ui.button(t("▶  Start focus", "▶  ফোকাস শুরু"), true, v -> { Sfx.tap(st, this); showTab(TAB_FOCUS, true); }),
                ui.lpm(Ui.M, Ui.W, 0, 18, 0, 0));
    }

    // ============================================================ FOCUS
    void initFocus() {
        if (focusInited) return;
        focusInited = true;
        selGoal = st.p.getInt("lastGoal", 45);
        selWork = st.pomoWork();
        selBreak = st.pomoBreak();
    }

    void buildFocus(LinearLayout col) {
        pad(col);
        initFocus();
        if (st.sessionOpen()) buildLive(col);
        else buildSetup(col);
    }

    private void buildSetup(LinearLayout col) {
        col.addView(header(t("Focus", "ফোকাস"), t("Pick what you'll study, then start.", "কী পড়বে ঠিক করো, তারপর শুরু করো।")));

        LinearLayout modes = ui.hbox();
        modes.addView(ui.chip(t("Focus", "ফোকাস"), selMode == 0, v -> { selMode = 0; rebuild(); }), ui.lpm(Ui.W, Ui.W, 0, 0, 8, 0));
        modes.addView(ui.chip("🍅 Pomodoro", selMode == 1, v -> { selMode = 1; rebuild(); }));
        col.addView(modes);

        // subject
        col.addView(section(t("Subject", "সাবজেক্ট")));
        List<String[]> subs = st.subjects();
        boolean found = false;
        for (String[] a : subs) if (a[0].equals(selSubject)) found = true;
        if (!found) {
            selSubject = "";
            for (String[] a : subs) if (a[0].equals(st.lastSubject())) selSubject = a[0];
            if (selSubject.isEmpty() && !subs.isEmpty()) selSubject = subs.get(0)[0];
        }
        LinearLayout sr = ui.hbox();
        for (final String[] a : subs) {
            int c = st.subjectColor(a[0]);
            sr.addView(ui.chipColor(a[0], a[0].equals(selSubject), c, v -> { selSubject = a[0]; rebuild(); }), ui.lpm(Ui.W, Ui.W, 0, 0, 8, 0));
        }
        sr.addView(ui.chip("+ " + t("New", "নতুন"), false, v -> addSubjectDialog(n -> { selSubject = n; rebuild(); })));
        col.addView(ui.hscroll(sr));

        if (selMode == 0) {
            col.addView(section(t("Goal", "লক্ষ্য")));
            LinearLayout gr = ui.hbox();
            int[] goals = {25, 45, 60, 90, 0};
            for (final int g : goals) {
                String lab = g == 0 ? t("Free", "ফ্রি") : g + t("m", " মি.");
                gr.addView(ui.chip(lab, selGoal == g, v -> { selGoal = g; rebuild(); }), ui.lpm(Ui.W, Ui.W, 0, 0, 8, 0));
            }
            boolean presetGoal = false;
            for (int g : goals) if (g == selGoal) presetGoal = true;
            gr.addView(ui.chip(presetGoal ? t("Custom", "কাস্টম") : selGoal + t("m", " মি.") + " ✎", !presetGoal,
                    v -> promptText(t("Custom focus goal", "নিজের ফোকাস লক্ষ্য"), t("Minutes (1–600)", "মিনিট (১–৬০০)"),
                            selGoal > 0 ? String.valueOf(selGoal) : "", InputType.TYPE_CLASS_NUMBER, s -> {
                                int m = parseInt(s, 0);
                                if (m >= 1) { selGoal = Math.min(600, m); rebuild(); }
                            })));
            col.addView(ui.hscroll(gr));
        } else {
            col.addView(section(t("Pomodoro (work / break)", "পোমোডোরো (কাজ / বিরতি)")));
            LinearLayout pr = ui.hbox();
            int[][] presets = {{25, 5}, {50, 10}, {15, 3}};
            boolean preset = false;
            for (final int[] pz : presets) {
                boolean sel = selWork == pz[0] && selBreak == pz[1];
                if (sel) preset = true;
                pr.addView(ui.chip(pz[0] + " / " + pz[1], sel, v -> { selWork = pz[0]; selBreak = pz[1]; rebuild(); }), ui.lpm(Ui.W, Ui.W, 0, 0, 8, 0));
            }
            pr.addView(ui.chip(preset ? t("Custom", "কাস্টম") : selWork + " / " + selBreak + " ✎", !preset, v -> pomoDialog()));
            col.addView(ui.hscroll(pr));
        }

        // ring preview
        RingView ring = new RingView(this);
        ring.setTicks(true);
        ring.setColors(pal.track, pal.accent, pal.text, pal.dim, pal.track);
        long previewSec = selMode == 1 ? selWork * 60L : selGoal * 60L;
        ring.setTexts(clock(previewSec), selMode == 1 ? t("POMODORO", "পোমোডোরো") : selGoal == 0 ? t("FREE FOCUS", "ফ্রি ফোকাস") : t("READY", "প্রস্তুত"));
        ring.setProgress(0f, false);
        LinearLayout holder = new LinearLayout(this);
        holder.setGravity(Gravity.CENTER);
        holder.addView(ring, new LinearLayout.LayoutParams(ui.dp(250), ui.dp(250)));
        col.addView(holder, ui.lpm(Ui.M, Ui.W, 0, 22, 0, 6));

        col.addView(ui.button(t("▶  Start", "▶  শুরু করো"), true, v -> startSession()), ui.lpm(Ui.M, Ui.W, 0, 10, 0, 0));
        col.addView(ui.tv(t("1 minute of real focus = 1 XP. Breaks don't count.", "১ মিনিট আসল ফোকাস = ১ XP। বিরতির সময় গোনা হয় না।"), 12, pal.dim),
                ui.lpm(Ui.W, Ui.W, 4, 10, 0, 0));

        if (st.guard() && !st.hasUsageAccess()) {
            LinearLayout gc = ui.card();
            gc.addView(ui.tvM("🛡 " + t("Focus Guard is off", "Focus Guard বন্ধ"), 16, pal.text));
            gc.addView(ui.tv(t("Allow usage access and you'll get an alert when you open Instagram, Facebook, TikTok, YouTube… during a session.",
                    "Usage access দিলে সেশনের সময় Instagram, Facebook, TikTok, YouTube… খুললে অ্যালার্ট পাবে।"), 13, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 6, 0, 12));
            gc.addView(ui.button(t("Allow usage access", "Usage access দাও"), false, v -> usageGuide()));
            col.addView(gc, ui.lpm(Ui.M, Ui.W, 0, 18, 0, 0));
        }

        List<String[]> rec = st.recentSessions(5);
        if (!rec.isEmpty()) {
            col.addView(section(t("Recent sessions", "সাম্প্রতিক সেশন")));
            LinearLayout rc = ui.card();
            for (int i = 0; i < rec.size(); i++) {
                if (i > 0) rc.addView(divider());
                rc.addView(sessionRow(rec.get(i)));
            }
            col.addView(rc);
            col.addView(ui.tv(t("Tap a session to delete it.", "মুছতে সেশনে ট্যাপ করো।"), 12, pal.dim), ui.lpm(Ui.W, Ui.W, 4, 8, 0, 0));
        }
    }

    void pomoDialog() {
        LinearLayout d = ui.dialogCard();
        d.addView(ui.tvM(t("Custom pomodoro", "কাস্টম পোমোডোরো"), 20, pal.text));
        final EditText w = ui.input(t("Work minutes", "কাজের মিনিট"), String.valueOf(selWork), InputType.TYPE_CLASS_NUMBER);
        final EditText b = ui.input(t("Break minutes", "বিরতির মিনিট"), String.valueOf(selBreak), InputType.TYPE_CLASS_NUMBER);
        d.addView(w, ui.lpm(Ui.M, Ui.W, 0, 14, 0, 0));
        d.addView(b, ui.lpm(Ui.M, Ui.W, 0, 10, 0, 0));
        final Dialog dlg = ui.dialog(d);
        LinearLayout btns = ui.hbox();
        btns.addView(ui.button(t("Cancel", "বাতিল"), false, v -> dlg.dismiss()), ui.lpm(0, Ui.W, 0, 0, 6, 0));
        btns.addView(ui.button(t("Save", "সেভ"), true, v -> {
            int wv = parseInt(w.getText().toString(), 25), bv = parseInt(b.getText().toString(), 5);
            selWork = Math.max(1, Math.min(180, wv));
            selBreak = Math.max(1, Math.min(60, bv));
            dlg.dismiss();
            rebuild();
        }), ui.lpm(0, Ui.W, 6, 0, 0, 0));
        weight(btns.getChildAt(0));
        weight(btns.getChildAt(1));
        d.addView(btns, ui.lpm(Ui.M, Ui.W, 0, 18, 0, 0));
        dlg.show();
    }

    void startSession() {
        Sfx.tap(st, this);
        askNotifPermission();
        st.setLastSubject(selSubject);
        st.p.edit().putInt("lastGoal", selGoal).apply();
        st.setPomo(selWork, selBreak);
        st.sessionStart(selMode, selMode == 0 ? selGoal : 0, selWork, selBreak, selSubject);
        FocusService.start(this);
        rebuild();
    }

    private void buildLive(LinearLayout col) {
        final boolean pomo = st.sessionMode() == 1;
        final boolean running = st.sessionRunning();
        LinearLayout hrow = ui.hbox();
        View hd = header(pomo ? "🍅 Pomodoro" : t("Focus", "ফোকাস"),
                st.sessionSubject().isEmpty() ? t("Deep work in progress", "ডিপ ওয়ার্ক চলছে") : st.sessionSubject());
        hrow.addView(hd, new LinearLayout.LayoutParams(0, Ui.W, 1f));
        hrow.addView(makePet(true, false), new LinearLayout.LayoutParams(ui.dp(112), ui.dp(112)));
        col.addView(hrow, ui.lpm(Ui.M, Ui.W, 0, 0, 0, 0));

        liveRing = new RingView(this);
        liveRing.setTicks(true);
        liveRing.setColors(pal.track, pal.accent, pal.text, pal.dim, pal.track);
        LinearLayout holder = new LinearLayout(this);
        holder.setGravity(Gravity.CENTER);
        holder.addView(liveRing, new LinearLayout.LayoutParams(ui.dp(290), ui.dp(290)));
        col.addView(holder, ui.lpm(Ui.M, Ui.W, 0, 4, 0, 10));

        liveInfo = ui.tvM("", 15, pal.text);
        liveInfo.setGravity(Gravity.CENTER);
        col.addView(liveInfo, ui.lpm(Ui.M, Ui.W, 0, 6, 0, 0));
        liveStrikes = ui.tv("", 13, pal.dim);
        liveStrikes.setGravity(Gravity.CENTER);
        col.addView(liveStrikes, ui.lpm(Ui.M, Ui.W, 0, 6, 0, 0));

        LinearLayout btns = ui.hbox();
        btns.addView(ui.button(running ? t("⏸  Pause", "⏸  পজ") : t("▶  Resume", "▶  চালু করো"), false, v -> {
            Sfx.tap(st, this);
            if (st.sessionRunning()) st.sessionPause();
            else st.sessionResume();
            FocusService.start(this);
            rebuild();
        }), ui.lpm(0, Ui.W, 0, 0, 6, 0));
        btns.addView(ui.button(t("✓  Finish", "✓  শেষ"), true, v -> finishSession()), ui.lpm(0, Ui.W, 6, 0, 0, 0));
        weight(btns.getChildAt(0));
        weight(btns.getChildAt(1));
        col.addView(btns, ui.lpm(Ui.M, Ui.W, 0, 22, 0, 0));

        TextView disc = ui.tvM(t("Discard session", "সেশন বাদ দাও"), 14, pal.danger);
        disc.setGravity(Gravity.CENTER);
        disc.setPadding(0, ui.dp(16), 0, ui.dp(8));
        disc.setClickable(true);
        disc.setOnClickListener(v -> confirm(t("Discard session?", "সেশন বাদ দেবে?"),
                t("No XP will be added for this session.", "এই সেশনের কোনো XP যোগ হবে না।"), t("Discard", "বাদ দাও"), true, () -> {
                    st.sessionDiscard();
                    FocusService.stop(this);
                    rebuild();
                }));
        col.addView(disc, ui.lpm(Ui.M, Ui.W, 0, 6, 0, 0));

        if (st.guard() && !st.hasUsageAccess()) {
            col.addView(ui.tv(t("Tip: allow usage access (Settings tab) to turn on Focus Guard.", "টিপ: Focus Guard চালু করতে Settings এ usage access দাও।"), 12, pal.dim),
                    ui.lpm(Ui.M, Ui.W, 0, 14, 0, 0));
        }

        updateLive();
        ticker = new Runnable() {
            @Override public void run() {
                try { updateLive(); } catch (Throwable t) { App.report(t); }
                if (ticker == this) h.postDelayed(this, 1000);
            }
        };
        h.postDelayed(ticker, 1000);
    }

    void updateLive() {
        if (liveRing == null || !st.sessionOpen()) return;
        boolean run = st.sessionRunning();
        long secs = st.sessionSeconds();
        if (st.sessionMode() == 1) {
            long[] ps = st.pomoState();
            boolean br = ps[0] == 1;
            liveRing.setArcColor(br ? pal.gold : pal.accent);
            liveRing.setTexts(clock(ps[1]), !run ? t("PAUSED", "পজ করা") : br ? t("BREAK", "বিরতি") : t("FOCUS #", "ফোকাস #") + (ps[3] + 1));
            liveRing.setProgress(1f - ps[1] / (float) ps[2], false);
        } else {
            int g = st.sessionGoal();
            liveRing.setTexts(clock(secs), !run ? t("PAUSED", "পজ করা") : g > 0 ? t("OF ", "/ ") + g + t(" MIN", " মিনিট") : t("FREE FOCUS", "ফ্রি ফোকাস"));
            liveRing.setProgress(g > 0 ? Math.min(1f, secs / (g * 60f)) : (secs % 3600) / 3600f, false);
        }
        int m = st.sessionMinutes();
        liveInfo.setText(t("Banked so far: ", "এখন পর্যন্ত: ") + dur(m) + "  →  +" + m + " XP");
        if (st.guard()) {
            liveStrikes.setText("🛡 " + t("Distractions caught: ", "ধরা distraction: ") + st.sessionStrikes());
            liveStrikes.setTextColor(st.sessionStrikes() > 0 ? pal.danger : pal.dim);
        } else {
            liveStrikes.setText("");
        }
        if (run) {
            int b = st.pollBoundary();
            if (b == 1) { Sfx.alarm(st, this); snack(t("🍅 Pomodoro done! Take a break.", "🍅 পোমোডোরো শেষ! বিরতি নাও।")); }
            else if (b == 2) { Sfx.alarm(st, this); snack(t("Break over — back to focus!", "বিরতি শেষ — আবার ফোকাসে!")); }
            else if (b == 3) {
                Sfx.alarm(st, this);
                final int mm = st.sessionMinutes();
                confirm(t("Goal reached 🎯", "লক্ষ্য পূরণ 🎯"), t("You focused " + dur(mm) + ". Bank " + mm + " XP now?", dur(mm) + " ফোকাস করেছ। এখনই " + mm + " XP নেবে?"),
                        t("Finish & bank", "শেষ করো"), false, this::finishSession);
            }
        }
    }

    void finishSession() {
        final int mins = st.sessionMinutes();
        if (mins < 1) {
            confirm(t("Too short", "খুব ছোট"), t("Less than 1 minute of focus — nothing to add. Discard this session?", "১ মিনিটেরও কম — যোগ করার মতো কিছু নেই। সেশন বাদ দেবে?"),
                    t("Discard", "বাদ দাও"), true, () -> {
                        st.sessionDiscard();
                        FocusService.stop(this);
                        rebuild();
                    });
            return;
        }
        if (mins > 180) {
            promptText(t("That's a long one!", "অনেক লম্বা সেশন!"), t("Minutes to bank (max " + mins + ")", "কত মিনিট নেবে (সর্বোচ্চ " + mins + ")"),
                    String.valueOf(mins), InputType.TYPE_CLASS_NUMBER, s -> bank(Math.min(mins, Math.max(0, parseInt(s, 0)))));
            return;
        }
        bank(mins);
    }

    void bank(int mins) {
        stopTicker();
        int got = st.sessionFinish(mins);
        FocusService.stop(this);
        if (got > 0) {
            Sfx.success(st, this);
            snack("+" + got + " XP 🌲");
        }
        afterChange();
    }

    // ============================================================ BOOST
    void buildBoost(LinearLayout col) {
        pad(col);
        col.addView(header(t("Boost", "বুস্ট"), t("Bonus XP for real learning. Penalties for lost time.", "আসল শেখার জন্য বোনাস XP। নষ্ট সময়ের জন্য পেনাল্টি।")));

        LinearLayout bh = ui.hbox();
        bh.addView(section(t("Bonus XP", "বোনাস XP")), new LinearLayout.LayoutParams(0, Ui.W, 1f));
        bh.addView(ui.chip("+ " + t("New", "নতুন"), true, v -> bonusDialog(null)));
        col.addView(bh);
        LinearLayout bc = ui.card();
        List<Store.Bonus> bl = st.bonuses();
        if (bl.isEmpty()) bc.addView(ui.tv(t("No bonus items. Tap “+ New” to create your own.", "কোনো বোনাস আইটেম নেই। নিজের বানাতে “+ নতুন” চাপো।"), 14, pal.dim));
        for (int i = 0; i < bl.size(); i++) {
            if (i > 0) bc.addView(divider());
            final Store.Bonus bn = bl.get(i);
            LinearLayout r = ui.hbox();
            r.setPadding(0, ui.dp(10), 0, ui.dp(10));
            TextView title = ui.tv(Store.bonusName(bn), 14, pal.text);
            title.setClickable(true);
            title.setOnClickListener(v -> bonusDialog(bn));
            r.addView(title, new LinearLayout.LayoutParams(0, Ui.W, 1f));
            r.addView(ui.tvM("+" + bn.xp, 14, pal.gold), ui.lpm(Ui.W, Ui.W, 8, 0, 10, 0));
            r.addView(smallBtn("−", pal.surface2, pal.dim, v -> {
                if (st.undoBonus(bn)) { Sfx.tap(st, this); snack("−" + bn.xp + " XP"); afterChange(); }
                else snack(t("Nothing to remove for this one today", "আজ এটার সরানোর মতো কিছু নেই"));
            }), new LinearLayout.LayoutParams(ui.dp(36), ui.dp(36)));
            r.addView(smallBtn("+", pal.accent, pal.onAccent, v -> {
                Sfx.success(st, this);
                st.useBonus(bn);
                snack("+" + bn.xp + " XP");
                afterChange();
            }), ui.lpm(ui.dp(36), ui.dp(36), 8, 0, 0, 0));
            bc.addView(r);
        }
        col.addView(bc);
        col.addView(ui.tv(t("Tap a name to edit or delete it. Bonus today: +", "নাম ট্যাপ করলে এডিট/ডিলিট করা যাবে। আজকের বোনাস: +") + st.bonus() + " XP", 12, pal.dim),
                ui.lpm(Ui.W, Ui.W, 4, 8, 0, 0));

        col.addView(section(t("Lost time (penalty)", "নষ্ট সময় (পেনাল্টি)")));
        LinearLayout pc = ui.card();
        pc.addView(ui.tv(t("Wasted minutes on distractions. 1 min = −1 XP.", "distraction এ নষ্ট মিনিট। ১ মিনিট = −১ XP।"), 13, pal.dim));
        LinearLayout add = ui.hbox();
        int[] pv = {3, 5, 10, 15, 30};
        for (final int m : pv) {
            add.addView(ui.chipColor("−" + m, false, pal.danger, v -> applyPenalty(m)), ui.lpm(Ui.W, Ui.W, 0, 0, 8, 0));
        }
        add.addView(ui.chipColor("✎ " + t("Custom", "কাস্টম"), false, pal.danger, v -> promptText(t("Minutes wasted", "কত মিনিট নষ্ট"), t("Minutes", "মিনিট"), "",
                InputType.TYPE_CLASS_NUMBER, s -> applyPenalty(parseInt(s, 0)))));
        pc.addView(ui.hscroll(add), ui.lpm(Ui.M, Ui.W, 0, 12, 0, 0));
        LinearLayout undo = ui.hbox();
        undo.addView(ui.tv(t("Undo:", "ফেরত:"), 13, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 0, 10, 0));
        for (final int m : new int[]{3, 5, 10}) {
            undo.addView(ui.chip("+" + m, false, v -> undoPenalty(m)), ui.lpm(Ui.W, Ui.W, 0, 0, 8, 0));
        }
        undo.addView(ui.chip("✎", false, v -> promptText(t("Give back minutes", "কত মিনিট ফেরত"), t("Minutes", "মিনিট"), "",
                InputType.TYPE_CLASS_NUMBER, s -> undoPenalty(parseInt(s, 0)))));
        pc.addView(ui.hscroll(undo), ui.lpm(Ui.M, Ui.W, 0, 10, 0, 0));
        pc.addView(ui.tv(t("Penalty today: −", "আজকের পেনাল্টি: −") + st.penalty() + " XP", 13, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 12, 0, 0));
        col.addView(pc);

        col.addView(section(t("Auto-tracking", "অটো-ট্র্যাকিং")));
        LinearLayout ac = ui.card();
        if (st.hasUsageAccess()) {
            int[] u = st.usageToday();
            for (int i = 0; i < 3; i++) ac.addView(statRow(i == 0 ? 0xFFE1306C : i == 1 ? 0xFF1877F2 : 0xFF25F4EE, Store.TRACKED_NAMES[i], u[i] + t(" min", " মি.")));
            ac.addView(divider());
            ac.addView(ui.tv(t("Auto penalty today: −", "আজকের অটো পেনাল্টি: −") + st.autoPenaltyToday() + " XP", 13, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 10, 0, 0));
            if (st.usagePartlyIgnored()) ac.addView(ui.tv(t("Minutes used before you allowed Usage access are not charged.", "Usage access দেওয়ার আগের মিনিটগুলো ধরা হয় না।"), 12, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 6, 0, 0));
        } else {
            ac.addView(ui.tvM(t("Track social-media time automatically", "সোশ্যাল মিডিয়ার সময় নিজে নিজে ট্র্যাক"), 16, pal.text));
            ac.addView(ui.tv(t("Allow usage access and every minute on Instagram, Facebook or TikTok becomes −1 XP. It uses Android's own stats; nothing leaves your phone.",
                    "Usage access দিলে Instagram, Facebook বা TikTok এ প্রতি মিনিট −১ XP হবে। Android এর নিজস্ব হিসাব ব্যবহার হয়; কিছুই ফোনের বাইরে যায় না।"), 13, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 6, 0, 12));
            ac.addView(ui.button(t("Allow usage access", "Usage access দাও"), true, v -> usageGuide()));
        }
        col.addView(ac);
    }

    void applyPenalty(int m) {
        if (m < 1) { snack(t("Enter minutes above 0", "০ এর বেশি মিনিট দাও")); return; }
        m = Math.min(m, 600);
        Sfx.warn(st, this);
        st.usePenalty(m);
        snack("−" + m + " XP");
        afterChange();
    }

    void undoPenalty(int m) {
        if (m < 1) return;
        if (!st.undoPenalty(m)) { snack(t("No penalty today", "আজ কোনো পেনাল্টি নেই")); return; }
        Sfx.tap(st, this);
        afterChange();
    }

    void bonusDialog(final Store.Bonus edit) {
        LinearLayout d = ui.dialogCard();
        d.addView(ui.tvM(edit == null ? t("New bonus", "নতুন বোনাস") : t("Edit bonus", "বোনাস এডিট"), 20, pal.text));
        final EditText n = ui.input(t("e.g. Watched a lecture", "যেমন: লেকচার দেখেছি"), edit == null ? "" : Store.bonusName(edit),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        final EditText x = ui.input(t("XP (1–500)", "XP (১–৫০০)"), edit == null ? "" : String.valueOf(edit.xp), InputType.TYPE_CLASS_NUMBER);
        d.addView(n, ui.lpm(Ui.M, Ui.W, 0, 14, 0, 0));
        d.addView(x, ui.lpm(Ui.M, Ui.W, 0, 10, 0, 0));
        final Dialog dlg = ui.dialog(d);
        LinearLayout btns = ui.hbox();
        btns.addView(ui.button(t("Cancel", "বাতিল"), false, v -> dlg.dismiss()), ui.lpm(0, Ui.W, 0, 0, 6, 0));
        btns.addView(ui.button(t("Save", "সেভ"), true, v -> {
            String name = Store.clean(n.getText().toString());
            int xp = parseInt(x.getText().toString(), 0);
            if (name.isEmpty() || xp < 1) { snack(t("Enter a name and XP", "নাম আর XP দাও")); return; }
            if (edit == null) st.addBonusItem(name, xp); else st.updateBonusItem(edit.id, name, xp);
            dlg.dismiss();
            afterChange();
        }), ui.lpm(0, Ui.W, 6, 0, 0, 0));
        weight(btns.getChildAt(0));
        weight(btns.getChildAt(1));
        d.addView(btns, ui.lpm(Ui.M, Ui.W, 0, 18, 0, 0));
        if (edit != null) {
            TextView del = ui.tvM(t("Delete this bonus", "এই বোনাস মুছো"), 14, pal.danger);
            del.setGravity(Gravity.CENTER);
            del.setPadding(0, ui.dp(14), 0, 0);
            del.setOnClickListener(v -> {
                dlg.dismiss();
                confirm(t("Delete bonus?", "বোনাস মুছবে?"), Store.bonusName(edit), t("Delete", "মুছো"), true, () -> { st.deleteBonusItem(edit.id); afterChange(); });
            });
            d.addView(del, ui.lpm(Ui.M, Ui.W, 0, 4, 0, 0));
        }
        dlg.show();
    }

    static String time(long ms) { return new SimpleDateFormat("HH:mm", Locale.US).format(new Date(ms)); }
}

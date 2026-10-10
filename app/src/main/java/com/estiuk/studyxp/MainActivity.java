package com.estiuk.studyxp;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Pexumi Focus — entry point: lifecycle, settings screen, backup, milestones. */
public class MainActivity extends TabsB {
    static boolean visible = false;
    Tutorial tut;
    static final int REQ_EXPORT = 21, REQ_IMPORT = 22;

    // ============================================================ lifecycle
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            st = new Store(this);
            Lang.bn = st.bn();
            String crash = App.pending();
            if (crash != null) {
                showCrash(crash, this::safeStart);
                return;
            }
        } catch (Throwable e) {
            App.report(e);
            showCrash(App.trace(e), null);
            return;
        }
        safeStart();
    }

    void safeStart() {
        try {
            startApp();
        } catch (Throwable e) {
            App.report(e);
            showCrash(App.trace(e), null);
        }
    }

    void startApp() {
        st.rollDay();
        st.sync();
        buildShell();
        started = true;
        showTab(TAB_HOME, false);
        try { st.syncUsage(); } catch (Throwable e) { App.report(e); }
        XpWidget.refresh(this);
        if (!st.p.getBoolean("tutDone", false)) {
            h.post(() -> showTutorial(Tutorial.intro(), true));
        } else {
            milestones();
        }
    }

    /** Full-screen skippable tour. The first-run intro is marked as seen once it is closed (skipped or finished). */
    void showTutorial(Tutorial.Slide[] slides, final boolean intro) {
        if (tut != null || frame == null || isFinishing()) return;
        tut = new Tutorial(this, slides, intro, () -> {
            tut = null;
            if (intro) st.p.edit().putBoolean("tutDone", true).apply();
            milestones();
        });
        frame.addView(tut, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override protected void onResume() {
        super.onResume();
        visible = true;
        if (!started) return;
        try {
            st.rollDay();
            st.sync();
            st.syncUsage();
            XpWidget.refresh(this);
            if (st.sessionOpen()) FocusService.start(this);
            rebuild();
            milestones();
        } catch (Throwable e) {
            App.report(e);
        }
    }

    @Override protected void onPause() {
        visible = false;
        stopTicker();
        super.onPause();
    }

    @Override protected void onDestroy() {
        visible = false;
        stopTicker();
        super.onDestroy();
    }

    @Override void buildTab(int tb, LinearLayout col) {
        switch (tb) {
            case TAB_FOCUS: buildFocus(col); break;
            case TAB_BOOST: buildBoost(col); break;
            case TAB_PLAN: buildPlan(col); break;
            case TAB_STATS: buildStats(col); break;
            case TAB_REWARDS: buildRewards(col); break;
            case TAB_SETTINGS: buildSettings(col); break;
            default: buildHome(col); break;
        }
    }

    @Override public void onBackPressed() {
        if (tut != null) { tut.close(); return; }
        if (started && tab != TAB_HOME) showTab(TAB_HOME, true);
        else super.onBackPressed();
    }

    // ============================================================ change pipeline
    @Override void afterChange() {
        try {
            st.sync();
            XpWidget.refresh(this);
            rebuild();
            milestones();
        } catch (Throwable e) {
            App.report(e);
        }
    }

    void milestones() {
        if (tut != null) return;
        final int lv = st.pollLevelUp();
        if (lv >= 0) {
            enqueue(() -> {
                Sfx.levelUp(st, this);
                TreeView tv = new TreeView(this, pal, lv);
                tv.setLayoutParams(ui.lpm(ui.dp(130), ui.dp(140), 0, 8, 0, 0));
                celebrate(t("Level up", "লেভেল আপ"), tv, t("Level ", "লেভেল ") + (lv + 1) + " · " + Store.levelName(lv),
                        Store.stageName(lv), "🌲 " + num(st.totalXp()) + " XP", pal.accent);
            });
        }
        if (st.pollHatch()) {
            enqueue(() -> {
                Sfx.levelUp(st, this);
                PetView pv = new PetView(this);
                pv.configure(st.petType(), st.level(), false, 1f, 3, false, "");
                pv.setLayoutParams(ui.lpm(ui.dp(170), ui.dp(170), 0, 8, 0, 0));
                celebrate(t("It hatched!", "ডিম ফুটেছে!"), pv, st.petName() + t(" is here 🐣", " এসে গেছে 🐣"),
                        t("Keep studying and it will keep growing.", "পড়তে থাকো, ও বড় হতে থাকবে।"), null, 0);
            });
        }
        for (final Store.Reward r : st.pollRewardUnlocks()) {
            enqueue(() -> {
                Sfx.success(st, this);
                celebrate(t("Reward unlocked", "পুরস্কার আনলক"), emoji("🎁"), r.name,
                        t("Open the Rewards tab to claim it.", "ক্লেইম করতে Rewards ট্যাবে যাও।"), null, 0);
            });
        }
        for (final int bi : st.checkBadges()) {
            enqueue(() -> {
                Sfx.success(st, this);
                String[] b = Store.BADGES[bi];
                celebrate(t("Badge earned", "ব্যাজ অর্জন"), emoji(b[1]), Lang.bn ? b[3] : b[2], Lang.bn ? b[5] : b[4], null, 0);
            });
        }
        if (st.pollStudyDay()) {
            snack("🔥 " + t("Study day complete! Streak: ", "স্টাডি ডে সম্পূর্ণ! স্ট্রিক: ") + st.streak());
        }
    }

    /** Rebuild window chrome after a language / theme change. */
    void restyle() {
        buildShell();
        showTab(tab, false);
    }

    interface BoolCb { void accept(boolean v); }

    // ============================================================ SETTINGS
    View toggleRow(String title, String sub, boolean on, final BoolCb cb) {
        LinearLayout r = ui.hbox();
        r.setPadding(0, ui.dp(10), 0, ui.dp(10));
        LinearLayout mid = ui.vbox();
        mid.addView(ui.tvM(title, 15, pal.text));
        if (sub != null) mid.addView(ui.tv(sub, 12, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 3, 0, 0));
        r.addView(mid, new LinearLayout.LayoutParams(0, Ui.W, 1f));
        Switch sw = new Switch(this);
        sw.setChecked(on);
        try {
            sw.setThumbTintList(ColorStateList.valueOf(on ? pal.accent : pal.dim));
            sw.setTrackTintList(ColorStateList.valueOf(on ? Ui.alpha(pal.accent, 110) : pal.track));
        } catch (Throwable ignored) { }
        sw.setOnCheckedChangeListener((b, v) -> { Sfx.tap(st, this); cb.accept(v); });
        r.addView(sw, ui.lpm(Ui.W, Ui.W, 10, 0, 0, 0));
        return r;
    }

    View actionRow(String title, String sub, int color, View.OnClickListener l) {
        LinearLayout r = ui.vbox();
        r.setPadding(0, ui.dp(12), 0, ui.dp(12));
        r.addView(ui.tvM(title, 15, color));
        if (sub != null) r.addView(ui.tv(sub, 12, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 3, 0, 0));
        r.setClickable(true);
        r.setOnClickListener(l);
        return r;
    }

    void buildSettings(LinearLayout col) {
        pad(col);
        TextView back = ui.tvM("←  " + t("Back", "পিছনে"), 15, pal.accent);
        back.setPadding(0, 0, ui.dp(20), ui.dp(12));
        back.setClickable(true);
        back.setOnClickListener(v -> showTab(TAB_HOME, true));
        col.addView(back);
        col.addView(header(t("Settings", "সেটিংস"), t("Make it yours.", "নিজের মতো সাজাও।")));

        // profile & look
        col.addView(section(t("Profile & look", "প্রোফাইল ও লুক")));
        LinearLayout c1 = ui.card();
        c1.addView(actionRow(t("Name", "নাম"), st.name(), pal.text, v -> promptText(t("Your name", "তোমার নাম"), t("Name", "নাম"), st.name(),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, s -> { st.setName(s); afterChange(); })));
        c1.addView(divider());
        c1.addView(ui.caps(t("Language", "ভাষা"), pal.dim), ui.lpm(Ui.W, Ui.W, 0, 14, 0, 8));
        LinearLayout lr = ui.hbox();
        lr.addView(ui.chip("English", !st.bn(), v -> { st.setBn(false); restyle(); }), ui.lpm(Ui.W, Ui.W, 0, 0, 8, 0));
        lr.addView(ui.chip("বাংলা", st.bn(), v -> { st.setBn(true); restyle(); }));
        c1.addView(lr);
        c1.addView(ui.caps(t("Theme", "থিম"), pal.dim), ui.lpm(Ui.W, Ui.W, 0, 16, 0, 8));
        LinearLayout tr = ui.hbox();
        tr.addView(ui.chip("🌲 " + t("Night Forest", "নাইট ফরেস্ট"), st.dark(), v -> { st.setDark(true); restyle(); }), ui.lpm(Ui.W, Ui.W, 0, 0, 8, 0));
        tr.addView(ui.chip("🌿 " + t("Morning Mist", "মর্নিং মিস্ট"), !st.dark(), v -> { st.setDark(false); restyle(); }));
        c1.addView(tr);
        col.addView(c1);

        // tutorials
        col.addView(section(t("Tutorials", "টিউটোরিয়াল")));
        LinearLayout ct = ui.card();
        ct.addView(actionRow("✨  " + t("Replay the intro tour", "ইন্ট্রো ট্যুর আবার দেখো"), t("The quick welcome tour", "ছোট্ট স্বাগত ট্যুর"), pal.accent,
                v -> showTutorial(Tutorial.intro(), false)));
        for (int gi = 0; gi < Tutorial.GUIDES.length; gi++) {
            final int g = gi;
            String[] gd = Tutorial.GUIDES[gi];
            ct.addView(divider());
            ct.addView(actionRow(gd[0] + "  " + (Lang.bn ? gd[2] : gd[1]), Lang.bn ? gd[4] : gd[3], pal.text,
                    v -> showTutorial(Tutorial.guide(g), false)));
        }
        col.addView(ct);

        // pet
        col.addView(section(t("Your pet", "তোমার পেট")));
        LinearLayout cp = ui.card();
        PetView prev = makePet(false, false);
        prev.setLayoutParams(ui.lpm(Ui.M, ui.dp(150), 0, 0, 0, 0));
        cp.addView(prev);
        LinearLayout pr = ui.hbox();
        pr.setGravity(Gravity.CENTER);
        String[] pn = {"🌱 Pexumi", "🦊 " + t("Fox", "শেয়াল"), "🐼 " + t("Panda", "পান্ডা"), "🐱 " + t("Cat", "বিড়াল"), "🐰 " + t("Bunny", "খরগোশ")};
        final int[] ptype = {4, 0, 1, 2, 3};
        for (int i = 0; i < 5; i++) {
            final int idx = ptype[i];
            pr.addView(ui.chip(pn[i], st.petType() == idx, v -> { st.setPetType(idx); rebuild(); }), ui.lpm(Ui.W, Ui.W, 3, 0, 3, 0));
        }
        cp.addView(ui.hscroll(pr), ui.lpm(Ui.M, Ui.W, 0, 8, 0, 0));
        cp.addView(actionRow(t("Pet name", "পেটের নাম"), st.petName() + t("  ·  tap to rename", "  ·  নাম বদলাতে ট্যাপ করো"), pal.text, v ->
                promptText(t("Name your pet", "পেটের নাম দাও"), t("Name", "নাম"), st.petName(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                        s -> { st.setPetName(s); afterChange(); })));
        cp.addView(ui.tv(t("It hatches at " + Store.HATCH_XP + " XP and grows with every level: leaf, scarf, glasses, glow, crown. Tap it to play!",
                "ও " + Store.HATCH_XP + " XP তে ডিম থেকে বের হয় আর প্রতি লেভেলে বড় হয়: পাতা, স্কার্ফ, চশমা, আলো, মুকুট। খেলতে ট্যাপ করো!"), 12, pal.dim));
        col.addView(cp);

        // feedback
        col.addView(section(t("Sound & feel", "সাউন্ড ও ফিল")));
        LinearLayout c2 = ui.card();
        c2.addView(toggleRow(t("Sounds", "সাউন্ড"), t("Chimes for XP, level-ups and alarms", "XP, লেভেল-আপ ও অ্যালার্মের সাউন্ড"), st.sfx(), v -> { st.setSfx(v); rebuild(); }));
        c2.addView(divider());
        c2.addView(toggleRow(t("Vibration", "ভাইব্রেশন"), t("Taps and alerts", "ট্যাপ ও অ্যালার্ট"), st.hap(), v -> { st.setHap(v); rebuild(); }));
        col.addView(c2);

        // guard
        col.addView(section("Focus Guard"));
        LinearLayout c3 = ui.card();
        c3.addView(toggleRow(t("Focus Guard", "Focus Guard"), t("Alert when a social app opens during a session", "সেশনে সোশ্যাল অ্যাপ খুললে অ্যালার্ট"), st.guard(), v -> { st.setGuard(v); rebuild(); }));
        c3.addView(divider());
        boolean ua = st.hasUsageAccess();
        c3.addView(actionRow(t("Usage access", "Usage access"), ua ? t("Allowed ✓ — also powers auto-penalty", "অনুমতি আছে ✓ — অটো-পেনাল্টিও এতে চলে")
                : t("Not allowed — tap to open Android settings", "অনুমতি নেই — Android সেটিংস খুলতে ট্যাপ করো"), ua ? pal.accent : pal.gold, v -> usageGuide()));
        col.addView(c3);

        // fix XP
        col.addView(section(t("Fix mistakes", "ভুল ঠিক করো")));
        LinearLayout c4 = ui.card();
        c4.addView(actionRow(t("Adjust total XP", "মোট XP ঠিক করো"), t("Added XP by mistake? Remove it here. Now: ", "ভুলে XP বেড়ে গেছে? এখানে কমাও। এখন: ") + num(st.totalXp()), pal.text, v -> adjustDialog()));
        String adj = st.p.getString("adj", "");
        if (!adj.isEmpty()) {
            String[] ls = adj.split("\n");
            StringBuilder sb = new StringBuilder();
            for (int i = ls.length - 1; i >= 0 && i > ls.length - 4; i--) {
                String[] a = ls[i].split("\\|", 3);
                if (a.length >= 2) sb.append(a[0]).append("   ").append(a[1].startsWith("-") ? a[1] : "+" + a[1]).append(a.length > 2 && !a[2].isEmpty() ? "   " + a[2] : "").append('\n');
            }
            c4.addView(ui.tv(sb.toString().trim(), 12, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 0, 0, 6));
        }
        c4.addView(divider());
        c4.addView(actionRow(t("Reset today", "আজ রিসেট"), t("Removes everything from today: focus, bonuses, penalties, ticked tasks, XP edits, streak credit", "আজকের সব মুছে ফেলে: ফোকাস, বোনাস, পেনাল্টি, টাস্ক টিক, XP এডিট, স্ট্রিক"), pal.text, v ->
                confirm(t("Reset today?", "আজ রিসেট করবে?"), t("Everything you earned or lost today is taken off your total XP. Other days stay.", "আজ যা অর্জন বা হারিয়েছ সব মোট XP থেকে বাদ যাবে। অন্য দিন ঠিক থাকবে।"), t("Reset", "রিসেট"), true, () -> {
                    FocusService.stop(this);
                    int removed = st.resetToday();
                    snack(t("Today reset · total XP −" + removed, "আজ রিসেট · মোট XP −" + removed));
                    afterChange();
                })));
        c4.addView(divider());
        c4.addView(actionRow(t("Reset ALL progress", "সব প্রোগ্রেস রিসেট"), t("XP, history, streak, badges. Keeps settings, subjects & your reward list.", "XP, হিস্ট্রি, স্ট্রিক, ব্যাজ মুছে যায়। সেটিংস, সাবজেক্ট ও পুরস্কার তালিকা থাকে।"), pal.danger, v ->
                confirm(t("Reset everything?", "সব রিসেট করবে?"), t("This can't be undone. Export a backup first if unsure.", "এটা ফেরানো যাবে না। নিশ্চিত না হলে আগে backup নাও।"), t("Continue", "এগোও"), true, () ->
                        confirm(t("Really sure?", "সত্যিই নিশ্চিত?"), t("All XP and history will be deleted.", "সব XP ও হিস্ট্রি মুছে যাবে।"), t("Delete all", "সব মুছো"), true, () -> {
                            FocusService.stop(this);
                            st.resetProgress();
                            snack(t("Progress reset", "প্রোগ্রেস রিসেট হয়েছে"));
                            afterChange();
                        }))));
        col.addView(c4);

        // backup
        col.addView(section(t("Backup", "ব্যাকআপ")));
        LinearLayout c5 = ui.card();
        c5.addView(actionRow(t("Export backup", "ব্যাকআপ এক্সপোর্ট"), t("Saves a .json file you can keep anywhere", "একটা .json ফাইল সেভ হয়, যেখানে খুশি রাখো"), pal.text, v -> {
            try {
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json")
                        .putExtra(Intent.EXTRA_TITLE, "pexumi_backup_" + st.date() + ".json");
                startActivityForResult(i, REQ_EXPORT);
            } catch (Throwable e) { snack(t("Can't open file picker", "ফাইল পিকার খোলা যাচ্ছে না")); }
        }));
        c5.addView(divider());
        c5.addView(actionRow(t("Import backup", "ব্যাকআপ ইমপোর্ট"), t("Replaces everything with a backup file", "ব্যাকআপ ফাইল দিয়ে সব বদলে দেয়"), pal.text, v -> {
            try {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                startActivityForResult(i, REQ_IMPORT);
            } catch (Throwable e) { snack(t("Can't open file picker", "ফাইল পিকার খোলা যাচ্ছে না")); }
        }));
        col.addView(c5);

        // privacy
        col.addView(section(t("Privacy & permissions", "প্রাইভেসি ও পারমিশন")));
        LinearLayout cv = ui.card();
        String[][] pv = {
                {"🚫", t("No internet permission. The app physically cannot send anything out of your phone.", "ইন্টারনেট পারমিশন নেই। অ্যাপ ফোনের বাইরে কিছু পাঠাতেই পারে না।")},
                {"💾", t("All data stays on this phone. A backup file exists only when you export one.", "সব ডেটা এই ফোনেই থাকে। ব্যাকআপ ফাইল শুধু তুমি এক্সপোর্ট করলেই হয়।")},
                {"📊", t("Usage access (optional): reads minutes spent on Instagram / Facebook / TikTok for Focus Guard and auto-penalty.", "Usage access (ঐচ্ছিক): Focus Guard ও অটো-পেনাল্টির জন্য Instagram / Facebook / TikTok এর মিনিট পড়ে।")},
                {"🔔", t("Notifications: the live session timer and alerts.", "নোটিফিকেশন: লাইভ সেশন টাইমার ও অ্যালার্ট।")},
                {"ℹ️", t("Android warns about every app installed outside the Play Store. That is about where the APK came from, not what it does.", "Play Store-এর বাইরে থেকে ইনস্টল করা যেকোনো অ্যাপ নিয়ে Android সতর্ক করে। এটা APK কোথা থেকে এসেছে তা নিয়ে, অ্যাপ কী করে তা নিয়ে নয়।")}};
        for (int i = 0; i < pv.length; i++) {
            if (i > 0) cv.addView(divider());
            LinearLayout r = ui.hbox();
            r.setPadding(0, ui.dp(10), 0, ui.dp(10));
            r.addView(ui.tv(pv[i][0], 16, pal.text), ui.lpm(ui.dp(30), Ui.W, 0, 0, 0, 0));
            TextView tx = ui.tv(pv[i][1], 13, pal.text);
            r.addView(tx, new LinearLayout.LayoutParams(0, Ui.W, 1f));
            cv.addView(r);
        }
        col.addView(cv);

        // about
        col.addView(section(t("About", "সম্পর্কে")));
        LinearLayout c6 = ui.card();
        c6.addView(ui.tvM(Brand.NAME + "  v" + Brand.VERSION, 16, pal.text));
        c6.addView(ui.tv(t("100% free · works offline · no ads · no account. Everything stays on this phone.",
                "১০০% ফ্রি · অফলাইনে চলে · বিজ্ঞাপন নেই · অ্যাকাউন্ট লাগে না। সবকিছু এই ফোনেই থাকে।"), 13, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 6, 0, 0));
        c6.addView(ui.tv(t("Widget: long-press your home screen → Widgets → ", "উইজেট: হোম স্ক্রিনে লং-প্রেস → Widgets → ") + Brand.NAME, 13, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 10, 0, 0));
        col.addView(c6);
    }

    void adjustDialog() {
        LinearLayout d = ui.dialogCard();
        d.addView(ui.tvM(t("Adjust total XP", "মোট XP ঠিক করো"), 20, pal.text));
        d.addView(ui.tv(t("Current total: ", "এখনকার মোট: ") + num(st.totalXp()) + " XP", 14, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 6, 0, 0));
        final EditText amt = ui.input(t("Amount (e.g. 50)", "পরিমাণ (যেমন 50)"), "", InputType.TYPE_CLASS_NUMBER);
        final EditText note = ui.input(t("Note (optional)", "নোট (ঐচ্ছিক)"), "", InputType.TYPE_CLASS_TEXT);
        d.addView(amt, ui.lpm(Ui.M, Ui.W, 0, 14, 0, 0));
        d.addView(note, ui.lpm(Ui.M, Ui.W, 0, 10, 0, 0));
        final Dialog dlg = ui.dialog(d);
        LinearLayout btns = ui.hbox();
        btns.addView(ui.buttonColor("−  " + t("Remove", "কমাও"), pal.danger, android.graphics.Color.WHITE, false, v -> applyAdjust(dlg, amt, note, -1)), ui.lpm(0, Ui.W, 0, 0, 6, 0));
        btns.addView(ui.button("+  " + t("Add", "বাড়াও"), true, v -> applyAdjust(dlg, amt, note, 1)), ui.lpm(0, Ui.W, 6, 0, 0, 0));
        weight(btns.getChildAt(0));
        weight(btns.getChildAt(1));
        d.addView(btns, ui.lpm(Ui.M, Ui.W, 0, 18, 0, 0));
        TextView cancel = ui.tvM(t("Cancel", "বাতিল"), 14, pal.dim);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(0, ui.dp(14), 0, 0);
        cancel.setOnClickListener(v -> dlg.dismiss());
        d.addView(cancel, ui.lpm(Ui.M, Ui.W, 0, 4, 0, 0));
        dlg.show();
    }

    private void applyAdjust(Dialog dlg, EditText amt, EditText note, int sign) {
        int v = parseInt(amt.getText().toString(), 0);
        if (v <= 0) { snack(t("Enter an amount above 0", "০ এর বেশি পরিমাণ দাও")); return; }
        int before = st.totalXp();
        st.adjustTotal(sign * v, note.getText().toString());
        dlg.dismiss();
        snack((sign < 0 ? "−" : "+") + v + " XP → " + num(st.totalXp()));
        afterChange();
        if (before == 0 && sign < 0) snack(t("XP can't go below 0", "XP ০ এর নিচে যায় না"));
    }

    // ============================================================ backup I/O
    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        try {
            if (req == REQ_EXPORT) {
                String json = st.exportJson();
                if (json == null) { snack(t("Export failed", "এক্সপোর্ট ব্যর্থ")); return; }
                try (OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                snack(t("Backup saved ✓", "ব্যাকআপ সেভ হয়েছে ✓"));
            } else if (req == REQ_IMPORT) {
                String text;
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
                    text = new String(bo.toByteArray(), StandardCharsets.UTF_8);
                }
                final String fin = text;
                confirm(t("Replace everything?", "সব বদলে দেবে?"), t("Your current data will be overwritten by this backup.", "এখনকার ডেটা এই ব্যাকআপ দিয়ে বদলে যাবে।"),
                        t("Import", "ইমপোর্ট"), true, () -> {
                            FocusService.stop(this);
                            if (st.importJson(fin)) {
                                st.pollLevelUp();
                                st.pollRewardUnlocks();
                                st.checkBadges();
                                XpWidget.refresh(this);
                                restyle();
                                snack(t("Backup restored ✓", "ব্যাকআপ রিস্টোর হয়েছে ✓"));
                            } else {
                                snack(t("That file isn't a valid backup", "এটা সঠিক ব্যাকআপ ফাইল না"));
                            }
                        });
            }
        } catch (Throwable e) {
            App.report(e);
            snack(t("File error", "ফাইল এরর"));
        }
    }
}

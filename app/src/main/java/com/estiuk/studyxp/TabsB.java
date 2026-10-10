package com.estiuk.studyxp;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Plan, Stats and Rewards screens. */
abstract class TabsB extends TabsA {
    int rewTab = 0, statsTab = 0, calYear = -1, calMonth = 0;
    String calSel = "";

    // ============================================================ PLAN
    void buildPlan(LinearLayout col) {
        pad(col);
        col.addView(header(t("Plan", "প্ল্যান"), t("Daily goal, tasks and subjects.", "দৈনিক লক্ষ্য, টাস্ক আর সাবজেক্ট।")));

        // daily goal
        col.addView(section(t("Daily focus goal", "দৈনিক ফোকাস লক্ষ্য")));
        LinearLayout gc = ui.card();
        gc.addView(ui.tvM(dur(st.goalMin()), 24, pal.text));
        gc.addView(ui.tv(t("Shown as the ring on Home.", "Home এর রিং এ দেখায়।"), 13, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 4, 0, 12));
        LinearLayout gr = ui.hbox();
        int[] gs = {30, 60, 90, 120, 180};
        boolean preset = false;
        for (final int g : gs) {
            boolean sel = st.goalMin() == g;
            if (sel) preset = true;
            gr.addView(ui.chip(dur(g), sel, v -> { st.setGoalMin(g); afterChange(); }), ui.lpm(Ui.W, Ui.W, 0, 0, 8, 0));
        }
        gr.addView(ui.chip(preset ? t("Custom", "কাস্টম") : "✎", !preset, v -> promptText(t("Daily goal (minutes)", "দৈনিক লক্ষ্য (মিনিট)"), t("Minutes", "মিনিট"),
                String.valueOf(st.goalMin()), InputType.TYPE_CLASS_NUMBER, s -> { st.setGoalMin(parseInt(s, st.goalMin())); afterChange(); })));
        gc.addView(ui.hscroll(gr));
        col.addView(gc);

        // tasks
        LinearLayout th = ui.hbox();
        th.addView(section(t("Tasks", "টাস্ক")), new LinearLayout.LayoutParams(0, Ui.W, 1f));
        th.addView(ui.chip("+ " + t("Add", "যোগ"), true, v -> taskDialog()));
        col.addView(th);
        List<Store.Task> tasks = st.tasks();
        LinearLayout tc = ui.card();
        if (tasks.isEmpty()) {
            tc.addView(ui.tv(t("No tasks yet. Add one — finishing it gives bonus XP ✅", "এখনো কোনো টাস্ক নেই। যোগ করো — শেষ করলে বোনাস XP ✅"), 14, pal.dim));
        }
        for (int i = 0; i < tasks.size(); i++) {
            final Store.Task k = tasks.get(i);
            if (i > 0) tc.addView(divider());
            LinearLayout r = ui.hbox();
            r.setPadding(0, ui.dp(10), 0, ui.dp(10));
            TextView box = ui.tvM(k.done ? "✓" : "", 16, pal.onAccent);
            box.setGravity(Gravity.CENTER);
            android.graphics.drawable.GradientDrawable bg = ui.shape(k.done ? pal.accent : Color.TRANSPARENT, 8, k.done ? pal.accent : pal.dim, 2);
            box.setBackground(bg);
            r.addView(box, new LinearLayout.LayoutParams(ui.dp(26), ui.dp(26)));
            LinearLayout mid = ui.vbox();
            TextView tt = ui.tvM(k.title, 15, k.done ? pal.dim : pal.text);
            if (k.done) tt.setPaintFlags(tt.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            mid.addView(tt);
            if (!k.subject.isEmpty()) mid.addView(ui.tv(k.subject, 12, st.subjectColor(k.subject)), ui.lpm(Ui.W, Ui.W, 0, 3, 0, 0));
            r.addView(mid, ui.lpm(0, Ui.W, 12, 0, 8, 0));
            weight(mid);
            r.addView(ui.tvM("+" + k.xp, 14, pal.gold));
            r.addView(smallBtn("✕", Color.TRANSPARENT, pal.dim, v -> confirm(t("Delete task?", "টাস্ক মুছবে?"), k.title,
                    t("Delete", "মুছো"), true, () -> {
                        if (k.done) { st.addBonus(-k.xp); st.unlogAct("T", k.title, k.xp); }
                        st.deleteTask(k.id);
                        afterChange();
                    })), ui.lpm(ui.dp(34), ui.dp(34), 6, 0, 0, 0));
            r.setOnClickListener(v -> {
                int d = st.toggleTask(k.id);
                if (d > 0) { Sfx.success(st, this); snack("+" + d + " XP ✅"); }
                else if (d < 0) { Sfx.tap(st, this); snack(d + " XP"); }
                afterChange();
            });
            tc.addView(r);
        }
        col.addView(tc);
        col.addView(ui.tv(t("Completed tasks clear themselves the next day.", "শেষ করা টাস্ক পরের দিন নিজে থেকে সরে যায়।"), 12, pal.dim), ui.lpm(Ui.W, Ui.W, 4, 8, 0, 0));

        // subjects
        LinearLayout sh = ui.hbox();
        sh.addView(section(t("Subjects", "সাবজেক্ট")), new LinearLayout.LayoutParams(0, Ui.W, 1f));
        sh.addView(ui.chip("+ " + t("Add", "যোগ"), true, v -> addSubjectDialog(n -> afterChange())));
        col.addView(sh);
        LinearLayout sc = ui.card();
        List<String[]> subs = st.subjects();
        if (subs.isEmpty()) sc.addView(ui.tv(t("No subjects. Add one to tag your sessions.", "কোনো সাবজেক্ট নেই। সেশন ট্যাগ করতে যোগ করো।"), 14, pal.dim));
        for (int i = 0; i < subs.size(); i++) {
            final String name = subs.get(i)[0];
            if (i > 0) sc.addView(divider());
            LinearLayout r = ui.hbox();
            r.setPadding(0, ui.dp(10), 0, ui.dp(10));
            View d = new View(this);
            d.setBackground(ui.oval(st.subjectColor(name)));
            r.addView(d, new LinearLayout.LayoutParams(ui.dp(12), ui.dp(12)));
            TextView nm = ui.tvM(name, 15, pal.text);
            r.addView(nm, ui.lpm(0, Ui.W, 12, 0, 0, 0));
            weight(nm);
            r.addView(smallBtn("✕", Color.TRANSPARENT, pal.dim, v -> confirm(t("Delete subject?", "সাবজেক্ট মুছবে?"),
                    t("Past sessions keep their XP.", "আগের সেশনের XP থাকবে।"), t("Delete", "মুছো"), true, () -> {
                        st.deleteSubject(name);
                        afterChange();
                    })), new LinearLayout.LayoutParams(ui.dp(34), ui.dp(34)));
            sc.addView(r);
        }
        col.addView(sc);
    }

    void taskDialog() {
        LinearLayout d = ui.dialogCard();
        d.addView(ui.tvM(t("New task", "নতুন টাস্ক"), 20, pal.text));
        final EditText e = ui.input(t("e.g. Solve 5 DSA problems", "যেমন: ৫টা DSA প্রবলেম সলভ"), "",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        d.addView(e, ui.lpm(Ui.M, Ui.W, 0, 14, 0, 0));
        final String[] subj = {""};
        final int[] xp = {10};
        d.addView(ui.caps(t("Subject (optional)", "সাবজেক্ট (ঐচ্ছিক)"), pal.dim), ui.lpm(Ui.W, Ui.W, 2, 16, 0, 8));
        final LinearLayout sr = ui.hbox();
        final List<String[]> subs = st.subjects();
        final Runnable[] paint = new Runnable[1];
        final LinearLayout xr = ui.hbox();
        paint[0] = () -> {
            sr.removeAllViews();
            sr.addView(ui.chip(t("None", "নেই"), subj[0].isEmpty(), v -> { subj[0] = ""; paint[0].run(); }), ui.lpm(Ui.W, Ui.W, 0, 0, 8, 0));
            for (final String[] a : subs) {
                sr.addView(ui.chipColor(a[0], a[0].equals(subj[0]), st.subjectColor(a[0]), v -> { subj[0] = a[0]; paint[0].run(); }), ui.lpm(Ui.W, Ui.W, 0, 0, 8, 0));
            }
            xr.removeAllViews();
            for (final int x : new int[]{5, 10, 15, 20, 30}) {
                xr.addView(ui.chipColor("+" + x, xp[0] == x, pal.gold, v -> { xp[0] = x; paint[0].run(); }), ui.lpm(Ui.W, Ui.W, 0, 0, 8, 0));
            }
        };
        paint[0].run();
        d.addView(ui.hscroll(sr));
        d.addView(ui.caps(t("Bonus XP when done", "শেষ করলে বোনাস XP"), pal.dim), ui.lpm(Ui.W, Ui.W, 2, 16, 0, 8));
        d.addView(ui.hscroll(xr));
        final Dialog dlg = ui.dialog(d);
        LinearLayout btns = ui.hbox();
        btns.addView(ui.button(t("Cancel", "বাতিল"), false, v -> dlg.dismiss()), ui.lpm(0, Ui.W, 0, 0, 6, 0));
        btns.addView(ui.button(t("Add", "যোগ করো"), true, v -> {
            String title = Store.clean(e.getText().toString());
            if (title.isEmpty()) { snack(t("Write a title first", "আগে টাইটেল লেখো")); return; }
            st.addTask(title, subj[0], xp[0]);
            dlg.dismiss();
            afterChange();
        }), ui.lpm(0, Ui.W, 6, 0, 0, 0));
        weight(btns.getChildAt(0));
        weight(btns.getChildAt(1));
        d.addView(btns, ui.lpm(Ui.M, Ui.W, 0, 18, 0, 0));
        dlg.show();
    }

    // ============================================================ STATS
    View tile(String big, String small, int color) {
        LinearLayout c = ui.card();
        c.setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(16));
        c.addView(ui.tvM(big, 22, color));
        c.addView(ui.tv(small, 12, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 4, 0, 0));
        return c;
    }

    void tileRow(LinearLayout col, View a, View b) {
        LinearLayout r = new LinearLayout(this);
        r.addView(a, ui.lpm(0, Ui.W, 0, 0, 6, 0));
        r.addView(b, ui.lpm(0, Ui.W, 6, 0, 0, 0));
        weight(a);
        weight(b);
        col.addView(r, ui.lpm(Ui.M, Ui.W, 0, 12, 0, 0));
    }

    String delta(int now, int before) {
        int d = now - before;
        return (d >= 0 ? "▲ +" : "▼ ") + d;
    }

    void buildStats(LinearLayout col) {
        pad(col);
        col.addView(header(t("Stats", "স্ট্যাটস"), t("Your progress at a glance.", "তোমার অগ্রগতি এক নজরে।")));
        LinearLayout sw = ui.hbox();
        sw.addView(ui.chip("📊 " + t("Overview", "ওভারভিউ"), statsTab == 0, v -> { statsTab = 0; rebuild(); }), ui.lpm(Ui.W, Ui.W, 0, 0, 8, 0));
        sw.addView(ui.chip("📅 " + t("Calendar", "ক্যালেন্ডার"), statsTab == 1, v -> { statsTab = 1; rebuild(); }));
        col.addView(sw, ui.lpm(Ui.M, Ui.W, 0, 0, 0, 4));
        if (statsTab == 1) { buildCalendar(col); return; }

        tileRow(col, tile(num(st.totalXp()), t("Total XP", "মোট XP"), pal.accent), tile(dur(st.totalFocusMin()), t("Total focus", "মোট ফোকাস"), pal.text));
        tileRow(col, tile(String.valueOf(st.totalSessions()), t("Sessions", "সেশন"), pal.text), tile(st.bestStreak() + t(" days", " দিন"), t("Best streak", "সেরা স্ট্রিক"), pal.gold));
        tileRow(col, tile(st.bestDayNet() + " XP", t("Best day", "সেরা দিন"), pal.accent), tile(String.valueOf(st.totalStrikes()), t("Guard strikes", "Guard স্ট্রাইক"), st.totalStrikes() > 0 ? pal.danger : pal.text));

        // chart
        col.addView(section(t("Last 7 days (net XP)", "শেষ ৭ দিন (নেট XP)")));
        LinearLayout cc = ui.card();
        BarChartView chart = new BarChartView(this);
        chart.setColors(pal.accent, pal.track, pal.dim, pal.text, pal.gold);
        chart.setData(st.weekNet(), st.weekLabels(), Store.STUDY_DAY_XP);
        cc.addView(chart, new LinearLayout.LayoutParams(Ui.M, ui.dp(190)));
        cc.addView(ui.tv(t("Gold line = " + Store.STUDY_DAY_XP + " XP study-day mark", "সোনালী লাইন = " + Store.STUDY_DAY_XP + " XP স্টাডি-ডে মার্ক"), 12, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 8, 0, 0));
        col.addView(cc);

        col.addView(section(t("Last 30 days", "শেষ ৩০ দিন")));
        LinearLayout tcard = ui.card();
        TrendView trend = new TrendView(this);
        int[] t30 = new int[30];
        for (int i = 0; i < 30; i++) t30[i] = st.dayNetOffset(i - 29);
        trend.setData(t30, new String[]{t("30 days ago", "৩০ দিন আগে"), t("15 days", "১৫ দিন"), t("today", "আজ")}, pal.accent, pal.track, pal.dim, pal.text, pal.gold);
        tcard.addView(trend, new LinearLayout.LayoutParams(Ui.M, ui.dp(170)));
        col.addView(tcard);

        // weekly report
        col.addView(section(t("Weekly report", "সাপ্তাহিক রিপোর্ট")));
        final int[] a = st.weekAgg(0), b = st.weekAgg(1);
        final String[] top = st.topSubjectWeek();
        LinearLayout wc = ui.card();
        wc.addView(ui.tvM(t("Last 7 days vs the 7 before", "শেষ ৭ দিন বনাম আগের ৭ দিন"), 15, pal.text));
        LinearLayout wl = ui.vbox();
        wl.addView(reportRow(t("Net XP", "নেট XP"), a[0] + " XP", delta(a[0], b[0]), a[0] >= b[0]));
        wl.addView(reportRow(t("Focus time", "ফোকাস সময়"), dur(a[1]), delta(a[1], b[1]) + t(" m", " মি."), a[1] >= b[1]));
        wl.addView(reportRow(t("Study days", "স্টাডি ডে"), a[2] + " / 7", delta(a[2], b[2]), a[2] >= b[2]));
        wl.addView(reportRow(t("Best day", "সেরা দিন"), a[3] + " XP", delta(a[3], b[3]), a[3] >= b[3]));
        wc.addView(wl, ui.lpm(Ui.M, Ui.W, 0, 8, 0, 0));
        if (top != null) wc.addView(ui.tv(t("Top subject: ", "সেরা সাবজেক্ট: ") + top[0] + " (" + dur(parseInt(top[1], 0)) + ")", 13, pal.accent), ui.lpm(Ui.W, Ui.W, 0, 8, 0, 12));
        else wc.addView(ui.tv("", 1, pal.dim));
        wc.addView(ui.button(t("Share report", "রিপোর্ট শেয়ার"), false, v -> {
            String txt = Brand.NAME + " — " + t("weekly report", "সাপ্তাহিক রিপোর্ট") + "\n"
                    + t("Net XP: ", "নেট XP: ") + a[0] + " (" + delta(a[0], b[0]) + ")\n"
                    + t("Focus: ", "ফোকাস: ") + dur(a[1]) + "\n"
                    + t("Study days: ", "স্টাডি ডে: ") + a[2] + "/7\n"
                    + t("Best day: ", "সেরা দিন: ") + a[3] + " XP\n"
                    + (top != null ? t("Top subject: ", "সেরা সাবজেক্ট: ") + top[0] + "\n" : "")
                    + t("Streak: ", "স্ট্রিক: ") + st.streak() + t(" days", " দিন") + "\n"
                    + t("Level: ", "লেভেল: ") + Store.levelName(st.level()) + " · " + num(st.totalXp()) + " XP";
            try {
                Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, txt);
                startActivity(Intent.createChooser(i, t("Share report", "রিপোর্ট শেয়ার")));
            } catch (Throwable e) { snack(t("Can't share right now", "এখন শেয়ার করা যাচ্ছে না")); }
        }));
        col.addView(wc);

        // heatmap
        col.addView(section(t("Last 15 weeks", "শেষ ১৫ সপ্তাহ")));
        LinearLayout hc = ui.card();
        HeatmapView hm = new HeatmapView(this);
        int n = HeatmapView.WEEKS * 7 + 7;
        int[] nets = new int[n];
        for (int i = 0; i < n; i++) nets[i] = st.dayNetOffset(i - (n - 1));
        hm.setData(nets, pal.track, pal.accent, pal.gold, off -> snack(st.dayOffset(off) + "  ·  " + st.dayNetOffset(off) + " XP"));
        hc.addView(hm, new LinearLayout.LayoutParams(Ui.M, ui.dp(150)));
        hc.addView(ui.tv(t("Darker green = more XP. Gold = a big day. Tap a square.", "গাঢ় সবুজ = বেশি XP। সোনালী = দারুণ দিন। স্কয়ারে ট্যাপ করো।"), 12, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 8, 0, 0));
        col.addView(hc);

        // subject breakdown
        col.addView(section(t("By subject", "সাবজেক্ট অনুযায়ী")));
        LinearLayout sc = ui.card();
        List<String[]> tot = st.subjectTotals();
        if (tot.isEmpty()) sc.addView(ui.tv(t("Finish a session with a subject to see this.", "সাবজেক্ট দিয়ে সেশন শেষ করলে এখানে দেখাবে।"), 14, pal.dim));
        int sum = 0;
        for (String[] s : tot) sum += parseInt(s[1], 0);
        for (int i = 0; i < tot.size(); i++) {
            String[] s = tot.get(i);
            int m = parseInt(s[1], 0);
            int col2 = st.subjectColor(s[0]);
            LinearLayout r = ui.hbox();
            r.addView(ui.tvM(s[0], 14, pal.text), new LinearLayout.LayoutParams(0, Ui.W, 1f));
            r.addView(ui.tv(dur(m) + "  ·  " + (sum > 0 ? Math.round(m * 100f / sum) : 0) + "%", 13, pal.dim));
            sc.addView(r, ui.lpm(Ui.M, Ui.W, 0, i == 0 ? 0 : 14, 0, 6));
            sc.addView(bar(sum > 0 ? m / (float) sum : 0f, col2, 8));
        }
        col.addView(sc);

        // sessions
        List<String[]> rec = st.recentSessions(15);
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

    // ---------------------------------------------------------------- calendar
    private static final String[] MON_EN = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    private static final String[] MON_BN = {"জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"};

    private String key(int y, int m, int d) { return String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d); }

    void buildCalendar(LinearLayout col) {
        Calendar now = Calendar.getInstance();
        final int ny = now.get(Calendar.YEAR), nm = now.get(Calendar.MONTH), nd = now.get(Calendar.DAY_OF_MONTH);
        if (calYear < 0) { calYear = ny; calMonth = nm; calSel = st.date(); }
        Calendar first = new GregorianCalendar(calYear, calMonth, 1);
        int days = first.getActualMaximum(Calendar.DAY_OF_MONTH), firstDow = first.get(Calendar.DAY_OF_WEEK) - 1;
        boolean isNow = calYear == ny && calMonth == nm;
        boolean isFuture = calYear > ny || (calYear == ny && calMonth > nm);
        int futureFrom = isFuture ? 1 : isNow ? nd + 1 : 99;

        int[] nets = new int[days];
        int active = 0, totalXp = 0, totalFocus = 0, bestDay = 0;
        for (int dd = 1; dd <= days; dd++) {
            if (dd >= futureFrom) continue;
            int[] ds = st.dayStats(key(calYear, calMonth, dd));
            nets[dd - 1] = ds[0] + ds[1] - ds[2];
            totalXp += Math.max(0, nets[dd - 1]);
            totalFocus += ds[0];
            if (nets[dd - 1] >= Store.STUDY_DAY_XP) active++;
            bestDay = Math.max(bestDay, nets[dd - 1]);
        }

        LinearLayout cc = ui.card();
        LinearLayout nav = ui.hbox();
        nav.addView(smallBtn("‹", pal.surface2, pal.text, v -> {
            calMonth--;
            if (calMonth < 0) { calMonth = 11; calYear--; }
            calSel = key(calYear, calMonth, 1);
            rebuild();
        }), new LinearLayout.LayoutParams(ui.dp(40), ui.dp(40)));
        TextView title = ui.tvM((Lang.bn ? MON_BN : MON_EN)[calMonth] + " " + calYear, 18, pal.text);
        title.setGravity(Gravity.CENTER);
        nav.addView(title, new LinearLayout.LayoutParams(0, Ui.W, 1f));
        nav.addView(smallBtn("›", isNow || isFuture ? pal.track : pal.surface2, isNow || isFuture ? pal.dim : pal.text, v -> {
            if (calYear == ny && calMonth == nm) return;
            calMonth++;
            if (calMonth > 11) { calMonth = 0; calYear++; }
            calSel = (calYear == ny && calMonth == nm) ? st.date() : key(calYear, calMonth, 1);
            rebuild();
        }), new LinearLayout.LayoutParams(ui.dp(40), ui.dp(40)));
        cc.addView(nav);

        String[] heads = Lang.bn ? new String[]{"র", "সো", "ম", "বু", "বৃ", "শু", "শ"} : new String[]{"S", "M", "T", "W", "T", "F", "S"};
        CalendarView cal = new CalendarView(this);
        int selDay = calSel.startsWith(key(calYear, calMonth, 1).substring(0, 8)) ? parseInt(calSel.substring(8), -1) : -1;
        cal.setData(heads, days, firstDow, nets, selDay, isNow ? nd : -1, futureFrom, pal.track, pal.accent, pal.gold, pal.text, pal.dim, pal.onAccent,
                day -> { calSel = key(calYear, calMonth, day); rebuild(); });
        cc.addView(cal, ui.lpm(Ui.M, Ui.W, 0, 10, 0, 0));

        LinearLayout lg = ui.hbox();
        lg.setGravity(Gravity.CENTER);
        lg.addView(ui.tv(t("less", "কম"), 11, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 0, 6, 0));
        int[][] lv = {{pal.track, 100}, {pal.accent, 70}, {pal.accent, 130}, {pal.accent, 205}, {pal.gold, 235}};
        for (int[] l : lv) {
            View sq = new View(this);
            sq.setBackground(ui.shape(Ui.alpha(l[0], l[1]), 4));
            lg.addView(sq, ui.lpm(ui.dp(16), ui.dp(16), 2, 0, 2, 0));
        }
        lg.addView(ui.tv(t("more", "বেশি"), 11, pal.dim), ui.lpm(Ui.W, Ui.W, 6, 0, 0, 0));
        cc.addView(lg, ui.lpm(Ui.M, Ui.W, 0, 12, 0, 0));
        col.addView(cc, ui.lpm(Ui.M, Ui.W, 0, 10, 0, 0));

        tileRow(col, tile(active + t(" days", " দিন"), t("Study days (20+ XP)", "স্টাডি ডে (২০+ XP)"), pal.accent), tile(num(totalXp) + " XP", t("Earned this month", "এই মাসে অর্জন"), pal.gold));
        tileRow(col, tile(dur(totalFocus), t("Focus this month", "এই মাসের ফোকাস"), pal.text), tile(bestDay + " XP", t("Best day", "সেরা দিন"), pal.accent));

        // selected day
        final String k = calSel;
        int[] ds = st.dayStats(k);
        int netDay = ds[0] + ds[1] - ds[2];
        String pretty = k;
        try {
            Date dt = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(k);
            pretty = new SimpleDateFormat("EEEE, d MMM yyyy", Locale.US).format(dt);
        } catch (Exception ignored) { }
        col.addView(section(t("Day details", "দিনের বিবরণ")));
        LinearLayout dc = ui.card();
        dc.addView(ui.tvM(pretty, 17, pal.text));
        dc.addView(ui.tv(dayLabel(k).equals(k) ? "" : dayLabel(k), 12, pal.accent));
        LinearLayout mini = new LinearLayout(this);
        mini.addView(miniStat(t("Focus", "ফোকাস"), dur(ds[0]), pal.text), ui.lpm(0, Ui.W, 0, 0, 4, 0));
        mini.addView(miniStat(t("Bonus", "বোনাস"), "+" + ds[1], pal.gold), ui.lpm(0, Ui.W, 4, 0, 4, 0));
        mini.addView(miniStat(t("Penalty", "পেনাল্টি"), "−" + ds[2], pal.danger), ui.lpm(0, Ui.W, 4, 0, 4, 0));
        mini.addView(miniStat(t("Net", "নেট"), (netDay >= 0 ? "+" : "") + netDay, pal.accent), ui.lpm(0, Ui.W, 4, 0, 0, 0));
        for (int i = 0; i < 4; i++) weight(mini.getChildAt(i));
        dc.addView(mini, ui.lpm(Ui.M, Ui.W, 0, 12, 0, 0));

        List<String[]> ses = st.sessionsOn(k), acts = st.actsOn(k);
        if (ses.isEmpty() && acts.isEmpty()) {
            dc.addView(ui.tv(netDay != 0 ? t("Only the totals were saved for this day.", "এই দিনের শুধু মোট হিসাব সেভ আছে।") : t("Nothing logged on this day.", "এই দিনে কিছু লগ হয়নি।"), 13, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 14, 0, 0));
        } else {
            dc.addView(ui.caps(t("What you did", "কী করেছ"), pal.dim), ui.lpm(Ui.W, Ui.W, 0, 16, 0, 4));
            for (String[] s2 : ses) dc.addView(dayLine("⏱", (s2[2].isEmpty() ? t("Focus", "ফোকাস") : s2[2]) + " · " + dur(parseInt(s2[1], 0)) + " · " + s2[0], "+" + s2[1], pal.accent));
            for (String[] a : acts) {
                boolean b = a[0].equals("B"), tk = a[0].equals("T");
                dc.addView(dayLine(b ? "✨" : tk ? "✅" : "⚠️", a[1], (b || tk ? "+" : "−") + a[2], b || tk ? pal.gold : pal.danger));
            }
        }
        col.addView(dc);
    }

    View miniStat(String label, String value, int color) {
        LinearLayout c = ui.vbox();
        c.setPadding(ui.dp(10), ui.dp(10), ui.dp(10), ui.dp(10));
        c.setBackground(ui.shape(pal.surface2, 12));
        c.addView(ui.tvM(value, 15, color));
        c.addView(ui.tv(label, 11, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 3, 0, 0));
        return c;
    }

    View dayLine(String icon, String text, String xp, int color) {
        LinearLayout r = ui.hbox();
        r.setPadding(0, ui.dp(7), 0, ui.dp(7));
        r.addView(ui.tv(icon, 15, pal.text), ui.lpm(ui.dp(26), Ui.W, 0, 0, 0, 0));
        TextView t1 = ui.tv(text, 14, pal.text);
        r.addView(t1, new LinearLayout.LayoutParams(0, Ui.W, 1f));
        r.addView(ui.tvM(xp, 14, color), ui.lpm(Ui.W, Ui.W, 8, 0, 0, 0));
        return r;
    }

    View reportRow(String label, String value, String change, boolean up) {
        LinearLayout r = ui.hbox();
        r.setPadding(0, ui.dp(7), 0, ui.dp(7));
        r.addView(ui.tv(label, 14, pal.dim), new LinearLayout.LayoutParams(0, Ui.W, 1f));
        r.addView(ui.tvM(value, 15, pal.text), ui.lpm(Ui.W, Ui.W, 0, 0, 12, 0));
        TextView c = ui.tvM(change, 12, up ? pal.accent : pal.danger);
        c.setGravity(Gravity.END);
        r.addView(c, new LinearLayout.LayoutParams(ui.dp(74), Ui.W));
        return r;
    }

    // ============================================================ REWARDS
    void buildRewards(LinearLayout col) {
        pad(col);
        col.addView(header(t("Rewards", "পুরস্কার"), t("Your own prizes, levels and badges.", "তোমার নিজের পুরস্কার, লেভেল আর ব্যাজ।")));
        LinearLayout tabs = ui.hbox();
        String[] names = {t("My rewards", "আমার পুরস্কার"), t("Levels", "লেভেল"), t("Badges", "ব্যাজ")};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            tabs.addView(ui.chip(names[i], rewTab == i, v -> { rewTab = idx; rebuild(); }), ui.lpm(Ui.W, Ui.W, 0, 0, 8, 0));
        }
        col.addView(ui.hscroll(tabs), ui.lpm(Ui.M, Ui.W, 0, 0, 0, 14));
        if (rewTab == 0) myRewards(col);
        else if (rewTab == 1) levelsList(col);
        else badgesGrid(col);
    }

    private void myRewards(LinearLayout col) {
        col.addView(ui.button("+  " + t("Add a reward", "পুরস্কার যোগ করো"), true, v -> rewardDialog(null)));
        col.addView(ui.tv(t("You set the prize and the XP it costs. When your total XP reaches it, claim it!",
                "পুরস্কার আর তার XP তুমিই ঠিক করো। মোট XP সেখানে পৌঁছালে ক্লেইম করো!"), 13, pal.dim), ui.lpm(Ui.W, Ui.W, 4, 10, 0, 6));
        List<Store.Reward> rs = st.rewards();
        if (rs.isEmpty()) {
            LinearLayout e = ui.card();
            e.addView(ui.tv(t("No rewards yet. Add something you'd love — a snack, a game hour, a gadget 🎁", "এখনো কোনো পুরস্কার নেই। পছন্দের কিছু যোগ করো — স্ন্যাক, গেম খেলার সময়, গ্যাজেট 🎁"), 14, pal.dim));
            col.addView(e, ui.lpm(Ui.M, Ui.W, 0, 10, 0, 0));
            return;
        }
        int xp = st.totalXp();
        for (final Store.Reward r : rs) {
            LinearLayout c = ui.card();
            boolean unlocked = r.xp <= xp;
            LinearLayout top = ui.hbox();
            LinearLayout nm = ui.vbox();
            TextView title = ui.tvM(r.name, 17, r.claimed ? pal.dim : pal.text);
            nm.addView(title);
            nm.addView(ui.tv(num(r.xp) + " XP", 13, pal.gold), ui.lpm(Ui.W, Ui.W, 0, 3, 0, 0));
            top.addView(nm, new LinearLayout.LayoutParams(0, Ui.W, 1f));
            top.addView(smallBtn("✎", pal.surface2, pal.dim, v -> rewardDialog(r)), new LinearLayout.LayoutParams(ui.dp(36), ui.dp(36)));
            c.addView(top);
            if (r.claimed) {
                LinearLayout cr = ui.hbox();
                cr.addView(ui.tvM("✓ " + t("Claimed", "ক্লেইম করা হয়েছে"), 14, pal.accent), new LinearLayout.LayoutParams(0, Ui.W, 1f));
                cr.addView(ui.chip(t("Undo", "ফেরত"), false, v -> { st.setClaimed(r.id, false); afterChange(); }));
                c.addView(cr, ui.lpm(Ui.M, Ui.W, 0, 12, 0, 0));
            } else if (unlocked) {
                c.addView(ui.button("🎁  " + t("Claim", "ক্লেইম করো"), true, v -> {
                    st.setClaimed(r.id, true);
                    Sfx.levelUp(st, this);
                    final String nme = r.name;
                    afterChange();
                    enqueue(() -> celebrate(t("Reward claimed", "পুরস্কার ক্লেইম"), emoji("🎁"), nme, t("You earned it. Enjoy!", "তুমি এটা অর্জন করেছ। উপভোগ করো!"), null, 0));
                }), ui.lpm(Ui.M, Ui.W, 0, 12, 0, 0));
            } else {
                c.addView(bar(xp / (float) r.xp, pal.gold, 8), ui.lpm(Ui.M, ui.dp(8), 0, 12, 0, 0));
                c.addView(ui.tv(num(r.xp - xp) + t(" XP to go", " XP বাকি"), 13, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 8, 0, 0));
            }
            col.addView(c, ui.lpm(Ui.M, Ui.W, 0, 10, 0, 0));
        }
    }

    TextView emoji(String e) {
        TextView t = ui.tv(e, 52, pal.text);
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(ui.lpm(Ui.W, Ui.W, 0, 10, 0, 0));
        return t;
    }

    void rewardDialog(final Store.Reward edit) {
        LinearLayout d = ui.dialogCard();
        d.addView(ui.tvM(edit == null ? t("New reward", "নতুন পুরস্কার") : t("Edit reward", "পুরস্কার এডিট"), 20, pal.text));
        final EditText n = ui.input(t("e.g. Movie night 🍿", "যেমন: মুভি নাইট 🍿"), edit == null ? "" : edit.name,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        final EditText x = ui.input(t("XP needed (e.g. 2000)", "কত XP লাগবে (যেমন 2000)"), edit == null ? "" : String.valueOf(edit.xp), InputType.TYPE_CLASS_NUMBER);
        d.addView(n, ui.lpm(Ui.M, Ui.W, 0, 14, 0, 0));
        d.addView(x, ui.lpm(Ui.M, Ui.W, 0, 10, 0, 0));
        d.addView(ui.tv(t("Your total XP now: ", "এখন মোট XP: ") + num(st.totalXp()), 12, pal.dim), ui.lpm(Ui.W, Ui.W, 4, 8, 0, 0));
        final Dialog dlg = ui.dialog(d);
        LinearLayout btns = ui.hbox();
        btns.addView(ui.button(t("Cancel", "বাতিল"), false, v -> dlg.dismiss()), ui.lpm(0, Ui.W, 0, 0, 6, 0));
        btns.addView(ui.button(t("Save", "সেভ"), true, v -> {
            String name = Store.clean(n.getText().toString());
            int xp = parseInt(x.getText().toString(), 0);
            if (name.isEmpty() || xp < 1) { snack(t("Enter a name and XP", "নাম আর XP দাও")); return; }
            if (edit == null) st.addReward(name, xp); else st.updateReward(edit.id, name, xp);
            dlg.dismiss();
            afterChange();
        }), ui.lpm(0, Ui.W, 6, 0, 0, 0));
        weight(btns.getChildAt(0));
        weight(btns.getChildAt(1));
        d.addView(btns, ui.lpm(Ui.M, Ui.W, 0, 18, 0, 0));
        if (edit != null) {
            TextView del = ui.tvM(t("Delete reward", "পুরস্কার মুছো"), 14, pal.danger);
            del.setGravity(Gravity.CENTER);
            del.setPadding(0, ui.dp(14), 0, 0);
            del.setOnClickListener(v -> { dlg.dismiss(); confirm(t("Delete reward?", "পুরস্কার মুছবে?"), edit.name, t("Delete", "মুছো"), true, () -> { st.deleteReward(edit.id); afterChange(); }); });
            d.addView(del, ui.lpm(Ui.M, Ui.W, 0, 4, 0, 0));
        }
        dlg.show();
    }

    private void levelsList(LinearLayout col) {
        int lv = st.level();
        for (int i = 0; i < Store.LEVELS.length; i++) {
            LinearLayout c = ui.card();
            c.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
            LinearLayout r = ui.hbox();
            boolean reached = i <= lv;
            TextView num = ui.tvM(String.valueOf(i + 1), 16, reached ? pal.onAccent : pal.dim);
            num.setGravity(Gravity.CENTER);
            num.setBackground(ui.oval(reached ? pal.accent : pal.track));
            r.addView(num, new LinearLayout.LayoutParams(ui.dp(38), ui.dp(38)));
            LinearLayout mid = ui.vbox();
            mid.addView(ui.tvM(Store.levelName(i), 16, i == lv ? pal.accent : reached ? pal.text : pal.dim));
            mid.addView(ui.tv(Store.stageName(i), 12, pal.dim), ui.lpm(Ui.W, Ui.W, 0, 3, 0, 0));
            r.addView(mid, ui.lpm(0, Ui.W, 14, 0, 0, 0));
            weight(mid);
            r.addView(ui.tvM(num(Store.LEVELS[i]) + " XP", 14, reached ? pal.accent : pal.dim));
            c.addView(r);
            if (i == lv && !st.maxLevel()) c.setBackground(ui.shape(pal.surface, 22, pal.accent, 2));
            col.addView(c, ui.lpm(Ui.M, Ui.W, 0, i == 0 ? 0 : 10, 0, 0));
        }
    }

    private void badgesGrid(LinearLayout col) {
        Set<String> have = st.unlockedBadges();
        col.addView(ui.tv(have.size() + " / " + Store.BADGES.length + t(" unlocked", " আনলক"), 14, pal.dim), ui.lpm(Ui.W, Ui.W, 4, 0, 0, 10));
        for (int i = 0; i < Store.BADGES.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            View a = badgeCard(i, have), b = i + 1 < Store.BADGES.length ? badgeCard(i + 1, have) : new View(this);
            row.addView(a, ui.lpm(0, Ui.M, 0, 0, 6, 0));
            row.addView(b, ui.lpm(0, Ui.M, 6, 0, 0, 0));
            weight(a);
            weight(b);
            col.addView(row, ui.lpm(Ui.M, Ui.W, 0, 0, 0, 12));
        }
    }

    private View badgeCard(int i, Set<String> have) {
        String[] b = Store.BADGES[i];
        boolean on = have.contains(b[0]);
        LinearLayout c = ui.card();
        c.setPadding(ui.dp(14), ui.dp(16), ui.dp(14), ui.dp(16));
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView em = ui.tv(b[1], 34, pal.text);
        em.setAlpha(on ? 1f : 0.25f);
        em.setGravity(Gravity.CENTER);
        c.addView(em);
        TextView title = ui.tvM(Lang.bn ? b[3] : b[2], 14, on ? pal.text : pal.dim);
        title.setGravity(Gravity.CENTER);
        c.addView(title, ui.lpm(Ui.M, Ui.W, 0, 8, 0, 0));
        TextView desc = ui.tv(Lang.bn ? b[5] : b[4], 11.5f, pal.dim);
        desc.setGravity(Gravity.CENTER);
        c.addView(desc, ui.lpm(Ui.M, Ui.W, 0, 4, 0, 0));
        if (on) {
            c.addView(ui.tvM("✓", 14, pal.accent), ui.lpm(Ui.W, Ui.W, 0, 8, 0, 0));
        } else {
            int tg = st.badgeTarget(i), pr = Math.min(tg, st.badgeProgress(i));
            c.addView(bar(pr / (float) tg, pal.accent, 6), ui.lpm(Ui.M, ui.dp(6), 0, 10, 0, 0));
        }
        return c;
    }

    static String fmt1(float f) { return String.format(Locale.US, "%.1f", f); }
}

package com.estiuk.studyxp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen, skippable slide tour. Used for the first-run intro and for the per-feature
 * guides in Settings → Tutorials. Swipe or use the buttons.
 */
final class Tutorial extends FrameLayout {
    static final int IMG = 0, EMOJI = 1, EGG = 2, PET = 3;

    static final class Slide {
        int kind;
        String emoji;
        String[] title, body;
        List<String[]> pts = new ArrayList<>();   // each {en, bn}
    }

    private static Slide s(int kind, String emoji, String te, String tb, String be, String bb, String... pts) {
        Slide x = new Slide();
        x.kind = kind;
        x.emoji = emoji;
        x.title = new String[]{te, tb};
        x.body = new String[]{be, bb};
        for (int i = 0; i + 1 < pts.length; i += 2) x.pts.add(new String[]{pts[i], pts[i + 1]});
        return x;
    }

    // ------------------------------------------------------------------ content
    static Slide[] intro() {
        return new Slide[]{
                s(IMG, "", "Meet Pexumi Focus", "Pexumi Focus এ স্বাগতম",
                        "Study, earn XP, grow your forest and raise a tiny buddy. Everything stays on your phone.",
                        "পড়ো, XP জমাও, তোমার বন আর ছোট্ট বন্ধুকে বড় করো। সবকিছু তোমার ফোনেই থাকে।"),
                s(EMOJI, "⏱️", "Focus = XP", "ফোকাস = XP",
                        "Open the Focus tab, pick a subject and start. Every focused minute turns into XP.",
                        "Focus ট্যাবে সাবজেক্ট বেছে শুরু করো। প্রতিটি ফোকাস মিনিট XP হয়ে যায়।",
                        "Free timer or Pomodoro (set your own times)", "ফ্রি টাইমার বা পোমোডোরো (নিজের সময় দিতে পারো)",
                        "Set a custom goal in minutes", "নিজের মতো মিনিটের লক্ষ্য দিতে পারো",
                        "The live timer also shows in your notification", "লাইভ টাইমার নোটিফিকেশনেও দেখা যায়"),
                s(EMOJI, "⚡", "Boost & penalties", "বুস্ট ও পেনাল্টি",
                        "Did something great, or wasted time? Log it in the Boost tab.",
                        "ভালো কিছু করেছ, বা সময় নষ্ট? Boost ট্যাবে লিখে রাখো।",
                        "Add your own bonus and penalty items", "নিজের বোনাস ও পেনাল্টি আইটেম যোগ করো",
                        "Made a mistake? Undo it from the log", "ভুল হলে লগ থেকে আনডু করো"),
                s(EMOJI, "✅", "Plan your day", "দিন সাজাও",
                        "Add subjects and today's tasks in the Plan tab. Ticking a task gives bonus XP.",
                        "Plan ট্যাবে সাবজেক্ট ও আজকের টাস্ক যোগ করো। টাস্ক টিক করলে বোনাস XP পাবে।",
                        "Reach " + Store.STUDY_DAY_XP + " XP in a day to keep your streak 🔥", Store.STUDY_DAY_XP + " XP হলেই স্ট্রিক 🔥 বজায় থাকে"),
                s(EMOJI, "📅", "See your progress", "অগ্রগতি দেখো",
                        "Stats shows weekly charts, a 30-day trend and a calendar. Tap any day for details.",
                        "Stats এ সাপ্তাহিক চার্ট, ৩০ দিনের ট্রেন্ড আর ক্যালেন্ডার আছে। যেকোনো দিনে ট্যাপ করলে বিস্তারিত দেখবে।"),
                s(EGG, "", "Your pet buddy", "তোমার পেট বন্ধু",
                        "A little egg is waiting. Study to " + Store.HATCH_XP + " XP and it hatches, then grows with every level.",
                        "একটা ছোট্ট ডিম অপেক্ষা করছে। " + Store.HATCH_XP + " XP হলে ফুটবে, তারপর প্রতি লেভেলে বড় হবে।",
                        "Pick Pexumi, fox, panda, cat or bunny in Settings", "Settings থেকে Pexumi, শেয়াল, পান্ডা, বিড়াল বা খরগোশ বেছে নাও",
                        "Tap your pet to play with it", "পেটকে ট্যাপ করে খেলো"),
                s(EMOJI, "🏆", "Levels, rewards & badges", "লেভেল, পুরস্কার ও ব্যাজ",
                        "Level up your forest, set your own rewards and collect badges.",
                        "বনের লেভেল বাড়াও, নিজের পুরস্কার ঠিক করো আর ব্যাজ জমাও।",
                        "Badges always match your real progress: if XP drops, so do the badges that depend on it",
                        "ব্যাজ সবসময় আসল অগ্রগতির সাথে মেলে: XP কমলে তার ওপর নির্ভরশীল ব্যাজও কমে"),
                s(EMOJI, "🔒", "Private by design", "প্রাইভেসি আগে",
                        "No account, no ads, no internet permission. Your data never leaves this phone.",
                        "অ্যাকাউন্ট নেই, বিজ্ঞাপন নেই, ইন্টারনেট পারমিশনও নেই। তোমার ডেটা এই ফোনের বাইরে যায় না।",
                        "Optional: allow Usage access so Focus Guard can warn you about social apps",
                        "ঐচ্ছিক: Usage access দিলে Focus Guard সোশ্যাল অ্যাপ নিয়ে সতর্ক করবে",
                        "You can replay this tour anytime in Settings → Tutorials", "এই ট্যুর যেকোনো সময় Settings → Tutorials এ আবার দেখতে পারো")
        };
    }

    /** {emoji, EN title, BN title, EN subtitle, BN subtitle} for each guide. */
    static final String[][] GUIDES = {
            {"⏱️", "Focus & Pomodoro", "ফোকাস ও পোমোডোরো", "Start, pause, goals and breaks", "শুরু, পজ, লক্ষ্য ও বিরতি"},
            {"⚡", "Boost & penalties", "বুস্ট ও পেনাল্টি", "Custom bonuses, lost time, undo", "নিজের বোনাস, নষ্ট সময়, আনডু"},
            {"✅", "Plan & tasks", "প্ল্যান ও টাস্ক", "Subjects, daily tasks, streak", "সাবজেক্ট, টাস্ক, স্ট্রিক"},
            {"📅", "Stats & calendar", "স্ট্যাটস ও ক্যালেন্ডার", "Charts, 30-day trend, day details", "চার্ট, ৩০ দিনের ট্রেন্ড, দিনের বিস্তারিত"},
            {"🐣", "Pet & levels", "পেট ও লেভেল", "Hatching, growing, playing", "ফোটা, বড় হওয়া, খেলা"},
            {"🎁", "Rewards & badges", "পুরস্কার ও ব্যাজ", "Set your own prizes, collect badges", "নিজের পুরস্কার, ব্যাজ জমানো"},
            {"🛡️", "Focus Guard & permissions", "Focus Guard ও পারমিশন", "Usage access, greyed-out switch fix", "Usage access, ধূসর সুইচ ঠিক করা"},
            {"💾", "Backup & resets", "ব্যাকআপ ও রিসেট", "Keep your data safe, fix mistakes", "ডেটা নিরাপদ রাখো, ভুল ঠিক করো"}
    };

    static Slide[] guide(int i) {
        switch (i) {
            case 0:
                return new Slide[]{
                        s(EMOJI, "⏱️", "Start a session", "সেশন শুরু করো",
                                "Go to the Focus tab, choose a subject, then press Start.",
                                "Focus ট্যাবে যাও, সাবজেক্ট বেছে Start চাপো।",
                                "Free mode counts up; Goal mode finishes at your minutes", "ফ্রি মোড উপরের দিকে গোনে; গোল মোড তোমার মিনিটে শেষ হয়",
                                "Tap the goal chip to type any number of minutes", "যেকোনো মিনিট দিতে goal চিপে ট্যাপ করো",
                                "Press Finish to bank your XP", "XP পেতে Finish চাপো"),
                        s(EMOJI, "🍅", "Pomodoro", "পোমোডোরো",
                                "Work in rounds with short breaks. You choose both lengths.",
                                "ছোট বিরতি দিয়ে রাউন্ডে পড়ো। দুটো সময়ই তুমি ঠিক করো।",
                                "Choose Custom to set your own work and break minutes", "Custom বেছে নিজের কাজ ও বিরতির মিনিট দাও",
                                "Break time doesn't count as XP", "বিরতির সময় XP হয় না",
                                "You get an alert when a round or break ends", "রাউন্ড বা বিরতি শেষ হলে অ্যালার্ট পাবে")};
            case 1:
                return new Slide[]{
                        s(EMOJI, "⚡", "Bonuses", "বোনাস",
                                "Tap a bonus to add its XP right away. Make your own with the + button; edit or delete any time.",
                                "বোনাসে ট্যাপ করলেই XP যোগ হয়। + বোতামে নিজের বোনাস বানাও; যখন খুশি এডিট বা ডিলিট করো।"),
                        s(EMOJI, "🚫", "Penalties", "পেনাল্টি",
                                "Wasted 3 minutes? Add a custom penalty with any number. Penalties never take today's XP below zero.",
                                "৩ মিনিট নষ্ট? যেকোনো সংখ্যার কাস্টম পেনাল্টি দাও। পেনাল্টিতে আজকের XP শূন্যের নিচে নামে না।",
                                "Everything is in the log, and you can undo each entry", "সব লগে থাকে, প্রতিটি এন্ট্রি আনডু করা যায়")};
            case 2:
                return new Slide[]{
                        s(EMOJI, "📚", "Subjects", "সাবজেক্ট",
                                "Add your subjects once, then pick them when you focus. Stats shows time per subject.",
                                "সাবজেক্ট একবার যোগ করো, ফোকাসের সময় বেছে নাও। Stats এ সাবজেক্ট ধরে সময় দেখবে।"),
                        s(EMOJI, "✅", "Tasks & streak", "টাস্ক ও স্ট্রিক",
                                "Write today's tasks and tick them off for bonus XP.",
                                "আজকের টাস্ক লেখো আর টিক দিয়ে বোনাস XP নাও।",
                                "Reach " + Store.STUDY_DAY_XP + " XP in a day to count it as a study day", "দিনে " + Store.STUDY_DAY_XP + " XP হলে সেটা স্টাডি ডে",
                                "Study days in a row build your streak 🔥", "পরপর স্টাডি ডে মিলে স্ট্রিক হয় 🔥")};
            case 3:
                return new Slide[]{
                        s(EMOJI, "📊", "Overview", "ওভারভিউ",
                                "See this week's bars, your subjects and your 30-day XP trend.",
                                "এই সপ্তাহের বার, তোমার সাবজেক্ট আর ৩০ দিনের XP ট্রেন্ড দেখো।"),
                        s(EMOJI, "📅", "Calendar", "ক্যালেন্ডার",
                                "Switch to the Calendar chip. Darker days mean more XP. Tap a day to see what you did and delete a wrong entry.",
                                "Calendar চিপে যাও। গাঢ় দিন মানে বেশি XP। কোনো দিনে ট্যাপ করলে কী করেছ দেখবে, ভুল এন্ট্রি মুছতেও পারবে।")};
            case 4:
                return new Slide[]{
                        s(EGG, "", "The egg", "ডিম",
                                "Your buddy starts as an egg and hatches at " + Store.HATCH_XP + " XP.",
                                "তোমার বন্ধু ডিম হিসেবে শুরু করে, " + Store.HATCH_XP + " XP তে ফোটে।"),
                        s(PET, "", "Grow together", "একসাথে বড় হও",
                                "Your pet gets new looks as you level up: leaf, scarf, glasses, glow and a crown. Its mood follows how your day is going.",
                                "লেভেল বাড়লে পেট নতুন সাজ পায়: পাতা, স্কার্ফ, চশমা, আলো আর মুকুট। তোমার দিন কেমন যাচ্ছে তার সাথে ওর মনও বদলায়।",
                                "Change type or rename in Settings → Your pet", "Settings → Your pet এ ধরন বা নাম বদলাও",
                                "Tap it to play", "খেলতে ট্যাপ করো")};
            case 5:
                return new Slide[]{
                        s(EMOJI, "🎁", "Your own rewards", "নিজের পুরস্কার",
                                "In Rewards, add prizes you'd love (a movie night, a treat) and the XP needed. Claim them once unlocked.",
                                "Rewards এ তোমার পছন্দের পুরস্কার (মুভি নাইট, ট্রিট) আর দরকারি XP যোগ করো। আনলক হলে ক্লেইম করো।"),
                        s(EMOJI, "🏅", "Badges", "ব্যাজ",
                                "Badges show what you've earned right now. They follow your real numbers, so if XP, hours or streak go down, the badges that depend on them go too.",
                                "ব্যাজ এখন তুমি যা অর্জন করেছ তা দেখায়। এগুলো আসল সংখ্যার সাথে চলে, তাই XP, ঘণ্টা বা স্ট্রিক কমলে তার ওপর নির্ভরশীল ব্যাজও চলে যায়।")};
            case 6:
                return new Slide[]{
                        s(EMOJI, "🛡️", "Focus Guard", "Focus Guard",
                                "While a session runs, Focus Guard warns you if you open apps like Instagram, Facebook, TikTok or YouTube.",
                                "সেশন চলার সময় Instagram, Facebook, TikTok বা YouTube খুললে Focus Guard সতর্ক করে।",
                                "It needs \"Usage access\" (optional)", "এর জন্য \"Usage access\" লাগে (ঐচ্ছিক)"),
                        s(EMOJI, "🔓", "Switch is greyed out?", "সুইচ ধূসর?",
                                "Android blocks special permissions for apps installed from a file. Fix it once:",
                                "ফাইল থেকে ইনস্টল করা অ্যাপের স্পেশাল পারমিশন Android আটকে রাখে। একবার ঠিক করো:",
                                "1. Settings → Apps → " + Brand.NAME, "১. Settings → Apps → " + Brand.NAME,
                                "2. Tap ⋮ (top right) → Allow restricted settings", "২. উপরে ডানের ⋮ চাপো → Allow restricted settings",
                                "3. Go back and switch Usage access ON", "৩. ফিরে গিয়ে Usage access ON করো")};
            default:
                return new Slide[]{
                        s(EMOJI, "💾", "Backup", "ব্যাকআপ",
                                "Settings → Backup saves a .json file. Keep it in Drive or send it to yourself, and import it on a new phone.",
                                "Settings → Backup এ একটা .json ফাইল সেভ হয়। Drive এ রাখো বা নিজেকে পাঠাও, নতুন ফোনে ইমপোর্ট করো।",
                                "Updating the app by installing the new APK keeps your data", "নতুন APK ইনস্টল করে আপডেট করলে ডেটা থাকে"),
                        s(EMOJI, "🧹", "Resets", "রিসেট",
                                "Reset today removes today's focus, bonuses, penalties, ticked tasks, XP edits and streak credit. Other days stay.",
                                "Reset today আজকের ফোকাস, বোনাস, পেনাল্টি, টাস্ক টিক, XP এডিট ও স্ট্রিক মুছে দেয়। অন্য দিন ঠিক থাকে।",
                                "Adjust total XP fixes a wrong total", "Adjust total XP ভুল মোট XP ঠিক করে",
                                "Reset ALL progress starts fresh (keeps settings and rewards)", "Reset ALL progress নতুন করে শুরু করে (সেটিংস ও পুরস্কার থাকে)")};
        }
    }

    // ------------------------------------------------------------------ view
    private final BaseActivity act;
    private final Ui ui;
    private final Pal pal;
    private final Slide[] slides;
    private final Runnable onClose;
    private final boolean intro;
    private final FrameLayout stage;
    private final LinearLayout dots;
    private final TextView step, next, back;
    private int idx = 0;
    private ValueAnimator bob;
    private final GestureDetector gd;

    Tutorial(final BaseActivity a, Slide[] slides, boolean intro, Runnable onClose) {
        super(a);
        this.act = a;
        this.ui = a.ui;
        this.pal = a.pal;
        this.slides = slides;
        this.intro = intro;
        this.onClose = onClose;
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{pal.surface2, pal.bg});
        setBackground(bg);
        setClickable(true);

        LinearLayout col = ui.vbox();
        col.setPadding(ui.dp(22), a.topInset + ui.dp(14), ui.dp(22), a.bottomInset + ui.dp(18));
        addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout top = ui.hbox();
        step = ui.caps("", pal.dim);
        top.addView(step, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView skip = ui.tvM(intro ? Lang.t("Skip", "স্কিপ") : Lang.t("Close", "বন্ধ"), 15, pal.accent);
        skip.setPadding(ui.dp(14), ui.dp(8), ui.dp(4), ui.dp(8));
        skip.setClickable(true);
        skip.setOnClickListener(v -> close());
        top.addView(skip);
        col.addView(top, ui.lpm(Ui.M, Ui.W, 0, 0, 0, 4));

        stage = new FrameLayout(a);
        col.addView(stage, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        dots = ui.hbox();
        dots.setGravity(Gravity.CENTER);
        col.addView(dots, ui.lpm(Ui.M, Ui.W, 0, 8, 0, 14));

        LinearLayout btns = ui.hbox();
        back = ui.button(Lang.t("Back", "পিছনে"), false, v -> go(-1));
        next = ui.button("", true, v -> go(1));
        btns.addView(back, ui.lpm(0, Ui.W, 0, 0, 6, 0));
        btns.addView(next, ui.lpm(0, Ui.W, 6, 0, 0, 0));
        ((LinearLayout.LayoutParams) back.getLayoutParams()).weight = 1f;
        ((LinearLayout.LayoutParams) next.getLayoutParams()).weight = 2f;
        col.addView(btns, ui.lpm(Ui.M, Ui.W, 0, 0, 0, 0));

        gd = new GestureDetector(a, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null || e2 == null) return false;
                float dx = e2.getX() - e1.getX(), dy = e2.getY() - e1.getY();
                if (Math.abs(dx) > ui.dp(70) && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                    go(dx < 0 ? 1 : -1);
                    return true;
                }
                return false;
            }
        });
        render(0, false);
    }

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        try { gd.onTouchEvent(ev); } catch (Throwable ignored) { }
        return super.dispatchTouchEvent(ev);
    }

    @Override protected void onDetachedFromWindow() {
        if (bob != null) bob.cancel();
        super.onDetachedFromWindow();
    }

    void go(int dir) {
        int n = idx + dir;
        if (n < 0) return;
        if (n >= slides.length) { close(); return; }
        Sfx.tap(act.st, act);
        idx = n;
        render(dir, true);
    }

    void close() {
        if (bob != null) bob.cancel();
        ViewGroup p = (ViewGroup) getParent();
        if (p != null) p.removeView(this);
        if (onClose != null) onClose.run();
    }

    private void render(int dir, boolean animate) {
        Slide sl = slides[idx];
        step.setText((idx + 1) + " / " + slides.length);
        back.setVisibility(idx == 0 ? View.INVISIBLE : View.VISIBLE);
        boolean last = idx == slides.length - 1;
        next.setText(last ? (intro ? Lang.t("Let's go!", "চলো শুরু করি!") : Lang.t("Done", "শেষ")) : Lang.t("Next", "পরের"));

        dots.removeAllViews();
        for (int i = 0; i < slides.length; i++) {
            View d = new View(act);
            d.setBackground(ui.shape(i == idx ? pal.accent : pal.track, 4));
            dots.addView(d, ui.lpm(ui.dp(i == idx ? 22 : 7), ui.dp(7), 3, 0, 3, 0));
        }

        if (bob != null) bob.cancel();
        stage.removeAllViews();
        ScrollView sv = new ScrollView(act);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout c = ui.vbox();
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(0, ui.dp(8), 0, ui.dp(8));

        View vis = visual(sl);
        c.addView(vis);

        TextView t = ui.tvM(sl.title[Lang.bn ? 1 : 0], 26, pal.text);
        t.setGravity(Gravity.CENTER);
        c.addView(t, ui.lpm(Ui.M, Ui.W, 0, 22, 0, 0));
        TextView b = ui.tv(sl.body[Lang.bn ? 1 : 0], 15.5f, pal.dim);
        b.setGravity(Gravity.CENTER);
        b.setLineSpacing(ui.dp(3), 1f);
        c.addView(b, ui.lpm(Ui.M, Ui.W, 6, 10, 6, 0));

        if (!sl.pts.isEmpty()) {
            LinearLayout box = ui.vbox();
            box.setPadding(ui.dp(16), ui.dp(12), ui.dp(16), ui.dp(12));
            box.setBackground(ui.shape(pal.surface, 18, pal.stroke, 1));
            for (int i = 0; i < sl.pts.size(); i++) {
                LinearLayout r = ui.hbox();
                r.setGravity(Gravity.TOP);
                TextView dot = ui.tvM("•", 15, pal.accent);
                r.addView(dot, ui.lpm(ui.dp(16), Ui.W, 0, 0, 0, 0));
                TextView tx = ui.tv(sl.pts.get(i)[Lang.bn ? 1 : 0], 14, pal.text);
                tx.setLineSpacing(ui.dp(2), 1f);
                r.addView(tx, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                box.addView(r, ui.lpm(Ui.M, Ui.W, 0, i == 0 ? 0 : 8, 0, 0));
            }
            c.addView(box, ui.lpm(Ui.M, Ui.W, 0, 18, 0, 0));
        }
        sv.addView(c, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        stage.addView(sv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (sl.kind == EMOJI) {
            bob = ValueAnimator.ofFloat(0f, 1f);
            bob.setDuration(1100);
            bob.setRepeatCount(ValueAnimator.INFINITE);
            bob.setRepeatMode(ValueAnimator.REVERSE);
            bob.setInterpolator(new AccelerateDecelerateInterpolator());
            final View target = vis;
            bob.addUpdateListener(a -> target.setTranslationY(-ui.dp(9) * (Float) a.getAnimatedValue()));
            bob.start();
        }
        if (animate) {
            sv.setAlpha(0f);
            sv.setTranslationX(dir * ui.dp(36));
            sv.animate().alpha(1f).translationX(0).setDuration(240).start();
        }
    }

    private View visual(Slide sl) {
        FrameLayout box = new FrameLayout(act);
        switch (sl.kind) {
            case IMG: {
                ImageView iv = new ImageView(act);
                iv.setImageResource(R.drawable.pexumi_duo);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                final float r = ui.dp(28);
                iv.setOutlineProvider(new ViewOutlineProvider() {
                    @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r); }
                });
                iv.setClipToOutline(true);
                box.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(200)));
                box.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(200)));
                return box;
            }
            case EGG:
            case PET: {
                PetView pv = new PetView(act);
                pv.setBubbleColors(pal.surface, pal.text);
                String say = sl.kind == EGG ? Lang.t("Tap me!", "আমাকে ট্যাপ করো!") : Lang.t("Let's study together!", "চলো একসাথে পড়ি!");
                pv.configure(act.st.petType(), sl.kind == EGG ? 0 : Math.max(2, act.st.level()), sl.kind == EGG, 0.7f, 3, false, say);
                box.addView(pv, new FrameLayout.LayoutParams(ui.dp(210), ui.dp(210), Gravity.CENTER));
                box.setLayoutParams(new LinearLayout.LayoutParams(ui.dp(210), ui.dp(210)));
                return box;
            }
            default: {
                TextView glow = new TextView(act);
                glow.setBackground(ui.oval(Ui.alpha(pal.accent, 38)));
                box.addView(glow, new FrameLayout.LayoutParams(ui.dp(170), ui.dp(170), Gravity.CENTER));
                TextView e = ui.tv(sl.emoji, 76, pal.text);
                e.setGravity(Gravity.CENTER);
                box.addView(e, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
                TextView sp1 = ui.tv("✨", 22, pal.text);
                FrameLayout.LayoutParams l1 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
                l1.topMargin = ui.dp(6);
                l1.rightMargin = ui.dp(34);
                box.addView(sp1, l1);
                TextView sp2 = ui.tv("🌿", 20, pal.text);
                FrameLayout.LayoutParams l2 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
                l2.bottomMargin = ui.dp(10);
                l2.leftMargin = ui.dp(36);
                box.addView(sp2, l2);
                box.setLayoutParams(new LinearLayout.LayoutParams(ui.dp(210), ui.dp(180)));
                return box;
            }
        }
    }
}

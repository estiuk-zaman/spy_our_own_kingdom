package com.estiuk.studyxp;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** All data + XP rules for istii_the_great. Everything lives in local SharedPreferences. */
final class Store {
    static final Object LOCK = new Object();

    static final int[] LEVELS = {0, 500, 1500, 3000, 5000, 8000, 12000};
    static final String[] NAMES_EN = {"Beginner", "Learner", "Builder", "Problem Solver", "Engineer", "Advanced", "Master"};
    static final String[] NAMES_BN = {"নবীন", "শিক্ষার্থী", "নির্মাতা", "সমস্যা সমাধানকারী", "ইঞ্জিনিয়ার", "অ্যাডভান্সড", "মাস্টার"};
    static final String[] STAGES_EN = {"Seed", "Sprout", "Sapling", "Young Pine", "Evergreen", "Grove", "Ancient Forest"};
    static final String[] STAGES_BN = {"বীজ", "অঙ্কুর", "চারাগাছ", "তরুণ পাইন", "চিরসবুজ", "কুঞ্জবন", "প্রাচীন অরণ্য"};
    static final String[] BONUS_EN = {
            "Concept understood independently", "Problem solved independently", "Code written independently",
            "Explained in own words", "Mistake found & fixed", "Practice / Revision", "Wall / hard problem attempt"};
    static final String[] BONUS_BN = {
            "নিজে কনসেপ্ট বুঝেছি", "নিজে সমস্যা সমাধান করেছি", "নিজে কোড লিখেছি",
            "নিজের ভাষায় ব্যাখ্যা করেছি", "ভুল খুঁজে ঠিক করেছি", "অনুশীলন / রিভিশন", "কঠিন সমস্যার চেষ্টা"};
    static final int[] BONUS_VALS = {10, 15, 15, 10, 15, 10, 15};
    static final int STUDY_DAY_XP = 20;

    static final String[] TRACKED_NAMES = {"Instagram", "Facebook", "TikTok"};
    static final String[][] TRACKED = {
            {"com.instagram.android", "com.instagram.lite"},
            {"com.facebook.katana", "com.facebook.lite"},
            {"com.zhiliaoapp.musically", "com.ss.android.ugc.trill"}};
    /** Apps that trigger the Focus Guard alert while a session runs. */
    static final String[] DISTRACT = {"com.instagram.android", "com.instagram.lite", "com.facebook.katana", "com.facebook.lite",
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.google.android.youtube", "com.twitter.android",
            "com.snapchat.android", "com.reddit.frontpage"};

    // badges: id, emoji, EN title, BN title, EN description, BN description
    static final String[][] BADGES = {
            {"first", "🌱", "First step", "প্রথম পদক্ষেপ", "Finish your first focus session", "প্রথম ফোকাস সেশন শেষ করো"},
            {"xp100", "⭐", "100 XP", "১০০ XP", "Reach 100 total XP", "মোট ১০০ XP অর্জন করো"},
            {"xp500", "🌿", "500 XP", "৫০০ XP", "Reach 500 total XP", "মোট ৫০০ XP অর্জন করো"},
            {"xp1500", "🌳", "1,500 XP", "১,৫০০ XP", "Reach 1,500 total XP", "মোট ১,৫০০ XP অর্জন করো"},
            {"xp5000", "🏆", "5,000 XP", "৫,০০০ XP", "Reach 5,000 total XP", "মোট ৫,০০০ XP অর্জন করো"},
            {"xp12000", "👑", "Master", "মাস্টার", "Reach 12,000 total XP", "মোট ১২,০০০ XP অর্জন করো"},
            {"streak3", "🔥", "3-day streak", "৩ দিনের স্ট্রিক", "Study 3 days in a row", "পরপর ৩ দিন পড়ো"},
            {"streak7", "🔥", "7-day streak", "৭ দিনের স্ট্রিক", "Study 7 days in a row", "পরপর ৭ দিন পড়ো"},
            {"streak30", "☄️", "30-day streak", "৩০ দিনের স্ট্রিক", "Study 30 days in a row", "পরপর ৩০ দিন পড়ো"},
            {"hours10", "⏱️", "10 hours", "১০ ঘণ্টা", "Focus for 10 hours in total", "মোট ১০ ঘণ্টা ফোকাস করো"},
            {"hours50", "⌛", "50 hours", "৫০ ঘণ্টা", "Focus for 50 hours in total", "মোট ৫০ ঘণ্টা ফোকাস করো"},
            {"hours100", "💎", "100 hours", "১০০ ঘণ্টা", "Focus for 100 hours in total", "মোট ১০০ ঘণ্টা ফোকাস করো"},
            {"pomo4", "🍅", "Pomodoro x4", "পোমোডোরো x৪", "Complete 4 pomodoros", "৪টি পোমোডোরো শেষ করো"},
            {"tasks25", "✅", "Task master", "টাস্ক মাস্টার", "Complete 25 tasks", "২৫টি টাস্ক শেষ করো"},
            {"night", "🦉", "Night owl", "রাতজাগা পাখি", "Finish a session after 10 PM", "রাত ১০টার পরে একটি সেশন শেষ করো"},
            {"early", "🌅", "Early bird", "ভোরের পাখি", "Finish a session between 4 and 7 AM", "ভোর ৪টা–৭টার মধ্যে একটি সেশন শেষ করো"},
            {"long", "🧠", "Deep work", "ডিপ ওয়ার্ক", "Focus 90+ minutes in one session", "এক সেশনে ৯০+ মিনিট ফোকাস করো"},
            {"clean", "🛡️", "Clean focus", "নিখুঁত ফোকাস", "A 25+ min session with zero distractions", "২৫+ মিনিটের সেশন, একটাও distraction ছাড়া"},
            {"perfect", "🌟", "Perfect week", "পারফেক্ট সপ্তাহ", "7 study days in the last 7 days", "শেষ ৭ দিনের সব দিন পড়ো"},
            {"subjects3", "🎨", "Explorer", "এক্সপ্লোরার", "Study 3 different subjects", "৩টি আলাদা সাবজেক্ট পড়ো"}};

    final Context ctx;
    final SharedPreferences p;

    Store(Context c) {
        ctx = c.getApplicationContext();
        p = ctx.getSharedPreferences("studyxp", 0);
        if (!p.contains("rewards")) {
            p.edit().putString("rewards", defaultRewards()).apply();
        } else if (!p.getBoolean("rwMig", false)) {
            // one-time: the old placeholder "Hair Dryer" reward (never claimed) is swapped for friendlier defaults
            try {
                JSONArray a = new JSONArray(p.getString("rewards", "[]"));
                if (a.length() == 1 && a.getJSONObject(0).optString("n").startsWith("Hair Dryer") && !a.getJSONObject(0).optBoolean("c")) {
                    p.edit().putString("rewards", defaultRewards()).apply();
                }
            } catch (Exception ignored) {
            }
            p.edit().putBoolean("rwMig", true).apply();
        }
        if (!p.contains("subjects")) p.edit().putString("subjects", "General|0").apply();
        if (!p.contains("bonusList")) {
            try {
                JSONArray a = new JSONArray();
                for (int i = 0; i < BONUS_VALS.length; i++) {
                    a.put(new JSONObject().put("id", "d" + i).put("n", "").put("x", BONUS_VALS[i]).put("d", i));
                }
                p.edit().putString("bonusList", a.toString()).apply();
            } catch (Exception ignored) {
            }
        }
    }

    static String defaultRewards() {
        try {
            JSONArray a = new JSONArray();
            String[] n = {"Favourite snack 🍫", "Movie night 🍿", "Gaming session 🎮", "Treat yourself 🍕"};
            int[] x = {200, 600, 1500, 5000};
            for (int i = 0; i < n.length; i++) a.put(new JSONObject().put("id", "r" + i).put("n", n[i]).put("x", x[i]).put("c", false));
            return a.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    // ============================================================ custom bonus list
    static final class Bonus {
        String id, name;   // name empty => built-in, localised
        int xp, def = -1;
    }

    List<Bonus> bonuses() {
        List<Bonus> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(p.getString("bonusList", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Bonus b = new Bonus();
                b.id = o.optString("id");
                b.name = o.optString("n");
                b.xp = o.optInt("x", 10);
                b.def = o.optInt("d", -1);
                out.add(b);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    static String bonusName(Bonus b) {
        if (!b.name.isEmpty()) return b.name;
        if (b.def >= 0 && b.def < BONUS_EN.length) return Lang.bn ? BONUS_BN[b.def] : BONUS_EN[b.def];
        return "Bonus";
    }

    private void saveBonuses(List<Bonus> l) {
        try {
            JSONArray a = new JSONArray();
            for (Bonus b : l) a.put(new JSONObject().put("id", b.id).put("n", b.name).put("x", b.xp).put("d", b.def));
            p.edit().putString("bonusList", a.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    void addBonusItem(String name, int xp) {
        List<Bonus> l = bonuses();
        Bonus b = new Bonus();
        b.id = "b" + Long.toString(System.nanoTime(), 36);
        b.name = clean(name);
        b.xp = Math.max(1, Math.min(500, xp));
        l.add(b);
        saveBonuses(l);
    }

    void updateBonusItem(String id, String name, int xp) {
        List<Bonus> l = bonuses();
        for (Bonus b : l) if (b.id.equals(id)) { b.name = clean(name); b.xp = Math.max(1, Math.min(500, xp)); }
        saveBonuses(l);
    }

    void deleteBonusItem(String id) {
        List<Bonus> l = bonuses();
        for (int i = 0; i < l.size(); i++) if (l.get(i).id.equals(id)) { l.remove(i); break; }
        saveBonuses(l);
    }

    /** Applies a bonus and writes it to the day log. */
    void useBonus(Bonus b) {
        addBonus(b.xp);
        logAct("B", bonusName(b), b.xp);
    }

    /** Takes one use of the bonus back. Returns false when today has no such bonus to remove. */
    boolean undoBonus(Bonus b) {
        if (bonus() < b.xp) return false;
        addBonus(-b.xp);
        unlogAct("B", bonusName(b), b.xp);
        return true;
    }

    void usePenalty(int mins) {
        addPenalty(mins);
        logAct("P", Lang.bn ? "নষ্ট সময়" : "Wasted time", mins);
    }

    boolean undoPenalty(int mins) {
        if (penalty() <= 0) return false;
        int m = Math.min(mins, penalty());
        addPenalty(-m);
        unlogAct("P", null, mins);
        return true;
    }

    // ============================================================ day activity log (bonuses / tasks / penalties)
    /** Lines: day|K|xp|title   K = B bonus, T task, P penalty. */
    void logAct(String kind, String title, int xp) {
        String line = date() + "|" + kind + "|" + xp + "|" + clean(title);
        String old = p.getString("alog", "");
        String all = old.isEmpty() ? line : old + "\n" + line;
        String[] ls = all.split("\n");
        if (ls.length > 400) {
            StringBuilder sb = new StringBuilder();
            for (int i = ls.length - 400; i < ls.length; i++) { if (sb.length() > 0) sb.append('\n'); sb.append(ls[i]); }
            all = sb.toString();
        }
        p.edit().putString("alog", all).apply();
    }

    /** Removes the newest matching line of today. title == null matches any title; xp <= 0 matches any xp. */
    void unlogAct(String kind, String title, int xp) {
        String all = p.getString("alog", "");
        if (all.isEmpty()) return;
        String[] ls = all.split("\n");
        String today = date();
        int hit = -1;
        for (int i = ls.length - 1; i >= 0 && hit < 0; i--) {
            String[] a = ls[i].split("\\|", 4);
            if (a.length < 4 || !a[0].equals(today) || !a[1].equals(kind)) continue;
            if (title != null && !a[3].equals(clean(title))) continue;
            if (xp > 0) { try { if (Integer.parseInt(a[2]) != xp) continue; } catch (Exception e) { continue; } }
            hit = i;
        }
        if (hit < 0) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ls.length; i++) {
            if (i == hit) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(ls[i]);
        }
        p.edit().putString("alog", sb.toString()).apply();
    }

    /** Entries for a day, oldest first: {kind, title, xp}. */
    List<String[]> actsOn(String day) {
        List<String[]> out = new ArrayList<>();
        String all = p.getString("alog", "");
        if (all.isEmpty()) return out;
        for (String line : all.split("\n")) {
            String[] a = line.split("\\|", 4);
            if (a.length == 4 && a[0].equals(day)) out.add(new String[]{a[1], a[3], a[2]});
        }
        return out;
    }

    /** Sessions logged on a day, oldest first: {time, minutes, subject}. */
    List<String[]> sessionsOn(String day) {
        List<String[]> out = new ArrayList<>();
        for (String[] s : recentSessions(150)) if (s[0].equals(day)) out.add(0, new String[]{s[1], s[2], s[3]});
        return out;
    }

    // ============================================================ pet
    static final int HATCH_XP = 20;
    /** 0 fox, 1 panda, 2 cat, 3 bunny, 4 Pexumi (the app mascot, default). */
    int petType() { return p.getInt("petType", 4); }
    void setPetType(int t) { p.edit().putInt("petType", Math.max(0, Math.min(4, t))).apply(); }
    String petName() { return p.getString("petName", "Pexumi"); }
    void setPetName(String s) { p.edit().putString("petName", s.trim().isEmpty() ? "Pexumi" : clean(s)).apply(); }
    boolean hatched() { return totalXp() >= HATCH_XP; }

    /** True once, the first time the pet has hatched. */
    boolean pollHatch() {
        if (hatched() && !p.getBoolean("hatchSeen", false)) {
            p.edit().putBoolean("hatchSeen", true).apply();
            return true;
        }
        return false;
    }

    /** 0 sleepy (nothing today), 1 warming up, 2 over half the goal, 3 goal reached. */
    int petMood() {
        if (focus() == 0 && bonus() == 0) return 0;
        float f = focus() / (float) Math.max(1, goalMin());
        return f >= 1f ? 3 : f >= 0.5f ? 2 : 1;
    }

    // ============================================================ dates
    static String fmt(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    String date() { return fmt(Calendar.getInstance()); }

    String dayOffset(int n) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, n);
        return fmt(c);
    }

    void rollDay() {
        String today = date();
        String saved = p.getString("day", "");
        if (!today.equals(saved)) {
            SharedPreferences.Editor e = p.edit();
            if (!saved.isEmpty()) {
                e.putString("h_" + saved, p.getInt("todayFocus", 0) + "," + p.getInt("todayBonus", 0) + "," + p.getInt("todayPenalty", 0));
            }
            e.putString("day", today).putInt("todayFocus", 0).putInt("todayBonus", 0).putInt("todayPenalty", 0)
                    .putInt("todayNet", 0).putInt("loggedNet", 0).putLong("usageBase", 0).putLong("usageOff", 0).apply();
            // drop tasks that were completed on an earlier day
            try {
                JSONArray a = new JSONArray(p.getString("tasks", "[]")), out = new JSONArray();
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    if (!(o.optBoolean("d") && !today.equals(o.optString("dd")))) out.put(o);
                }
                p.edit().putString("tasks", out.toString()).apply();
            } catch (Exception ignored) {
            }
        }
    }

    // ============================================================ XP
    int focus() { return p.getInt("todayFocus", 0); }
    int bonus() { return p.getInt("todayBonus", 0); }
    int penalty() { return p.getInt("todayPenalty", 0); }
    int net() { return focus() + bonus() - penalty(); }
    int totalXp() { return p.getInt("totalXp", 0); }

    void sync() {
        int net = net();
        int prev = p.getInt("loggedNet", 0);
        if (net != prev) {
            p.edit().putInt("totalXp", Math.max(0, totalXp() + net - prev)).putInt("loggedNet", net).apply();
        }
        updateStreak();
    }

    void addFocus(int mins, String subject) {
        p.edit().putInt("todayFocus", focus() + mins)
                .putInt("totFocus", p.getInt("totFocus", 0) + mins)
                .putInt("totSessions", p.getInt("totSessions", 0) + 1)
                .putInt("maxSession", Math.max(p.getInt("maxSession", 0), mins)).apply();
        String s = clean(subject);
        if (!s.isEmpty()) p.edit().putInt("sm_" + s, p.getInt("sm_" + s, 0) + mins).apply();
        logSession(mins, s);
        sync();
    }

    void addBonus(int x) { p.edit().putInt("todayBonus", Math.max(0, bonus() + x)).apply(); sync(); }
    void addPenalty(int m) { p.edit().putInt("todayPenalty", Math.max(0, penalty() + m)).apply(); sync(); }

    /** Manual correction of the overall XP (e.g. XP was added by mistake). */
    void adjustTotal(int delta, String note) {
        p.edit().putInt("totalXp", Math.max(0, totalXp() + delta)).apply();
        String line = date() + "|" + delta + "|" + clean(note);
        String old = p.getString("adj", "");
        String all = old.isEmpty() ? line : old + "\n" + line;
        String[] ls = all.split("\n");
        if (ls.length > 30) {
            StringBuilder sb = new StringBuilder();
            for (int i = ls.length - 30; i < ls.length; i++) { if (sb.length() > 0) sb.append('\n'); sb.append(ls[i]); }
            all = sb.toString();
        }
        p.edit().putString("adj", all).apply();
    }

    // ============================================================ levels
    int level() {
        int xp = totalXp(), lv = 0;
        for (int i = 0; i < LEVELS.length; i++) if (xp >= LEVELS[i]) lv = i;
        return lv;
    }

    boolean maxLevel() { return level() >= LEVELS.length - 1; }

    float levelProgress() {
        int lv = level();
        if (lv >= LEVELS.length - 1) return 1f;
        int base = LEVELS[lv], next = LEVELS[lv + 1];
        return Math.max(0f, Math.min(1f, (totalXp() - base) / (float) (next - base)));
    }

    static String levelName(int i) { return Lang.bn ? NAMES_BN[i] : NAMES_EN[i]; }
    static String stageName(int i) { return Lang.bn ? STAGES_BN[i] : STAGES_EN[i]; }

    int pollLevelUp() {
        int lv = level();
        int seen = p.getInt("seenLevel", -1);
        p.edit().putInt("seenLevel", lv).apply();
        return (seen >= 0 && lv > seen) ? lv : -1;
    }

    // ============================================================ streak
    void updateStreak() {
        String d = date();
        String last = p.getString("lastStudyDay", "");
        if (net() >= STUDY_DAY_XP && !d.equals(last)) {
            int s = p.getInt("streak", 0);
            s = last.equals(dayOffset(-1)) ? s + 1 : 1;
            p.edit().putString("lastStudyDay", d).putInt("streak", s)
                    .putString("pbDay", d).putInt("pb", p.getInt("bestStreak", 0))
                    .putInt("bestStreak", Math.max(s, p.getInt("bestStreak", 0))).apply();
        }
    }

    int streak() {
        String last = p.getString("lastStudyDay", "");
        if (last.equals(date()) || last.equals(dayOffset(-1))) return p.getInt("streak", 0);
        return 0;
    }

    int bestStreak() { return Math.max(p.getInt("bestStreak", 0), streak()); }
    boolean studyDayDone() { return date().equals(p.getString("lastStudyDay", "")); }

    boolean pollStudyDay() {
        if (studyDayDone() && !date().equals(p.getString("celebrated", ""))) {
            p.edit().putString("celebrated", date()).apply();
            return true;
        }
        return false;
    }

    // ============================================================ history
    int[] dayStats(String day) {
        if (day.equals(date())) return new int[]{focus(), bonus(), penalty()};
        String s = p.getString("h_" + day, null);
        if (s == null) return new int[]{0, 0, 0};
        try {
            String[] a = s.split(",");
            return new int[]{Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2])};
        } catch (Exception e) {
            return new int[]{0, 0, 0};
        }
    }

    int dayNetOffset(int off) {
        int[] d = dayStats(dayOffset(off));
        return d[0] + d[1] - d[2];
    }

    int[] weekNet() {
        int[] r = new int[7];
        for (int i = 0; i < 7; i++) r[i] = dayNetOffset(i - 6);
        return r;
    }

    int[] weekFocus() {
        int[] r = new int[7];
        for (int i = 0; i < 7; i++) r[i] = dayStats(dayOffset(i - 6))[0];
        return r;
    }

    String[] weekLabels() {
        String[] en = {"S", "M", "T", "W", "T", "F", "S"};
        String[] bn = {"র", "সো", "ম", "বু", "বৃ", "শু", "শ"};
        String[] r = new String[7];
        for (int i = 0; i < 7; i++) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE, i - 6);
            r[i] = (Lang.bn ? bn : en)[c.get(Calendar.DAY_OF_WEEK) - 1];
        }
        return r;
    }

    /** {net, focus, studyDays, bestDay} for 7 days; weeksBack 0 = last 7 days, 1 = the 7 before. */
    int[] weekAgg(int weeksBack) {
        int net = 0, foc = 0, days = 0, best = 0;
        for (int k = 0; k < 7; k++) {
            int off = -weeksBack * 7 - k;
            int[] d = dayStats(dayOffset(off));
            int n = d[0] + d[1] - d[2];
            net += n;
            foc += d[0];
            if (n >= STUDY_DAY_XP) days++;
            best = Math.max(best, n);
        }
        return new int[]{net, foc, days, best};
    }

    int bestDayNet() {
        int best = Math.max(0, net());
        for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
            if (e.getKey().startsWith("h_") && e.getValue() instanceof String) {
                try {
                    String[] a = ((String) e.getValue()).split(",");
                    best = Math.max(best, Integer.parseInt(a[0]) + Integer.parseInt(a[1]) - Integer.parseInt(a[2]));
                } catch (Exception ignored) {
                }
            }
        }
        return best;
    }

    int totalFocusMin() { return Math.max(p.getInt("totFocus", 0), 0); }
    int totalSessions() { return p.getInt("totSessions", 0); }
    int totalStrikes() { return p.getInt("totStrikes", 0); }

    // ============================================================ sessions log
    static String clean(String s) {
        return s == null ? "" : s.replace('|', ' ').replace('\n', ' ').trim();
    }

    private void logSession(int mins, String subject) {
        String t = new SimpleDateFormat("yyyy-MM-dd|HH:mm", Locale.US).format(new Date());
        String line = t + "|" + mins + "|" + clean(subject);
        String old = p.getString("sessions", "");
        String all = old.isEmpty() ? line : old + "\n" + line;
        String[] ls = all.split("\n");
        if (ls.length > 150) {
            StringBuilder sb = new StringBuilder();
            for (int i = ls.length - 150; i < ls.length; i++) { if (sb.length() > 0) sb.append('\n'); sb.append(ls[i]); }
            all = sb.toString();
        }
        p.edit().putString("sessions", all).apply();
    }

    /** Newest first. Each entry: {day, time, minutes, subject, lineIndex}. */
    List<String[]> recentSessions(int max) {
        List<String[]> out = new ArrayList<>();
        String all = p.getString("sessions", "");
        if (all.isEmpty()) return out;
        String[] ls = all.split("\n");
        for (int i = ls.length - 1; i >= 0 && out.size() < max; i--) {
            String[] a = ls[i].split("\\|", 4);
            if (a.length >= 3) out.add(new String[]{a[0], a[1], a[2], a.length > 3 ? a[3] : "", String.valueOf(i)});
        }
        return out;
    }

    /** Removes one logged session and takes its XP back. */
    boolean deleteSession(int idx) {
        String all = p.getString("sessions", "");
        if (all.isEmpty()) return false;
        String[] ls = all.split("\n");
        if (idx < 0 || idx >= ls.length) return false;
        String[] a = ls[idx].split("\\|", 4);
        if (a.length < 3) return false;
        int mins;
        try { mins = Integer.parseInt(a[2]); } catch (Exception e) { return false; }
        String day = a[0], subj = a.length > 3 ? a[3] : "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ls.length; i++) {
            if (i == idx) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(ls[i]);
        }
        SharedPreferences.Editor e = p.edit();
        e.putString("sessions", sb.toString());
        e.putInt("totFocus", Math.max(0, p.getInt("totFocus", 0) - mins));
        e.putInt("totSessions", Math.max(0, p.getInt("totSessions", 0) - 1));
        if (!subj.isEmpty()) e.putInt("sm_" + subj, Math.max(0, p.getInt("sm_" + subj, 0) - mins));
        if (day.equals(date())) {
            e.putInt("todayFocus", Math.max(0, focus() - mins)).apply();
            sync();
        } else {
            String h = p.getString("h_" + day, null);
            if (h != null) {
                try {
                    String[] x = h.split(",");
                    e.putString("h_" + day, Math.max(0, Integer.parseInt(x[0]) - mins) + "," + x[1] + "," + x[2]);
                } catch (Exception ignored) {
                }
            }
            e.putInt("totalXp", Math.max(0, totalXp() - mins)).apply();
        }
        return true;
    }

    // ============================================================ subjects
    List<String[]> subjects() {
        List<String[]> out = new ArrayList<>();
        String s = p.getString("subjects", "");
        if (s.isEmpty()) return out;
        for (String line : s.split("\n")) {
            String[] a = line.split("\\|");
            if (a.length >= 1 && !a[0].isEmpty()) out.add(new String[]{a[0], a.length > 1 ? a[1] : "0"});
        }
        return out;
    }

    private void saveSubjects(List<String[]> l) {
        StringBuilder sb = new StringBuilder();
        for (String[] a : l) { if (sb.length() > 0) sb.append('\n'); sb.append(a[0]).append('|').append(a[1]); }
        p.edit().putString("subjects", sb.toString()).apply();
    }

    boolean addSubject(String name, int color) {
        name = clean(name);
        if (name.isEmpty()) return false;
        List<String[]> l = subjects();
        for (String[] a : l) if (a[0].equalsIgnoreCase(name)) return false;
        l.add(new String[]{name, String.valueOf(color)});
        saveSubjects(l);
        return true;
    }

    void deleteSubject(String name) {
        List<String[]> l = subjects();
        for (int i = 0; i < l.size(); i++) if (l.get(i)[0].equals(name)) { l.remove(i); break; }
        saveSubjects(l);
        if (name.equals(lastSubject())) setLastSubject("");
    }

    int subjectColor(String name) {
        for (String[] a : subjects()) {
            if (a[0].equals(name)) {
                try { return Pal.SUBJ[Integer.parseInt(a[1]) % Pal.SUBJ.length]; } catch (Exception ignored) { }
            }
        }
        return 0xFF8CA999;
    }

    String lastSubject() { return p.getString("lastSubject", ""); }
    void setLastSubject(String s) { p.edit().putString("lastSubject", clean(s)).apply(); }

    /** All-time minutes per subject, biggest first. Entries: {name, minutes}. */
    List<String[]> subjectTotals() {
        List<String[]> out = new ArrayList<>();
        for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
            if (e.getKey().startsWith("sm_") && e.getValue() instanceof Integer && (Integer) e.getValue() > 0) {
                out.add(new String[]{e.getKey().substring(3), String.valueOf(e.getValue())});
            }
        }
        java.util.Collections.sort(out, (x, y) -> Integer.parseInt(y[1]) - Integer.parseInt(x[1]));
        return out;
    }

    /** Subject with the most minutes in the last 7 days: {name, minutes} or null. */
    String[] topSubjectWeek() {
        String from = dayOffset(-6);
        Map<String, Integer> m = new HashMap<>();
        for (String[] s : recentSessions(150)) {
            if (s[0].compareTo(from) >= 0 && !s[3].isEmpty()) {
                try { m.put(s[3], (m.containsKey(s[3]) ? m.get(s[3]) : 0) + Integer.parseInt(s[2])); } catch (Exception ignored) { }
            }
        }
        String best = null;
        int bv = 0;
        for (Map.Entry<String, Integer> e : m.entrySet()) if (e.getValue() > bv) { bv = e.getValue(); best = e.getKey(); }
        return best == null ? null : new String[]{best, String.valueOf(bv)};
    }

    // ============================================================ daily goal
    int goalMin() { return p.getInt("goalMin", 90); }
    void setGoalMin(int m) { p.edit().putInt("goalMin", Math.max(5, Math.min(900, m))).apply(); }

    // ============================================================ tasks
    static final class Task {
        String id, title, subject;
        int xp;
        boolean done;
    }

    List<Task> tasks() {
        List<Task> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(p.getString("tasks", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Task t = new Task();
                t.id = o.optString("id");
                t.title = o.optString("t");
                t.subject = o.optString("s");
                t.xp = o.optInt("x", 10);
                t.done = o.optBoolean("d");
                out.add(t);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    void addTask(String title, String subject, int xp) {
        try {
            JSONArray a = new JSONArray(p.getString("tasks", "[]"));
            a.put(new JSONObject().put("id", Long.toString(System.nanoTime(), 36)).put("t", clean(title)).put("s", clean(subject)).put("x", xp).put("d", false));
            p.edit().putString("tasks", a.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    /** Toggles a task. Returns the XP delta that was applied (+xp when completed, -xp when un-done). */
    int toggleTask(String id) {
        try {
            JSONArray a = new JSONArray(p.getString("tasks", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                if (o.optString("id").equals(id)) {
                    boolean nowDone = !o.optBoolean("d");
                    int xp = o.optInt("x", 10);
                    String ttl = o.optString("t");
                    o.put("d", nowDone).put("dd", nowDone ? date() : "");
                    p.edit().putString("tasks", a.toString()).apply();
                    if (nowDone) {
                        p.edit().putInt("totTasks", p.getInt("totTasks", 0) + 1).apply();
                        addBonus(xp);
                        logAct("T", ttl, xp);
                        return xp;
                    }
                    p.edit().putInt("totTasks", Math.max(0, p.getInt("totTasks", 0) - 1)).apply();
                    addBonus(-xp);
                    unlogAct("T", ttl, xp);
                    return -xp;
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    void deleteTask(String id) {
        try {
            JSONArray a = new JSONArray(p.getString("tasks", "[]")), out = new JSONArray();
            for (int i = 0; i < a.length(); i++) if (!a.getJSONObject(i).optString("id").equals(id)) out.put(a.getJSONObject(i));
            p.edit().putString("tasks", out.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    // ============================================================ custom rewards
    static final class Reward {
        String id, name;
        int xp;
        boolean claimed;
    }

    List<Reward> rewards() {
        List<Reward> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(p.getString("rewards", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Reward r = new Reward();
                r.id = o.optString("id");
                r.name = o.optString("n");
                r.xp = o.optInt("x");
                r.claimed = o.optBoolean("c");
                out.add(r);
            }
        } catch (Exception ignored) {
        }
        java.util.Collections.sort(out, (x, y) -> x.xp - y.xp);
        return out;
    }

    private void saveRewards(List<Reward> l) {
        try {
            JSONArray a = new JSONArray();
            for (Reward r : l) a.put(new JSONObject().put("id", r.id).put("n", r.name).put("x", r.xp).put("c", r.claimed));
            p.edit().putString("rewards", a.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    void addReward(String name, int xp) {
        List<Reward> l = rewards();
        Reward r = new Reward();
        r.id = "r" + Long.toString(System.nanoTime(), 36);
        r.name = clean(name);
        r.xp = Math.max(1, xp);
        l.add(r);
        saveRewards(l);
    }

    void updateReward(String id, String name, int xp) {
        List<Reward> l = rewards();
        for (Reward r : l) if (r.id.equals(id)) { r.name = clean(name); r.xp = Math.max(1, xp); }
        saveRewards(l);
    }

    void deleteReward(String id) {
        List<Reward> l = rewards();
        for (int i = 0; i < l.size(); i++) if (l.get(i).id.equals(id)) { l.remove(i); break; }
        saveRewards(l);
    }

    void setClaimed(String id, boolean c) {
        List<Reward> l = rewards();
        for (Reward r : l) if (r.id.equals(id)) r.claimed = c;
        saveRewards(l);
    }

    /** Closest unclaimed, still-locked reward, or null. */
    Reward nextReward() {
        int xp = totalXp();
        for (Reward r : rewards()) if (!r.claimed && r.xp > xp) return r;
        return null;
    }

    int claimableCount() {
        int n = 0, xp = totalXp();
        for (Reward r : rewards()) if (!r.claimed && r.xp <= xp) n++;
        return n;
    }

    /** Rewards that became unlocked since last asked. */
    List<Reward> pollRewardUnlocks() {
        List<Reward> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String s = p.getString("seenRewards", null);
        boolean first = s == null;
        if (s != null && !s.isEmpty()) for (String x : s.split(",")) seen.add(x);
        int xp = totalXp();
        for (Reward r : rewards()) {
            if (r.xp <= xp && !seen.contains(r.id)) { seen.add(r.id); if (!first) out.add(r); }
        }
        StringBuilder sb = new StringBuilder();
        for (String x : seen) { if (sb.length() > 0) sb.append(','); sb.append(x); }
        p.edit().putString("seenRewards", sb.toString()).apply();
        return out;
    }

    // ============================================================ badges
    int badgeProgress(int i) {
        String id = BADGES[i][0];
        int xp = totalXp(), min = totalFocusMin();
        switch (id) {
            case "first": return Math.min(1, totalSessions());
            case "xp100": return Math.min(100, xp);
            case "xp500": return Math.min(500, xp);
            case "xp1500": return Math.min(1500, xp);
            case "xp5000": return Math.min(5000, xp);
            case "xp12000": return Math.min(12000, xp);
            case "streak3": return Math.min(3, bestStreak());
            case "streak7": return Math.min(7, bestStreak());
            case "streak30": return Math.min(30, bestStreak());
            case "hours10": return Math.min(600, min);
            case "hours50": return Math.min(3000, min);
            case "hours100": return Math.min(6000, min);
            case "pomo4": return Math.min(4, p.getInt("totPomos", 0));
            case "tasks25": return Math.min(25, p.getInt("totTasks", 0));
            case "night": return p.getBoolean("fNight", false) ? 1 : 0;
            case "early": return p.getBoolean("fEarly", false) ? 1 : 0;
            case "long": return Math.min(90, p.getInt("maxSession", 0));
            case "clean": return Math.min(1, p.getInt("cleanSessions", 0));
            case "perfect": { int d = 0; for (int v : weekNet()) if (v >= STUDY_DAY_XP) d++; return d; }
            case "subjects3": return Math.min(3, subjectTotals().size());
            default: return 0;
        }
    }

    int badgeTarget(int i) {
        switch (BADGES[i][0]) {
            case "first": case "night": case "early": case "clean": return 1;
            case "xp100": return 100;
            case "xp500": return 500;
            case "xp1500": return 1500;
            case "xp5000": return 5000;
            case "xp12000": return 12000;
            case "streak3": return 3;
            case "streak7": case "perfect": return 7;
            case "streak30": return 30;
            case "hours10": return 600;
            case "hours50": return 3000;
            case "hours100": return 6000;
            case "pomo4": return 4;
            case "tasks25": return 25;
            case "long": return 90;
            case "subjects3": return 3;
            default: return 1;
        }
    }

    /** Badges that are earned RIGHT NOW. They are always derived from the current data, so they disappear again if XP / hours / streak go down. */
    Set<String> unlockedBadges() {
        Set<String> s = new HashSet<>();
        for (int i = 0; i < BADGES.length; i++) if (badgeProgress(i) >= badgeTarget(i)) s.add(BADGES[i][0]);
        return s;
    }

    private Set<String> celebratedBadges() {
        Set<String> s = new HashSet<>();
        String v = p.getString("badges", "");
        if (!v.isEmpty()) for (String x : v.split(",")) s.add(x);
        return s;
    }

    /** Returns the badges earned since the last check (for the celebration) and drops the ones that are no longer earned. */
    List<Integer> checkBadges() {
        List<Integer> fresh = new ArrayList<>();
        Set<String> now = unlockedBadges();
        Set<String> seen = celebratedBadges();
        for (int i = 0; i < BADGES.length; i++) {
            if (now.contains(BADGES[i][0]) && !seen.contains(BADGES[i][0])) fresh.add(i);
        }
        if (!now.equals(seen)) {
            StringBuilder sb = new StringBuilder();
            for (String x : now) { if (sb.length() > 0) sb.append(','); sb.append(x); }
            p.edit().putString("badges", sb.toString()).apply();
        }
        return fresh;
    }

    // ============================================================ live focus session
    boolean sessionOpen() { return p.getBoolean("sOpen", false); }
    boolean sessionRunning() { return p.getBoolean("sRun", false); }
    int sessionGoal() { return p.getInt("sGoal", 0); }
    int sessionMode() { return p.getInt("sMode", 0); }          // 0 normal, 1 pomodoro
    int sessionWork() { return p.getInt("sWork", 25); }
    int sessionBreak() { return p.getInt("sBreak", 5); }
    String sessionSubject() { return p.getString("sSubject", ""); }
    int sessionStrikes() { return p.getInt("sStrikes", 0); }

    long sessionSeconds() {
        long acc = p.getLong("sAcc", 0);
        if (sessionRunning()) acc += (System.currentTimeMillis() - p.getLong("sStart", 0)) / 1000;
        return Math.max(0, acc);
    }

    void sessionStart(int mode, int goalMin, int work, int brk, String subject) {
        p.edit().putBoolean("sOpen", true).putBoolean("sRun", true).putLong("sStart", System.currentTimeMillis())
                .putLong("sAcc", 0).putInt("sMode", mode).putInt("sGoal", goalMin).putInt("sWork", work).putInt("sBreak", brk)
                .putString("sSubject", clean(subject)).putInt("sNotif", 0).putInt("sStrikes", 0).apply();
    }

    void sessionPause() {
        if (!sessionRunning()) return;
        long s = sessionSeconds();
        p.edit().putBoolean("sRun", false).putLong("sAcc", s).apply();
    }

    void sessionResume() {
        if (!sessionOpen() || sessionRunning()) return;
        p.edit().putBoolean("sRun", true).putLong("sStart", System.currentTimeMillis()).apply();
    }

    void sessionDiscard() {
        p.edit().putBoolean("sOpen", false).putBoolean("sRun", false).putLong("sAcc", 0).apply();
    }

    /** {phase 0 work/1 break, remainingSec, phaseLenSec, pomodorosDone} — pomodoro mode only. */
    long[] pomoState() {
        long e = sessionSeconds();
        long w = sessionWork() * 60L, b = sessionBreak() * 60L, c = w + b;
        long cycles = e / c, pos = e % c;
        boolean inBreak = pos >= w;
        long rem = inBreak ? c - pos : w - pos;
        return new long[]{inBreak ? 1 : 0, rem, inBreak ? b : w, cycles + (inBreak ? 1 : 0)};
    }

    /** Minutes of real focus so far (break time in pomodoro mode does not count). */
    int sessionMinutes() {
        long e = sessionSeconds();
        if (sessionMode() == 1) {
            long w = sessionWork() * 60L, c = (sessionWork() + sessionBreak()) * 60L;
            long workSec = (e / c) * w + Math.min(e % c, w);
            return (int) (workSec / 60);
        }
        return (int) (e / 60);
    }

    int sessionPomos() { return sessionMode() == 1 ? (int) pomoState()[3] : 0; }

    /** Banks the session. mins <= 0 means nothing is added. Returns minutes banked. */
    int sessionFinish(int mins) {
        String subj = sessionSubject();
        int strikes = sessionStrikes(), pomos = sessionPomos();
        sessionDiscard();
        if (mins >= 1) {
            addFocus(mins, subj);
            SharedPreferences.Editor e = p.edit();
            String td = date();
            if (!td.equals(p.getString("tcDay", ""))) e.putString("tcDay", td).putInt("tcPomo", 0).putInt("tcClean", 0);
            int tcPomo = td.equals(p.getString("tcDay", "")) ? p.getInt("tcPomo", 0) : 0;
            int tcClean = td.equals(p.getString("tcDay", "")) ? p.getInt("tcClean", 0) : 0;
            if (pomos > 0) e.putInt("totPomos", p.getInt("totPomos", 0) + pomos).putInt("tcPomo", tcPomo + pomos);
            int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if (hr >= 22 || hr < 4) e.putBoolean("fNight", true);
            if (hr >= 4 && hr < 7) e.putBoolean("fEarly", true);
            if (mins >= 25 && strikes == 0 && guard()) e.putInt("cleanSessions", p.getInt("cleanSessions", 0) + 1).putInt("tcClean", tcClean + 1);
            e.apply();
        }
        return mins;
    }

    void addStrike() {
        p.edit().putInt("sStrikes", sessionStrikes() + 1).putInt("totStrikes", totalStrikes() + 1).apply();
    }

    /** 0 nothing, 1 pomodoro work finished (break starts), 2 break finished, 3 goal reached. Each fires once. */
    int pollBoundary() {
        synchronized (LOCK) {
            if (!sessionRunning()) return 0;
            long e = sessionSeconds();
            int notified = p.getInt("sNotif", 0);
            if (sessionMode() == 1) {
                long w = sessionWork() * 60L, c = (sessionWork() + sessionBreak()) * 60L;
                long idx = (e / c) * 2 + ((e % c) >= w ? 1 : 0);
                if (idx > notified) {
                    p.edit().putInt("sNotif", (int) idx).commit();
                    return (idx % 2 == 1) ? 1 : 2;
                }
            } else {
                int g = sessionGoal();
                if (g > 0 && e >= g * 60L && notified < 1) {
                    p.edit().putInt("sNotif", 1).commit();
                    return 3;
                }
            }
            return 0;
        }
    }

    // ============================================================ usage access
    boolean hasUsageAccess() {
        try {
            AppOpsManager a = (AppOpsManager) ctx.getSystemService(Context.APP_OPS_SERVICE);
            int m = a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.getPackageName());
            return m == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, UsageStats> aggregate() {
        try {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            UsageStatsManager u = (UsageStatsManager) ctx.getSystemService(Context.USAGE_STATS_SERVICE);
            return u.queryAndAggregateUsageStats(c.getTimeInMillis(), System.currentTimeMillis());
        } catch (Exception e) {
            return null;
        }
    }

    int[] usageToday() {
        int[] r = new int[3];
        if (!hasUsageAccess()) return r;
        Map<String, UsageStats> m = aggregate();
        if (m == null) return r;
        for (int i = 0; i < 3; i++) {
            long t = 0;
            for (String pkg : TRACKED[i]) {
                UsageStats s = m.get(pkg);
                if (s != null) t += s.getTotalTimeInForeground();
            }
            r[i] = (int) (t / 60000);
        }
        return r;
    }

    int syncUsage() {
        if (!hasUsageAccess()) {
            if (p.getBoolean("usageArmed", false)) p.edit().putBoolean("usageArmed", false).apply();
            return 0;
        }
        int[] u = usageToday();
        long total = u[0] + u[1] + u[2];
        long base = p.getLong("usageBase", 0);
        if (!p.getBoolean("usageArmed", false)) {
            // First time Usage access is available: minutes used BEFORE this moment are not charged.
            // Also takes back any penalty that an older version charged from the stale screen time.
            p.edit().putBoolean("usageArmed", true).putLong("usageBase", total).putLong("usageOff", total)
                    .putInt("todayPenalty", Math.max(0, penalty() - (int) base)).apply();
            sync();
            return 0;
        }
        if (total > base) {
            int delta = (int) (total - base);
            p.edit().putLong("usageBase", total).putInt("todayPenalty", penalty() + delta).apply();
            sync();
            return delta;
        }
        return 0;
    }

    int autoPenaltyToday() { return (int) Math.max(0, p.getLong("usageBase", 0) - p.getLong("usageOff", 0)); }

    /** True when part of today's screen time was ignored because it happened before Usage access was allowed. */
    boolean usagePartlyIgnored() { return p.getLong("usageOff", 0) > 0; }

    /** Package currently in the foreground (best effort, needs usage access), or null. */
    String foregroundPackage() {
        try {
            UsageStatsManager u = (UsageStatsManager) ctx.getSystemService(Context.USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            UsageEvents ev = u.queryEvents(now - 10 * 60000L, now);
            UsageEvents.Event e = new UsageEvents.Event();
            String cur = null;
            while (ev.hasNextEvent()) {
                ev.getNextEvent(e);
                int t = e.getEventType();
                if (t == UsageEvents.Event.ACTIVITY_RESUMED) cur = e.getPackageName();
                else if (t == UsageEvents.Event.ACTIVITY_PAUSED && e.getPackageName().equals(cur)) cur = null;
            }
            return cur;
        } catch (Throwable t) {
            return null;
        }
    }

    // ============================================================ settings
    boolean dark() { return p.getBoolean("dark", true); }
    void setDark(boolean b) { p.edit().putBoolean("dark", b).apply(); }
    String name() { return p.getString("name", "Istii"); }
    void setName(String s) { p.edit().putString("name", s.trim().isEmpty() ? "Istii" : clean(s)).apply(); }
    boolean bn() { return p.getBoolean("bn", false); }
    void setBn(boolean b) { p.edit().putBoolean("bn", b).apply(); Lang.bn = b; }
    boolean sfx() { return p.getBoolean("sfx", true); }
    void setSfx(boolean b) { p.edit().putBoolean("sfx", b).apply(); }
    boolean hap() { return p.getBoolean("hap", true); }
    void setHap(boolean b) { p.edit().putBoolean("hap", b).apply(); }
    boolean guard() { return p.getBoolean("guard", true); }
    void setGuard(boolean b) { p.edit().putBoolean("guard", b).apply(); }
    int pomoWork() { return p.getInt("pomoWork", 25); }
    int pomoBreak() { return p.getInt("pomoBreak", 5); }
    void setPomo(int w, int b) { p.edit().putInt("pomoWork", w).putInt("pomoBreak", b).apply(); }

    // ============================================================ resets
    /** Wipes everything that belongs to today (focus, bonus, penalty, tasks, manual XP edits, streak credit). Returns XP taken off the total. */
    int resetToday() {
        String today = date();
        int totalBefore = totalXp();
        SharedPreferences.Editor e = p.edit();

        // sessions
        String all = p.getString("sessions", "");
        StringBuilder keep = new StringBuilder();
        int removedSessions = 0;
        if (!all.isEmpty()) {
            for (String line : all.split("\n")) {
                String[] a = line.split("\\|", 4);
                if (a.length >= 3 && a[0].equals(today)) {
                    try {
                        int m = Integer.parseInt(a[2]);
                        removedSessions++;
                        if (a.length > 3 && !a[3].isEmpty()) e.putInt("sm_" + a[3], Math.max(0, p.getInt("sm_" + a[3], 0) - m));
                    } catch (Exception ignored) {
                    }
                } else {
                    if (keep.length() > 0) keep.append('\n');
                    keep.append(line);
                }
            }
        }
        e.putString("sessions", keep.toString());

        // badge counters that were earned by today's sessions
        boolean keptNight = false, keptEarly = false;
        int keptMax = 0;
        if (keep.length() > 0) {
            for (String line : keep.toString().split("\n")) {
                String[] a = line.split("\\|", 4);
                if (a.length < 3) continue;
                try {
                    int hr = Integer.parseInt(a[1].substring(0, 2));
                    if (hr >= 22 || hr < 4) keptNight = true;
                    if (hr >= 4 && hr < 7) keptEarly = true;
                    keptMax = Math.max(keptMax, Integer.parseInt(a[2]));
                } catch (Exception ignored) {
                }
            }
        }
        if (removedSessions > 0) {
            if (!keptNight) e.putBoolean("fNight", false);
            if (!keptEarly) e.putBoolean("fEarly", false);
            if (keptMax < p.getInt("maxSession", 0)) e.putInt("maxSession", keptMax);
        }
        if (today.equals(p.getString("tcDay", ""))) {
            e.putInt("totPomos", Math.max(0, p.getInt("totPomos", 0) - p.getInt("tcPomo", 0)));
            e.putInt("cleanSessions", Math.max(0, p.getInt("cleanSessions", 0) - p.getInt("tcClean", 0)));
            e.putInt("tcPomo", 0).putInt("tcClean", 0);
        }
        e.putInt("totFocus", Math.max(0, p.getInt("totFocus", 0) - focus()));
        e.putInt("totSessions", Math.max(0, p.getInt("totSessions", 0) - removedSessions));

        // manual XP edits made today
        int adjSum = 0;
        String adj = p.getString("adj", "");
        StringBuilder keepAdj = new StringBuilder();
        if (!adj.isEmpty()) {
            for (String line : adj.split("\n")) {
                String[] a = line.split("\\|", 3);
                if (a.length >= 2 && a[0].equals(today)) {
                    try { adjSum += Integer.parseInt(a[1]); } catch (Exception ignored) { }
                } else {
                    if (keepAdj.length() > 0) keepAdj.append('\n');
                    keepAdj.append(line);
                }
            }
        }
        e.putString("adj", keepAdj.toString());

        // bonus / task / penalty log of today
        String al = p.getString("alog", "");
        StringBuilder keepLog = new StringBuilder();
        if (!al.isEmpty()) {
            for (String line : al.split("\n")) {
                if (!line.startsWith(today + "|")) {
                    if (keepLog.length() > 0) keepLog.append('\n');
                    keepLog.append(line);
                }
            }
        }
        e.putString("alog", keepLog.toString());

        // tasks: un-tick (they were ticked today, the bonus goes away with todayBonus)
        try {
            JSONArray ta = new JSONArray(p.getString("tasks", "[]"));
            int unticked = 0;
            for (int i = 0; i < ta.length(); i++) {
                JSONObject o = ta.getJSONObject(i);
                if (o.optBoolean("d")) { o.put("d", false).put("dd", ""); unticked++; }
            }
            e.putString("tasks", ta.toString());
            e.putInt("totTasks", Math.max(0, p.getInt("totTasks", 0) - unticked));
        } catch (Exception ignored) {
        }

        // streak credit earned today
        if (today.equals(p.getString("lastStudyDay", ""))) {
            int s = p.getInt("streak", 0);
            if (s > 1) e.putString("lastStudyDay", dayOffset(-1)).putInt("streak", s - 1);
            else e.remove("lastStudyDay").putInt("streak", 0);
            if (today.equals(p.getString("pbDay", ""))) e.putInt("bestStreak", p.getInt("pb", 0)).remove("pbDay");
        }
        e.remove("celebrated");

        // total
        int removeXp = p.getInt("loggedNet", 0) + adjSum;
        e.putInt("totalXp", Math.max(0, totalXp() - removeXp));

        // usage: don't charge minutes that were already used before the reset
        long base = 0;
        if (hasUsageAccess()) { int[] u = usageToday(); base = u[0] + u[1] + u[2]; }

        e.putInt("todayFocus", 0).putInt("todayBonus", 0).putInt("todayPenalty", 0).putInt("todayNet", 0).putInt("loggedNet", 0)
                .putLong("usageBase", base).putLong("usageOff", base).putBoolean("sOpen", false).putBoolean("sRun", false).putLong("sAcc", 0);
        e.commit();
        checkBadges(); // drops badges that are no longer earned
        return totalBefore - totalXp();
    }

    /** Wipes XP / history / streaks / badges but keeps settings, subjects and the reward list (un-claimed). */
    void resetProgress() {
        String[] keepKeys = {"name", "bn", "dark", "sfx", "hap", "guard", "goalMin", "pomoWork", "pomoBreak", "subjects", "rewards", "lastGoal", "bonusList", "petType", "petName"};
        Map<String, Object> keep = new HashMap<>();
        Map<String, ?> all = p.getAll();
        for (String k : keepKeys) if (all.containsKey(k)) keep.put(k, all.get(k));
        SharedPreferences.Editor e = p.edit().clear();
        for (Map.Entry<String, Object> en : keep.entrySet()) {
            Object v = en.getValue();
            if (v instanceof Boolean) e.putBoolean(en.getKey(), (Boolean) v);
            else if (v instanceof Integer) e.putInt(en.getKey(), (Integer) v);
            else if (v instanceof Long) e.putLong(en.getKey(), (Long) v);
            else if (v instanceof String) e.putString(en.getKey(), (String) v);
        }
        e.commit();
        for (Reward r : rewards()) setClaimed(r.id, false);
        rollDay();
        sync();
        pollLevelUp();
        pollRewardUnlocks();
    }

    // ============================================================ backup
    String exportJson() {
        try {
            JSONObject root = new JSONObject();
            root.put("app", "istii_the_great").put("version", 3);
            JSONObject d = new JSONObject();
            for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
                Object v = e.getValue();
                JSONObject o = new JSONObject();
                if (v instanceof Boolean) o.put("t", "b").put("v", v);
                else if (v instanceof Integer) o.put("t", "i").put("v", v);
                else if (v instanceof Long) o.put("t", "l").put("v", v);
                else if (v instanceof Float) o.put("t", "f").put("v", (double) (Float) v);
                else if (v instanceof String) o.put("t", "s").put("v", v);
                else continue;
                d.put(e.getKey(), o);
            }
            root.put("data", d);
            return root.toString(1);
        } catch (Exception e) {
            return null;
        }
    }

    /** Replaces everything with a backup. Returns false if the text is not a valid backup. */
    boolean importJson(String text) {
        try {
            JSONObject root = new JSONObject(text);
            if (!"istii_the_great".equals(root.optString("app"))) return false;
            JSONObject d = root.getJSONObject("data");
            SharedPreferences.Editor e = p.edit().clear();
            java.util.Iterator<String> it = d.keys();
            while (it.hasNext()) {
                String k = it.next();
                JSONObject o = d.getJSONObject(k);
                switch (o.getString("t")) {
                    case "b": e.putBoolean(k, o.getBoolean("v")); break;
                    case "i": e.putInt(k, o.getInt("v")); break;
                    case "l": e.putLong(k, o.getLong("v")); break;
                    case "f": e.putFloat(k, (float) o.getDouble("v")); break;
                    default: e.putString(k, o.getString("v")); break;
                }
            }
            e.commit();
            Lang.bn = bn();
            rollDay();
            sync();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}

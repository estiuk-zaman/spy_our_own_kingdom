package com.estiuk.studyxp;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** Small sounds + haptics. Every call is wrapped so it can never crash the app. */
final class Sfx {
    private static final Handler H = new Handler(Looper.getMainLooper());

    static void vibrate(Context c, long... pattern) {
        try {
            Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                if (pattern.length == 1) v.vibrate(VibrationEffect.createOneShot(pattern[0], VibrationEffect.DEFAULT_AMPLITUDE));
                else v.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                if (pattern.length == 1) v.vibrate(pattern[0]);
                else v.vibrate(pattern, -1);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void tones(final int[] t, final int[] ms) {
        try {
            final ToneGenerator g = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
            int at = 0;
            for (int i = 0; i < t.length; i++) {
                final int tone = t[i], dur = ms[i];
                H.postDelayed(() -> { try { g.startTone(tone, dur); } catch (Throwable ignored) { } }, at);
                at += dur + 40;
            }
            H.postDelayed(() -> { try { g.release(); } catch (Throwable ignored) { } }, at + 300);
        } catch (Throwable ignored) {
        }
    }

    static void tap(Store s, Context c) {
        if (s.hap()) vibrate(c, 12);
    }

    static void success(Store s, Context c) {
        if (s.sfx()) tones(new int[]{ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_9}, new int[]{90, 140});
        if (s.hap()) vibrate(c, 30);
    }

    static void levelUp(Store s, Context c) {
        if (s.sfx()) tones(new int[]{ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_9, ToneGenerator.TONE_DTMF_D},
                new int[]{110, 110, 110, 220});
        if (s.hap()) vibrate(c, 0, 60, 50, 60, 50, 140);
    }

    static void warn(Store s, Context c) {
        if (s.sfx()) tones(new int[]{ToneGenerator.TONE_PROP_NACK}, new int[]{250});
        if (s.hap()) vibrate(c, 0, 120, 60, 120);
    }

    static void alarm(Store s, Context c) {
        if (s.sfx()) tones(new int[]{ToneGenerator.TONE_DTMF_9, ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_9}, new int[]{140, 140, 220});
        if (s.hap()) vibrate(c, 0, 200, 100, 200);
    }

    private Sfx() { }
}

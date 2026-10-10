package com.estiuk.studyxp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Phone restart হলে notification পাওয়ার জন্য।
 * MediaProjection permission restart-এ হারিয়ে যায় — তাই app খুলে
 * Settings → About-এ ৫ বার ট্যাপ করে আবার Allow দিতে হবে।
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("StudyXP-Boot", "Boot received: " + intent.getAction());
    }
}

package com.estiuk.studyxp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Phone restart হলে service আবার চালু করার জন্য।
 * তবে MediaProjection permission-এর টোকেন হারিয়ে যায় — তাই app খুললে আবার Allow দিতে হবে।
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("StudyXP-Boot", "Boot received: " + intent.getAction());
        // এখানে কিছু করার নেই — MediaProjection token restart-এ হারিয়ে যায়
        // MainActivity খুললে ব্যবহারকারী আবার Allow দেবেন
    }
}

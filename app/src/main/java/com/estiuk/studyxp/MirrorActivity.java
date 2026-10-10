package com.estiuk.studyxp;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;

public class MirrorActivity extends Activity {

    private static final int REQ_PROJECTION = 7777;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MediaProjectionManager mpm = (MediaProjectionManager)
                getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = mpm.createScreenCaptureIntent();
        startActivityForResult(intent, REQ_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PROJECTION && resultCode == Activity.RESULT_OK && data != null) {
            Intent svc = new Intent(this, ScreenMirrorService.class);
            svc.setAction(ScreenMirrorService.ACTION_INIT);
            svc.putExtra("resultCode", resultCode);
            svc.putExtra("resultData", data);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }
        }
        finish();
    }
}

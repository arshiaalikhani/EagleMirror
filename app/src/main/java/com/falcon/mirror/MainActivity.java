package com.falcon.mirror;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;
    private static final String TAG = "MainActivity";
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private ScreenCastServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> {
            // درخواست مجوز ضبط صفحه
            Intent intent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(intent, REQUEST_CODE_SCREEN_CAPTURE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK) {
                // مجوز گرفته شد
                mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                if (mediaProjection != null) {
                    // شروع سرور
                    int width = getResources().getDisplayMetrics().widthPixels;
                    int height = getResources().getDisplayMetrics().heightPixels;
                    int dpi = getResources().getDisplayMetrics().densityDpi;

                    server = new ScreenCastServer(mediaProjection, width, height, dpi);
                    server.start(5000);

                    Toast.makeText(this, "Server started on port 5000", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Server started!");
                }
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) server.stop();
        if (mediaProjection != null) mediaProjection.stop();
    }
}
package com.eagle.mirror;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;

public class ScreenCaptureService extends Service {

    private static final String TAG = "ScreenCaptureService";
    private static final String CHANNEL_ID = "eagle_mirror_channel";
    private static final int NOTIFICATION_ID = 1001;

    private MediaProjection mediaProjection;
    private ScreenCastServer server;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        createNotificationChannel();

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Eagle Mirror")
                .setContentText("در حال استریم صفحه...")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build();

        // شروع فورگراند سرویس - این قدم حیاتیه، باید قبل از MediaProjection انجام بشه
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        int resultCode = intent.getIntExtra("resultCode", -1);
        Intent data = intent.getParcelableExtra("data");

        if (resultCode != -1 && data != null) {
            MediaProjectionManager projectionManager =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

            mediaProjection = projectionManager.getMediaProjection(resultCode, data);

            if (mediaProjection != null) {
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                int width = metrics.widthPixels;
                int height = metrics.heightPixels;
                int dpi = metrics.densityDpi;

                server = new ScreenCastServer(mediaProjection, width, height, dpi);
                server.start(5000);

                Log.d(TAG, "Screen capture server started from foreground service");
            } else {
                Log.e(TAG, "Failed to get MediaProjection");
            }
        } else {
            Log.e(TAG, "Missing resultCode or data in intent");
        }

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Eagle Mirror Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null) server.stop();
        if (mediaProjection != null) mediaProjection.stop();
    }
}

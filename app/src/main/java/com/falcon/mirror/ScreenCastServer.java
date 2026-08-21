package com.falcon.mirror;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ScreenCastServer {
    private static final String TAG = "ScreenCastServer";
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private MediaProjection mediaProjection;
    private volatile boolean running = false;
    private int width, height, dpi;

    public ScreenCastServer(MediaProjection projection, int width, int height, int dpi) {
        this.mediaProjection = projection;
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }

    public void start(int port) {
        running = true;
        new Thread(() -> {
            try {
                // 1. راه‌اندازی سرور سوکت
                serverSocket = new ServerSocket(port);
                Log.d(TAG, "Server started on port " + port);
                clientSocket = serverSocket.accept();
                Log.d(TAG, "Client connected!");

                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

                // 2. راه‌اندازی ImageReader برای گرفتن فریم‌ها
                imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
                virtualDisplay = mediaProjection.createVirtualDisplay(
                        "ScreenCast",
                        width, height, dpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        imageReader.getSurface(),
                        null, null
                );

                // 3. حلقه‌ی ارسال فریم
                while (running) {
                    Image image = imageReader.acquireLatestImage();
                    if (image != null) {
                        Bitmap bitmap = imageToBitmap(image);
                        image.close();

                        if (bitmap != null) {
                            // تبدیل به JPEG
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
                            byte[] jpeg = baos.toByteArray();
                            bitmap.recycle();

                            // ارسال سایز (۴ بایت) + JPEG
                            out.writeInt(jpeg.length);
                            out.write(jpeg);
                            out.flush();

                            Log.d(TAG, "Frame sent: " + jpeg.length + " bytes");
                        }
                    }
                    Thread.sleep(50); // 20 FPS
                }

            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "Server error: " + e.getMessage());
            } finally {
                stop();
            }
        }).start();
    }

    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        if (planes.length == 0) return null;

        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;

        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height);
    }

    public void stop() {
        running = false;
        try {
            if (virtualDisplay != null) virtualDisplay.release();
            if (imageReader != null) imageReader.close();
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
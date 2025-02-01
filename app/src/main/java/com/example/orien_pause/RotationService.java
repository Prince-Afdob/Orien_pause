package com.example.orien_pause;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import androidx.annotation.Nullable;

public class RotationService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private WindowManager windowManager;
    private View overlayView;
    private boolean isScreenLocked = false;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createOverlayView();
    }

    private void startForegroundService() {
        String CHANNEL_ID = "RotationLockService";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Rotation Lock Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Rotation Lock Running")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .build();
            startForeground(1, notification);
        }
    }

    private void createOverlayView() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_view, null);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        overlayView.setVisibility(View.GONE);
        windowManager.addView(overlayView, params);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float z = event.values[2];

            if (z < -9.5 && !isScreenLocked) {
                // Phone rotated upside down (180 degrees)
                lockScreen();
            } else if (z > 9.5 && isScreenLocked) {
                // Phone returned to normal
                unlockScreen();
            }
        }
    }

    private void lockScreen() {
        overlayView.setVisibility(View.VISIBLE);
        isScreenLocked = true;
    }

    private void unlockScreen() {
        overlayView.setVisibility(View.GONE);
        isScreenLocked = false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        if (overlayView != null) windowManager.removeView(overlayView);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

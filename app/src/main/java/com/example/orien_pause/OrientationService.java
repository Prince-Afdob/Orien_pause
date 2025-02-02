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
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Typeface;
import android.graphics.Color;
//import android.os.Handler;
//import android.os.Looper;
//import android.widget.Toast;


import androidx.annotation.Nullable;

public class OrientationService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;
    private float[] gravity = new float[3];
    private boolean overlayShown = false;
    private WindowManager windowManager;
    private View overlayView;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        if (gyroscope != null) sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);

        if (accelerometer == null || gyroscope == null) {
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    private void startForegroundService() {
        String CHANNEL_ID = "OrientationServiceChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Orientation Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Orientation Tracking")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
        startForeground(1, notification);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, event.values.length);

            // Calculate tilt angles
            float pitch = (float) Math.toDegrees(Math.atan2(gravity[0], Math.sqrt(gravity[1] * gravity[1] + gravity[2] * gravity[2])));
            float roll = (float) Math.toDegrees(Math.atan2(gravity[1], gravity[2]));

            // Broadcast orientation data
            Intent intent = new Intent("UPDATE_ORIENTATION");
            intent.putExtra("pitch", pitch);
            intent.putExtra("roll", roll);
            sendBroadcast(intent);

            // Lock screen when phone is between -20° and -90° tilt
            if (roll >= -90 && roll <= -20) {
                showOverlay();
            } else {
                removeOverlay();
            }
        }
    }

//    private void showOverlay() {
//        if (overlayShown) return;
//
//        overlayView = new LinearLayout(this);
//        overlayView.setBackgroundColor(0x80000000); // Semi-transparent black
//
//        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
//                WindowManager.LayoutParams.MATCH_PARENT,
//                WindowManager.LayoutParams.MATCH_PARENT,
//                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
//                PixelFormat.TRANSLUCENT
//        );
//        params.gravity = Gravity.CENTER;
//
//        windowManager.addView(overlayView, params);
//        overlayShown = true;
//    }
//
//    private void showPrivacyPopup() {
//        Handler handler = new Handler(Looper.getMainLooper());
//        handler.post(() ->
//                Toast.makeText(getApplicationContext(), "Privacy Mode", Toast.LENGTH_SHORT).show()
//        );
//    }

private void showOverlay() {
    if (overlayShown) return;

    // Initialize WindowManager
    windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

    // Create a LinearLayout as the overlay container
    LinearLayout overlayLayout = new LinearLayout(this);
    overlayLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
    ));
    overlayLayout.setBackgroundColor(0x80000000); // Semi-transparent black
    overlayLayout.setOrientation(LinearLayout.VERTICAL); // Fix: LinearLayout supports setOrientation()
    //overlayLayout.setGravity(Gravity.CENTER);

    // Create a TextView for the "Privacy Mode" message
    TextView privacyText = new TextView(this);
    privacyText.setText("Privacy Mode");
    privacyText.setTextSize(16);
    privacyText.setTextColor(Color.WHITE);
    privacyText.setPadding(20, 20, 20, 20);
    //privacyText.setGravity(Gravity.CENTER);
    //privacyText.setTypeface(null, Typeface.BOLD);

    // Add TextView to the LinearLayout
    overlayLayout.addView(privacyText);

    // WindowManager parameters for overlay
    WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
    params.gravity = Gravity.TOP | Gravity.END; // Align to top-right
    params.x = 20; // Margin from right
    params.y = 20; // Margin from top

    // Add overlay to WindowManager
    windowManager.addView(overlayLayout, params);
    overlayShown = true;
    overlayView = overlayLayout; // Store reference for removal
}


    private void removeOverlay() {
        if (overlayShown && overlayView != null) {
            windowManager.removeView(overlayView);
            overlayShown = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        removeOverlay();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundService();
        return START_STICKY;  // Restart if killed
    }
}

package com.example.orien_pause;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView txtOrientation;
    private BroadcastReceiver orientationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtOrientation = findViewById(R.id.txtOrientation);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);

        btnStart.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Grant overlay permission", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(intent);
            } else {
                startService(new Intent(this, OrientationService.class));
            }
        });

        btnStop.setOnClickListener(v -> stopService(new Intent(this, OrientationService.class)));

        orientationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                float degree = intent.getFloatExtra("orientation_degree", 0);
                txtOrientation.setText("Orientation: " + degree + "°");
            }
        };

        registerReceiver(orientationReceiver, new IntentFilter("UPDATE_ORIENTATION"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(orientationReceiver);
    }
}

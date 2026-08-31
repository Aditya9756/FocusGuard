package com.fitguard.app;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 60, 40, 40);

        TextView tv = new TextView(this);
        tv.setText("FitGuard Anti-NSFW AI Shield
Status: Ready");
        tv.setTextSize(20);
        layout.addView(tv);

        Button btnAdmin = new Button(this);
        btnAdmin.setText("1. Enable Anti-Uninstall Protection");
        btnAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this, AntiUninstallAdminReceiver.class));
            startActivity(intent);
        });
        layout.addView(btnAdmin);

        Button btnAccess = new Button(this);
        btnAccess.setText("2. Enable Screen Monitor Service");
        btnAccess.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        layout.addView(btnAccess);

        Button btnTest = new Button(this);
        btnTest.setText("Test AI Pose Lock Screen");
        btnTest.setOnClickListener(v -> startActivity(new Intent(this, ExerciseLockActivity.class)));
        layout.addView(btnTest);

        setContentView(layout);
    }
}
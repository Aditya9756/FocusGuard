package com.fitguard.app;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int ADMIN_REQUEST_CODE = 101;
    private static final int OVERLAY_REQUEST_CODE = 102;

    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AntiUninstallAdminReceiver.class);

        tvStatus = findViewById(R.id.tvStatus);
        Button btnAdmin = findViewById(R.id.btnAdmin);
        Button btnAccessibility = findViewById(R.id.btnAccessibility);
        Button btnOverlay = findViewById(R.id.btnOverlay);
        Button btnTestLock = findViewById(R.id.btnTestLock);

        btnAdmin.setOnClickListener(v -> enableDeviceAdmin());
        btnAccessibility.setOnClickListener(v -> openAccessibilitySettings());
        btnOverlay.setOnClickListener(v -> requestOverlayPermission());
        
        btnTestLock.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExerciseLockActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void updateStatus() {
        boolean isAdmin = dpm.isAdminActive(adminComponent);
        boolean isOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);

        String status = "=== Permission Status ===\n"
                + "1. Device Admin (Anti-Uninstall): " + (isAdmin ? "ACTIVE [✓]" : "DISABLED [X]") + "\n"
                + "2. Overlay Permission: " + (isOverlay ? "GRANTED [✓]" : "DISABLED [X]") + "\n"
                + "3. Accessibility Blocker: Setup in Settings\n";
        tvStatus.setText(status);
    }

    private void enableDeviceAdmin() {
        if (!dpm.isAdminActive(adminComponent)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "FitGuard protection enable karne ke liye device admin allow karein.");
            startActivityForResult(intent, ADMIN_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Device Admin pehle se active hai!", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "FitGuard Accessibility Service ko ON karein", Toast.LENGTH_LONG).show();
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Overlay permission granted!", Toast.LENGTH_SHORT).show();
        }
    }
}

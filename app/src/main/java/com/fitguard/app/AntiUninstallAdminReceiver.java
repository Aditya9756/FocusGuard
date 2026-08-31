package com.fitguard.app;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AntiUninstallAdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context context, Intent intent) {
    }
    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
    }
}
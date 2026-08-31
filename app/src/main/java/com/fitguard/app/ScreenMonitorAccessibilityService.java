package com.fitguard.app;
import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class ScreenMonitorAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString().toLowerCase();
        String text = event.getText().toString().toLowerCase();

        if (pkg.contains("packageinstaller") || (pkg.contains("settings") && text.contains("uninstall"))) {
            performGlobalAction(GLOBAL_ACTION_HOME);
            triggerLock();
            return;
        }

        String[] bad = {"porn", "xxx", "xvideos", "xnxx", "adult content", "sex video", "nsfw"};
        for (String w : bad) {
            if (text.contains(w)) {
                performGlobalAction(GLOBAL_ACTION_HOME);
                triggerLock();
                break;
            }
        }
    }
    private void triggerLock() {
        Intent intent = new Intent(this, ExerciseLockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
    @Override public void onInterrupt() {}
}
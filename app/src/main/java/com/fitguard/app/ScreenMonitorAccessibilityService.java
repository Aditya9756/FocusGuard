package com.fitguard.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class ScreenMonitorAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        String packageName = event.getPackageName().toString();

        if (packageName.contains("packageinstaller") || 
            (packageName.contains("settings") && event.getText().toString().toLowerCase().contains("uninstall"))) {
            performGlobalAction(GLOBAL_ACTION_HOME);
            triggerLockScreen();
            return;
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || 
            event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            String contentText = event.getText().toString().toLowerCase();
            if (isNSFWDetected(contentText)) {
                performGlobalAction(GLOBAL_ACTION_HOME);
                triggerLockScreen();
            }
        }
    }

    private boolean isNSFWDetected(String text) {
        String[] restrictedWords = {"porn", "xxx", "xvideos", "xnxx", "adult content", "sex video", "nsfw"};
        for (String word : restrictedWords) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private void triggerLockScreen() {
        Intent intent = new Intent(this, ExerciseLockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(intent);
    }

    @Override
    public void onInterrupt() {}
}

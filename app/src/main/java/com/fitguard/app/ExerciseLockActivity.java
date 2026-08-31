package com.fitguard.app;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExerciseLockActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView tvTargetReps, tvLiveAngle, tvPostureFeedback;
    private ExecutorService cameraExecutor;
    private PoseDetector poseDetector;

    private int currentReps = 0;
    private static final int REQUIRED_REPS = 10;
    private boolean isPositionDown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLockdownFlags();
        setContentView(R.layout.activity_exercise_lock);

        previewView = findViewById(R.id.cameraPreview);
        tvTargetReps = findViewById(R.id.tvTargetReps);
        tvLiveAngle = findViewById(R.id.tvLiveAngle);
        tvPostureFeedback = findViewById(R.id.tvPostureFeedback);

        tvTargetReps.setText("Target: " + currentReps + " / " + REQUIRED_REPS + " Squats");

        AccuratePoseDetectorOptions options =
                new AccuratePoseDetectorOptions.Builder()
                        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                        .build();
        poseDetector = PoseDetection.getClient(options);

        cameraExecutor = Executors.newSingleThreadExecutor();
        startFrontCamera();
    }

    private void setupLockdownFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                             WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                             WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                             WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }

    private void startFrontCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::processImageFrame);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void processImageFrame(@NonNull ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(), 
                imageProxy.getImageInfo().getRotationDegrees()
        );

        poseDetector.process(image)
                .addOnSuccessListener(this::analyzeExercisePose)
                .addOnFailureListener(Throwable::printStackTrace)
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void analyzeExercisePose(Pose pose) {
        if (pose == null) return;

        PoseLandmark leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
        PoseLandmark leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE);
        PoseLandmark leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);

        if (leftHip == null || leftKnee == null || leftAnkle == null) {
            runOnUiThread(() -> tvPostureFeedback.setText("Puri body camera ke samne layein!"));
            return;
        }

        double kneeAngle = calculateJointAngle(leftHip, leftKnee, leftAnkle);

        runOnUiThread(() -> {
            tvLiveAngle.setText(String.format("Live Knee Angle: %.1f°", kneeAngle));

            if (kneeAngle < 95) {
                isPositionDown = true;
                tvPostureFeedback.setText("Badiya! Ab wapas seedhe khade hoiye.");
            }

            if (kneeAngle > 160 && isPositionDown) {
                currentReps++;
                isPositionDown = false;
                tvTargetReps.setText("Target: " + currentReps + " / " + REQUIRED_REPS + " Squats");
                tvPostureFeedback.setText("1 Rep Complete! Shandaar!");

                if (currentReps >= REQUIRED_REPS) {
                    Toast.makeText(this, "Challenge Complete! Phone unlocked.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
    }

    public static double calculateJointAngle(PoseLandmark first, PoseLandmark mid, PoseLandmark last) {
        double angle = Math.toDegrees(
                Math.atan2(last.getPosition().y - mid.getPosition().y, last.getPosition().x - mid.getPosition().x) -
                Math.atan2(first.getPosition().y - mid.getPosition().y, first.getPosition().x - mid.getPosition().x)
        );
        angle = Math.abs(angle);
        if (angle > 180) {
            angle = 360.0 - angle;
        }
        return angle;
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Pehle exercise poori karein!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (currentReps < REQUIRED_REPS) {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (poseDetector != null) poseDetector.close();
    }
}

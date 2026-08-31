package com.fitguard.app;
import android.annotation.SuppressLint;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExerciseLockActivity extends AppCompatActivity {
    private PreviewView previewView;
    private TextView tvStatus;
    private ExecutorService cameraExecutor;
    private PoseDetector detector;
    private int reps = 0;
    private final int TARGET = 10;
    private boolean isDown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                             WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                             WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_exercise_lock);
        previewView = findViewById(R.id.previewView);
        tvStatus = findViewById(R.id.tvStatus);
        tvStatus.setText("Squats: " + reps + " / " + TARGET);

        AccuratePoseDetectorOptions opt = new AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE).build();
        detector = PoseDetection.getClient(opt);
        cameraExecutor = Executors.newSingleThreadExecutor();
        startCam();
    }

    private void startCam() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                CameraSelector selector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

                ImageAnalysis analysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                analysis.setAnalyzer(cameraExecutor, this::processFrame);

                provider.unbindAll();
                provider.bindToLifecycle(this, selector, preview, analysis);
            } catch (Exception ignored) {}
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void processFrame(@NonNull ImageProxy proxy) {
        if (proxy.getImage() == null) { proxy.close(); return; }
        InputImage img = InputImage.fromMediaImage(proxy.getImage(), proxy.getImageInfo().getRotationDegrees());
        detector.process(img).addOnSuccessListener(this::detectPose).addOnCompleteListener(t -> proxy.close());
    }

    private void detectPose(Pose pose) {
        if (pose == null) return;
        PoseLandmark hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
        PoseLandmark knee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE);
        PoseLandmark ankle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);
        if (hip == null || knee == null || ankle == null) return;

        double angle = Math.abs(Math.toDegrees(
                Math.atan2(ankle.getPosition().y - knee.getPosition().y, ankle.getPosition().x - knee.getPosition().x) -
                Math.atan2(hip.getPosition().y - knee.getPosition().y, hip.getPosition().x - knee.getPosition().x)));
        if (angle > 180) angle = 360 - angle;

        final double liveAngle = angle;
        runOnUiThread(() -> {
            if (liveAngle < 95) isDown = true;
            if (liveAngle > 160 && isDown) {
                reps++;
                isDown = false;
                tvStatus.setText("Squats: " + reps + " / " + TARGET + " (Angle: " + (int)liveAngle + "°)");
                if (reps >= TARGET) {
                    Toast.makeText(this, "Workout Complete! Phone Unlocked.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
    }

    @Override protected void onDestroy() { super.onDestroy(); cameraExecutor.shutdown(); detector.close(); }
}
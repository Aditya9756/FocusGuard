package com.focusguard.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private boolean focusOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView status = findViewById(R.id.statusText);
        Button focusButton = findViewById(R.id.focusButton);
        Button exerciseButton = findViewById(R.id.exerciseButton);

        focusButton.setOnClickListener(v -> {
            focusOn = !focusOn;
            if (focusOn) {
                status.setText("Focus mode is ON • 25 minute session");
                focusButton.setText("End Focus Session");
            } else {
                status.setText("Focus mode is OFF");
                focusButton.setText("Start Focus Session");
            }
        });

        exerciseButton.setOnClickListener(v ->
            startActivity(new Intent(this, ExerciseActivity.class))
        );
    }
}

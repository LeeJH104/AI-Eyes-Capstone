package com.example.obstacledetectiontest;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStartObstacleDetection = findViewById(R.id.btn_start_obstacle_detection);

        btnStartObstacleDetection.setOnClickListener(v -> {
            // 버튼을 누르면 ObstacleActivity를 시작합니다.
            Intent intent = new Intent(MainActivity.this, ObstacleActivity.class);
            startActivity(intent);
        });
    }
}
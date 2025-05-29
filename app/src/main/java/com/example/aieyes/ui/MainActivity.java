package com.example.aieyes.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.aieyes.R;
import com.example.aieyes.utils.PermissionHelper;
import com.example.aieyes.utils.TTSManager;
import com.example.aieyes.utils.VibrationHelper;

public class MainActivity extends AppCompatActivity {

    private TTSManager ttsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // 1. TTSManager 초기화 (반드시 Context를 넘겨야 함)
        ttsManager = new TTSManager(this);

        // 권한 요청 버튼
        Button permissionBtn = findViewById(R.id.permission_button);
        permissionBtn.setOnClickListener(v -> {
            if (PermissionHelper.hasAllPermissions(this)) {
                Toast.makeText(this, "이미 모든 권한이 허용되어 있습니다.", Toast.LENGTH_SHORT).show();
            } else {
                PermissionHelper.requestPermissions(this);
            }
        });

        // 음성 안내 버튼
        Button speakBtn = findViewById(R.id.speak_button);
        speakBtn.setOnClickListener(v -> {
            ttsManager.speak("안녕하세요. 음성 안내를 시작합니다.");
            VibrationHelper.vibrateShort(this);  // 짧은 진동
            Toast.makeText(this, "음성과 짧은 진동 호출됨", Toast.LENGTH_SHORT).show();
        });

        // 진동 테스트 버튼
        Button vibrateBtn = findViewById(R.id.vibrate_button);
        vibrateBtn.setOnClickListener(v -> {
            VibrationHelper.vibrateLong(this);  // 긴 진동
            Toast.makeText(this, "긴 진동 호출됨", Toast.LENGTH_SHORT).show();
        });
    }

    // 권한 요청 결과 콜백 처리
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        PermissionHelper.handlePermissionResult(requestCode, grantResults, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 3. Activity 종료 시 반드시 shutdown 호출 (메모리 누수 방지)
        ttsManager.shutdown();
    }
}
package com.example.aieyes.ui;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aieyes.R;
import com.example.aieyes.utils.PermissionHelper;
import com.example.aieyes.utils.STTManager;
import com.example.aieyes.utils.TTSManager;
import com.example.aieyes.utils.VibrationHelper;

/*
1. 권한 확인 및 요청 (PermissionHelper 활용)
앱이 실행되면 필요한 권한(마이크, 카메라, 위치, 진동)을 확인하고, 부족한 경우 사용자에게 요청합니다.
권한이 모두 허용되면 앱의 주요 기능을 초기화합니다.
사용자가 권한 요청을 거부하고 "다시 묻지 않음"을 선택한 경우, 앱 설정 화면으로 유도합니다.

2. STT (Speech-to-Text, 음성 인식)
사용자가 STT 버튼을 누르면 음성 인식을 시작합니다.
사용자가 STT로 입력한 내용을 텍스트(TextView)에 출력합니다.
음성으로 "긴 진동", "짧은 진동", "종료" 등의 명령어를 말하면 해당 기능을 수행합니다.
“긴 진동” → 길게 진동, "짧은 진동" → 짧게 진동, “종료” → 앱 종료

3. TTS (Text-to-Speech, 음성 출력)
TTS 버튼을 누르면 화면에 표시된 텍스트(TextView)를 음성으로 읽어줍니다.

4. 진동 피드백 (VibrationHelper)
특정 음성 명령("긴 진동", "짧은 진동")에 따라 스마트폰의 진동을 작동시켜 사용자에게 피드백을 줍니다.
시각장애인 사용자를 위한 촉각 정보 전달 수단으로 활용됩니다.

5. 자원 정리
액티비티 종료 시 TTS와 STT 관련 자원을 안전하게 해제하여 메모리 누수 방지.
*/

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private TTSManager ttsManager;
    private STTManager sttManager;

    // PermissionHelper 인스턴스를 클래스 필드로 저장
    private PermissionHelper permissionHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // PermissionHelper 초기화 및 권한 확인 + 요청 시작
        permissionHelper = new PermissionHelper(this, new PermissionHelper.OnPermissionResultListener() {
            @Override
            public void onPermissionGranted() {
                // 권한이 모두 승인되었을 때만 앱 주요 기능 초기화
                initializeAppFeatures();
            }
        });

        // 권한 확인 및 요청 실행
        permissionHelper.checkAndRequestPermissions();
    }

    /**
     * 권한이 모두 승인되었을 때 호출되는 앱 초기화 메서드
     * STT, TTS, 버튼 등 주요 UI 기능 초기화는 여기서 수행
     */
    private void initializeAppFeatures() {
        textView = findViewById(R.id.textView);
        Button btnSTT = findViewById(R.id.btnSTT);
        Button btnTTS = findViewById(R.id.btnTTS);

        textView.setText("녹음된 내용");

        // TTS 및 STT 초기화
        ttsManager = new TTSManager(this);
        sttManager = new STTManager(this);

        // STT 결과 콜백 설정
        sttManager.setOnSTTResultListener(new STTManager.OnSTTResultListener() {
            @Override
            public void onSTTResult(String result) {
                textView.setText(result);

                // 입력된 음성 결과에 따라 진동 테스트
                if (result.contains("긴 진동")) {
                    VibrationHelper.vibrateLong(MainActivity.this);
                } else if (result.contains("짧은 진동")) {
                    VibrationHelper.vibrateShort(MainActivity.this);
                } else if (result.contains("종료")) {
                    ttsManager.speak("앱을 종료합니다");
                    new Handler().postDelayed(() -> finish(), 1500); // 1.5초 후 종료
                }
            }

            @Override
            public void onSTTError(int errorCode) {
                Toast.makeText(MainActivity.this, "STT 오류 발생", Toast.LENGTH_SHORT).show();
            }
        });

        // STT 버튼 클릭 시 음성 인식 시작
        btnSTT.setOnClickListener(v -> sttManager.startListening());

        // TTS 버튼 클릭 시 현재 텍스트 읽어줌
        btnTTS.setOnClickListener(v -> {
            String text = textView.getText().toString();
            if (!text.isEmpty()) {
                ttsManager.speak(text);
            }
        });
    }

    /**
     * 권한 요청 결과가 돌아왔을 때 PermissionHelper로 전달
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 앱 종료 전 TTS, STT 해제
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) ttsManager.shutdown();
        if (sttManager != null) sttManager.destroy();
    }
}
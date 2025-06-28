package com.example.aieyes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aieyes.R;
import com.example.aieyes.ui.NavigationActivity;
import com.example.aieyes.ui.ReceiptActivity;
import com.example.aieyes.utils.GestureManager;
import com.example.aieyes.utils.PermissionHelper;
import com.example.aieyes.utils.STTManager;
import com.example.aieyes.utils.TTSManager;
import com.example.aieyes.utils.VibrationHelper;

/**
 * MainActivity
 * - 앱 실행 시 기능 선택을 안내하고
 * - STT, 제스처로 네비게이션 또는 영수증 기능을 실행할 수 있도록 함
 * - 모든 제스처 처리는 GestureManager 를 통해 구성
 */
public class MainActivity extends AppCompatActivity {

    private TTSManager ttsManager;
    private STTManager sttManager;

    private final String introMessage = "기능을 선택해주세요. 오른쪽은 네비게이션, 왼쪽은 영수증입니다. 또는 음성으로 말씀해주세요.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 좌우 화면 구성 포함한 XML 사용

        // 권한 확인 및 요청
        // 권한 승인된 경우만 초기화
        PermissionHelper permissionHelper = new PermissionHelper(this, this::initializeMainFeatures);
        permissionHelper.checkAndRequestPermissions();
    }

    private void initializeMainFeatures() {
        // TTS/STT 초기화
        ttsManager = new TTSManager(this);
        sttManager = new STTManager(this);

        // 음성 인식 결과 처리
        sttManager.setOnSTTResultListener(new STTManager.OnSTTResultListener() {
            @Override
            public void onSTTResult(String result) {
                handleVoiceCommand(result.trim().toLowerCase());
            }

            @Override
            public void onSTTError(int errorCode) {
                Toast.makeText(MainActivity.this, "음성 인식 오류", Toast.LENGTH_SHORT).show();
            }
        });

        // 제스처 연결
        findViewById(R.id.rootLayout).setOnTouchListener(
                GestureManager.createGestureListener(this, new GestureManager.OnGestureListener() {
                    @Override
                    public void onSwipeLeft() {
                        // 👈 왼쪽 스와이프: 영수증
                        ttsManager.speak("영수증 기능을 선택하셨습니다.", () -> {
                            VibrationHelper.vibrateLong(MainActivity.this);
                            startActivity(new Intent(MainActivity.this, ReceiptActivity.class));
                        });
                    }

                    @Override
                    public void onSwipeRight() {
                        // 👉 오른쪽 스와이프: 네비게이션
                        ttsManager.speak("네비게이션 기능을 선택하셨습니다.", () -> {
                            VibrationHelper.vibrateLong(MainActivity.this);
                            startActivity(new Intent(MainActivity.this, NavigationActivity.class));
                        });
                    }

                    @Override
                    public void onDoubleTap() {
                        ttsManager.speak(introMessage, () -> sttManager.startListening());
                    }

                    @Override
                    public void onSwipeUp() {}

                    @Override
                    public void onSwipeDown() {}
                })
        );

        // UI 텍스트 표시
        ((TextView) findViewById(R.id.tv_navigation)).setText("네비게이션 기능:\\n\\n 👉 오른쪽 스와이프 또는 '네비게이션'이라고 말하세요");
        ((TextView) findViewById(R.id.tv_receipt)).setText("영수증 기능:\\n\\n 👈 왼쪽 스와이프 또는 '영수증'이라고 말하세요");

        // 안내 및 STT 시작
        speakIntroAndListen();
    }

    private void speakIntroAndListen() {
        ttsManager.speak(introMessage, () -> {
            VibrationHelper.vibrateShort(MainActivity.this); // STT 대기 진동
            sttManager.startListening();
        });
    }

    private void handleVoiceCommand(String voice) {
        if (voice.contains("네비게이션")) {
            // 🎤 네비게이션 선택 시 TTS 안내 후 화면 이동
            ttsManager.speak("네비게이션 기능을 선택하셨습니다.", () -> {
                VibrationHelper.vibrateLong(this); // 긴 진동 피드백
                startActivity(new Intent(MainActivity.this, NavigationActivity.class));
            });
        } else if (voice.contains("영수증")) {
            // 🧾 영수증 기능 선택 시 TTS 안내 후 화면 이동
            ttsManager.speak("영수증 기능을 선택하셨습니다.", () -> {
                VibrationHelper.vibrateLong(this); // 긴 진동 피드백
                startActivity(new Intent(MainActivity.this, ReceiptActivity.class));
            });
        } else if (voice.contains("다시")) {
            ttsManager.speak("다시 안내해 드릴게요.", this::speakIntroAndListen);
        } else if (voice.contains("종료")) {
            ttsManager.speak("앱을 종료합니다.", this::finish);
        } else {
            ttsManager.speak("명령을 인식하지 못했습니다. 다시 말씀해주세요.", () -> sttManager.startListening());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        speakIntroAndListen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) ttsManager.shutdown();
        if (sttManager != null) sttManager.destroy();
    }
}
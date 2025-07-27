package com.example.aieyes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aieyes.R;
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
    private final String introMessage = "기능을 선택해주세요. 오른쪽 스와이프는 네비게이션, 왼쪽 스와이프는 영수증입니다. 또는 음성으로 말씀해주세요.";
    private PermissionHelper permissionHelper; // 권한을 처리하는 헬퍼 클래스
    private boolean isInitialized = false; // 초기화 여부 플래그
    private boolean isSelected = false; // 중복 실행 방지 및 타이밍 충돌 방지

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 좌우 화면 구성 포함한 XML 사용

        // 권한 헬퍼 초기화 및 권한 요청 처리
        permissionHelper = new PermissionHelper(this, this::initializeMainFeatures);
        // 실제 권한 확인 및 사용자에게 요청
        permissionHelper.checkAndRequestPermissions();
    }

    private void initializeMainFeatures() {
        if (isInitialized) return;  // 중복 방지
        isInitialized = true;   // 초기화 수행

        Log.d("MainActivity", "🔧 initializeMainFeatures() 진입"); // 반드시 찍히는지 확인

        // TTS/STT 초기화
        ttsManager = new TTSManager(this);
        ttsManager.setOnTTSReadyListener(() -> {
            // ✅ TTS가 준비된 이후에 STT도 초기화
            sttManager = new STTManager(this);
            // 음성 인식 결과 처리
            sttManager.setOnSTTResultListener(new STTManager.OnSTTResultListener() {
                @Override
                public void onSTTResult(String result) {
                    handleVoiceCommand(result.trim().toLowerCase());
                }

                @Override
                public void onSTTError(int errorCode) {
                    sttManager.restartListening(); // 아무 입력이 없을 때 실행
                }
            });

            // ✅ 그 후 안내 메시지를 말하고 STT 대기 시작
            speakIntroAndListen();

            // ✅ 제스처 초기화도 여기서 가능 (안전한 시점)
            handleGestures();
        });
    }

    private void speakIntroAndListen() {
        ttsManager.speak(introMessage, () -> {
            VibrationHelper.vibrateShort(MainActivity.this); // STT 대기 진동
            sttManager.restartListening();
        });
    }

    private void handleVoiceCommand(String voice) {
        if (isSelected) return; // 이미 기능이 선택 되었으면 무시

        if (voice.contains("네비게이션") || voice.contains("내비게이션") || voice.contains("네비") || voice.contains("내비")) {
            isSelected = true;
            // 🎤 네비게이션 선택 시 TTS 안내 후 화면 이동
            ttsManager.speak("내비게이션 기능을 선택하셨습니다.", () -> {
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
            ttsManager.speak("명령을 인식하지 못했습니다. 다시 말씀해주세요.", () -> sttManager.restartListening());
        }
    }

    private void handleGestures() {
        // 제스처 연결
        LinearLayout rootLayout = findViewById(R.id.rootLayout);
        rootLayout.setOnTouchListener(GestureManager.createGestureListener(this, new GestureManager.OnGestureListener() {
            @Override
            public void onSwipeLeft() {
                Log.d("Gesture", "왼쪽 스와이프 감지됨");
                if (isSelected) return; // 이미 기능 선택됨 → 무시

                isSelected = true;
                // 👈 왼쪽 스와이프: 영수증
                ttsManager.speak("영수증 기능을 선택하셨습니다.", () -> {
                    VibrationHelper.vibrateLong(MainActivity.this);
                    startActivity(new Intent(MainActivity.this, ReceiptActivity.class));
                });
            }

            @Override
            public void onSwipeRight() {
                Log.d("Gesture", "오른쪽 스와이프 감지됨");
                if (isSelected) return; // 이미 기능 선택됨 → 무시

                isSelected = true;
                // 👉 오른쪽 스와이프: 네비게이션
                ttsManager.speak("네비게이션 기능을 선택하셨습니다.", () -> {
                    VibrationHelper.vibrateLong(MainActivity.this);
                    startActivity(new Intent(MainActivity.this, NavigationActivity.class));
                });
            }

            @Override
            public void onDoubleTap() {
                ttsManager.speak(introMessage, () -> sttManager.restartListening());
            }

            @Override
            public void onSwipeUp() {}

            @Override
            public void onSwipeDown() {}
            })
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        isSelected = false; // 다시 돌아왔을 때만 초기화

        // 권한이 모두 허용된 상태인데 초기화가 안 됐으면 수동으로 실행
        if (!isInitialized && PermissionHelper.arePermissionsGranted(this)) {
            initializeMainFeatures();
        }

        // Null 체크로 예외 방지
        if (ttsManager != null) {
            speakIntroAndListen();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) ttsManager.shutdown();
        if (sttManager != null) sttManager.destroy();
    }
}

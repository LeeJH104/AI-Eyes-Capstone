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
import com.example.capstone_map.feature.NavigationActivity;
import com.example.aieyes.ui.ObstacleActivity;

public class MainActivity extends AppCompatActivity {

    private TTSManager ttsManager;
    private STTManager stTManager;
    private final String introMessage = "기능을 선택해주세요. 오른쪽 스와이프는 네비게이션, 왼쪽 스와이프는 영수증, 아래쪽 스와이프는 장애물 탐지입니다. 또는 음성으로 네비게이션, 영수증, 장애물 탐지라고 말씀해주세요.";
    private PermissionHelper permissionHelper; // 권한을 처리하는 헬퍼 클래스
    private boolean isInitialized = false; // 초기화 여부 플래그
    private boolean isSelected = false; // 중복 실행 방지 및 타이밍 충돌 방지

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 권한 헬퍼 초기화 및 권한 요청 처리
        permissionHelper = new PermissionHelper(this, this::initializeMainFeatures);
        // 실제 권한 확인 및 사용자에게 요청
        permissionHelper.checkAndRequestPermissions();
    }

    private void initializeMainFeatures() {
        if (isInitialized) return;  // 중복 방지
        isInitialized = true;   // 초기화 수행

        Log.d("MainActivity", "🔧 initializeMainFeatures() 진입");

        // TTS/STT 초기화
        ttsManager = new TTSManager(this);
        ttsManager.setOnTTSReadyListener(() -> {
            stTManager = new STTManager(this);
            // 음성 인식 결과 처리
            stTManager.setOnSTTResultListener(new STTManager.OnSTTResultListener() {
                @Override
                public void onSTTResult(String result) {
                    handleVoiceCommand(result.trim().toLowerCase());
                }

                @Override
                public void onSTTError(int errorCode) {
                    stTManager.restartListening(); // 아무 입력이 없을 때 실행
                }
            });

            speakIntroAndListen();
            handleGestures();
        });
    }

    private void speakIntroAndListen() {
        ttsManager.speak(introMessage, () -> {
            VibrationHelper.vibrateShort(MainActivity.this); // STT 대기 진동
            stTManager.restartListening();
        });
    }

    private void handleVoiceCommand(String voice) {
        if (isSelected) return; // 이미 기능이 선택 되었으면 무시

        if (voice.contains("네비게이션") || voice.contains("내비게이션") || voice.contains("네비") || voice.contains("내비")) {
            isSelected = true;
            ttsManager.speak("내비게이션 기능을 선택하셨습니다.", () -> {
                VibrationHelper.vibrateLong(this);
                VibrationHelper.vibrateLong(this);
                startActivity(new Intent(MainActivity.this, NavigationActivity.class));
            });
        } else if (voice.contains("영수증")) {
            isSelected = true;
            ttsManager.speak("영수증 기능을 선택하셨습니다.", () -> {
                VibrationHelper.vibrateLong(this);
                startActivity(new Intent(MainActivity.this, ReceiptActivity.class));
            });
        } else if (voice.contains("장애물 탐지") || voice.contains("장애물")) {
            isSelected = true;
            ttsManager.speak("장애물 탐지 기능을 선택하셨습니다.", () -> {
                VibrationHelper.vibrateLong(this);
                startActivity(new Intent(MainActivity.this, com.example.aieyes.ui.ObstacleActivity.class));
            });
        } else if (voice.contains("다시")) {
            ttsManager.speak("다시 안내해 드릴게요.", this::speakIntroAndListen);
        } else if (voice.contains("종료")) {
            ttsManager.speak("앱을 종료합니다.", this::finish);
        } else {
            ttsManager.speak("명령을 인식하지 못했습니다. 다시 말씀해주세요.", () -> stTManager.restartListening());
        }
    }

    private void handleGestures() {
        LinearLayout rootLayout = findViewById(R.id.rootLayout);
        rootLayout.setOnTouchListener(GestureManager.createGestureListener(this, new GestureManager.OnGestureListener() {
            @Override
            public void onSwipeLeft() {
                Log.d("Gesture", "왼쪽 스와이프 감지됨");
                if (isSelected) return;
                isSelected = true;
                ttsManager.speak("영수증 기능을 선택하셨습니다.", () -> {
                    VibrationHelper.vibrateLong(MainActivity.this);
                    startActivity(new Intent(MainActivity.this, ReceiptActivity.class));
                });
            }

            @Override
            public void onSwipeRight() {
                Log.d("Gesture", "오른쪽 스와이프 감지됨");
                if (isSelected) return;
                isSelected = true;
                ttsManager.speak("네비게이션 기능을 선택하셨습니다.", () -> {
                    VibrationHelper.vibrateLong(MainActivity.this);
                    startActivity(new Intent(MainActivity.this, NavigationActivity.class));
                });
            }

            @Override
            public void onDoubleTap() {
                ttsManager.speak(introMessage, () -> stTManager.restartListening());
            }

            @Override
            public void onSwipeUp() {}

            @Override
            public void onSwipeDown() {
                Log.d("Gesture", "아래쪽 스와이프 감지됨");
                if (isSelected) return;
                isSelected = true;
                ttsManager.speak("장애물 탐지 기능을 선택하셨습니다.", () -> {
                    VibrationHelper.vibrateLong(MainActivity.this);
                    startActivity(new Intent(MainActivity.this, com.example.aieyes.ui.ObstacleActivity.class));
                });
            }
        }));
    }

    @Override
    protected void onResume() {
    super.onResume();
    // 다른 액티비티에서 돌아왔을 때, 다시 기능을 선택할 수 있도록 플래그를 초기화합니다.
    isSelected = false;

    // ▼▼▼ [수정된 부분 시작] ▼▼▼
    // isInitialized가 true라는 것은 최초 실행이 아니라 다른 화면에서 돌아왔다는 의미입니다.
    // 이 때 다시 안내 메시지를 재생하고 음성 인식을 시작하도록 호출합니다.
    if (isInitialized) {
        Log.d("MainActivity", "onResume: 화면으로 복귀하여 안내 메시지 다시 시작");
        speakIntroAndListen();
    }
    // ▲▲▲ [수정된 부분 끝] ▲▲▲

    // 이 코드는 그대로 유지합니다.
    // (혹시 권한을 늦게 허용했을 경우, 최초 초기화를 실행하기 위함)
    if (!isInitialized && PermissionHelper.arePermissionsGranted(this)) {
        initializeMainFeatures();
    }
}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) ttsManager.shutdown();
        if (stTManager != null) stTManager.destroy();
    }
}
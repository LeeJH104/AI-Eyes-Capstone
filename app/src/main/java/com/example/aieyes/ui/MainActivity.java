package com.example.aieyes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.NonNull;

import com.example.aieyes.R;
import com.example.aieyes.utils.GestureManager;
import com.example.aieyes.utils.PermissionHelper;
import com.example.aieyes.utils.STTManager;
import com.example.aieyes.utils.TTSManager;
import com.example.aieyes.utils.VibrationHelper;
import com.example.capstone_map.feature.NavigationActivity;
import com.example.aieyes.ui.ObstacleActivity;

/**
 * 앱의 메인 화면 액티비티.
 * 기능 선택 및 각 기능 화면으로 연결하는 역할을 합니다.
 */
public class MainActivity extends AppCompatActivity {

    // --- 멤버 변수 선언 --- //
    private TTSManager ttsManager;
    private STTManager stTManager;
    private PermissionHelper permissionHelper;

    private final String introMessage = "기능을 선택해주세요. 오른쪽 스와이프는 네비게이션, 왼쪽 스와이프는 영수증, 아래쪽 스와이프는 장애물 탐지입니다. 또는 음성으로 네비게이션, 영수증, 장애물 탐지라고 말씀해주세요.";

    private boolean isInitialized = false; // 앱 기능 초기화 여부 플래그
    private boolean isSelected = false;    // 기능 중복 선택 방지 플래그

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TTS 매니저 생성 및 초기화
        ttsManager = new TTSManager(this);
        // TTS 준비 완료 후, 권한 확인 및 요청
        ttsManager.setOnTTSReadyListener(() -> {
            if (PermissionHelper.arePermissionsGranted(this)) {
                // 권한 있으면, 바로 기능 시작
                initializeMainFeatures();
            } else {
                // 권한 없으면, 음성 안내 후 요청
                requestPermissionsWithTTS();
            }
        });
    }

    // TTS 음성으로 권한 요청 안내
    private void requestPermissionsWithTTS() {
        ttsManager.speak("앱 사용을 위해 권한을 승인해주세요.", () -> {
            // 음성 안내 후, 실제 권한 요청
            permissionHelper = new PermissionHelper(this, this::initializeMainFeatures);
            permissionHelper.checkAndRequestPermissions();
        });
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionHelper != null) {
            permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // 모든 권한 승인 후, 앱 핵심 기능 초기화
    private void initializeMainFeatures() {
        if (isInitialized) return;  // 중복 초기화 방지
        isInitialized = true;

        Log.d("MainActivity", "🔧 initializeMainFeatures() 진입");

        // STT 매니저 생성 및 리스너 설정
        stTManager = new STTManager(this);
        stTManager.setOnSTTResultListener(new STTManager.OnSTTResultListener() {
            // 음성 인식 성공 시
            @Override
            public void onSTTResult(String result) {
                handleVoiceCommand(result.trim().toLowerCase());
            }
            // 음성 인식 에러 시
            @Override
            public void onSTTError(int errorCode) {
                stTManager.restartListening();  // 다시 듣기 모드로 전환
            }
        });

        speakIntroAndListen(); // 초기 안내 음성 출력 및 STT 시작
        handleGestures();      // 제스처 인식 활성화
    }

    // TTS로 초기 안내 후 STT 시작
    private void speakIntroAndListen() {
        ttsManager.speak(introMessage, () -> {
            // 안내 후 진동 피드백 및 STT 시작
            VibrationHelper.vibrateShort(MainActivity.this);
            stTManager.restartListening();
        });
    }

    // 음성 명령 처리
    private void handleVoiceCommand(String voice) {
        if (isSelected) return; // 기능 중복 선택 방지

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
                startActivity(new Intent(MainActivity.this, ObstacleActivity.class));
            });
        } else if (voice.contains("다시")) {
            ttsManager.speak("다시 안내해 드릴게요.", this::speakIntroAndListen);
        } else if (voice.contains("종료")) {
            ttsManager.speak("앱을 종료합니다.", this::finish);
        } else {
            ttsManager.speak("명령을 인식하지 못했습니다. 다시 말씀해주세요.", () -> stTManager.restartListening());
        }
    }

    // 제스처 인식 리스너 설정
    private void handleGestures() {
        LinearLayout rootLayout = findViewById(R.id.rootLayout);
        rootLayout.setOnTouchListener(GestureManager.createGestureListener(this, new GestureManager.OnGestureListener() {
            @Override
            public void onSwipeLeft() {
                if (isSelected) return;
                isSelected = true;
                ttsManager.speak("영수증 기능을 선택하셨습니다.", () -> {
                    VibrationHelper.vibrateLong(MainActivity.this);
                    startActivity(new Intent(MainActivity.this, ReceiptActivity.class));
                });
            }
            @Override
            public void onSwipeRight() {
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
                if (isSelected) return;
                isSelected = true;
                ttsManager.speak("장애물 탐지 기능을 선택하셨습니다.", () -> {
                    VibrationHelper.vibrateLong(MainActivity.this);
                    startActivity(new Intent(MainActivity.this, ObstacleActivity.class));
                });
            }
        }));
    }

    // 화면 복귀 시 호출
    @Override
    protected void onResume() {
        super.onResume();
        isSelected = false; // 기능 선택 플래그 초기화

        // 앱 초기화 완료 시, 안내 재시작
        if (isInitialized) {
            speakIntroAndListen();
        }
        // 아직 초기화되지 않은 상태에서 (예: 설정 화면에서) 돌아왔는데, 권한이 모두 부여된 경우
        else if (PermissionHelper.arePermissionsGranted(this)) {
            initializeMainFeatures();
        }
    }

    // 화면 벗어날 때 호출
    @Override
    protected void onPause() {
        super.onPause();
        if (stTManager != null) {
            stTManager.stopListening();
        }
    }

    // 액티비티 종료 시 호출
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) ttsManager.shutdown();
        if (stTManager != null) stTManager.destroy();
    }
}
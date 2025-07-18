package com.example.aieyes.ui;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aieyes.R;
import com.example.aieyes.utils.PermissionHelper;
import com.example.aieyes.utils.STTManager;
import com.example.aieyes.utils.TTSManager;
import com.example.aieyes.utils.VibrationHelper;
import com.example.aieyes.utils.GestureManager;

/*
1. 권한 확인 및 요청 (PermissionHelper 활용)
   - 앱이 실행되면 필요한 권한(마이크, 카메라, 위치, 진동)을 확인하고, 부족한 경우 사용자에게 요청합니다.
   - 권한이 모두 허용되면 앱의 주요 기능을 초기화합니다.
   - 사용자가 권한 요청을 거부하고 "다시 묻지 않음"을 선택한 경우, 앱 설정 화면으로 유도합니다.

2. STT (Speech-to-Text, 음성 인식)
   - 사용자가 STT 버튼을 누르면 음성 인식을 시작합니다.
   - 사용자가 STT로 입력한 내용을 텍스트(TextView)에 출력합니다.
   - 음성으로 "긴 진동", "짧은 진동", "종료" 등의 명령어를 말하면 해당 상태가 저장됩니다.
   - 진동은 즉시 실행되지 않으며, 이후 제스처를 통해 실행됩니다.
   - “긴 진동” → 오른쪽 스와이프 시 길게 진동, "짧은 진동" → 왼쪽 스와이프 시 짧게 진동, “종료” → 앱 종료

3. TTS (Text-to-Speech, 음성 출력)
   - TTS 버튼을 누르거나 화면을 더블탭하면, 현재 TextView에 표시된 텍스트를 음성으로 읽어줍니다.
   - 앱 최초 실행 시, 사용 방법 안내 메시지를 TTS로 자동 안내합니다.

4. 진동 피드백 (VibrationHelper)
   - 음성 명령을 통해 진동 유형을 설정하고, 이후 제스처를 통해 해당 진동을 실행합니다.
   - 시각장애인 사용자를 위한 촉각 정보 전달 수단으로 활용됩니다.

5. 자원 정리
   - 액티비티 종료 시 TTS와 STT 관련 자원을 안전하게 해제하여 메모리 누수 방지.

6. 제스처 기능 (GestureManager 활용)
   - 화면 전체(root layout)에 제스처 리스너를 설정하여 다음 제스처를 인식합니다:
       • 더블탭: 현재 텍스트 내용을 음성으로 읽어줌 (TTS)
       • 오른쪽 스와이프: "긴 진동" 상태일 경우, 진동을 길게 울림
       • 왼쪽 스와이프: "짧은 진동" 상태일 경우, 진동을 짧게 울림
   - 사용자가 음성으로 먼저 "긴 진동" 또는 "짧은 진동"을 말한 뒤 제스처를 실행해야 진동이 작동합니다.
*/

public class TestActivity extends AppCompatActivity {

    private TextView textView; // 음성 인식 결과 또는 안내 텍스트를 보여줄 텍스트뷰
    private TextView guideTextView; // 앱 사용 방법을 보여줄 텍스트뷰
    private TTSManager ttsManager; // 텍스트를 음성으로 출력해주는 TTS 매니저
    private STTManager sttManager; // 음성을 텍스트로 변환하는 STT 매니저
    private PermissionHelper permissionHelper; // 권한을 처리하는 헬퍼 클래스

    private String pendingVibration = ""; // "long" 또는 "short" 값으로 진동 대기 상태 저장

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity_main);

        // 권한 헬퍼 초기화 및 권한 요청 처리
        permissionHelper = new PermissionHelper(this, new PermissionHelper.OnPermissionResultListener() {
            @Override
            public void onPermissionGranted() {
                // 권한이 승인되었을 때 기능 초기화 수행
                initializeAppFeatures();
            }
        });

        // 실제 권한 확인 및 사용자에게 요청
        permissionHelper.checkAndRequestPermissions();
    }

    // 권한이 승인된 후 앱 주요 기능 초기화
    private void initializeAppFeatures() {
        // 텍스트뷰 초기화
        textView = findViewById(R.id.textView);
        guideTextView = findViewById(R.id.guideTextView); // 사용 방법 안내 텍스트뷰
        Button btnSTT = findViewById(R.id.btnSTT);
        Button btnTTS = findViewById(R.id.btnTTS);

        // 텍스트 크기 설정 (저시력자 배려)
        textView.setTextSize(28);
        guideTextView.setTextSize(24);

        // 사용 방법 안내 텍스트 설정
        guideTextView.setText("[사용 방법]\n1. 화면 더블탭: 텍스트 읽기\n2. '긴 진동' 말한 뒤 오른쪽 스와이프\n3. '짧은 진동' 말한 뒤 왼쪽 스와이프");

        // TTS, STT 매니저 초기화
        ttsManager = new TTSManager(this);
        sttManager = new STTManager(this);

        // 앱 첫 실행 시 사용 방법 안내를 음성으로 출력
        // TTS 초기화 완료 후 음성 안내 시작
        ttsManager.setOnTTSReadyListener(() -> {
            ttsManager.speak("화면을 더블탭하면 텍스트를 읽습니다. 긴 진동 또는 짧은 진동을 말한 뒤 스와이프 하세요.");
        });

        // STT 결과 수신 콜백 처리
        sttManager.setOnSTTResultListener(new STTManager.OnSTTResultListener() {
            @Override
            public void onSTTResult(String result) {
                // 텍스트뷰에 음성 인식 결과 표시
                textView.setText(result);

                // 음성 명령어 처리
                if (result.contains("긴 진동")) {
                    pendingVibration = "long"; // 오른쪽 스와이프 대기 상태
                    ttsManager.speak("긴 진동 대기 중. 오른쪽으로 스와이프하세요.");
                } else if (result.contains("짧은 진동")) {
                    pendingVibration = "short"; // 왼쪽 스와이프 대기 상태
                    ttsManager.speak("짧은 진동 대기 중. 왼쪽으로 스와이프하세요.");
                } else if (result.contains("종료")) {
                    ttsManager.speak("앱을 종료합니다.");
                    new Handler().postDelayed(() -> finish(), 1500); // 1.5초 후 앱 종료
                } else {
                    pendingVibration = ""; // 다른 명령어일 경우 대기 초기화
                }
            }

            @Override
            public void onSTTError(int errorCode) {
                Toast.makeText(TestActivity.this, "STT 오류 발생", Toast.LENGTH_SHORT).show();
            }
        });

        // STT 버튼 클릭 시 음성 인식 시작
        btnSTT.setOnClickListener(v -> sttManager.startListening());

        // TTS 버튼 클릭 시 텍스트 읽기 실행 (테스트용으로 유지)
        btnTTS.setOnClickListener(v -> {
            String text = textView.getText().toString();
            if (!text.isEmpty()) {
                ttsManager.speak(text);
            }
        });

        // 제스처 리스너 연결 (화면 전체에 적용)
        // 노란 밑줄은 주의사항 또는 권장사항을 의미. 잘 작동하면 무시해도 됨
        // 더 간단하게 findViewById(...).setOnTouchListener로 해도 되지만 아래 방법이 코드 재사용 쉬움, 디버깅 쉬움
        LinearLayout testLayout = findViewById(R.id.testLayout);
        testLayout.setOnTouchListener(GestureManager.createGestureListener(this, new GestureManager.OnGestureListener() {
            @Override
            public void onSwipeLeft() {
                Log.d("GestureTest", "왼쪽 스와이프");
                // 왼쪽 스와이프 시 짧은 진동 조건 확인
                if ("short".equals(pendingVibration)) {
                    VibrationHelper.vibrateShort(TestActivity.this);
                    ttsManager.speak("짧은 진동");
                } else {
                    Toast.makeText(TestActivity.this, "왼쪽 스와이프", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onSwipeRight() {
                Log.d("GestureTest", "오른쪽 스와이프");
                // 오른쪽 스와이프 시 긴 진동 조건 확인
                if ("long".equals(pendingVibration)) {
                    VibrationHelper.vibrateLong(TestActivity.this);
                    ttsManager.speak("긴 진동");
                } else {
                    Toast.makeText(TestActivity.this, "오른쪽 스와이프", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onSwipeUp() {}

            @Override
            public void onSwipeDown() {}

            @Override
            public void onDoubleTap() {
                // 더블탭 시 텍스트 읽기
                String text = textView.getText().toString();
                if (!text.isEmpty()) {
                    ttsManager.speak(text);
                }
            }
        }));
    }

    // 권한 요청 결과 콜백 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // 액티비티 종료 시 리소스 정리
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) ttsManager.shutdown();
        if (sttManager != null) sttManager.destroy();
    }
}

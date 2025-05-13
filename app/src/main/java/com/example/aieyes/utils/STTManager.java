package com.example.aieyes.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

/*
🔐 필요 권한
AndroidManifest.xml에 꼭 추가하세요:
xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.INTERNET"/>
*/

/**
 * STTManager
 * 음성 인식을 쉽게 사용할 수 있도록 감싸주는 유틸 클래스
 * 내부적으로 SpeechRecognizer + RecognizerIntent를 사용함
 */
public class STTManager {

    private final Activity activity; // STT를 사용할 Activity context
    private SpeechRecognizer speechRecognizer; // 실제 음성 인식을 담당하는 객체
    private Intent sttIntent; // 음성 인식 동작을 정의하는 인텐트
    private OnSTTResultListener resultListener; // 외부에서 인식 결과를 받을 리스너

    /**
     * 생성자: Activity를 받아서 STT 관련 객체 초기화
     */
    public STTManager(Activity activity) {
        this.activity = activity;

        // 1. SpeechRecognizer 생성
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);

        // 2. 음성 인식 이벤트를 처리할 리스너 설정
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d("STT", "음성 인식 준비 완료");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d("STT", "사용자 말하기 시작됨");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // 음성 세기 변화 감지 (시각 피드백 구현 시 사용 가능)
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // 추가 데이터 수신 (거의 사용되지 않음)
            }

            @Override
            public void onEndOfSpeech() {
                Log.d("STT", "사용자 말하기 종료");
            }

            @Override
            public void onError(int error) {
                String errorMsg = "음성 인식 오류 발생: " + error;
                Log.e("STT", errorMsg);
                Toast.makeText(activity, errorMsg, Toast.LENGTH_SHORT).show();

                if (resultListener != null) {
                    resultListener.onSTTError(error); // 외부에 오류 코드 전달
                }
            }

            @Override
            public void onResults(Bundle results) {
                // 최종 결과 수신
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0); // 가장 첫 번째 인식 결과 사용
                    Log.d("STT", "인식 결과: " + text);
                    if (resultListener != null) {
                        resultListener.onSTTResult(text);
                    }
                } else {
                    Log.w("STT", "인식 결과 없음");
                    if (resultListener != null) {
                        resultListener.onSTTError(-1); // 결과 없음도 에러 처리
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // 중간 결과 처리 안함
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // 기타 이벤트 (사용 안 함)
            }
        });

        // 3. 음성 인식 Intent 설정 (인식 방식/언어 지정)
        sttIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN);
        sttIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activity.getPackageName());
    }

    /**
     * 음성 인식을 시작하는 메서드
     */
    public void startListening() {
        if (speechRecognizer != null) {
            Log.d("STT", "음성 인식 시작");
            speechRecognizer.startListening(sttIntent);
        }
    }

    /**
     * 음성 인식을 중단하는 메서드
     */
    public void stopListening() {
        if (speechRecognizer != null) {
            Log.d("STT", "음성 인식 중단");
            speechRecognizer.stopListening();
        }
    }

    /**
     * 사용이 끝난 후 자원을 해제하는 메서드 (메모리 누수 방지)
     */
    public void destroy() {
        if (speechRecognizer != null) {
            Log.d("STT", "STT 리소스 해제");
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    /**
     * 음성 인식 결과 수신을 위한 리스너 설정
     */
    public void setOnSTTResultListener(OnSTTResultListener listener) {
        this.resultListener = listener;
    }

    /**
     * 음성 인식 결과 콜백 인터페이스
     */
    public interface OnSTTResultListener {
        void onSTTResult(String result);       // 인식 성공 시 호출
        void onSTTError(int errorCode);        // 오류 발생 시 호출
    }
}

/*
STTManager 사용 예시:

import com.example.aieyes.utils.STTManager;

public class MainActivity extends AppCompatActivity {

    private STTManager sttManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. STTManager 초기화
        sttManager = new STTManager(this);

        // 2. STT 결과 리스너 등록
        sttManager.setOnSTTResultListener(new STTManager.OnSTTResultListener() {
            @Override
            public void onSTTResult(String result) {
                // 음성 인식 결과 처리
                Log.d("STT_RESULT", result);
                ttsManager.speak("당신이 말한 내용은 " + result + "입니다.");
            }

            @Override
            public void onSTTError(int errorCode) {
                // 오류 처리
                ttsManager.speak("음성 인식에 실패했습니다.");
            }
        });

        // 3. 버튼 클릭 시 STT 실행
        Button sttBtn = findViewById(R.id.stt_button);
        sttBtn.setOnClickListener(v -> {
            sttManager.startListening();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sttManager.destroy(); // STT 리소스 해제
        ttsManager.shutdown(); // TTS 리소스 해제
    }
}
*/



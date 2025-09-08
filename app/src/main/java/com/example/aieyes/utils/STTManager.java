package com.example.aieyes.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

public class STTManager {

    private final Activity activity;
    private SpeechRecognizer speechRecognizer;
    private Intent sttIntent;
    private OnSTTResultListener resultListener;
    private boolean isListening = false;
    private final Handler mainHandler;

    public STTManager(Activity activity) {
        this.activity = activity;
        this.mainHandler = new Handler(Looper.getMainLooper());
        // 생성 시점에 메인 스레드에서 초기화를 한 번 수행
        mainHandler.post(this::initialize);
    }

    // SpeechRecognizer를 생성하고 설정하는 내부 메서드 (동기적으로 작동해야 함)
    private void initialize() {
        // 기존 객체가 있다면 파괴
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d("STT", "음성 인식 준비 완료");
                isListening = true;
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d("STT", "사용자 말하기 시작됨");
            }

            @Override
            public void onEndOfSpeech() {
                Log.d("STT", "사용자 말하기 종료");
            }

            @Override
            public void onError(int error) {
                // isListening 상태를 false로 바꿔야 재시작이 가능
                if (isListening) {
                    Log.d("STT", "Error code: " + error);
                    isListening = false;
                    if (resultListener != null) {
                        resultListener.onSTTError(error);
                    }
                }
            }

            @Override
            public void onResults(Bundle results) {
                Log.d("STT", "음성 인식 결과 받음");
                isListening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    if (resultListener != null) {
                        resultListener.onSTTResult(matches.get(0));
                    }
                } else {
                    if (resultListener != null) {
                        resultListener.onSTTError(SpeechRecognizer.ERROR_NO_MATCH);
                    }
                }
            }

            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        sttIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN);
        sttIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activity.getPackageName());
    }

    public void startListening() {
        mainHandler.post(() -> {
            if (speechRecognizer != null && !isListening) {
                Log.d("STT", "음성 인식 시작");
                speechRecognizer.startListening(sttIntent);
            }
        });
    }

    public void stopListening() {
        mainHandler.post(() -> {
            if (speechRecognizer != null && isListening) {
                Log.d("STT", "음성 인식 강제 중단");
                speechRecognizer.stopListening();
            }
        });
    }

    // ▼▼▼ [핵심 수정 부분] ▼▼▼
    // 파괴, 생성, 시작의 모든 과정을 하나의 Handler 작업 안에서 순차적으로 실행하여
    // 스레드 실행 순서가 꼬이는 문제를 해결합니다.
    public void restartListening() {
        mainHandler.post(() -> {
            Log.d("STT", "음성 인식 재시작 요청");
            isListening = false; // 상태 초기화

            // 1. 기존 리소스가 있다면 확실히 해제
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
            }

            // 2. SpeechRecognizer 새로 생성 및 리스너/인텐트 설정 (initialize() 메서드 호출)
            initialize();

            // 3. 음성 인식 시작
            speechRecognizer.startListening(sttIntent);
        });
    }

    public void destroy() {
        mainHandler.post(() -> {
            if (speechRecognizer != null) {
                Log.d("STT", "STT 리소스 해제");
                speechRecognizer.destroy();
                speechRecognizer = null;
            }
        });
    }

    public void setOnSTTResultListener(OnSTTResultListener listener) {
        this.resultListener = listener;
    }


    public interface OnSTTResultListener {
        void onSTTResult(String result);
        void onSTTError(int errorCode);
    }
}
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
    private Handler mainHandler;

    public STTManager(Activity activity) {
        this.activity = activity;
        this.mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(this::initialize);
    }

    private void initialize() {
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
                // onResults나 onError에서 isListening 상태를 관리
            }

            @Override
            public void onError(int error) {
                isListening = false;
                if (resultListener != null) {
                    resultListener.onSTTError(error);
                }
            }

            @Override
            public void onResults(Bundle results) {
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
        
        // ▼▼▼ 이 타임아웃 설정 하나만 남겨둡니다 ▼▼▼
        sttIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 300000); // 5분
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
            if (speechRecognizer != null) { // isListening 체크 제거하여 확실히 중지
                Log.d("STT", "음성 인식 강제 중단");
                speechRecognizer.stopListening();
            }
        });
    }

    public void restartListening() {
        mainHandler.post(() -> {
            if (speechRecognizer != null) {
                Log.d("STT", "음성 인식 재시작 요청");
                speechRecognizer.cancel();
                speechRecognizer.startListening(sttIntent);
            }
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
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

public class STTManager {

    private final Activity activity;
    private SpeechRecognizer speechRecognizer;
    private Intent sttIntent;
    private OnSTTResultListener resultListener;
    private boolean isListening = false;

    public STTManager(Activity activity) {
        this.activity = activity;
        initialize();
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
                isListening = false;
            }

            @Override
            public void onError(int error) {
                isListening = false;
                String errorMsg;
                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO: errorMsg = "오디오 에러"; break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: errorMsg = "퍼미션 없음"; break;
                    case SpeechRecognizer.ERROR_NO_MATCH: errorMsg = "음성을 인식하지 못했습니다. 다시 시도해주세요."; break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: errorMsg = "입력 시간 초과"; break;
                    default: errorMsg = "알 수 없는 오류 발생: " + error;
                }
                Log.e("STT", errorMsg);
                if (resultListener != null) {
                    resultListener.onSTTError(error);
                }
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0);
                    Log.d("STT", "인식 결과: " + text);
                    if (resultListener != null) {
                        resultListener.onSTTResult(text);
                    }
                } else {
                    Log.w("STT", "인식 결과 없음");
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

        sttIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);//dd
        sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN);
        sttIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activity.getPackageName());
        sttIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 30000); // 타임아웃 30초
    }

    public void startListening() {
        if (speechRecognizer != null && !isListening) {
            Log.d("STT", "음성 인식 시작");
            activity.runOnUiThread(() -> speechRecognizer.startListening(sttIntent));
        }
    }

    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            Log.d("STT", "음성 인식 중단");
            activity.runOnUiThread(() -> speechRecognizer.stopListening());
        }
    }

    // ▼▼▼ 이 부분이 핵심적인 수정사항입니다 ▼▼▼
    /**
     * 음성 인식을 다시 시작하는 메서드.
     * 기존 객체를 파괴하지 않고, 멈췄다가 다시 시작하여 안정성을 높입니다.
     */
    public void restartListening() {
        Log.d("STT", "음성 인식 재시작 요청");
        stopListening(); // 일단 정지
        startListening(); // 이어서 다시 시작
    }

    public void destroy() {
        if (speechRecognizer != null) {
            Log.d("STT", "STT 리소스 해제");
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    public void setOnSTTResultListener(OnSTTResultListener listener) {
        this.resultListener = listener;
    }

    public interface OnSTTResultListener {
        void onSTTResult(String result);
        void onSTTError(int errorCode);
    }
}
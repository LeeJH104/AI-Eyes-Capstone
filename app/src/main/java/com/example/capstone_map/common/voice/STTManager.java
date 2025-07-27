package com.example.capstone_map.common.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

public class STTManager {

    private final SpeechRecognizer speechRecognizer;
    private final Intent speechIntent;

    public interface OnResultCallback {
        void onResult(String result);
        void onError(String errorMessage);
    }

    public STTManager(Context context) {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);

        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN);
        speechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
    }

    public void listen(OnResultCallback callback) {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                Log.d("STT", "듣기 준비됨");
            }

            @Override public void onBeginningOfSpeech() {
                Log.d("STT", "말 시작됨");
            }

            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {
                Log.d("STT", "말 끝남");
            }

            @Override public void onError(int error) {
                String message = "알 수 없는 오류";

                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO: message = "오디오 오류"; break;
                    case SpeechRecognizer.ERROR_CLIENT: message = "클라이언트 오류"; break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: message = "권한 없음"; break;
                    case SpeechRecognizer.ERROR_NETWORK: message = "네트워크 오류"; break;
                    case SpeechRecognizer.ERROR_NO_MATCH: message = "말 인식 실패"; break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: message = "시간 초과"; break;
                }

                Log.e("STT", "에러: " + message);
                callback.onError(message);
            }

            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    callback.onResult(matches.get(0));
                } else {
                    callback.onError("결과 없음");
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(speechIntent);
    }

    public void destroy() {
        speechRecognizer.destroy();
    }
}

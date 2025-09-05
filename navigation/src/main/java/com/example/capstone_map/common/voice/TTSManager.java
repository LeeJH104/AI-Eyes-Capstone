package com.example.capstone_map.common.voice;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

public class TTSManager {
    private TextToSpeech tts;
    private boolean isInitialized = false;

    public interface OnSpeakCallback {
        void onStart();
        void onDone();
        default void onError() {}
    }

    public TTSManager(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTSManager", "TTS: 한국어 미지원 또는 데이터 없음");
                } else {
                    isInitialized = true;
                }
            } else {
                Log.e("TTSManager", "TTS 초기화 실패");
            }
        });
    }

    public void speak(String text, OnSpeakCallback callback) {
        if (!isInitialized) {
            Log.e("TTSManager", "TTS가 아직 초기화되지 않음");
            return;
        }

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                new Handler(Looper.getMainLooper()).post(callback::onStart);
            }

            @Override
            public void onDone(String utteranceId) {
                new Handler(Looper.getMainLooper()).post(callback::onDone);
            }

            @Override
            public void onError(String utteranceId) {
                Log.e("TTS", "TTS 오류 발생");
                new Handler(Looper.getMainLooper()).post(callback::onError);
            }
        });

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID");
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}

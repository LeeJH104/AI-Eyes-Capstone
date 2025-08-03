
package com.example.aieyes.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;

public class TTSManager {
    private TextToSpeech tts;
    private Runnable onReadyListener;

    public TTSManager(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN);
                if (onReadyListener != null) onReadyListener.run();
            }
        });
    }

    public void setOnTTSReadyListener(Runnable listener) {
        this.onReadyListener = listener;
    }

    public void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_id");
    }

    public void speak(String text, Runnable onDone) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_id");
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}
            @Override
            public void onDone(String utteranceId) { onDone.run(); }
            @Override
            public void onError(String utteranceId) {}
        });
    }

    public void shutdown() {
        tts.shutdown();
    }
}
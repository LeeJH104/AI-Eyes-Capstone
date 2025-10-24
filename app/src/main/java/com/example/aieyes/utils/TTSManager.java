package com.example.aieyes.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;
import java.util.UUID;

/**
 * TTSManager는 TextToSpeech 기능을 담당하는 클래스입니다.
 * 텍스트를 음성으로 읽어주는 기능 제공
 *  - 초기화 완료 여부 확인
 *  - 음성 안내 종료 후 후속 동작(Runnable) 실행 가능
 */
public class TTSManager {
    private TextToSpeech tts;   // 안드로이드의 TTS 객체
    private boolean isInitialized = false;  // 초기화 여부 체크용 변수
    private OnTTSReadyListener readyListener;  // 초기화 완료 시 실행될 콜백 인터페이스

    /**
     * 초기화 완료 리스너 정의
     * 외부에서 TTS 초기화가 완료되었는지 확인할 때 사용
     */
    public interface OnTTSReadyListener {
        void onReady(); // 초기화 완료 시 호출될 메서드
    }

    /**
     * 생성자
     * - TTS 객체를 생성하고 초기화함
     * - 초기화가 완료되면 한국어(Locale.KOREAN)로 언어 설정을 시도함
     *
     * @param context: 현재 Activity나 Application의 Context
     */
    public TTSManager(Context context) {
        // TTS 객체 생성 및 초기화
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // 언어를 한국어로 설정
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTSManager", "TTS: 한국어가 지원되지 않음");
                } else {
                    isInitialized = true;
                    if (readyListener != null) {
                        readyListener.onReady(); // 초기화 완료 후 콜백 실행
                    }
                }
            } else {
                Log.e("TTSManager", "TTS 초기화 실패");
            }
        });
    }

    /**
     * 초기화 완료 콜백 등록 함수
     * 이미 초기화되어 있으면 바로 실행
     * @param listener 초기화 완료 시 호출될 리스너
     */
    public void setOnTTSReadyListener(OnTTSReadyListener listener) {
        this.readyListener = listener;
        if (isInitialized && listener != null) {
            listener.onReady(); // 이미 초기화된 상태면 즉시 실행
        }
    }

    /**
     * 텍스트를 음성으로 읽어주는 기본 함수
     * 읽기만 하고 후속 동작은 없음
     *
     * @param text: 읽을 텍스트 (한글 지원)
     */
    public void speak(String text) {
        if (isInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
        } else {
            Log.e("TTSManager", "TTS가 초기화되지 않았습니다.");
        }
    }

    /**
     * 텍스트를 읽은 후 특정 작업(Runnable)을 수행하는 함수
     * - 음성이 끝난 후에 자동으로 콜백이 실행됨
     *
     * @param text   읽을 텍스트
     * @param onDone 음성 출력이 완료되었을 때 실행할 코드 (예: STT 시작, 화면 전환 등)
     */
    public void speak(String text, Runnable onDone) {
        if (!isInitialized) {
            Log.e("TTSManager", "TTS가 초기화되지 않았습니다.");
            return;
        }

        // 고유한 발화 ID 생성
        String utteranceId = UUID.randomUUID().toString();

        // 음성 출력 완료 리스너 등록
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                // 음성 시작 시 호출 (필요하면 사용 가능)
            }

            @Override
            public void onDone(String utteranceId) {
                // 음성 출력이 끝난 후 UI 스레드에서 Runnable 실행
                if (onDone != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(onDone);
                }
            }

            @Override
            public void onError(String utteranceId) {
                Log.e("TTSManager", "TTS 음성 출력 오류");
            }
        });

        // 텍스트 읽기 실행
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    /**
     * 현재 진행 중인 TTS 음성 출력을 즉시 중지합니다.
     */
    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    /**
     * shutdown 함수
     * TTS 사용 종료 후 리소스 해제 메서드
     * Activity 종료 시 호출 권장
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();       // 현재 읽고 있는 음성 중지
            tts.shutdown();   // TTS 엔진 종료
        }
    }
}

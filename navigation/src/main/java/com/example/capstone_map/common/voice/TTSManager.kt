package com.example.capstone_map.common.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

class TTSManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    // 초기화 전 호출된 speak 요청을 저장
    private val pendingQueue = ConcurrentLinkedQueue<PendingSpeak>()

    data class PendingSpeak(
        val text: String,
        val callback: OnSpeakCallback?
    )

    interface OnSpeakCallback {
        fun onStart()
        fun onDone()
        fun onError() {}
    }

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.KOREAN)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTSManager", "TTS: 한국어 미지원 또는 데이터 없음")
                } else {
                    isInitialized = true
                    Log.d("TTSManager", "TTS 초기화 성공")

                    // 대기 중이던 요청들 실행
                    processPendingQueue()
                }
            } else {
                Log.e("TTSManager", "TTS 초기화 실패")
                // 실패한 경우 큐 비우기
                pendingQueue.clear()
            }
        }
    }

    fun speak(text: String, callback: OnSpeakCallback? = null) {
        if (!isInitialized) {
            Log.w("TTSManager", "TTS 아직 초기화 안됨. 큐에 추가: $text")
            pendingQueue.offer(PendingSpeak(text, callback))
            return
        }

        executeSpeak(text, callback)
    }

    private fun executeSpeak(text: String, callback: OnSpeakCallback?) {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Handler(Looper.getMainLooper()).post {
                    callback?.onStart()
                }
            }

            override fun onDone(utteranceId: String?) {
                Handler(Looper.getMainLooper()).post {
                    callback?.onDone()
                }
            }

            override fun onError(utteranceId: String?) {
                Log.e("TTS", "TTS 오류 발생")
                Handler(Looper.getMainLooper()).post {
                    callback?.onError()
                }
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
    }

    private fun processPendingQueue() {
        Log.d("TTSManager", "대기 중인 TTS 요청 ${pendingQueue.size}개 처리 시작")

        while (pendingQueue.isNotEmpty()) {
            val pending = pendingQueue.poll()
            if (pending != null) {
                executeSpeak(pending.text, pending.callback)
            }
        }
    }


    fun stop() {
        tts?.stop()  //  음성만 중단, 엔진은 살아있음
    }
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        pendingQueue.clear()
    }
}
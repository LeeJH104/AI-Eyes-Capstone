package com.example.aieyes.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

/*
AndroidManifest.xml 권한 설정
Android 12(API 31) 이상에서는 아래 권한이 필요하지 않습니다
그러나 호환성 유지를 위해 다음 권한을 추가하는 것이 안전합니다:

xml
<uses-permission android:name="android.permission.VIBRATE" />
*/

/**
 * VibrationHelper
 * - 진동 피드백을 제공하는 공통 유틸 클래스
 * - 사용자 입력 성공, 오류, 알림 등 상황에 따라 진동을 다르게 줄 수 있음
 */
public class VibrationHelper {

    /**
     * 짧은 진동 (입력 성공, 확인 응답 등)
     * @param context 현재 액티비티 또는 애플리케이션의 Context
     */
    public static void vibrateShort(Context context) {
        vibrate(context, 100);  // 100ms = 0.1초
    }

    /**
     * 긴 진동 (입력 오류, 경고 등)
     * @param context 현재 액티비티 또는 애플리케이션의 Context
     */
    public static void vibrateLong(Context context) {
        vibrate(context, 400);  // 400ms = 0.4초
    }

    /**
     * 진동 발생 공통 메서드
     * Android 버전에 따라 적절한 방식으로 진동을 울림
     * @param context 현재 Context
     * @param durationMillis 진동 지속 시간 (밀리초)
     */
    private static void vibrate(Context context, int durationMillis) {
        try {
            // 시스템 서비스에서 Vibrator 객체를 가져옴
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

            if (vibrator == null) {
                Log.w("VibrationHelper", "⚠️ 기기에서 진동 기능을 지원하지 않음");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0 이상은 VibrationEffect를 사용해야 함
                VibrationEffect effect = VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(effect);
            } else {
                // 하위 버전은 deprecated된 방식 사용
                vibrator.vibrate(durationMillis);
            }

        } catch (Exception e) {
            Log.e("VibrationHelper", "진동 오류: " + e.getMessage());
        }
    }
}

/*
사용 예시 (어디서든 호출 가능)

// 성공한 입력에 짧은 진동
VibrationHelper.vibrateShort(getApplicationContext());

// 오류 상황에 긴 진동
VibrationHelper.vibrateLong(this);  // Activity라면 this
*/

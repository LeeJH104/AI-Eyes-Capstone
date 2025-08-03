package com.example.aieyes.utils;

import android.content.Context;
import android.os.Vibrator;

public class VibrationHelper {
    public static void vibrateShort(Context context) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(100);
    }
    public static void vibrateLong(Context context) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(500);
    }
}
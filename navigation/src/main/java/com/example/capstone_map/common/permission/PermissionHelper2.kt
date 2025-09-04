package com.example.capstone_map.common.permission

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

fun registerPermissionLauncher(
    activity: ComponentActivity,
    callback: (Boolean) -> Unit
): ActivityResultLauncher<String> {
    return activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        callback
    )
}

// 1. 런처 등록 함수
fun ComponentActivity.registerMicAndGpsPermissionLauncher(
    callback: (micGranted: Boolean, gpsGranted: Boolean) -> Unit
): ActivityResultLauncher<Array<String>> {
    return registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        val gpsGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        callback(micGranted, gpsGranted)
    }
}

// 2. 요청 값 배열 미리 준비
val micAndGpsPermissions = arrayOf(
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.ACCESS_FINE_LOCATION
)
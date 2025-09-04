package com.example.capstone_map.common.location




import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class LocationPermissionManager(
    private val activity: Activity,
    private val permissionLauncher: ActivityResultLauncher<String>
) {

    interface PermissionCallback {
        fun onGranted()
        fun onDenied()
    }

    fun checkAndRequestPermission(callback: PermissionCallback) {
        if (hasLocationPermission()) {
            callback.onGranted()
        } else {
            launchPermissionRequest(callback)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun launchPermissionRequest(callback: PermissionCallback) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        // Activity에서 이 launcher 결과 처리 필요 (아래에 따로 설명)
        // 권한 승인 후 onGranted() 호출되도록 연결해야 함
        // ➤ 아래 예제 참조
    }
}


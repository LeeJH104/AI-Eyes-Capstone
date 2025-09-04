package com.example.capstone_map.common.permission

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.activity.ComponentActivity // ✅ 이것만 사용
import androidx.core.content.ContextCompat

// PermissionHelper.java


fun interface PermissionResultCallback {
    fun onResult(granted: Boolean)
}
object PermissionHelper {
    private const val REQUEST_MIC_PERMISSION = 1000

    fun requestMicrophonePermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_MIC_PERMISSION
            )
        }
    }


    fun registerLocationPermissionLauncher(
        activity: ComponentActivity,
        callback: PermissionResultCallback
    ): ActivityResultLauncher<String> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            callback.onResult(isGranted)
        }
    }



    //아래 코드는 사용자가 마이크 권한 요청에 대해 허용했는지, 거부했는지를 판단해서 그에 맞는 처리를 하는 로직입니다.
    //권한 요청 허용 안하면 설정창으로 이동
    fun handlePermissionResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_MIC_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(activity, "마이크 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 거부 시 바로 설정으로 이동 유도
                AlertDialog.Builder(activity)
                    .setTitle("권한이 필요합니다")
                    .setMessage("마이크 권한이 꺼져 있어요. 설정에서 직접 켜주세요.")
                    .setPositiveButton("설정으로 이동") { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", activity.packageName, null)
                        intent.data = uri
                        activity.startActivity(intent)
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        }
    }

}


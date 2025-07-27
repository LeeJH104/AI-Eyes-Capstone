package com.example.capstone_map.common.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

public class PermissionChecker {

     /**
      "이 앱이 내 위치를 써도 돼요?" 라고 사용자에게 물어보도록 하고, 허락을 받았는 코드
      권한이 있으면 True 없 : false
      */
    public static boolean hasLocationPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }
    /**
     * 권한이 없는 경우 사용자에게 요청해야 함을 나타냅니다.
     */
    public static boolean shouldRequestLocationPermission(Context context) {
        return !hasLocationPermission(context);
    }
}

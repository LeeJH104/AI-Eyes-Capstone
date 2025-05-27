package com.example.aieyes.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * PermissionHelper
 * 실행 중 권한 요청을 쉽게 처리하기 위한 유틸 클래스
 */
public class PermissionHelper {

    // 요청할 권한 목록 (필요시 수정 가능)
    public static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,         // STT (마이크)
            Manifest.permission.CAMERA,               // 영수증/장애물 촬영
            Manifest.permission.ACCESS_FINE_LOCATION  // 길안내용 GPS
    };

    // 권한 요청을 구분하기 위한 고유 코드
    public static final int REQUEST_CODE = 100;

    /**
     * 모든 권한이 허용되었는지 확인하는 메서드
     * @param activity 현재 Activity
     * @return true: 모든 권한 허용됨, false: 하나라도 거부됨
     */
    public static boolean hasAllPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // 안드로이드 6.0 이하 버전은 자동 허용
            return true;
        }

        // 권한 배열에서 하나라도 거부된 것이 있는지 확인
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false; // 하나라도 허용 안 된 경우 false 반환
            }
        }

        return true; // 전부 허용된 경우
    }

    /**
     * 아직 허용되지 않은 권한들을 사용자에게 요청하는 메서드
     * @param activity 현재 Activity
     */
    public static void requestPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, REQUEST_CODE);
    }

    /**
     * 권한 요청 결과 처리 함수 (Activity의 onRequestPermissionsResult에서 호출 필요)
     * @param requestCode 요청 코드 (100)
     * @param grantResults 권한 결과 배열
     * @param activity 현재 Activity (Toast 출력용)
     * @return true: 모두 허용됨, false: 하나라도 거부됨
     */
    public static boolean handlePermissionResult(int requestCode, @NonNull int[] grantResults, Activity activity) {
        if (requestCode == REQUEST_CODE) {
            boolean allGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(activity, "모든 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "일부 권한이 거부되어 앱 기능이 제한될 수 있습니다.", Toast.LENGTH_LONG).show();
            }

            return allGranted;
        }

        return false;
    }
}

/*
사용 예시 (MainActivity 또는 기능 시작 전에)

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 앱 실행 시 권한 확인
        if (!PermissionHelper.hasAllPermissions(this)) {
            // 아직 허용되지 않은 권한이 있다면 요청
            PermissionHelper.requestPermissions(this);
        }
    }

    */
/**
     * 사용자가 권한 요청 결과에 응답하면 자동 호출됨
     *//*

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // PermissionHelper에게 결과 처리를 위임
        boolean granted = PermissionHelper.handlePermissionResult(requestCode, grantResults, this);

        if (granted) {
            // 안전하게 기능 실행 가능
        } else {
            // 일부 권한 거부됨 → 제한 기능 안내
        }
    }
}
*/

package com.example.aieyes.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/*
⚠️ 기술적 제약 정리
STT로 "허용" 말하면 직접 권한 창의 버튼 누르기 / 불가 / 보안 정책상 안드로이드 OS에서 막혀 있음
 → 권한은 처음 1번만 승인하면 되므로 처음에만 다른 사람의 도움을 받아 승인하기
*/

/**
 * PermissionHelper는 앱에서 필요한 권한을 요청하고 처리하는 유틸리티 클래스입니다.
 * Android 6.0 이상에서 동작하며, 권한이 거부된 경우 앱 설정으로 유도하는 기능도 포함됩니다.
 */
public class PermissionHelper {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    // 요청할 권한 목록 (필요에 따라 수정 가능)
    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.VIBRATE
    };

    private final Activity activity;
    private final OnPermissionResultListener listener;

    /**
     * 권한 결과 콜백 인터페이스
     */
    public interface OnPermissionResultListener {
        void onPermissionGranted();
    }

    public PermissionHelper(Activity activity, OnPermissionResultListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    /**
     * 권한이 모두 부여되었는지 확인하고, 아니라면 요청을 시작함
     */
    public void checkAndRequestPermissions() {
        boolean allGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            listener.onPermissionGranted();
        } else {
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * 권한 요청 결과를 처리하는 메서드 (Activity의 onRequestPermissionsResult에서 호출해야 함)
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE) return;

        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            Log.d("PermissionHelper", "✅ 모든 권한 승인됨 → onPermissionGranted() 호출됨");
            // 모든 권한이 승인됨
            listener.onPermissionGranted();
        } else {
            // 하나라도 거부된 경우 처리
            boolean shouldShowRationale = false;
            for (String permission : permissions) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    shouldShowRationale = true;
                    break;
                }
            }

            if (shouldShowRationale) {
                // 일부 거부 → 다시 요청 가능
                Toast.makeText(activity, "권한이 거부되었습니다. 권한을 승인되어야 앱을 사용할 수 있습니다.", Toast.LENGTH_SHORT).show();
                checkAndRequestPermissions();
            } else {
                // '다시 묻지 않음' 선택된 권한 있음 → 설정으로 유도
                new AlertDialog.Builder(activity)
                        .setTitle("권한 설정 필요")
                        .setMessage("앱을 사용하려면 설정에서 권한을 직접 허용해 주세요.")
                        .setCancelable(false)
                        .setPositiveButton("설정으로 이동", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                            intent.setData(uri);
                            activity.startActivity(intent);
                        })
                        .setNegativeButton("앱 종료", (dialog, which) -> activity.finish())
                        .show();
            }
        }
    }

    /**
     * 권한이 이미 다 허용됐는지 확인
     */
    public static boolean arePermissionsGranted(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}

/*
사용 예시 (MainActivity 또는 기능 시작 전에)

public class MainActivity extends AppCompatActivity {

    private PermissionHelper permissionHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 레이아웃 필요 시 설정 (예: setContentView(R.layout.activity_main);)

        // 권한 유틸리티 초기화 및 확인
        permissionHelper = new PermissionHelper(this, new PermissionHelper.OnPermissionResultListener() {
            @Override
            public void onPermissionGranted() {
                // 모든 권한이 허용되었을 때 실행할 코드
                Toast.makeText(MainActivity.this, "권한 승인됨!", Toast.LENGTH_SHORT).show();

                // 예: 앱 주요 기능 실행
            }
        });

        // 권한 확인 및 요청
        permissionHelper.checkAndRequestPermissions();
    }

    // 권한 요청 결과를 PermissionHelper에 전달
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
*/

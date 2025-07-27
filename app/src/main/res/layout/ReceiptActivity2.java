package com.example.aieyes.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.aieyes.R;
import com.example.aieyes.utils.TTSManager;
import com.example.aieyes.utils.VibrationHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReceiptActivity extends AppCompatActivity {
    private static final String TAG = "ReceiptActivity";
    private static final int CAMERA_REQUEST_CODE = 1001;
    private static final int CAMERA_PERMISSION_CODE = 1002;
    
    // Flask 서버 URL (실제 서버 IP로 변경 필요)
    private static final String SERVER_URL = "http://192.168.31.230:5000/api/process-receipt";
    
    private TTSManager ttsManager;
    private TextView resultTextView;
    private OkHttpClient httpClient;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receipt_activity);
        
        // UI 초기화
        initializeUI();
        
        // 유틸리티 클래스 초기화
        initializeManagers();
        
        // 네트워크 클라이언트 초기화
        httpClient = new OkHttpClient();
        
        // 영수증 촬영 시작
        startReceiptProcess();
    }
    
    private void initializeUI() {
        resultTextView = findViewById(R.id.resultTextView);
        if (resultTextView == null) {
            // 레이아웃에 TextView가 없을 경우 프로그래밍 방식으로 생성
            resultTextView = new TextView(this);
            resultTextView.setTextSize(24);
            resultTextView.setPadding(50, 50, 50, 50);
            setContentView(resultTextView);
        }
    }
    
    private void initializeManagers() {
        // TTS 매니저 초기화
        ttsManager = new TTSManager(this);
    }
    
    private void startReceiptProcess() {
        // TTS가 준비되면 안내 시작
        ttsManager.setOnTTSReadyListener(() -> {
            VibrationHelper.vibrateShort(this); // 짧은 진동으로 시작 알림
            
            // "영수증을 촬영해주세요" 안내 후 카메라 실행
            ttsManager.speak("영수증을 촬영해주세요", this::checkCameraPermissionAndLaunch);
        });
    }
    
    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            // 카메라 권한 요청
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            // 권한이 있으면 카메라 실행
            launchCamera();
        }
    }
    
    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            speakError("카메라를 사용할 수 없습니다. 다시 시도해주세요.");
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                speakError("카메라 권한이 필요합니다. 설정에서 권한을 허용해주세요.");
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getExtras() != null) {
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                if (imageBitmap != null) {
                    // 촬영된 이미지를 서버로 전송
                    ttsManager.speak("영수증을 분석중입니다. 잠시만 기다려주세요.");
                    sendImageToServer(imageBitmap);
                } else {
                    speakError("사진 촬영에 실패했습니다. 다시 촬영해주세요.");
                }
            }
        } else {
            speakError("사진 촬영이 취소되었습니다. 다시 촬영해주세요.");
        }
    }
    
    private void sendImageToServer(Bitmap bitmap) {
        try {
            // 비트맵을 Base64로 인코딩
            String base64Image = bitmapToBase64(bitmap);
            
            // JSON 요청 생성
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("image", "data:image/jpeg;base64," + base64Image);
            
            // HTTP 요청 생성
            RequestBody requestBody = RequestBody.create(
                jsonRequest.toString(),
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build();
            
            // 비동기 요청 전송
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "서버 요청 실패: " + e.getMessage());
                    runOnUiThread(() -> speakError("서버 연결에 실패했습니다. 다시 촬영해주세요."));
                }
                
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try {
                        String responseBody = response.body().string();
                        
                        if (response.isSuccessful()) {
                            // 성공적인 응답 처리
                            handleServerResponse(responseBody);
                        } else {
                            Log.e(TAG, "서버 오류: " + response.code() + " - " + responseBody);
                            runOnUiThread(() -> speakError("합계를 인식하지 못했습니다. 다시 촬영해주세요."));
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "응답 읽기 실패: " + e.getMessage());
                        runOnUiThread(() -> speakError("서버 응답을 처리하지 못했습니다. 다시 촬영해주세요."));
                    }
                }
            });
            
        } catch (JSONException e) {
            Log.e(TAG, "JSON 생성 실패: " + e.getMessage());
            speakError("요청 생성에 실패했습니다. 다시 촬영해주세요.");
        }
    }
    
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
    
    private void handleServerResponse(String responseBody) {
        try {
            JSONObject responseJson = new JSONObject(responseBody);
            
            if (responseJson.has("data")) {
                JSONObject data = responseJson.getJSONObject("data");
                String totalPrice = data.optString("total_price", "");
                
                if (!totalPrice.isEmpty() && !totalPrice.equals("0")) {
                    // 성공: 합계 추출 완료
                    runOnUiThread(() -> {
                        String resultMessage = "합계는 " + formatPrice(totalPrice) + "원입니다.";
                        
                        // 화면에 결과 표시
                        displayResult(resultMessage);
                        
                        // TTS로 결과 안내
                        ttsManager.speak(resultMessage);
                        
                        // 성공 진동
                        VibrationHelper.vibrateShort(this);
                    });
                } else {
                    // 합계를 찾지 못함
                    runOnUiThread(() -> speakError("합계를 인식하지 못했습니다. 다시 촬영해주세요."));
                }
            } else if (responseJson.has("error")) {
                // 서버에서 오류 반환
                String errorMessage = responseJson.getString("error");
                Log.e(TAG, "서버 오류: " + errorMessage);
                runOnUiThread(() -> speakError("합계를 인식하지 못했습니다. 다시 촬영해주세요."));
            } else {
                // 예상치 못한 응답 형식
                Log.e(TAG, "예상치 못한 응답: " + responseBody);
                runOnUiThread(() -> speakError("예상치 못한 응답입니다. 다시 촬영해주세요."));
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "JSON 파싱 실패: " + e.getMessage());
            runOnUiThread(() -> speakError("응답 분석에 실패했습니다. 다시 촬영해주세요."));
        }
    }
    
    private String formatPrice(String price) {
        try {
            // 숫자가 아닌 문자 제거 (콤마, 공백 등)
            String cleanPrice = price.replaceAll("[^0-9]", "");
            if (!cleanPrice.isEmpty()) {
                long amount = Long.parseLong(cleanPrice);
                // 천단위 콤마 추가
                return String.format("%,d", amount);
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "가격 포맷팅 실패: " + price);
        }
        return price; // 포맷팅 실패 시 원본 반환
    }
    
    private void displayResult(String message) {
        resultTextView.setText(message);
        resultTextView.setVisibility(TextView.VISIBLE);
    }
    
    private void speakError(String errorMessage) {
        ttsManager.speak(errorMessage);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        
        // 오류 진동 (긴 진동)
        VibrationHelper.vibrateLong(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 리소스 정리
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
    }
}

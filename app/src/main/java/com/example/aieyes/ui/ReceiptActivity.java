package com.example.aieyes.ui;

<<<<<<< HEAD
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
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
import java.util.concurrent.TimeUnit; // TimeUnit 임포트 추가

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReceiptActivity extends AppCompatActivity {

    private static final String TAG = "ReceiptActivity";
    private static final int CAMERA_REQUEST_CODE = 1001;
    private static final int CAMERA_PERMISSION_CODE = 1002;
    // 중요: 서버 PC의 IP 주소를 정확히 입력해야 합니다.
    private static final String SERVER_URL = "http://192.168.31.230:5000/api/process-receipt";

    private TTSManager ttsManager;
    private TextView resultTextView;
    // OkHttpClient를 멤버 변수로 선언
    private OkHttpClient httpClient;

=======
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aieyes.R;

public class ReceiptActivity extends AppCompatActivity {
>>>>>>> origin/main
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receipt_activity);
<<<<<<< HEAD

        initializeUI();
        initializeManagers();
        initializeHttpClient();

        // "영수증 촬영" 버튼 리스너
        findViewById(R.id.btnCamera).setOnClickListener(v -> {
            // TTS로 안내 후 카메라 권한 확인 및 실행
            ttsManager.speak("영수증을 화면에 맞춰 촬영해주세요.", this::checkCameraPermissionAndLaunch);
        });

        // "테스트 분석" 버튼 리스너
        findViewById(R.id.btnTest).setOnClickListener(v -> runTestMode());
    }

    private void initializeHttpClient() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // 연결 타임아웃
                .readTimeout(120, TimeUnit.SECONDS)    // 응답 읽기 타임아웃
                .writeTimeout(120, TimeUnit.SECONDS)   // 요청 쓰기 타임아웃
                .build();
    }

    private void initializeUI() {
        resultTextView = findViewById(R.id.resultTextView);
    }

    private void initializeManagers() {
        ttsManager = new TTSManager(this);
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            speakError("카메라를 실행할 수 없습니다. 다시 시도해 주십시오.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                speakError("카메라 권한이 없어 촬영을 진행할 수 없습니다.");
            }
        }
    }

    /**
     * 카메라 촬영 결과를 처리하는 핵심 메소드
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            // 사용자가 사진을 찍고 '확인'을 눌렀을 때
            if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                if (imageBitmap != null) {
                    ttsManager.speak("촬영된 영수증을 분석합니다.", null);
                    displayResult("분석 중...");
                    sendImageToServer(imageBitmap);
                } else {
                    // 사진 데이터가 없는 희귀한 경우
                    speakError("사진을 가져오지 못했습니다. 다시 촬영해 주세요.");
                }
            } else {
                // 사용자가 카메라 앱에서 '취소' 또는 '뒤로가기'를 눌렀을 때
                ttsManager.speak("촬영이 취소되었습니다. 다시 촬영하려면 영수증 촬영 버튼을 누르세요.", null);
            }
        }
    }

    private void runTestMode() {
        try {
            Bitmap testBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.receipt_test);
            if (testBitmap != null) {
                ttsManager.speak("테스트 영수증을 분석합니다.", null);
                displayResult("테스트 분석 중...");
                sendImageToServer(testBitmap);
            } else {
                speakError("테스트 이미지를 불러올 수 없습니다.");
            }
        } catch (Exception e) {
            speakError("테스트 이미지를 불러오는 중 오류가 발생했습니다.");
            Log.e(TAG, "Error loading test image", e);
        }
    }

    private void sendImageToServer(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "receipt.jpg",
                        RequestBody.create(byteArray, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Server request failed", e);
                runOnUiThread(() -> speakError("서버 연결에 실패했습니다."));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Server error: " + response.code() + " - " + response.body().string());
                    runOnUiThread(() -> speakError("서버에서 오류가 발생했습니다."));
                    return;
                }
                final String responseBody = response.body().string();
                handleServerResponse(responseBody);
            }
        });
    }

    private void handleServerResponse(String responseBody) {
        try {
            JSONObject responseJson = new JSONObject(responseBody);
            if (responseJson.optBoolean("success")) {
                JSONObject data = responseJson.getJSONObject("data");
                String totalPrice = data.optString("total_price", "");

                if (!totalPrice.isEmpty() && !totalPrice.equals("0")) {
                    String resultMessage = "총 금액은 " + formatPrice(totalPrice) + "원입니다. 다시 분석하려면 영수증 촬영 버튼을 누르세요.";
                    runOnUiThread(() -> {
                        displayResult("총 금액: " + formatPrice(totalPrice) + "원");
                        ttsManager.speak(resultMessage, null);
                        VibrationHelper.vibrateShort(ReceiptActivity.this);
                    });
                } else {
                    runOnUiThread(() -> speakError("영수증에서 총액을 찾을 수 없습니다. 다시 촬영해 주십시오."));
                }
            } else {
                String errorMessage = responseJson.optString("error", "알 수 없는 오류");
                runOnUiThread(() -> speakError("분석에 실패했습니다. 다시 시도해주세요."));
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing failed", e);
            runOnUiThread(() -> speakError("서버 응답을 해석할 수 없습니다."));
        }
    }

    private String formatPrice(String price) {
        try {
            String cleanPrice = price.replaceAll("[^0-9]", "");
            if (!cleanPrice.isEmpty()) {
                long amount = Long.parseLong(cleanPrice);
                return String.format("%,d", amount);
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Price formatting failed for: " + price);
        }
        return price;
    }

    private void displayResult(String message) {
        if (resultTextView != null) {
            runOnUiThread(() -> {
                resultTextView.setText(message);
                resultTextView.setVisibility(View.VISIBLE);
            });
        }
    }

    private void speakError(String errorMessage) {
        // TTS 콜백을 null로 지정하여 무한 반복 및 자동 재촬영 방지
        ttsManager.speak(errorMessage, null);
        VibrationHelper.vibrateLong(this);
        
        runOnUiThread(() -> {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
    }
}
=======
    }
}
>>>>>>> origin/main

package com.example.aieyes.feature;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.aieyes.R;
import com.example.aieyes.utils.STTManager;
import com.example.aieyes.utils.TTSManager;
import com.example.aieyes.utils.VibrationHelper;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    // private static final String SERVER_URL = "http://192.168.0.215:5000/api/receipt/process-receipt";
    // ▼▼▼ ngrok 주소는 실행할 때마다 여기를 바꿔주세요 ▼▼▼
    private static final String SERVER_URL = "https://62b2d7a86c54.ngrok-free.app/api/receipt/process-receipt";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 102;

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private TTSManager ttsManager;
    private GestureDetector gestureDetector;
    private OkHttpClient httpClient;
    private Handler delayHandler;
    private Runnable delayRunnable;
    private ExecutorService cameraExecutor;
    private TextView resultTextView;
    private TextView instructionTextSmall;
    private STTManager sttManager;
    private boolean isCapturing = false;

    // ▼▼▼ STT 재시작을 위한 콜백 (핵심 수정) ▼▼▼
    private final Runnable sttRestartCallback = () -> {
        isCapturing = false;
        runOnUiThread(() -> {
            if (sttManager != null && isAudioPermissionGranted()) {
                Log.d(TAG, "모든 TTS가 종료되어 STT를 재시작합니다.");
                sttManager.restartListening();
            }
        });
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receipt_activity);

        previewView = findViewById(R.id.previewView);
        ttsManager = new TTSManager(this);
        cameraExecutor = Executors.newSingleThreadExecutor();
        resultTextView = findViewById(R.id.resultTextView);
        instructionTextSmall = findViewById(R.id.instructionTextSmall);
        
        initializeHttpClient();
        delayHandler = new Handler(Looper.getMainLooper());
        setupGestureDetector(previewView);
        
        resultTextView.setText("촬영을 기다리는 중");
        instructionTextSmall.setText("");

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        boolean cameraGranted = isCameraPermissionGranted();
        boolean audioGranted = isAudioPermissionGranted();

        if (cameraGranted && audioGranted) {
            initializeSttManager();
            startCamera();
        } else if (!cameraGranted) {
            requestCameraPermission();
        } else {
            requestAudioPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAndRequestPermissions();
                } else {
                    ttsManager.speak("카메라 권한이 없어 영수증 촬영을 진행할 수 없습니다.", this::finish);
                }
                break;
            case AUDIO_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAndRequestPermissions();
                } else {
                    ttsManager.speak("음성 권한이 없어 음성 명령을 사용할 수 없습니다. 더블탭으로만 촬영 가능합니다.", this::startCamera);
                }
                break;
        }
    }

    private void triggerCapture(String triggerSource) {
        if (isCapturing) return;
        isCapturing = true;
        Log.d(TAG, triggerSource + " 촬영이 시작되었습니다.");
        if (sttManager != null) {
            sttManager.stopListening();
        }
        runOnUiThread(() -> {
            resultTextView.setText("촬영 중...");
            instructionTextSmall.setText("");
        });
        ttsManager.speak("3초 후 영수증을 촬영합니다.", () -> {
            new Handler(Looper.getMainLooper()).postDelayed(this::captureImage, 3000);
        });
    }

    private void captureImage() {
        if (imageCapture == null) {
            speakError("카메라 준비 중 오류가 발생했습니다.", sttRestartCallback); // 오류 발생 시에도 STT 재시작
            return;
        }
        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @ExperimentalGetImage
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                runOnUiThread(() -> {
                    ttsManager.speak("촬영이 완료 되었습니다.", null); // ★수정: 여기서는 STT를 켜지 않음
                    resultTextView.setText("분석을 기다리는 중...");
                    instructionTextSmall.setText("");
                });
                Bitmap bitmap = imageProxyToBitmap(imageProxy);
                imageProxy.close();
                if (bitmap != null) {
                    sendImageToServer(bitmap);
                } else {
                    speakError("이미지 변환에 실패했습니다. 다시 시도해주세요.", sttRestartCallback);
                }
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "이미지 캡처 실패", exception);
                speakError("촬영에 실패했습니다. 다시 시도해주세요.", sttRestartCallback);
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
                    String resultMessage = "총 금액은 " + totalPrice + "원입니다. 다시 촬영하려면 화면을 두 번 터치하거나 촬영이라고 말씀해주세요.";
                    runOnUiThread(() -> {
                        // ▼▼▼ 수정: TTS가 끝난 후 STT 재시작 콜백을 전달 ▼▼▼
                        ttsManager.speak(resultMessage, sttRestartCallback);
                        VibrationHelper.vibrateShort(ReceiptActivity.this);
                        resultTextView.setText("합계: " + totalPrice + "원");
                        instructionTextSmall.setText("촬영을 기다리는 중");
                    });
                } else {
                    // ▼▼▼ 수정: 오류 TTS 후 STT 재시작 콜백을 전달 ▼▼▼
                    speakError("영수증에서 총액을 찾을 수 없습니다. 다시 촬영해 주십시오.", sttRestartCallback);
                }
            } else {
                String errorMessage = responseJson.optString("error", "");
                if (errorMessage.contains("OCR failed")) {
                    speakError("영수증에서 글자를 찾지 못했습니다. 다시 촬영해주세요.", sttRestartCallback);
                } else {
                    speakError("서버에서 분석 중 오류가 발생했습니다. 다시 촬영해주세요.", sttRestartCallback);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON 파싱 실패", e);
            speakError("서버 응답을 처리하는 중 오류가 발생했습니다.", sttRestartCallback);
        } finally {
            // ▼▼▼ 수정: finally에서 STT 재시작 로직 완전 삭제 ▼▼▼
            // (TTS 콜백이 STT 재시작을 담당함)
        }
    }
    
    // ▼▼▼ 수정: speakError가 STT 재시작 콜백(onDone)을 받도록 변경 ▼▼▼
    private void speakError(String errorMessage, Runnable onDone) {
        isCapturing = false;
        runOnUiThread(() -> {
            if (ttsManager != null) {
                // ▼▼▼ 수정: TTS가 끝난 후 onDone 콜백(STT 재시작) 실행 ▼▼▼
                ttsManager.speak(errorMessage, onDone);
            }
            VibrationHelper.vibrateLong(this);
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            resultTextView.setText("오류 발생");
            instructionTextSmall.setText("촬영을 기다리는 중");
        });
    }
    
    // ▼▼▼ 기존 speakError(String)은 삭제됨 ▼▼▼
    
    @Override
    protected void onResume() {
        super.onResume();
        isCapturing = false;
        // ▼▼▼ 수정: onResume에서는 STT를 바로 켜지 않고, startCamera의 콜백에서 켜도록 함 ▼▼▼
        // (앱 복귀 시 자동 촬영 방지)
    }

    private void initializeSttManager() {
        if (!isAudioPermissionGranted()) return;
        sttManager = new STTManager(this);
        sttManager.setOnSTTResultListener(new STTManager.OnSTTResultListener() {
            @Override
            public void onSTTResult(String result) {
                String voice = result.trim().toLowerCase();
                Log.d(TAG, "STT 인식 결과: " + voice);
                if (!isCapturing && (voice.contains("촬영") || voice.contains("촬영하기") || voice.contains("찍어") || voice.contains("찍어줘") || voice.contains("사진"))) {
                    triggerCapture("음성 명령으로");
                } else if (!isCapturing) {
                    sttManager.restartListening();
                }
            }
            @Override
            public void onSTTError(int errorCode) {
                Log.d(TAG, "STT Error or Timeout. Code: " + errorCode);
                if (!isCapturing && sttManager != null) {
                    sttManager.restartListening();
                }
            }
        });
    }
    
    private boolean isCameraPermissionGranted() { return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED; }
    private boolean isAudioPermissionGranted() { return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED; }
    private void requestCameraPermission() { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE); }
    private void requestAudioPermission() { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_REQUEST_CODE); }
    private void setupGestureDetector(android.view.View targetView) { gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() { @Override public boolean onDoubleTap(MotionEvent e) { if (!isCapturing) { triggerCapture("더블탭으로"); } return true; } }); targetView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event)); }
    
    private void startCamera() { 
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this); 
        cameraProviderFuture.addListener(() -> { 
            try { 
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get(); 
                Preview preview = new Preview.Builder().build(); 
                imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build(); 
                preview.setSurfaceProvider(previewView.getSurfaceProvider()); 
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA; 
                cameraProvider.unbindAll(); 
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture); 

                // ▼▼▼ 수정: TTS 잔음을 STT가 인식하지 못하도록 0.5초 딜레이 추가 ▼▼▼
                ttsManager.speak("화면을 두 번 터치하거나 촬영하기라고 말씀해주세요.", () -> {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (sttManager != null) {
                            sttManager.startListening();
                        }
                    }, 500); // 0.5초 딜레이
                });
            } catch (ExecutionException | InterruptedException e) { 
                Log.e(TAG, "CameraX 바인딩 실패", e); 
                ttsManager.speak("카메라를 시작할 수 없습니다. 앱을 다시 실행해주세요.", this::finish); 
            } 
        }, ContextCompat.getMainExecutor(this)); 
    }
    
    private void initializeHttpClient() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(180, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .build();
    }
    
    @Override protected void onPause() { super.onPause(); if (sttManager != null) { sttManager.stopListening(); } }
    @Override protected void onDestroy() { super.onDestroy(); if (ttsManager != null) { ttsManager.shutdown(); } if (cameraExecutor != null) { cameraExecutor.shutdown(); } if (sttManager != null) { sttManager.destroy(); } }
    
    @ExperimentalGetImage private Bitmap imageProxyToBitmap(ImageProxy imageProxy) { if (imageProxy == null || imageProxy.getImage() == null) return null; if (imageProxy.getFormat() == ImageFormat.JPEG) { ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer(); byte[] bytes = new byte[buffer.remaining()]; buffer.get(bytes); return BitmapFactory.decodeByteArray(bytes, 0, bytes.length); } else if (imageProxy.getFormat() == ImageFormat.YUV_420_888) { Image image = imageProxy.getImage(); ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); int ySize = yBuffer.remaining(); int uSize = uBuffer.remaining(); int vSize = vBuffer.remaining(); byte[] nv21 = new byte[ySize + uSize + vSize]; yBuffer.get(nv21, 0, ySize); vBuffer.get(nv21, ySize, vSize); uBuffer.get(nv21, ySize + vSize, uSize); YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null); ByteArrayOutputStream out = new ByteArrayOutputStream(); yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out); byte[] jpegBytes = out.toByteArray(); return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length); } else { Log.e(TAG, "지원하지 않는 이미지 포맷: " + imageProxy.getFormat()); return null; } }
    
    private void sendImageToServer(Bitmap bitmap) {
        sendImageToServerWithRetry(bitmap, 0);
    }

    private void sendImageToServerWithRetry(Bitmap bitmap, int retryCount) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] byteArray = stream.toByteArray();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "receipt.jpg", RequestBody.create(byteArray, MediaType.parse("image/jpeg")))
                .build();
        Request request = new Request.Builder().url(SERVER_URL).post(requestBody).build();

        if (delayHandler != null) {
            if (delayRunnable != null) delayHandler.removeCallbacks(delayRunnable);
            delayRunnable = () -> ttsManager.speak("잠시만 기다려주세요", null);
            delayHandler.postDelayed(delayRunnable, 3000);
        }

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // 네트워크 연결 자체 실패 (타임아웃, ngrok 꺼짐 등)
                if (delayHandler != null && delayRunnable != null) delayHandler.removeCallbacks(delayRunnable);
                Log.e(TAG, "서버 요청 실패 (onFailure)", e);
                if (retryCount < 2) {
                    runOnUiThread(() -> ttsManager.speak("네트워크 오류가 발생했습니다. 다시 시도합니다.", null));
                    sendImageToServerWithRetry(bitmap, retryCount + 1);
                } else {
                    speakError("서버에 연결할 수 없습니다. 네트워크 상태를 확인해주세요.", sttRestartCallback);
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (delayHandler != null && delayRunnable != null) delayHandler.removeCallbacks(delayRunnable);
                // 핵심: HTTP 200 OK가 아니면 재시도 또는 오류 처리
                if (!response.isSuccessful()) {
                    Log.e(TAG, "서버가 오류를 응답했습니다. 코드: " + response.code());
                    if (retryCount < 2) {
                        runOnUiThread(() -> ttsManager.speak("서버에서 오류가 발생했습니다. 다시 시도합니다.", null));
                        sendImageToServerWithRetry(bitmap, retryCount + 1);
                    } else {
                        speakError("서버에서 응답을 받지 못했습니다. (오류 코드: " + response.code() + ")", sttRestartCallback);
                    }
                    response.body().close();
                    return;
                }

                // HTTP 200 OK면, 응답 본문을 무조건 handleServerResponse로 넘김
                final String responseBody = response.body().string();
                handleServerResponse(responseBody);
            }
        });
    }
}
//20251026-1048
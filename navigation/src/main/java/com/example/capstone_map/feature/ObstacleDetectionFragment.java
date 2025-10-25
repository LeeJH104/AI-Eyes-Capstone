package com.example.capstone_map.feature; // ★ 패키지 이름 유지

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraCharacteristics;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.capstone_map.R; // ★ R 임포트 유지
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;

import org.tensorflow.lite.task.vision.detector.Detection;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ObstacleDetectionFragment extends Fragment implements ObjectDetectorHelper.DetectorListener {

    private static final String TAG = "ObstacleFragmentJava"; // 로그 태그
    private SoundPool soundPool;
    private int detectionSoundId;
    private final String UTTERANCE_ID = "ai_eyes_utterance";

    private PreviewView previewView;
    private TextView txtResult;
    private Button btnToggleAnalysis;
    private ProgressBar progressBar;

    private UploadApi api;
    private TextToSpeech tts;
    private ExecutorService cameraExecutor;
    private boolean isContinuousAnalysis = false;
    private long lastCloudApiCallTime = 0;
    private static final long CLOUD_API_INTERVAL_MS = 5000;
    private ObjectDetectorHelper objectDetectorHelper;
    private Bitmap bitmapBuffer = null;
    private static final Set<String> DANGEROUS_OBJECTS = new HashSet<>(Arrays.asList(
            "car", "bicycle", "motorcycle", "bus", "truck", "person", "chair", "table"
    ));

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // ★ Activity 코드와 달리, View가 준비된 후 카메라를 시작하는 안전한 방식 유지
                    if (previewView != null) {
                        previewView.post(this::startCamera);
                    }
                } else {
                    Toast.makeText(requireContext(), "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                    // ★ Activity의 finish() 호출은 Fragment에 적합하지 않으므로 제거
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraExecutor = Executors.newSingleThreadExecutor();
        // ★ this -> requireContext()
        objectDetectorHelper = new ObjectDetectorHelper(requireContext(), "1.tflite", 0.5f, 2, 5, this);

        setupNetwork();
        setupTTS();
        setupSoundPool();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // ★ R.layout.fragment_obstacle 유지
        return inflater.inflate(R.layout.fragment_obstacle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ★★★★★ View ID를 ObstacleActivity 코드 기준으로 변경 ★★★★★
        // (fragment_obstacle.xml의 ID도 일치시켜야 함)
        previewView = view.findViewById(R.id.obstaclePreviewView);
        txtResult = view.findViewById(R.id.obstacleTxtResult);
        btnToggleAnalysis = view.findViewById(R.id.obstacleBtnToggleAnalysis);
        progressBar = view.findViewById(R.id.obstacleProgressBar);
        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

        btnToggleAnalysis.setOnClickListener(v -> toggleAnalysis());

        // View가 완전히 배치된 후 카메라 권한 확인/시작
        previewView.post(() -> {
            checkCameraPermission();
        });

        // UI 초기 상태 설정
        resetState();
    }

    private void toggleAnalysis() {
        isContinuousAnalysis = !isContinuousAnalysis;
        if (isContinuousAnalysis) {
            btnToggleAnalysis.setText("분석 중지");
            tts.speak("연속 분석을 시작합니다.", TextToSpeech.QUEUE_FLUSH, null, null);
            resetState();
        } else {
            btnToggleAnalysis.setText("분석 시작");
            txtResult.setText("분석이 중지되었습니다.");
            tts.speak("분석을 중지합니다.", TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void checkCameraPermission() {
        // ★ this -> requireContext()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // ★★★★★ startCamera 로직을 ObstacleActivity 기준으로 대폭 수정 ★★★★★
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private void startCamera() {
        Log.d(TAG, "startCamera() called");
        // ★ this -> requireContext()
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            Log.d(TAG, "CameraProvider future listener entered");
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder()
                        .build();

                if (previewView == null) {
                    Log.w(TAG, "PreviewView is null, cannot set SurfaceProvider");
                    return;
                }
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // ★ Activity 코드처럼 광각 카메라 우선 탐색
                CameraSelector cameraSelector = getWideAngleCameraSelector(cameraProvider);
                if (cameraSelector == null) {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                }

                // ★ Activity 코드의 ImageAnalysis 설정 적용 (RGBA_8888)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480)) // 분석용 해상도
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) // ★ 포맷 변경
                        .build();

                // ★ Activity 코드의 Analyzer 로직 적용 (copyPixelsFromBuffer)
                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (bitmapBuffer == null) {
                        // 비트맵 버퍼 초기화
                        bitmapBuffer = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
                    }
                    // RGBA 버퍼에서 비트맵으로 픽셀 복사 (toBitmap()보다 빠름)
                    bitmapBuffer.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());

                    int imageRotation = imageProxy.getImageInfo().getRotationDegrees();

                    if (isContinuousAnalysis && objectDetectorHelper != null) {
                        objectDetectorHelper.detect(bitmapBuffer, imageRotation);
                    }

                    // ★★★ 매우 중요: 처리가 끝나면 imageProxy를 닫아야 함
                    imageProxy.close();
                });

                cameraProvider.unbindAll();
                Log.d(TAG, "Attempting to bindToLifecycle (Preview + Analysis)...");

                // ★ this (LifecycleOwner) -> getViewLifecycleOwner()
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageAnalysis);

                Log.d(TAG, "bindToLifecycle (Preview + Analysis) successful!");

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera provider binding failed", e);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed to bind use cases. Unsupported combination or resolution?", e);
            } catch (Exception e) {
                Log.e(TAG, "Unknown error starting camera", e);
            }
            // ★ this -> requireContext()
        }, ContextCompat.getMainExecutor(requireContext()));
    }
    // ★★★★★ startCamera 수정 완료 ★★★★★


    // ★ (ObstacleActivity와 동일한) 광각 카메라 선택 로직
    @ExperimentalCamera2Interop
    @SuppressWarnings("deprecation")
    private CameraSelector getWideAngleCameraSelector(ProcessCameraProvider cameraProvider) {
        CameraSelector wideAngleCameraSelector = null;
        float minFocalLength = Float.MAX_VALUE;
        try {
            for (CameraInfo cameraInfo : cameraProvider.getAvailableCameraInfos()) {
                if (Camera2CameraInfo.from(cameraInfo).getCameraCharacteristic(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    @SuppressLint("RestrictedApi")
                    CameraCharacteristics characteristics = Camera2CameraInfo.extractCameraCharacteristics(cameraInfo);
                    float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    if (focalLengths != null && focalLengths.length > 0) {
                        float currentMinFocalLength = focalLengths[0];
                        for (float length : focalLengths) {
                            if (length < currentMinFocalLength) {
                                currentMinFocalLength = length;
                            }
                        }
                        if (currentMinFocalLength < minFocalLength) {
                            minFocalLength = currentMinFocalLength;
                            wideAngleCameraSelector = cameraInfo.getCameraSelector();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ★ 로그 태그 통일
            Log.e(TAG, "Failed to get camera characteristics.", e);
        }
        return wideAngleCameraSelector;
    }

    @Override
    public void onResults(List<Detection> results, long inferenceTime) {
        // ★ Activity 코드의 progressBar 체크 방식 적용 및 Fragment의 null 체크 보강
        if (progressBar == null || progressBar.getVisibility() == View.VISIBLE || !isContinuousAnalysis) return;

        boolean shouldCallCloudApi = false;
        // ★ Fragment의 results null 체크 유지 (안정성)
        if (results != null) {
            for (Detection detection : results) {
                String label = detection.getCategories().get(0).getLabel();
                if (DANGEROUS_OBJECTS.contains(label)) {
                    shouldCallCloudApi = true;
                    break;
                }
            }
        }

        if (shouldCallCloudApi && (System.currentTimeMillis() - lastCloudApiCallTime > CLOUD_API_INTERVAL_MS)) {
            lastCloudApiCallTime = System.currentTimeMillis();
            if (soundPool != null) {
                soundPool.play(detectionSoundId, 1, 1, 1, 0, 1.0f);
            }
            // ★ runOnUiThread -> if (isAdded()) { requireActivity().runOnUiThread(...) }
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                    if (txtResult != null) txtResult.setText("탐지 중...");
                });
            }
            String base64Image = bitmapToBase64(bitmapBuffer);
            sendImageToServer(base64Image);
        }
    }

    @Override
    public void onError(String error) {
        // ★ runOnUiThread -> if (isAdded()) { requireActivity().runOnUiThread(...) }
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show());
        }
    }

    private void sendImageToServer(String base64Image) {
        JsonObject json = new JsonObject();
        json.addProperty("image", base64Image);
        json.addProperty("level", 2);

        if (api == null) {
            Log.e(TAG, "API client is null.");
            // ★ Fragment의 안정성 코드 유지
            if (isAdded()) requireActivity().runOnUiThread(() -> resetState());
            return;
        }

        api.sendImage(json).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                // ★ Fragment의 isAdded() 체크 유지
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    String resultText = response.body().get("result").getAsString();
                    // ★ runOnUiThread -> requireActivity().runOnUiThread(...)
                    requireActivity().runOnUiThread(() -> {
                        if (txtResult != null) txtResult.setText(resultText);
                    });
                    Bundle params = new Bundle();
                    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID);
                    if (tts != null) {
                        tts.speak(resultText, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID);
                    }
                } else {
                    // ★★★★★★★★★★★★★★ 수정 지점 1 ★★★★★★★★★★★★★★
                    Log.e(TAG, "API Response Not Successful. Code: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "API Error Body: " + response.errorBody().string());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading errorBody: " + e.getMessage());
                    }

                    // UI 스레드에서 상태 변경
                    requireActivity().runOnUiThread(() -> {
                        // 1. 사용자에게 오류 알림
                        if (tts != null) {
                            tts.speak("서버 오류가 발생하여 분석을 중지합니다.", TextToSpeech.QUEUE_FLUSH, null, null);
                        }

                        // 2. 연속 분석 플래그를 false로 변경 (루프 중단!)
                        isContinuousAnalysis = false;

                        // 3. 버튼 텍스트 복구
                        if (btnToggleAnalysis != null) {
                            btnToggleAnalysis.setText("분석 시작");
                        }

                        // 4. 상태 리셋 (이때 isContinuousAnalysis가 false이므로 "분석을 시작..." 문구가 나옴)
                        resetState();

                        // 5. 텍스트를 오류 메시지로 덮어쓰기
                        if (txtResult != null) {
                            txtResult.setText("서버 오류 발생 (Code: " + response.code() + ")");
                        }
                    });
                    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                // ★ Fragment의 isAdded() 체크 유지
                if (!isAdded()) return;
                Log.e("RetrofitError", "Cloud API 통신 실패", t);
                // ★★★★★★★★★★★★★★ 수정 지점 2 ★★★★★★★★★★★★★★
                requireActivity().runOnUiThread(() -> {
                    // 1. 사용자에게 오류 알림
                    if (tts != null) {
                        tts.speak("네트워크 오류가 발생하여 분석을 중지합니다.", TextToSpeech.QUEUE_FLUSH, null, null);
                    }

                    // 2. 연속 분석 플래그를 false로 변경 (루프 중단!)
                    isContinuousAnalysis = false;

                    // 3. 버튼 텍스트 복구
                    if (btnToggleAnalysis != null) {
                        btnToggleAnalysis.setText("분석 시작");
                    }

                    // 4. 상태 리셋
                    resetState();

                    // 5. 텍스트를 오류 메시지로 덮어쓰기
                    if (txtResult != null) {
                        txtResult.setText("네트워크 연결 실패");
                    }
                });
                // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
            }
        });
    }

    private void setupTTS() {
        // ★ this -> requireContext()
        tts = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {}

                    @Override
                    public void onDone(String utteranceId) {
                        if (UTTERANCE_ID.equals(utteranceId)) {
                            // ★ runOnUiThread -> if (isAdded()) { requireActivity().runOnUiThread(...) }
                            if (isAdded()) requireActivity().runOnUiThread(() -> resetState());
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        // ★ runOnUiThread -> if (isAdded()) { requireActivity().runOnUiThread(...) }
                        if (isAdded()) requireActivity().runOnUiThread(() -> resetState());
                    }
                });
            }
        });
    }

    private void setupSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();
        // ★ this -> requireContext(), R.raw.detection_sound 참조 유지
        detectionSoundId = soundPool.load(requireContext(), R.raw.detection_sound, 1);
    }

    // ★ Activity 코드의 resetState 로직 적용
    private void resetState() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (txtResult != null) {
            if (isContinuousAnalysis) {
                txtResult.setText("주변을 계속 분석 중입니다...");
            } else {
                txtResult.setText("분석을 시작하려면 버튼을 누르세요.");
            }
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        // ★ 기존 Fragment의 null 체크 유지 (안정성)
        if (bitmap == null) return null;
        Bitmap resizedBitmap = getResizedBitmap(bitmap, 640);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void setupNetwork() {
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(logger)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api-v2-dot-obstacledetection.du.r.appspot.com")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(UploadApi.class);
    }

    // ★★★★★ Fragment 생명주기 메서드(onPause, onDestroyView, onDestroy)는 ★★★★★
    // ★★★★★ 기존 Fragment의 것을 그대로 유지하는 것이 안정적입니다. ★★★★★

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        isContinuousAnalysis = false;
        if (btnToggleAnalysis != null) {
            btnToggleAnalysis.setText("분석 시작");
        }
        resetState();
        if (tts != null) {
            tts.stop();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
        // View 참조 해제
        previewView = null;
        txtResult = null;
        btnToggleAnalysis = null;
        progressBar = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        if (objectDetectorHelper != null) {
            // objectDetectorHelper.close(); // Helper에 close 메서드가 있다면 호출
            objectDetectorHelper = null;
        }
        api = null;
    }
}
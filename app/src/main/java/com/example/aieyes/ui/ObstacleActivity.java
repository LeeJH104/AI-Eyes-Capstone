package com.example.aieyes.ui;

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
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

import com.example.aieyes.R;
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

public class ObstacleActivity extends AppCompatActivity implements ObjectDetectorHelper.DetectorListener {

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
                if (isGranted) { startCamera(); } else {
                    Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obstacle);

        previewView = findViewById(R.id.previewView);
        txtResult = findViewById(R.id.txt_result);
        btnToggleAnalysis = findViewById(R.id.btn_toggle_analysis);
        progressBar = findViewById(R.id.progressBar);
        cameraExecutor = Executors.newSingleThreadExecutor();
        objectDetectorHelper = new ObjectDetectorHelper(this, "ssd_mobilenet_v1_1_metadata_1.tflite", 0.5f, 2, 5, this);

        btnToggleAnalysis.setOnClickListener(v -> toggleAnalysis());

        setupNetwork();
        setupTTS();
        setupSoundPool();
        checkCameraPermission();
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                CameraSelector cameraSelector = getWideAngleCameraSelector(cameraProvider);
                if (cameraSelector == null) {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                }
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (bitmapBuffer == null) {
                        bitmapBuffer = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
                    }
                    bitmapBuffer.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());
                    int imageRotation = imageProxy.getImageInfo().getRotationDegrees();
                    if (isContinuousAnalysis) {
                        objectDetectorHelper.detect(bitmapBuffer, imageRotation);
                    }
                    imageProxy.close();
                });
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraXApp", "Camera provider binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

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
            Log.e("CameraXApp", "Failed to get camera characteristics.", e);
        }
        return wideAngleCameraSelector;
    }

    @Override
    public void onResults(List<Detection> results, long inferenceTime) {
        if (progressBar.getVisibility() == View.VISIBLE || !isContinuousAnalysis) return;

        boolean shouldCallCloudApi = false;
        for (Detection detection : results) {
            String label = detection.getCategories().get(0).getLabel();
            if (DANGEROUS_OBJECTS.contains(label)) {
                shouldCallCloudApi = true;
                break;
            }
        }

        if (shouldCallCloudApi && (System.currentTimeMillis() - lastCloudApiCallTime > CLOUD_API_INTERVAL_MS)) {
            lastCloudApiCallTime = System.currentTimeMillis();
            if (soundPool != null) {
                soundPool.play(detectionSoundId, 1, 1, 1, 0, 1.0f);
            }
            runOnUiThread(() -> {
                progressBar.setVisibility(View.VISIBLE);
                txtResult.setText("탐지 중...");
            });
            String base64Image = bitmapToBase64(bitmapBuffer);
            sendImageToServer(base64Image);
        }
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> Toast.makeText(this, error, Toast.LENGTH_SHORT).show());
    }

    private void sendImageToServer(String base64Image) {
        JsonObject json = new JsonObject();
        json.addProperty("image", base64Image);
        json.addProperty("level", 2);

        api.sendImage(json).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String resultText = response.body().get("result").getAsString();
                    runOnUiThread(() -> txtResult.setText(resultText));
                    Bundle params = new Bundle();
                    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID);
                    tts.speak(resultText, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID);
                } else {
                    runOnUiThread(() -> resetState());
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e("RetrofitError", "Cloud API 통신 실패", t);
                runOnUiThread(() -> resetState());
            }
        });
    }

    private void setupTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {}

                    @Override
                    public void onDone(String utteranceId) {
                        if (UTTERANCE_ID.equals(utteranceId)) {
                            runOnUiThread(() -> resetState());
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        runOnUiThread(() -> resetState());
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
        detectionSoundId = soundPool.load(this, R.raw.detection_sound, 1);
    }

    private void resetState() {
        progressBar.setVisibility(View.GONE);
        if (isContinuousAnalysis) {
            txtResult.setText("주변을 계속 분석 중입니다...");
        } else {
            txtResult.setText("분석을 시작하려면 버튼을 누르세요.");
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
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
                .baseUrl("https://hyunho.pythonanywhere.com/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(UploadApi.class);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (soundPool != null) {
            soundPool.release();
        }
    }
}
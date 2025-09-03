package com.example.obstacledetectiontest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.speech.tts.TextToSpeech;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.obstacledetectiontest.UploadApi;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    Uri imageUri;
    ImageView imgResult;
    TextView txtResult;
    UploadApi api;
    TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }

        imgResult = findViewById(R.id.img_result);
        txtResult = findViewById(R.id.txt_result);
        Button btnTakePhoto = findViewById(R.id.btn_take_photo);

        btnTakePhoto.setOnClickListener(v -> dispatchTakePictureIntent());

        // 📡 Retrofit 객체 생성
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.0.36:5000/") // 임의 flask 서버 주소
                .client(new OkHttpClient.Builder().addInterceptor(logger).build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(UploadApi.class);

        // 🗣️ TTS 초기화 추가
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS 언어를 지원하지 않음", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "TTS 초기화 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile;

        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        if (photoFile != null) {
            imageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
    }

    private String convertImageToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(this, "사진 촬영 완료!", Toast.LENGTH_SHORT).show();

            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                imgResult.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            String base64 = convertImageToBase64(imageUri);
            if (base64 != null) {
                JsonObject json = new JsonObject();
                json.addProperty("image", base64);

                Call<JsonObject> call = api.sendImage(json);
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String result = response.body().get("result").getAsString();
                            txtResult.setText(result);
                            Toast.makeText(MainActivity.this, "AI 분석 완료", Toast.LENGTH_SHORT).show();

                            Log.d("TTS_DEBUG", "TTS 내용: " + result);
                            tts.speak(result, TextToSpeech.QUEUE_FLUSH, null, null);
                        } else {
                            txtResult.setText("서버 응답 실패");
                            Log.e("TTS_DEBUG", "서버 응답 실패");
                        }
                    }


                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        txtResult.setText("네트워크 오류");
                        Log.e("RetrofitError", "통신 실패", t);
                        t.printStackTrace();
                    }

                });
            }
        }
    }
}

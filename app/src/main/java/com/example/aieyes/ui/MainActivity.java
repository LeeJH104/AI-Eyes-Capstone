package com.example.aieyes.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.aieyes.R;
import com.example.aieyes.utils.STTManager;
import com.example.aieyes.utils.TTSManager;

public class MainActivity extends AppCompatActivity {

    private TextView resultTextView;
    private Button sttButton, ttsButton;
    private TTSManager ttsManager;
    private STTManager sttManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // View 연결
        resultTextView = findViewById(R.id.result_text);
        sttButton = findViewById(R.id.stt_button);
        ttsButton = findViewById(R.id.tts_button);

        // TTS, STT 매니저 초기화
        ttsManager = new TTSManager(this);
        sttManager = new STTManager(this);

        // STT 결과 리스너 설정
        sttManager.setOnSTTResultListener(new STTManager.OnSTTResultListener() {
            @Override
            public void onSTTResult(String result) {
                resultTextView.setText(result);
            }

            @Override
            public void onSTTError(int errorCode) {
                resultTextView.setText("인식 실패. 다시 시도해주세요.");
            }
        });

        // 버튼 클릭 리스너 설정
        sttButton.setOnClickListener(v -> sttManager.startListening());
        ttsButton.setOnClickListener(v -> {
            String text = resultTextView.getText().toString();
            ttsManager.speak(text);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ttsManager.shutdown();
        sttManager.destroy();
    }
}
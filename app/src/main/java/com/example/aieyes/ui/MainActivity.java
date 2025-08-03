package com.example.aieyes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.aieyes.R;

public class MainActivity extends AppCompatActivity {

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 시스템 바 영역 패딩 처리
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 영수증 분석 버튼 클릭 리스너
        findViewById(R.id.btnReceipt).setOnClickListener(v -> openReceiptActivity());

        // 네비게이션 버튼 클릭 리스너
        findViewById(R.id.btnNavigation).setOnClickListener(v -> 
            Toast.makeText(this, "네비게이션 기능은 미구현입니다.", Toast.LENGTH_SHORT).show()
        );

        // 스와이프 제스처 감지기 초기화
        setupGestureDetector();
    }

    // 제스처 감지기 설정 및 뷰에 리스너 연결
    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                
                float diffX = e2.getX() - e1.getX();
                // 스와이프 감지 (좌우)
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX < 0) { // 왼쪽으로 스와이프
                        openReceiptActivity();
                        return true;
                    } else { // 오른쪽으로 스와이프
                        Toast.makeText(MainActivity.this, "네비게이션 기능은 미구현입니다.", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
                return false;
            }
        });

        // 전체 화면 뷰(main)에 터치 리스너 설정
        findViewById(R.id.main).setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    // 영수증 액티비티를 여는 메소드
    private void openReceiptActivity() {
        Intent intent = new Intent(this, ReceiptActivity.class);
        startActivity(intent);
    }
}

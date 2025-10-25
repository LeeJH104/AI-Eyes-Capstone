package com.example.aieyes.utils;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * GestureManager는 공통 제스처 처리 유틸리티 클래스입니다.
 * 좌/우/상/하 스와이프 및 더블탭 제스처를 감지하고 콜백으로 전달합니다.
 */
public class GestureManager {

    /**
     * 제스처 콜백 인터페이스
     * 각 제스처 감지 시 호출될 메서드를 정의합니다.
     */
    public interface OnGestureListener {
        void onSwipeLeft();     // 왼쪽 스와이프 (예: 이전 화면)
        void onSwipeRight();    // 오른쪽 스와이프 (예: 다음 항목)
        void onSwipeUp();       // 위로 스와이프 (예: 장애물 탐지)
        void onSwipeDown();     // 아래 스와이프 (예: 설명 다시 듣기)
        void onDoubleTap();     // 더블탭 (예: 항목 선택)
    }

    /**
     * View에 적용할 수 있는 TouchListener를 생성하여 반환합니다.
     * @param context Context (Activity 등)
     * @param listener 사용자가 정의한 제스처 콜백
     * @return View.OnTouchListener
     */
    public static View.OnTouchListener createGestureListener(Context context, OnGestureListener listener) {

        // GestureDetector는 다양한 제스처 이벤트를 처리할 수 있는 유틸
        GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

            private static final int SWIPE_THRESHOLD = 100;       // 스와이프 최소 거리
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;  // 스와이프 최소 속도

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                listener.onDoubleTap();  // 더블탭 콜백 호출
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;  //  방어 코드 추가

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    // 좌우 스와이프
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            listener.onSwipeRight();  // 오른쪽 스와이프
                        } else {
                            listener.onSwipeLeft();   // 왼쪽 스와이프
                        }
                        return true;
                    }
                } else {
                    // 상하 스와이프
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            listener.onSwipeDown();  // 아래로 스와이프
                        } else {
                            listener.onSwipeUp();    // 위로 스와이프
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        // 터치 리스너 반환
        return (v, event) -> gestureDetector.onTouchEvent(event);
    }
}

/*
사용 예시 - 상황에 따라 같은 제스처여도 다르게 처리
private String currentMode = "nav"; // 상황에 따라 변경 예) receipt, obstacle

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    View rootView = findViewById(android.R.id.content); // 전체 뷰에 적용 가능

    rootView.setOnTouchListener(
            GestureManager.createGestureListener(this, new GestureManager.OnGestureListener() {
                @Override
                public void onSwipeRight() {
                    if (currentMode.equals("guide")) {
                        navigateToNextStop();
                    } else if (currentMode.equals("obstacle")) {
                        speak("오른쪽 방향에 장애물이 있습니다.");
                    }
                }

                @Override
                public void onSwipeLeft() {
                    Log.d("Gesture", "왼쪽 스와이프 - 현재 모드: " + currentMode);
                }

                @Override
                public void onSwipeUp() {
                    Log.d("Gesture", "위로 스와이프");
                }

                @Override
                public void onSwipeDown() {
                    Log.d("Gesture", "아래로 스와이프");
                }

                @Override
                public void onDoubleTap() {
                    Log.d("Gesture", "더블탭");
                }
            })
    );
}
*/

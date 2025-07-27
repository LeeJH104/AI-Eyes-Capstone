//Json 데이터 받아오는

//package com.example.capstone_map;
//
//
//import android.util.Log;
//
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//@RunWith(AndroidJUnit4.class)
//public class PoiSearchInstrumentedTest {
//
//    private static final String TAG = "POI_TEST";
//
//    @Test
//    public void testSearchPois() {
//        PoiSearchManager.searchPois("편의점", 37.5665, 126.9780, new PoiSearchCallback() {
//            @Override
//            public void onSuccess(String result) {
//                Log.d(TAG, "성공! 응답: " + result);
//            }
//
//            @Override
//            public void onFailure(String error) {
//                Log.e(TAG, "실패! 에러: " + error);
//            }
//        });
//
//        // ⚠️ 테스트 종료 전에 약간 기다려줘야 결과가 들어올 시간 있음 (간단한 방법):
//        try {
//            Thread.sleep(5000); // 5초 대기
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
//}

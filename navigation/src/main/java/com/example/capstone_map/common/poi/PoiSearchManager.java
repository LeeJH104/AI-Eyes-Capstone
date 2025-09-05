package com.example.capstone_map.common.poi;

import android.os.Handler;
import android.os.Looper;

import com.example.capstone_map.common.poi.PoiSearchCallback;

import java.io.IOException;
import java.net.URLEncoder;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;




public class PoiSearchManager {

    private static final String BASE_URL = "https://apis.openapi.sk.com/tmap/pois";
    private static final String APP_KEY = "MUUgFleM6h4uFPz6yYOW03Gbzskx5Gci1rdtifFf";
    private static final OkHttpClient client = new OkHttpClient();


    public static void searchPois(String keyword, double userLat, double UserLon, PoiSearchCallback callback) {
        try {
            String encodedKeyword = URLEncoder.encode(keyword, "UTF-8"); //utf8로 인코딩

            //URL 쓰기 ?뒤로 파라미터 보내기
            String url = BASE_URL + "?version=1"
                    + "&searchKeyword=" + encodedKeyword
                    + "&searchType=all"
                    + "&searchtypCd=R" // 거리순 정렬 (기본값은 A)
                    + "&centerLat=" +userLat // 얘네는 검색할때 기준 좌표를 말하는건데 이건 현재 사용자 위치를 중심으로 개발해야한다. 지금은 일단 이렇게만해놓자
                    + "&centerLon=" + UserLon
                    + "&radius=2"
                    + "&resCoordType=WGS84GEO"
                    + "&reqCoordType=WGS84GEO"
                    + "&count=10"; //일단 10개 받아오기

            Request request = new Request.Builder() //request 만들기
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .addHeader("appKey", APP_KEY)
                    .build();

            client.newCall(request).enqueue(
                    new Callback() { //콜백함수 정의
                    final Handler mainHandler = new Handler(Looper.getMainLooper()); //


                    // 이해가 안간다
                    @Override // 이 아래는 OKHTTP의 Callback 함수인데 그걸 Override한거야
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onFailure("요청 실패: " + e.getMessage())); //얘가 백그라운드에서 실행된 정보
                    }



                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            String geoJson = response.body().string();
                            mainHandler.post(() -> callback.onSuccess(geoJson));
                        } else {
                            mainHandler.post(() -> callback.onFailure("응답 오류: " + response.code()));
                        }
                    }
                });

        } catch (Exception e) {
            callback.onFailure("에러: " + e.getMessage());
        }
    }
}


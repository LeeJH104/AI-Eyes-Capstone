package com.example.capstone_map.common.route;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PedestrianRouteRequester {

    private static final String API_URL = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1";
    private static final String API_KEY = "MUUgFleM6h4uFPz6yYOW03Gbzskx5Gci1rdtifFf";

    public static void requestPedestrianRoute(
            double startX, double startY, String startName,
            double endX, double endY, String endName,
            JsonCallback callback
    ) {
        try {
            JSONObject body = new JSONObject();
            body.put("startX", startX);
            body.put("startY", startY);
            body.put("endX", endX);
            body.put("endY", endY);
            body.put("reqCoordType", "WGS84GEO");
            body.put("startName", URLEncoder.encode(startName, "UTF-8"));
            body.put("endName", URLEncoder.encode(endName, "UTF-8"));
            body.put("searchOption", "0");
            body.put("resCoordType", "WGS84GEO");
            body.put("sort", "index");

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("appKey", API_KEY)
                    .post(RequestBody.create(
                            body.toString(),
                            MediaType.parse("application/json")
                    ))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure("네트워크 오류: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onFailure("API 실패: " + response.message());
                        return;
                    }

                    try {
                        String jsonString = response.body().string();
                        JSONObject json = new JSONObject(jsonString);
                        callback.onSuccess(json);  // JSON 객체로 넘김

                    } catch (JSONException e) { // JSON예외가 발생할 수 있다.
                        callback.onFailure("JSON 파싱 오류: " + e.getMessage());
                    }
                }

            });

        } catch (Exception e) {
            callback.onFailure("예외 발생: " + e.getMessage());
        }
    }
}

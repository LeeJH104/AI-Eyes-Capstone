package com.example.capstone_map.common.route;

import org.json.JSONObject;

public class RouteCacheManager {

    // 👉 경로 JSON 캐시 (앱이 켜져 있는 동안 유지됨)
    private static JSONObject cachedRouteJson = null; //얘가 static이당

    /**
     * 보행자 경로를 요청하거나, 캐시된 JSON을 재사용합니다.
     * @param startX 출발 경도
     * @param startY 출발 위도
     * @param startName 출발지 이름
     * @param endX 도착 경도
     * @param endY 도착 위도
     * @param endName 도착지 이름
     * @param callback JSON 응답 콜백 (onSuccess, onFailure)
     */
    public static void fetchRouteIfNeeded(
            double startX, double startY, String startName,
            double endX, double endY, String endName,
            JsonCallback callback
    ) {
        if (cachedRouteJson != null) {
            callback.onSuccess(cachedRouteJson); // ✅ 캐시 재활용
            return;
        }

        // 실제 API 호출
        PedestrianRouteRequester.requestPedestrianRoute(
                startX, startY, startName,
                endX, endY, endName,
                new JsonCallback() {
                    @Override
                    public void onSuccess(JSONObject json) {
                        cachedRouteJson = json;        // ✅ 캐시에 저장
                        callback.onSuccess(json);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        callback.onFailure(errorMessage);


                    }
                }
        );
    }

    /**
     * 이미 요청된 경로가 있다면 가져옵니다.
     * @return 캐시된 JSON 객체 (없으면 null)
     */
    public static JSONObject getCachedRoute() {
        return cachedRouteJson;
    }

    /**
     * 캐시를 초기화합니다 (예: 다른 경로 요청할 때)
     */
    public static void clearCache() {
        cachedRouteJson = null;
    }
}

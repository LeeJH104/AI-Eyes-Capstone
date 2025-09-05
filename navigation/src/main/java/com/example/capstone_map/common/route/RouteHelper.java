package com.example.capstone_map.common.route;

import android.graphics.Color;
import android.util.Log;

import com.example.capstone_map.common.route.JsonCallback;
import com.example.capstone_map.common.route.RouteCacheManager;
import com.example.capstone_map.common.route.RouteJsonParser;
import com.example.capstone_map.common.route.RouteLineDrawer;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class RouteHelper {

    public static void drawWalkingRoute(TMapView tMapView,
                                        double startX, double startY, String startName,
                                        double endX, double endY, String endName) {

        // ❗ 변경: 요청 대신 캐시를 먼저 확인하는 구조
        RouteCacheManager.fetchRouteIfNeeded(
                startX, startY, startName,
                endX, endY, endName,
                new JsonCallback() {
                    @Override
                    public void onSuccess(JSONObject json) {
                        try {
                            List<TMapPoint> routePoints = RouteJsonParser.parseToTMapPoints(json);

                            RouteLineDrawer.drawRouteLine(
                                    tMapView,
                                    routePoints,
                                    "walkRoute",
                                    Color.BLUE,
                                    5
                            );
                        } catch (JSONException e) {
                            Log.e("RouteHelper", "파싱 오류: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e("RouteHelper", "길찾기 실패: " + errorMessage);
                    }
                }
        );
    }
}

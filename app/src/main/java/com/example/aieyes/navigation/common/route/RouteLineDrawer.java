package com.example.capstone_map.common.route;

import android.graphics.Color;
import android.util.Log;

import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;
import com.skt.Tmap.TMapPoint;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class RouteLineDrawer {

    /**
     * 지도에 경로 선을 그려줍니다.
     *
     * @param tMapView       지도 뷰 객체
     * @param routePoints    경로 좌표 리스트 (TMapPoint)
     * @param lineId         선의 ID (지도에서 식별용)
     * @param color          선 색깔 (예: Color.BLUE)
     * @param width          선 두께 (픽셀 단위)
     */
    public static void drawRouteLine(TMapView tMapView,
                                     List<TMapPoint> routePoints,
                                     String lineId,
                                     int color,
                                     float width) {

        TMapPolyLine polyLine = new TMapPolyLine();
        polyLine.setLineColor(color);
        polyLine.setLineWidth(width);

        for (TMapPoint point : routePoints) {
            polyLine.addLinePoint(point);
        }

        tMapView.addTMapPolyLine(lineId, polyLine);
    }

    /**
     * 보행자 경로를 요청하고 지도에 그려줍니다. (RouteHelper 기능 통합)
     *
     * @param tMapView   지도 뷰 객체
     * @param startX     출발 경도
     * @param startY     출발 위도
     * @param startName  출발지 이름
     * @param endX       도착 경도
     * @param endY       도착 위도
     * @param endName    도착지 이름
     */
    public static void drawWalkingRoute(TMapView tMapView,
                                        double startX, double startY, String startName,
                                        double endX, double endY, String endName) {

        drawWalkingRoute(tMapView, startX, startY, startName, endX, endY, endName,
                "walkRoute", Color.BLUE, 5);
    }

    /**
     * 보행자 경로를 요청하고 지도에 그려줍니다. (스타일 커스터마이징 가능)
     *
     * @param tMapView   지도 뷰 객체
     * @param startX     출발 경도
     * @param startY     출발 위도
     * @param startName  출발지 이름
     * @param endX       도착 경도
     * @param endY       도착 위도
     * @param endName    도착지 이름
     * @param lineId     선의 ID
     * @param color      선 색깔
     * @param width      선 두께
     */
    public static void drawWalkingRoute(TMapView tMapView,
                                        double startX, double startY, String startName,
                                        double endX, double endY, String endName,
                                        String lineId, int color, float width) {

        RouteCacheManager.fetchRouteIfNeeded(
                startX, startY, startName,
                endX, endY, endName,
                new JsonCallback() {
                    @Override
                    public void onSuccess(JSONObject json) {
                        try {
                            List<TMapPoint> routePoints = RouteJsonParser.parseToTMapPoints(json);
                            drawRouteLine(tMapView, routePoints, lineId, color, width);
                        } catch (JSONException e) {
                            Log.e("RouteLineDrawer", "파싱 오류: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e("RouteLineDrawer", "길찾기 실패: " + errorMessage);


                    }
                }
        );
    }
}
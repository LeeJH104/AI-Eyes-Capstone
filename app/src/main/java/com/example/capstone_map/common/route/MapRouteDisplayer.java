package com.example.capstone_map.common.route;



import android.graphics.Color;
import android.util.Log;

import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * 지도에 경로를 표시하는 클래스
 * 경로 라인, 출발/도착 마커, 경로 정보를 모두 관리합니다.
 */
public class MapRouteDisplayer {

    private static final String TAG = "MapRouteDisplayer";

    // 기본 설정값들
    private static final int DEFAULT_ROUTE_COLOR = Color.BLUE;
    private static final float DEFAULT_ROUTE_WIDTH = 5.0f;
    private static final String ROUTE_LINE_ID = "walking_route";
    private static final String START_MARKER_ID = "start_marker";
    private static final String END_MARKER_ID = "end_marker";

    private TMapView tMapView;

    public MapRouteDisplayer(TMapView tMapView) {
        this.tMapView = tMapView;
    }

    /**
     * 경로를 지도에 표시합니다 (기본 설정 사용)
     * @param startX 출발지 경도
     * @param startY 출발지 위도
     * @param startName 출발지 이름
     * @param endX 도착지 경도
     * @param endY 도착지 위도
     * @param endName 도착지 이름
     */
    public void displayRoute(double startX, double startY, String startName,
                             double endX, double endY, String endName) {
        displayRoute(startX, startY, startName, endX, endY, endName,
                DEFAULT_ROUTE_COLOR, DEFAULT_ROUTE_WIDTH);
    }

    /**
     * 경로를 지도에 표시합니다 (커스텀 설정)
     * @param startX 출발지 경도
     * @param startY 출발지 위도
     * @param startName 출발지 이름
     * @param endX 도착지 경도
     * @param endY 도착지 위도
     * @param endName 도착지 이름
     * @param routeColor 경로 선 색상
     * @param routeWidth 경로 선 두께
     */
    public void displayRoute(double startX, double startY, String startName,
                             double endX, double endY, String endName,
                             int routeColor, float routeWidth) {

        // 기존 경로 지우기
        clearRoute();

        // 캐시된 경로가 있으면 바로 표시, 없으면 API 호출
        RouteCacheManager.fetchRouteIfNeeded(
                startX, startY, startName,
                endX, endY, endName,
                new JsonCallback() {
                    @Override
                    public void onSuccess(JSONObject json) {
                        try {
                            // 1. 경로 라인 그리기
                            drawRouteLine(json, routeColor, routeWidth);

                            // 2. 출발/도착 마커 표시
                            addStartEndMarkers(startX, startY, startName, endX, endY, endName);

                            // 3. 지도 범위 조정 (선택사항)
                            adjustMapBounds(startX, startY, endX, endY);

                            Log.d(TAG, "경로 표시 완료");

                        } catch (JSONException e) {
                            Log.e(TAG, "경로 JSON 파싱 오류: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "경로 요청 실패: " + errorMessage);
                    }
                }
        );
    }

    /**
     * JSON에서 파싱한 경로를 지도에 라인으로 그립니다
     */
    private void drawRouteLine(JSONObject routeJson, int color, float width) throws JSONException {
        List<TMapPoint> routePoints = RouteJsonParser.parseToTMapPoints(routeJson);

        if (!routePoints.isEmpty()) {
            RouteLineDrawer.drawRouteLine(tMapView, routePoints, ROUTE_LINE_ID, color, width);
        }
    }

    /**
     * 출발지와 도착지에 마커를 추가합니다
     */
    private void addStartEndMarkers(double startX, double startY, String startName,
                                    double endX, double endY, String endName) {

        // 출발지 마커 (초록색)
        TMapMarkerItem startMarker = new TMapMarkerItem();
        startMarker.setIcon(android.graphics.BitmapFactory.decodeResource(
                tMapView.getContext().getResources(),
                android.R.drawable.ic_dialog_map)); // 기본 아이콘 사용
        startMarker.setPosition(0.5f, 1.0f); // 마커 위치 조정
        startMarker.setTMapPoint(new TMapPoint(startY, startX));
        startMarker.setName(startName);
        startMarker.setVisible(TMapMarkerItem.VISIBLE);

        tMapView.addMarkerItem(START_MARKER_ID, startMarker);

        // 도착지 마커 (빨간색)
        TMapMarkerItem endMarker = new TMapMarkerItem();
        endMarker.setIcon(android.graphics.BitmapFactory.decodeResource(
                tMapView.getContext().getResources(),
                android.R.drawable.ic_dialog_alert)); // 기본 아이콘 사용
        endMarker.setPosition(0.5f, 1.0f);
        endMarker.setTMapPoint(new TMapPoint(endY, endX));
        endMarker.setName(endName);
        endMarker.setVisible(TMapMarkerItem.VISIBLE);

        tMapView.addMarkerItem(END_MARKER_ID, endMarker);
    }

    /**
     * 출발지와 도착지가 모두 보이도록 지도 범위를 조정합니다
     */
    private void adjustMapBounds(double startX, double startY, double endX, double endY) {
        // 두 점의 중간점 계산
        double centerLat = (startY + endY) / 2;
        double centerLon = (startX + endX) / 2;

        // 중심점으로 지도 이동
        tMapView.setCenterPoint(centerLon, centerLat);

        // 적절한 줌 레벨 설정 (거리에 따라 조정)
        double distance = calculateDistance(startY, startX, endY, endX);
        int zoomLevel = getZoomLevelByDistance(distance);
        tMapView.setZoomLevel(zoomLevel);
    }

    /**
     * 두 점 사이의 거리를 계산합니다 (단위: km)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구 반지름 (km)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * 거리에 따른 적절한 줌 레벨을 반환합니다
     */
    private int getZoomLevelByDistance(double distanceKm) {
        if (distanceKm < 1) return 17;      // 1km 미만
        else if (distanceKm < 3) return 15; // 3km 미만
        else if (distanceKm < 10) return 13; // 10km 미만
        else if (distanceKm < 20) return 12; // 20km 미만
        else return 11;                     // 20km 이상
    }

    /**
     * 지도에서 기존 경로와 마커들을 모두 제거합니다
     */
    public void clearRoute() {
        // 경로 라인 제거
        tMapView.removeTMapPolyLine(ROUTE_LINE_ID);

        // 마커들 제거
        tMapView.removeMarkerItem(START_MARKER_ID);
        tMapView.removeMarkerItem(END_MARKER_ID);

        Log.d(TAG, "기존 경로 정리 완료");
    }

    /**
     * 현재 표시된 경로의 요약 정보를 가져옵니다
     * @return 거리와 시간 정보, 없으면 null
     */
    public RouteJsonParser.RouteSummary getRouteSummary() {
        JSONObject cachedRoute = RouteCacheManager.getCachedRoute();
        if (cachedRoute != null) {
            try {
                return RouteJsonParser.parseSummary(cachedRoute);
            } catch (JSONException e) {
                Log.e(TAG, "경로 요약 파싱 오류: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * 경로 캐시를 초기화합니다 (새로운 경로 요청 전에 호출)
     */
    public void clearRouteCache() {
        RouteCacheManager.clearCache();
        Log.d(TAG, "경로 캐시 초기화");
    }
}

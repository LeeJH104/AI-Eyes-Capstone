package com.example.capstone_map.common.map;



import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.example.capstone_map.common.location.tracker.LocationTracker;
import com.example.capstone_map.common.location.tracker.LocationUpdateCallback;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapView;

public class LocationToMapBinder {

    private final LocationTracker locationTracker;
    private final TMapView tMapView;

    public LocationToMapBinder(Context context, TMapView tMapView) {
        this.tMapView = tMapView;

        this.locationTracker = new LocationTracker(context, new LocationUpdateCallback() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d("GPS", "위치 변경됨: " + location.getLatitude() + ", " + location.getLongitude());
                updateLocationOnMap(location);
            }

            @Override
            public void onLocationAccuracyChanged(float accuracy) {
                // 생략 가능 (필요하면 로직 추가)
            }

            @Override
            public void onGPSSignalWeak() {
                // 생략 가능 (필요하면 토스트 등으로 알림)
            }

            @Override
            public void onGPSSignalRestored() {
                // 생략 가능
            }
        });
    }

    /**
     * 지도에 현재 위치 마커를 찍고 중심 이동
     */
    private void updateLocationOnMap(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        TMapPoint point = new TMapPoint(lat, lon);
        TMapMarkerItem marker = new TMapMarkerItem();
        marker.setTMapPoint(point);
        marker.setName("현재 위치");
        marker.setVisible(TMapMarkerItem.VISIBLE);

        tMapView.setCenterPoint(lon, lat);
        tMapView.addMarkerItem("currentLocation", marker);
    }

    public void start() {
        locationTracker.startTracking();
    }

    public void stop() {
        locationTracker.stopTracking();
    }
}

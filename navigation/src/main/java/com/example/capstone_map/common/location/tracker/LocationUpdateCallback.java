package com.example.capstone_map.common.location.tracker;

import android.location.Location;

public interface LocationUpdateCallback {
    void onLocationChanged(Location location);      // 새로운 위치가 도착했을 때
    void onLocationAccuracyChanged(float accuracy); // 정확도(미터 단위)가 변경됐을 때
    void onGPSSignalWeak();                         // GPS 신호가 약할 때 (정확도 낮음)
    void onGPSSignalRestored();                     // GPS 신호가 양호할 때
}

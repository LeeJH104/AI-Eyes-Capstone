package com.example.capstone_map.common.poi;

public interface PoiSearchCallback {
    void onSuccess(String geoJson);
    void onFailure(String errorMessage);
}

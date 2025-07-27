package com.example.capstone_map.common.poi;

import java.util.List;

public interface PoiSearchCallback {
    void onSuccess(String geoJson);
    void onFailure(String errorMessage);
}

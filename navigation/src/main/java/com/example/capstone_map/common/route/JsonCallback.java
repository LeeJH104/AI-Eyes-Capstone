package com.example.capstone_map.common.route;

import org.json.JSONObject;

public interface JsonCallback {
    void onSuccess(JSONObject json); // ← String 대신 JSONObject
    void onFailure(String errorMessage);
}

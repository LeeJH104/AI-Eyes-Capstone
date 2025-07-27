package com.example.capstone_map.common.route;

import com.skt.Tmap.TMapPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RouteJsonParser {

    // 위도/경도 쌍으로 파싱
    public static List<double[]> parseToLatLonList(JSONObject json) throws JSONException {
        List<double[]> points = new ArrayList<>();

        JSONArray features = json.getJSONArray("features");
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            String type = feature.getJSONObject("geometry").getString("type");

            if ("LineString".equals(type)) {
                JSONArray coords = feature.getJSONObject("geometry").getJSONArray("coordinates");

                for (int j = 0; j < coords.length(); j++) {
                    JSONArray coord = coords.getJSONArray(j);
                    points.add(new double[]{coord.getDouble(1), coord.getDouble(0)}); // 위도, 경도
                }
            }
        }
        return points;
    }

    // TMapPoint로 파싱
    public static List<TMapPoint> parseToTMapPoints(JSONObject json) throws JSONException {
        List<TMapPoint> points = new ArrayList<>();
        JSONArray features = json.getJSONArray("features");

        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            String type = feature.getJSONObject("geometry").getString("type");

            if ("LineString".equals(type)) {
                JSONArray coords = feature.getJSONObject("geometry").getJSONArray("coordinates");
                for (int j = 0; j < coords.length(); j++) {
                    JSONArray coord = coords.getJSONArray(j);
                    points.add(new TMapPoint(coord.getDouble(1), coord.getDouble(0))); // 위도, 경도
                }
            }
        }
        return points;
    }

    // 설명만 추출
    public static List<String> parseToDescriptions(JSONObject json) throws JSONException {
        List<String> descriptions = new ArrayList<>();
        JSONArray features = json.getJSONArray("features");

        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            JSONObject props = feature.optJSONObject("properties");
            if (props != null) {
                String desc = props.optString("description", "");
                if (!desc.isEmpty()) descriptions.add(desc);
            }
        }
        return descriptions;
    }


    // 총 거리와 시간 파싱
    public static RouteSummary parseSummary(JSONObject json) throws JSONException {
        JSONArray features = json.getJSONArray("features");

        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            JSONObject properties = feature.optJSONObject("properties");

            if (properties != null && "SP".equals(properties.optString("pointType"))) {
                int distance = properties.optInt("totalDistance", 0); // 단위: m
                int time = properties.optInt("totalTime", 0); // 단위: 초
                return new RouteSummary(distance, time);
            }
        }

        return new RouteSummary(0, 0); // 기본값
    }

    public static class RouteSummary {
        public final int totalDistance;
        public final int totalTime;

        public RouteSummary(int totalDistance, int totalTime) {
            this.totalDistance = totalDistance;
            this.totalTime = totalTime;
        }
    }
}

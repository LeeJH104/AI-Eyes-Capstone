package com.example.capstone_map.common.poi;//package com.example.capstone_map.common.poi;
//
//
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class PoiParsingManager {
//
//    public static List<Poi> parse(String jsonString) throws JSONException {
//        List<Poi> poiList = new ArrayList<>();
//
//        // JSON 최상위 객체
//        JSONObject root = new JSONObject(jsonString);
//
//        // 실제 POI 배열까지 접근
//        JSONObject searchPoiInfo = root.getJSONObject("searchPoiInfo");
//        JSONObject pois = searchPoiInfo.getJSONObject("pois");
//        JSONArray poiArray = pois.getJSONArray("poi");
//
//        // 반복문으로 파싱
//        for (int i = 0; i < poiArray.length(); i++) {
//            JSONObject poiItem = poiArray.getJSONObject(i);
//
//
//            //이름 + lat , lon 추출
//            String name = poiItem.optString("name", "");
//            double lat = poiItem.optDouble("noorLat", 0);
//            double lon = poiItem.optDouble("noorLon", 0);
//
//            // 도로명 주소 추출
//            String fullAddress = "";
//            if (poiItem.has("newAddressList")) {
//                JSONObject newAddressList = poiItem.getJSONObject("newAddressList");
//                JSONArray newAddressArray = newAddressList.optJSONArray("newAddress");
//                if (newAddressArray != null && newAddressArray.length() > 0) {
//                    JSONObject newAddress = newAddressArray.getJSONObject(0);
//                    fullAddress = newAddress.optString("fullAddressRoad", "");
//                }
//            }
//
//            // POI 객체 생성
//            Poi poi = new Poi(name, fullAddress, lat, lon);
//            poiList.add(poi);
//        }
//
//        return poiList;
//    }
//}

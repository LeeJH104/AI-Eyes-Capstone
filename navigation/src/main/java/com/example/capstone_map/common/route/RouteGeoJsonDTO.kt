package com.example.capstone_map.common.route

data class FeatureCollection(
    val type: String,
    val features: List<Feature>
)

data class Feature(
    val type: String,
    val geometry: Geometry,
    val properties: Properties
)

data class Geometry(
    val type: String,
    val coordinates: Coordinates
)
sealed class Coordinates {
    data class Point(val lon: Double, val lat: Double) : Coordinates()
    data class LineString(val points: List<Pair<Double, Double>>) : Coordinates()
    // 필요 시 Polygon, MultiLineString 등도 추가
}
data class Properties(
    val totalDistance: Int? = null,
    val totalTime: Int? = null,
    val index: Int? = null,
    val lineIndex: Int? = null,
    val pointIndex: Int? = null,

    val name: String? = null,
    val description: String? = null,

    // ★ 서버가 "" 또는 "11" 같은 문자열로 줌 → String? 권장
    val facilityType: String? = null,
    val facilityName: String? = null,

    val turnType: Int? = null,
    val pointType: String? = null,

    // ★ 문자열 숫자 가능 → String? 권장
    val nearPoiX: String? = null,
    val nearPoiY: String? = null,

    val distance: Int? = null,
    val time: Int? = null,
    val roadType: Int? = null,
    val categoryRoadType: Int? = null
) {
    val facilityTypeInt get() = facilityType?.toIntOrNull()
    val nearPoiXDouble get() = nearPoiX?.toDoubleOrNull()
    val nearPoiYDouble get() = nearPoiY?.toDoubleOrNull()
}
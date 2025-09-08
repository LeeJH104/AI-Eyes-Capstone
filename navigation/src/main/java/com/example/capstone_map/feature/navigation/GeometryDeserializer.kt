package com.example.capstone_map.feature.navigation // 또는 어댑터를 모아두는 다른 패키지

import com.example.capstone_map.common.route.Coordinates
import com.example.capstone_map.common.route.Geometry
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class GeometryDeserializer : JsonDeserializer<Geometry> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Geometry {
        val jsonObject = json.asJsonObject
        val geometryTypeString = jsonObject.get("type")?.asString // "type" 필드 (e.g., "Point", "LineString")

        // "coordinates" 필드는 geometryTypeString에 따라 구조가 다름
        val coordinatesJson = jsonObject.get("coordinates")
            ?: throw JsonParseException("Coordinates field is missing in Geometry JSON")

        val coordinates: Coordinates = when (geometryTypeString) {
            "Point" -> {
                // "Point"의 coordinates는 [경도, 위도] 형태의 숫자 배열
                // 예: "coordinates": [126.98765, 37.56123]
                val pointArray = context.deserialize<List<Double>>(
                    coordinatesJson,
                    object : TypeToken<List<Double>>() {}.type
                )
                if (pointArray.size < 2) {
                    throw JsonParseException("Point coordinates must have at least 2 elements (lon, lat)")
                }
                Coordinates.Point(lon = pointArray[0], lat = pointArray[1])
            }
            "LineString" -> {
                // "LineString"의 coordinates는 [[경도1, 위도1], [경도2, 위도2], ...] 형태의 숫자 배열의 배열
                // 예: "coordinates": [ [100.0, 0.0], [101.0, 1.0] ]
                val lineStringArray = context.deserialize<List<List<Double>>>(
                    coordinatesJson,
                    object : TypeToken<List<List<Double>>>() {}.type
                )
                val pointsList = lineStringArray.map {
                    if (it.size < 2) {
                        throw JsonParseException("Each point in LineString coordinates must have at least 2 elements (lon, lat)")
                    }
                    // GeoJSON 표준은 [경도, 위도] 순서
                    it[0] to it[1] // Pair(경도, 위도)
                }
                Coordinates.LineString(points = pointsList)
            }
            // TODO: 필요한 다른 Geometry 타입 (e.g., "Polygon", "MultiPoint", "MultiLineString")에 대한 처리 추가
            else -> throw JsonParseException("Unsupported geometry type: $geometryTypeString")
        }

        return Geometry(type = geometryTypeString ?: "Unknown", coordinates = coordinates)
    }
}

package com.example.capstone_map.feature.navigation

import com.example.capstone_map.common.route.Coordinates
import com.example.capstone_map.common.route.Geometry
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class GeometryDeserializer : JsonDeserializer<Geometry> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Geometry {
        val jsonObj = json.asJsonObject
        val type = jsonObj["type"].asString
        val coords = jsonObj["coordinates"]

        val parsedCoordinates = when (type) {
            "Point" -> {
                val arr = coords.asJsonArray
                Coordinates.Point(arr[0].asDouble, arr[1].asDouble)
            }

            "LineString" -> {
                val arr = coords.asJsonArray
                Coordinates.LineString(
                    arr.map { pt ->
                        val p = pt.asJsonArray
                        Pair(p[0].asDouble, p[1].asDouble)
                    }
                )
            }

            else -> throw IllegalArgumentException("지원하지 않는 geometry type: $type")
        }

        return Geometry(type, parsedCoordinates)
    }
}

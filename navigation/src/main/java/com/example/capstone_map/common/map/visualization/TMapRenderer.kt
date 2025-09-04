package com.example.capstone_map.common.map.visualization

import android.graphics.BitmapFactory
import android.graphics.Color

import com.example.capstone_map.common.route.Coordinates
import com.example.capstone_map.common.route.Feature

import com.skt.Tmap.TMapMarkerItem
import com.skt.Tmap.TMapPoint
import com.skt.Tmap.TMapPolyLine
import com.skt.Tmap.TMapView

object TMapRenderer {

    //feature start, end파라미터로 받아서 mark생성
    fun showStartEndMarkers(tMapView: TMapView, start: Feature, end: Feature) {
        val startCoords = start.geometry.coordinates as? Coordinates.Point
        val endCoords = end.geometry.coordinates as? Coordinates.Point

        if (startCoords == null || endCoords == null) {
            // 좌표 타입이 Point가 아니면 마커 표시 안 함
            return
        }

        val startPoint = TMapPoint(startCoords.lat, startCoords.lon)
        val endPoint = TMapPoint(endCoords.lat, endCoords.lon)


        // marker생성
        val startMarker = TMapMarkerItem().apply {
            tMapPoint = startPoint
            name = "출발지"
            //icon = BitmapFactory.decodeResource(tMapView.context.resources, R.drawable)
            id = "START"
            calloutTitle = "출발"
            setCanShowCallout(true)
        }

        val endMarker = TMapMarkerItem().apply {
            tMapPoint = endPoint
            name = "도착지"
//            icon = BitmapFactory.decodeResource(tMapView.context.resources, R.drawable.maker)
            id = "END"
            calloutTitle = "도착"
            setCanShowCallout(true)
        }

        //map 에 marker추가

        tMapView.addMarkerItem(startMarker.id, startMarker)
        tMapView.addMarkerItem(endMarker.id, endMarker)



    }



    fun drawRouteLines(tMapView: TMapView, lineFeatures: List<Feature>) {
        for ((index, feature) in lineFeatures.withIndex()) {
            val coords = feature.geometry.coordinates
            if (coords is Coordinates.LineString) {
                val polyline = TMapPolyLine().apply {
                    lineWidth = 5f
                    lineColor = Color.BLUE
                }

                for ((lon, lat) in coords.points) {
                    polyline.addLinePoint(TMapPoint(lat, lon))
                }

                tMapView.addTMapPolyLine("ROUTE_LINE_$index", polyline)
            }
        }
    }



}

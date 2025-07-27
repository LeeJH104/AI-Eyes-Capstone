package com.example.capstone_map.common.poi



data class TmapSearchPoiResponse(
    val searchPoiInfo: SearchPoiInfo
)

data class SearchPoiInfo(
    val totalCount: String,
    val count: String,
    val page: String,
    val pois: Pois
)

data class Pois(
    val poi: List<Poi>
)

data class Poi(
    val id: String,
    val newAddressList: NewAddressList?, // ← 있을 수도 없을 수도 있음
    val pnsLat: String,
    val pnsLon: String, //출입구 좌표
    val name: String,
    val telNo: String? = null,
    val radius: Double? = null // ← ✅ radius 추가 (단위: 미터 등, nullable)

)

data class NewAddressList(
    val newAddress: List<NewAddress>
)

data class NewAddress(
    val fullAddressRoad: String
)


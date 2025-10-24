package com.example.capstone_map.feature.poisearch.viewmodel

import android.content.ContentValues.TAG
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import com.example.capstone_map.common.poi.PoiSearchCallback
import com.example.capstone_map.common.poi.PoiSearchManager
import com.example.capstone_map.feature.poisearch.state.POISearchState
import com.example.capstone_map.feature.poisearch.state.Searching
import com.example.capstone_map.common.location.oncefetcher.LocationFetcher
import com.example.capstone_map.common.poi.TmapSearchPoiResponse
import com.example.capstone_map.common.route.Geometry
import com.example.capstone_map.common.sharedVM.SharedNavigationViewModel
import com.example.capstone_map.common.voice.STTManager
import com.example.capstone_map.common.voice.TTSManager
import com.example.capstone_map.feature.navigation.GeometryDeserializer
import com.example.capstone_map.feature.navigation.state.StartNavigationPreparation
import com.example.capstone_map.feature.navigation.viewmodel.NavigationViewModel
import com.example.capstone_map.feature.poisearch.state.ListingCandidates
import com.example.capstone_map.feature.poisearch.state.LocationError
import com.example.capstone_map.feature.poisearch.state.Parsing
import com.example.capstone_map.feature.poisearch.state.ParsingCompleted
import com.example.capstone_map.feature.poisearch.state.ParsingFailed
import com.example.capstone_map.feature.poisearch.state.SearchCompleted
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.Locale
import kotlin.math.roundToInt

class POISearchViewModel(
    private val stateViewModel: SharedNavigationViewModel,
    private val navigationViewModel: NavigationViewModel,
    private val locationFetcher: LocationFetcher,
    private val ttsManager: TTSManager,
    private val sttManager: STTManager

) : ViewModel() {



    val poiSearchState = MutableLiveData<POISearchState>()
    private val candidates = mutableListOf<String>() // 예시: 실제로는 POI 모델을 써야 함
    private var currentIndex = 0

    fun updateState(state: POISearchState) {
        poiSearchState.value = state
        val prev = poiSearchState.value


        Log.d(TAG, "state: ${prev?.let { it::class.simpleName } ?: "null"} -> ${state}")
        // 콜백/비동기에서 호출될 수 있으니 postValue가 안전

        stateViewModel.setNavState("POI", ListingCandidates)

        state.handle(this)
    }



    //1. 현위치 가져오기 데이터형식 : Location
    fun fetchCurrentLocation() {

        locationFetcher.fetchLocation { location ->
            if (location != null) {
                // 현재 위치를 stateViewModel에 저장
                stateViewModel.currentLocation.postValue(location)
                updateState(Searching)

            } else {
                // 위치를 가져올 수 없을 때 처리
                updateState(LocationError)
                // 예: 사용자에게 알림 표시, 기본 위치 설정 등
            }
        }
    }

    // 2. 내위치 + keyword로 검색하기
    fun fetchCandidatesFromAPI() {
        val destination =  getDestination()
        val location = getLocation()


        if (location != null) {
            val lat = location.latitude
            val lon = location.longitude

            PoiSearchManager.searchPois(destination, lat, lon, object : PoiSearchCallback {
                override fun onSuccess(geoJson: String) {
                    if (geoJson.isNotBlank() && geoJson.contains("searchPoiInfo")) { //응답이 정상적인 구조인지 확인하는 절차

                        stateViewModel.geoJsonData.postValue(geoJson)

                        stateViewModel.geoJsonData.value = geoJson  // setValue()

                        // JSON 결과를 StateViewModel의 LiveData에 저장
                        //  결과 로그
                        Log.d("PoiSearch", "API 결과 수신 \n$geoJson")
                        updateState(SearchCompleted)

                    } else {
                        Log.w("PoiSearch", "응답 형식이 올바르지 않음: $geoJson")
                    }
                }

                override fun onFailure(errorMessage: String) {
                    // 실패 처리 로그 등
                    Log.e("PoiSearch", "검색 실패: $errorMessage")

//                   t ODO("검색실패하면 다시 앞의 DestinationViewmodel로 가야될듯")

                }
            })
        } else {
            Log.w("PoiSearch", "위치를 찾을 수 없습니다.")
        }
    }

    //3. 검색완료 -> 파싱하기상태로 전환
    fun  SearchingComplete() {
        updateState(Parsing)

    }



    // 4 Json 파싱하기
    fun parseGeoJson() {
        val json = stateViewModel.geoJsonData.value
        Log.d("GeoJson", "parseGeoJson() 진입, json isNull=${json == null}, length=${json?.length ?: 0}")

        if (json.isNullOrBlank()) {
            Log.w("GeoJson", "json이 비어있어 함수 종료")
            return
        }

        // Gson을 관대한 파서로 (숫자-문자 혼용 대비)
// Gson 인스턴스를 만드는 곳 (예시)
        val gson: Gson = GsonBuilder()
            .registerTypeAdapter(Geometry::class.java, GeometryDeserializer()) // 이 부분을 추가!
            .create()
        try {
            Log.d("GeoJson", "Gson 파싱 시작")
            val parsed = gson.fromJson(json, TmapSearchPoiResponse::class.java)
            Log.d("GeoJson", "parsed null? ${parsed == null}")

            val info = parsed?.searchPoiInfo
            Log.d("GeoJson", "searchPoiInfo null? ${info == null}")

            val poisContainer = info?.pois
            Log.d("GeoJson", "pois null? ${poisContainer == null}")

            val poiList = poisContainer?.poi
            Log.d("GeoJson", "poiList null? ${poiList == null}")

            if (poiList.isNullOrEmpty()) {
                Log.w("GeoJson", "poiList가 비어있음 → size=0 (스키마/필드명 확인 필요)")
                // 그래도 상태는 완료로 넘길지 판단
                updateState(ParsingFailed)
                return
            }

//            // 후보지 저장
//            stateViewModel.poiList.postValue(poiList)
//            stateViewModel.currentPoiIndex.postValue(0)
//            // 성공 후
            stateViewModel.poiList.value = poiList          //  setValue
            stateViewModel.currentPoiIndex.value = 0        //  setValue
            updateState(ParsingCompleted)                   // 이제 ListingCandidates에서 바로 읽힘

            Log.d("GeoJson", "파싱 성공: ${poiList.size}개 후보지")
            // 샘플 3개만 미리보기


        } catch (e: Exception) {
            // 메시지 없이 죽는 경우가 있어 stacktrace도 같이
            Log.e("GeoJson", "파싱 실패", e)
            // TODO 실패 상태 처리
        }

    }


    // 5. 파싱완료후 다음 상태 (후보지 나열상태)로
    fun showNextCandidate() {
        updateState(ListingCandidates)

        speak("다음후보지는 오른쪽스와이프 , 해당 후보지를 선택하려면 왼쪽 스와이프를 진행하세요")
    }




    // 6-1 현재 인덱스 읽어주는 함수
    fun readCurrentPoi() {
        val list = stateViewModel.poiList.value
        val idx = stateViewModel.currentPoiIndex.value
        Log.d("POI_READ", "idx=$idx, listSize=${list?.size}")

        if (list.isNullOrEmpty()) {
            Log.w("POI_READ", "poiList null or empty → 읽기 중단")
            return
        }

        // idx가 null이거나 범위를 벗어나면 보정
        val safeIndex = (idx ?: 0).coerceIn(0, list.lastIndex)
        if (idx != safeIndex) {
            Log.w("POI_READ", "index($idx) 보정 → $safeIndex")
            stateViewModel.currentPoiIndex.value = safeIndex
        }

        val poi = list[safeIndex]
        val name = poi.name ?: "(이름 없음)"
        val address = poi.newAddressList?.newAddress?.firstOrNull()?.fullAddressRoad ?: "주소 정보 없음"
        val distanceText = formatDistance(poi.radius)  // ✅ Double 처리

        Log.d("POI_READ", "읽기: #${safeIndex + 1}/${list.size} $name / $address / $distanceText")
        speak("${safeIndex + 1}번 후보지는 $name. 주소는 $address. 거리 $distanceText")
    }

    // 6-2 primary 행동: 다음 후보지로 이동 (순환)
    fun nextPoiIndex() {
        val list = stateViewModel.poiList.value ?: run {
            Log.w("POI_READ", "nextPoiIndex: poiList null")
            return
        }
        if (list.isEmpty()) {
            Log.w("POI_READ", "nextPoiIndex: list empty")
            return
        }

        val current = stateViewModel.currentPoiIndex.value ?: 0
        val next = (current + 1) % list.size   // ✅ 순환. 마지막 다음은 0번으로
        Log.d("POI_READ", "index: $current -> $next / size=${list.size}")

        stateViewModel.currentPoiIndex.value = next  // ✅ 동기 반영
    }

    // 6-3 현재 후보지 선택후 저장
    fun confirmCandidate() {
        val list = stateViewModel.poiList.value ?: run {
            Log.w("POI_READ", "confirmCandidate: poiList null")
            return
        }
        val idx = stateViewModel.currentPoiIndex.value ?: run {
            Log.w("POI_READ", "confirmCandidate: index null")
            return
        }
        if (idx !in list.indices) {
            Log.w("POI_READ", "confirmCandidate: index OOB idx=$idx size=${list.size}")
            return
        }

        val currentPOI = list[idx]
        stateViewModel.decidedDestinationPOI.value = currentPOI
        Log.d("POI_READ", "선택됨: #${idx + 1} ${currentPOI.name}")
        speak("좋아요. ${currentPOI.name}로 안내를 시작할게요.")
        navigationViewModel.updateState(StartNavigationPreparation)
    }

    // Double → 미터/킬로미터 단위 문자열 변환
    private fun formatDistance(km: Double?): String {
        km ?: return "거리 정보 없음"
        val m = (km * 1000).roundToInt()
        return if (m < 1000) "${m}미터" else String.format(Locale.KOREA, "%.1f킬로미터", km)
    }



    // 가져오기
    fun getDestination(): String? {
        return stateViewModel.destinationText.value
    }

    fun getLocation(): Location? {
        return stateViewModel.currentLocation.value
    }


//

    fun speak(text: String, onDone: (() -> Unit)? = null) { //함수 넘겨도되고 안 넘겨도돼
        ttsManager.speak(text, object : TTSManager.OnSpeakCallback {
            override fun onStart() {}
            override fun onDone() {
                onDone?.invoke()
            }
        })
    }

}

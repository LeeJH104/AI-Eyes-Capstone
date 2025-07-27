package com.example.capstone_map.feature.poisearch.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import com.example.capstone_map.feature.poisearch.state.POISearchState
import com.example.capstone_map.feature.poisearch.state.Searching
import com.example.capstone_map.common.location.oncefetcher.LocationFetcher
import com.example.capstone_map.common.poi.PoiSearchCallback
import com.example.capstone_map.common.poi.PoiSearchManager
import com.example.capstone_map.common.poi.TmapSearchPoiResponse
import com.example.capstone_map.common.viewmodel.NavigationStateViewModel
import com.example.capstone_map.common.voice.STTManager
import com.example.capstone_map.common.voice.TTSManager
import com.example.capstone_map.feature.navigation.state.StartNavigationPreparation
import com.example.capstone_map.feature.navigation.viewmodel.NavigationViewModel
import com.example.capstone_map.feature.poisearch.state.ListingCandidates
import com.example.capstone_map.feature.poisearch.state.LocationError
import com.example.capstone_map.feature.poisearch.state.Parsing
import com.example.capstone_map.feature.poisearch.state.ParsingCompleted
import com.example.capstone_map.feature.poisearch.state.SearchCompleted
import com.google.gson.Gson

class POISearchViewModel(
    private val stateViewModel: NavigationStateViewModel,
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
                        // JSON 결과를 StateViewModel의 LiveData에 저장
                        updateState(SearchCompleted)

                    } else {
                        Log.w("PoiSearch", "응답 형식이 올바르지 않음: $geoJson")
                    }
                }

                override fun onFailure(errorMessage: String) {
                    // 실패 처리 로그 등
                    Log.e("PoiSearch", "검색 실패: $errorMessage")

                    TODO("검색실패하면 다시 앞의 DestinationViewmodel로 가야될듯")

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
        val json = stateViewModel.geoJsonData.value ?: return
        try {
            val parsed = Gson().fromJson(json, TmapSearchPoiResponse::class.java)
            val poiList = parsed.searchPoiInfo.pois.poi

            // 후보지 리스트 저장 (viewModel에 있는 리스트에 저장해도 되고)
            stateViewModel.poiList.postValue(poiList)
            stateViewModel.currentPoiIndex.postValue(0) //인덱스 0으로 초기화

            Log.d("GeoJson", "파싱 성공: ${poiList.size}개 후보지")

        } catch (e: Exception) {
            Log.e("GeoJson", "파싱 실패: ${e.message}")
            TODO("실패후 상태처리")
            // 필요시 실패 상태도 처리
        }


        updateState(ParsingCompleted) // ✅ 실행됨

    }

    // 5. 파싱완료후 다음 상태 (후보지 나열상태)로
    fun showNextCandidate() {
        updateState(ListingCandidates)
}



    // 6-1 현재 인덱스 읽어주는 함수

    fun readCurrentPoi() {
        val index = stateViewModel.currentPoiIndex.value ?: return
        val poiList = stateViewModel.poiList.value ?: return
        if (index >= poiList.size) return

        val poi = poiList[index]
        val name = poi.name
        val address = poi.newAddressList
            ?.newAddress
            ?.firstOrNull()
            ?.fullAddressRoad ?: "주소 정보 없음"
        val distance = poi.radius

        speak("${index + 1}번 후보지는 $name 주소는 $address 거리는$distance")
    }

    // 6-2 primary 행동 다음후보지 선택
    fun nextPoiIndex() {
        val currentindex = stateViewModel.currentPoiIndex.value ?: 0
        //즉, 초기값이 없을 때도 앱이 멈추지 않고 0부터 시작하도록 만든 것.
        // index 에러 날듯
        stateViewModel.currentPoiIndex.postValue(currentindex + 1)
    }

    // 6-3 현재 후보지 선택후 저장
    fun confirmCandidate() {
        val currentPoiIndex = stateViewModel.currentPoiIndex.value ?: return
        val poiList = stateViewModel.poiList.value ?: return
        if (currentPoiIndex >= poiList.size) return

        val currentPOI = poiList[currentPoiIndex]
        stateViewModel.decidedDestinationPOI.postValue(currentPOI)
        // 상태 전이 다음 보행경로 길안내 viewmodel, state로 넘어가야함
        navigationViewModel.updateState(StartNavigationPreparation)


    }


    fun skipCandidate() {

    }

    fun handleNoCandidates() {
        // 필요 시 목적지 입력 단계로 다시 보내기
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

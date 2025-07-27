package com.example.capstone_map.feature.navigation.viewmodel



import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.capstone_map.common.location.tracker.LocationTracker
import com.example.capstone_map.common.location.tracker.LocationUpdateCallback
import com.example.capstone_map.common.route.Feature
import com.example.capstone_map.common.route.FeatureCollection
import com.example.capstone_map.common.route.JsonCallback
import com.example.capstone_map.common.route.RouteCacheManager
import com.example.capstone_map.common.viewmodel.NavigationStateViewModel
import com.example.capstone_map.common.voice.STTManager
import com.example.capstone_map.common.voice.TTSManager
import com.example.capstone_map.feature.navigation.sensor.CompassManager
import com.example.capstone_map.feature.navigation.state.AligningDirection
import com.example.capstone_map.feature.navigation.state.GuidingNavigation
import com.example.capstone_map.feature.navigation.state.NavigationError
import com.example.capstone_map.feature.navigation.state.NavigationFinished
import com.example.capstone_map.feature.navigation.state.NavigationState
import com.example.capstone_map.feature.navigation.state.RouteDataParsing
import com.example.capstone_map.feature.navigation.state.RouteSearching
import com.example.capstone_map.feature.poisearch.state.LocationError
import com.example.capstone_map.feature.poisearch.state.Searching
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs
import kotlin.math.min


class NavigationViewModel(

    private val context: Context, // ✅ 추가
    private val stateViewModel: NavigationStateViewModel,
    private val ttsManager: TTSManager,
    private val sttManager: STTManager




) : ViewModel() {


    private var locationTracker: LocationTracker? = null
    private var isTrackingLocation = false // 현재 추적 중인지 상태 저장
    val navigationState = MutableLiveData<NavigationState>()
    private val candidates = mutableListOf<String>() // 예시: 실제로는 POI 모델을 써야 함
    private var currentIndex = 0
    private var lastSpokenIndex = -1 // 중복 안내 방지용
    private val compassManager = CompassManager(context)




    fun updateState(newState: NavigationState) {
        val previousState = navigationState.value
        navigationState.value = newState

        handleLocationTrackingTransition(previousState, newState)
        newState.handle(this)
    }


    /** 경로안내 준비  */
    fun prepareNavigation() {
        // 목적지/경로 옵션 설정 완료 여부 확인 (상태 관리)
        // UI 초기화, 버튼 리스너 등록 등 행동 처리


        updateState(RouteSearching)
    }


    /** 경로 검색후 json으로 받아와서 statviewmodel (모든 viewmodel이 데이터를 공유하는)에 넣기*/
    fun requestRouteToDestination() {
        val location = stateViewModel.currentLocation.value
        val destinationPoi = stateViewModel.decidedDestinationPOI.value

        if (location == null || destinationPoi == null) {
            updateState(NavigationError("현위치나 목적지가 없습니다"))
            return
        }

        val startX = location.longitude
        val startY = location.latitude
        val startName = "현위치"

        val endX = destinationPoi.pnsLon.toDoubleOrNull()
        val endY = destinationPoi.pnsLat.toDoubleOrNull()
        val endName = destinationPoi.name ?: "목적지"

        if (endX == null || endY == null) {
            updateState(NavigationError("목적지 좌표가 유효하지 않습니다"))
            return
        }

        //이위에까지는 변수에 값을 넣어주고
        //이 아래는 값을 넣은 변수들로 http req하는거임
        RouteCacheManager.fetchRouteIfNeeded(
            startX, startY, startName,
            endX, endY, endName,
            object : JsonCallback { //api보내고 받은 데이터를 가져오는 콜백
                override fun onSuccess(json: JSONObject) {
                    try {
                        val jsonString = json.toString()
                        stateViewModel.routeJsonData.postValue(jsonString) // ✅ 문자열로 저장

                        updateState(RouteDataParsing) // 다음 상태로 넘김

                    } catch (e: Exception) {
                        updateState(NavigationError("경로 응답 파싱 실패: ${e.message}"))
                    }
                }

                override fun onFailure(errorMessage: String) {
                    updateState(NavigationError("경로 요청 실패: $errorMessage"))
                }
            }
        )
    }

    /** 받아온 데이터 파싱 */
    fun parseRawJson() {
        val gson = Gson()
        val routeJsonString = stateViewModel.routeJsonData.value ?: return

        val routeData = gson.fromJson(routeJsonString, FeatureCollection::class.java)

        val pointFeatures = mutableListOf<Feature>()
        val lineFeatures = mutableListOf<Feature>()

        for (feature in routeData.features) {
            when (feature.geometry.type) {
                "Point" -> {
                    pointFeatures.add(feature)
                    Log.d("ROUTE_POINT", "Point: ${feature.geometry.type}, ${feature.properties.description}")


                }
                "LineString" -> {
                    lineFeatures.add(feature)
                    Log.d(
                        "ROUTE_LINE",
                        "Line: ${feature.geometry.type}, ${feature.properties.description}"
                    )
                }
            }
        }

        // 정렬
        val sortedPoints = pointFeatures.sortedBy { it.properties.pointIndex ?: Int.MAX_VALUE }
        val sortedLines = lineFeatures.sortedBy { it.properties.lineIndex ?: Int.MAX_VALUE }

        Log.d("ROUTE_CHECK", "Total Points: ${sortedPoints.size}, Total Lines: ${sortedLines.size}")


        // ViewModel에 저장

        stateViewModel.routePointFeatures.postValue(sortedPoints)
        stateViewModel.routeLineFeatures.postValue(sortedLines)
        updateState(AligningDirection)


    }


    /**  CompassManager의 방향(사용자 핸드폰들고있는 방향)을 주기적으로 ViewModel에 저장
     */
    fun startCompassTracking() {
        compassManager.start()

        // 주기적으로 방향을 stateViewModel에 반영
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                stateViewModel.currentAzimuth.postValue(compassManager.currentAzimuth)
            }
        }, 0, 500) // 0.5초마다 업데이트
    }


    /**   첫 포인트와 방향 일치 여부 검사 함수*/

    fun alignDirectionToFirstPoint() {
        val currentAzimuth = stateViewModel.currentAzimuth.value ?: return
        val userLocation = stateViewModel.currentLocation.value ?: return
        val firstPoint = stateViewModel.routePointFeatures.value?.firstOrNull() ?: return

        val lat = firstPoint.geometry.coordinates.asJsonArray[1].asDouble
        val lon = firstPoint.geometry.coordinates.asJsonArray[0].asDouble

        val targetLocation = Location("").apply {
            latitude = lat
            longitude = lon
        }

        val bearing = userLocation.bearingTo(targetLocation)

        val rawDiff = abs(currentAzimuth - bearing)
        val diff = min(rawDiff, 360 - rawDiff)


        if (diff > 20f) {
            speak("휴대폰을 ${if (currentAzimuth > bearing) "왼쪽" else "오른쪽"}으로 돌려주세요")
        } else {
            speak("방향이 맞춰졌습니다. 안내를 시작합니다.") {
                updateState(GuidingNavigation)
            }
        }
    }




    //tracking 및 point도착시 description speak
    fun startTrackingLocation() {
        if (isTrackingLocation) {
            Log.d("TRACKING", "이미 추적 중 → 다시 시작 안 함")
            return
        }
        if (locationTracker == null) {
            locationTracker = LocationTracker(context, object : LocationUpdateCallback {
                override fun onLocationChanged(location: Location) {
                    Log.d("TRACKING", "📡 위치 갱신됨 → ${location.latitude}, ${location.longitude}")
                    stateViewModel.currentLocation.postValue(location)
                    checkAndSpeakNextPoint(location)
                }

                override fun onLocationAccuracyChanged(accuracy: Float) {
                    Log.d("TRACKING", "📶 정확도 변경됨 → $accuracy")
                }

                override fun onGPSSignalWeak() {
                    Log.w("TRACKING", "⚠️ GPS 신호 약함")
                }

                override fun onGPSSignalRestored() {
                    Log.d("TRACKING", "✅ GPS 신호 정상 복구")
                }
            })
        }

        locationTracker?.startTracking()
        Log.i("TRACKING", "🟢 위치 추적 시작됨")
    }



    fun stopTrackingLocation() {
        locationTracker?.stopTracking()
        Log.i("TRACKING", "🔴 위치 추적 중지됨")
    }



    //
    private fun handleLocationTrackingTransition(oldState: NavigationState?, newState: NavigationState) {
        val shouldStartTracking = newState is GuidingNavigation || newState is AligningDirection
        val shouldStopTracking = newState is NavigationFinished || newState is NavigationError

        // 상태가 처음 추적 가능한 범위에 진입했을 때만 start
        if (!isTrackingLocation && shouldStartTracking) {
            startTrackingLocation()
            isTrackingLocation = true
        }

        // 추적 중인데 종료 상태에 도달하면 stop
        if (isTrackingLocation && shouldStopTracking) {
            stopTrackingLocation()
            isTrackingLocation = false
        }
    }






    private fun checkAndSpeakNextPoint(location: Location) {
        val pointFeatures = stateViewModel.routePointFeatures.value ?: return
        val lastPointIndex = pointFeatures.maxOfOrNull { it.properties.pointIndex ?: -1 } ?: return

        Log.d("NAVIGATION", "📍 현재 위치: (${location.latitude}, ${location.longitude})")

        for (feature in pointFeatures) {
            val index = feature.properties.pointIndex ?: continue
            if (index <= lastSpokenIndex) {
                Log.d("NAVIGATION", "✅ 이미 말한 포인트 index $index → 건너뜀")
                continue
            }

            val coordsArray = feature.geometry.coordinates.asJsonArray
            if (coordsArray.size() < 2) {
                Log.w("NAVIGATION", "⚠️ 좌표 개수가 부족함 → 건너뜀")
                continue
            }

            val lon = coordsArray[0].asDouble
            val lat = coordsArray[1].asDouble

            val targetLocation = Location("").apply {
                longitude = lon
                latitude = lat
            }

            val distance = location.distanceTo(targetLocation)
            Log.d("NAVIGATION", "📍 index $index 도착지까지 거리: ${"%.2f".format(distance)}m")

            if (distance < 15f) {
                val description = feature.properties.description
                if (!description.isNullOrBlank()) {
                    Log.i("NAVIGATION", "🗣️ 안내 시작: $description")
                    speak(description) {
                        Log.i("NAVIGATION", "✅ 안내 완료: index $index")
                        lastSpokenIndex = index

                        // 🟡 도착 지점인지 확인
                        if (index == lastPointIndex) {
                            handleArrival()
                        }
                    }
                } else {
                    Log.w("NAVIGATION", "⚠️ 안내 문구 없음 (index $index)")
                }

                break // 이미 처리한 포인트는 더 이상 반복 안 함
            }
        }
    }



    fun speak(text: String, onDone: (() -> Unit)? = null) { //함수 넘겨도되고 안 넘겨도돼
        ttsManager.speak(text, object : TTSManager.OnSpeakCallback {
            override fun onStart() {}
            override fun onDone() {
                onDone?.invoke()
            }
        })
    }


    private fun handleArrival() {
        Log.i("NAVIGATION", "🏁 목적지 도착 처리 시작")
        speak("목적지에 도착했습니다. 안내를 종료합니다.") {
            updateState(NavigationFinished)
        }
    }






//    fun fetchCurrentLocation() {
//
//        locationFetcher.fetchLocation { location ->
//            if (location != null) {
//                // 현재 위치를 stateViewModel에 저장
//                stateViewModel.currentLocation.postValue(location)
//                updateState(Searching)
//
//            } else {
//                // 위치를 가져올 수 없을 때 처리
//                updateState(LocationError)
//                // 예: 사용자에게 알림 표시, 기본 위치 설정 등
//            }
//        }
//    }
//
//    /** 방향 정렬 상태로 변경 */
//    fun startAligningDirection() {
//
//
//
//    }
//
//    /** 경로 요약 안내 시작 */
//    fun announceRoute(summary: String) {
//        DestinationState.value = DestinationState.AnnounceRouteSummary(summary)
//    }
//
//    /** 실제 길안내 시작 */
//    fun startNavigation(currentStep: Int, totalSteps: Int, nextDirection: String) {
//        DestinationState.value = DestinationState.GuidingNavigation(
//            currentStep = currentStep,
//            totalSteps = totalSteps,
//            nextDirection = nextDirection
//        )
//    }
//
//


//    /** 다음 단계로 진행 */
//    fun nextStep(currentStep: Int, totalSteps: Int, nextDirection: String) {
//        if (currentStep >= totalSteps) {
//            finishNavigation()
//        } else {
//            _navigationState.value = NavigationState.GuidingNavigation(
//                currentStep = currentStep,
//                totalSteps = totalSteps,
//                nextDirection = nextDirection
//            )
//        }
//    }
//
//    /** 안내 종료 */
//    fun finishNavigation() {
//        _navigationState.value = NavigationState.NavigationFinished
//    }
//
//    /** 에러 처리 */
//    fun showError(message: String) {
//        _navigationState.value = NavigationState.NavigationError(message)
//    }
//
//    /** 상태 초기화 (필요 시 사용) */
//    fun resetState() {
//        _navigationState.value = NavigationState.StartNavigationPreparation
//    }
//}
}
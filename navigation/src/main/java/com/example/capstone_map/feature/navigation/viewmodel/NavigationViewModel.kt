package com.example.capstone_map.feature.navigation.viewmodel



import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.capstone_map.common.location.tracker.LocationTracker
import com.example.capstone_map.common.location.tracker.LocationUpdateCallback
import com.example.capstone_map.common.route.Coordinates
import com.example.capstone_map.common.route.Feature
import com.example.capstone_map.common.route.FeatureCollection
import com.example.capstone_map.common.route.Geometry
import com.example.capstone_map.common.route.JsonCallback
import com.example.capstone_map.common.route.RouteCacheManager
import com.example.capstone_map.common.viewmodel.SharedNavigationViewModel
import com.example.capstone_map.common.voice.STTManager
import com.example.capstone_map.common.voice.TTSManager
import com.example.capstone_map.feature.navigation.GeometryDeserializer
import com.example.capstone_map.feature.navigation.SafeDoubleAdapterNullable
import com.example.capstone_map.feature.navigation.SafeDoubleAdapterPrimitive
import com.example.capstone_map.feature.navigation.SafeIntAdapterNullable
import com.example.capstone_map.feature.navigation.SafeIntAdapterPrimitive
import com.example.capstone_map.feature.navigation.SafeLongAdapterNullable
import com.example.capstone_map.feature.navigation.SafeLongAdapterPrimitive
import com.example.capstone_map.feature.navigation.sensor.NewCompassManager
import com.example.capstone_map.feature.navigation.state.AligningDirection
import com.example.capstone_map.feature.navigation.state.GuidingNavigation
import com.example.capstone_map.feature.navigation.state.NavigationError
import com.example.capstone_map.feature.navigation.state.NavigationFinished
import com.example.capstone_map.feature.navigation.state.NavigationState
import com.example.capstone_map.feature.navigation.state.RouteDataParsing
import com.example.capstone_map.feature.navigation.state.RouteSearching
import com.example.capstone_map.feature.navigation.state.StartNavigationPreparation
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs
import kotlin.math.min


class NavigationViewModel(

    private val context: Context, // ✅ 추가
    private val stateViewModel: SharedNavigationViewModel,
    private val ttsManager: TTSManager,
    private val sttManager: STTManager




) : ViewModel() {


    @Volatile private var isSpeaking = false

    private var locationTracker: LocationTracker? = null
    private var isTrackingLocation = false // 현재 추적 중인지 상태 저장
    val navigationState = MutableLiveData<NavigationState>()
    private val candidates = mutableListOf<String>() // 예시: 실제로는 POI 모델을 써야 함
    private var currentIndex = 0
    private var lastSpokenIndex = -1 // 중복 안내 방지용
    // private val compassManager = CompassManager(context)

    private val compassManager = NewCompassManager(context) { deg ->
        // 각도 변화 임계값 필터(선택)
        val last = stateViewModel.currentAzimuth.value
        if (last == null || angleDelta(last, deg) >= 3f) { // 3도 이상 변할 때만 반영 예시
            stateViewModel.currentAzimuth.postValue(deg)
        }
    }


    private var alignmentJob: Job? = null


    fun updateState(newState: NavigationState) {
        val applyOnMain: () -> Unit = let@{
            // 같은 타입이면 불필요한 전이/handle 방지
            val previousState = navigationState.value
            if (previousState?.javaClass == newState.javaClass) return@let

            //  LiveData는 메인에서 setValue
            navigationState.value = newState

            //  공용 상태에도 현재 네비게이션 상태 그대로 반영 (임의로 StartNavigationPreparation 덮어쓰지 않음)
            stateViewModel.setNavState("NAV", newState)

            //  부수효과(추적 on/off 등) → 이전/신규 상태 기준으로 처리
            handleLocationTrackingTransition(previousState, newState)

            //  상태 진입 동작도 메인에서
            newState.handle(this)
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            // 이미 메인 스레드면 즉시 적용
            applyOnMain()
        } else {
            // 백그라운드(OkHttp 등)에서 호출된 경우 메인으로 스위치
            viewModelScope.launch(Dispatchers.Main) { applyOnMain() }
        }
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
                        Log.d(
                            "NAVIGATION_RAW_JSON",
                            "Received JSON: $jsonString"
                        ) // ✅ 원본 JSON 데이터 출력
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
// Gson 인스턴스를 만드는 곳 (예시)
        val gson: Gson = GsonBuilder()
            .registerTypeAdapter(Geometry::class.java, GeometryDeserializer()) // 이 부분을 추가!
            .create()


        val routeJsonString = stateViewModel.routeJsonData.value ?: return

        val routeData = gson.fromJson(routeJsonString, FeatureCollection::class.java)

        val pointFeatures = mutableListOf<Feature>()
        val lineFeatures = mutableListOf<Feature>()

        for (feature in routeData.features) {
            when (feature.geometry.type) {
                "Point" -> {
                    pointFeatures.add(feature)
                    Log.d(
                        "ROUTE_POINT",
                        "Point: ${feature.geometry.type}, ${feature.properties.description}"
                    )


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

    }


    fun stopCompassTracking() {
        compassManager.stop()
    }


    /**   첫 포인트와 방향 일치 여부 검사 함수*/


    fun alignDirectionToFirstPoint() {
        val azimuth = stateViewModel.currentAzimuth.value ?: return
        val curr    = stateViewModel.currentLocation.value ?: return

        val next = nextPointByIndex() ?: return
        val p = next.geometry.coordinates as? Coordinates.Point ?: return
        val target = toLocation(p)

        val bearing = ((curr.bearingTo(target) + 360) % 360)
        val diff = ((bearing - azimuth + 540) % 360) - 180  // -180..+180 (최소 회전경로)
        val absDiff = kotlin.math.abs(diff)
        val threshold = 20f

        if (absDiff > threshold) {
            val dir = if (diff > 0) "오른쪽" else "왼쪽"
            speak("휴대폰을 $dir 으로 돌려주세요")
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
        Log.i("TRACKING", " 위치 추적 중지됨")
    }


    //
    private fun handleLocationTrackingTransition(
        oldState: NavigationState?,
        newState: NavigationState
    ) {
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
                Log.d("NAVIGATION", "이미 말한 포인트 index $index → 건너뜀")
                continue
            }

            val coords = feature.geometry.coordinates
            if (coords !is Coordinates.Point) {
                Log.w("NAVIGATION", "⚠Point 타입이 아님 (index $index) → 건너뜀")
                continue
            }

            val targetLocation = Location("").apply {
                longitude = coords.lon
                latitude = coords.lat
            }

            val distance = location.distanceTo(targetLocation)
            Log.d("NAVIGATION", "index $index 도착지까지 거리: ${"%.2f".format(distance)}m")

            if (distance < 15f) {
                val description = feature.properties.description
                if (!description.isNullOrBlank()) {
                    Log.i("NAVIGATION", "🗣️ 안내 시작: $description")
                    lastSpokenIndex = index

                    speak(description) {
                        Log.i("NAVIGATION", " 안내 완료: index $index")
                        //lastSpokenIndex = index

                        // 🟡 도착 지점인지 확인
                        if (index == lastPointIndex) {
                            handleArrival()
                        }
                    }
                } else {
                    Log.w("NAVIGATION", "⚠ 안내 문구 없음 (index $index)")

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


    fun startAlignmentLoop(intervalMs: Long = 300L) {
        if (alignmentJob?.isActive == true) return
        alignmentJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive && navigationState.value is AligningDirection) {
                alignDirectionToFirstPoint()   // ← 아래 함수가 말해줌
                delay(intervalMs)
            }
        }
    }

    fun stopAlignmentLoop() {
        alignmentJob?.cancel()
        alignmentJob = null
    }

    private fun angleDelta(a: Float, b: Float): Float {
        var d = (a - b + 540f) % 360f - 180f
        return kotlin.math.abs(d)
    }

    private var guidanceJob: Job? = null

    fun startGuidanceLoop(intervalMs: Long = 500L) {
        if (guidanceJob?.isActive == true) return
        guidanceJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive && navigationState.value is GuidingNavigation) {
                guideTowardNextPoint()   // 다음 포인트 기준으로 헤딩 체크/안내
                delay(intervalMs)
            }
        }
    }


    private var lastSpeakAt = 0L
    private val speakIntervalMs = 2500L // TTS 남발 방지

    private fun guideTowardNextPoint() {
        val azimuth = stateViewModel.currentAzimuth.value ?: return
        val curr    = stateViewModel.currentLocation.value ?: return

        val next = nextPointByIndexSkipNear(curr) ?: return
        val p = next.geometry.coordinates as? Coordinates.Point ?: return
        val target = toLocation(p)

        val bearing = ((curr.bearingTo(target) + 360) % 360)
        val diff = ((bearing - azimuth + 540) % 360) - 180
        val absDiff = kotlin.math.abs(diff)
        val threshold = 20f


        if (isSpeaking) return

        // TTS 남발 방지(네 코드에 already 존재)
        if (System.currentTimeMillis() - lastSpeakAt < speakIntervalMs) return

        if (absDiff > threshold) {
            val dir = if (diff > 0) "오른쪽" else "왼쪽"
            speak("휴대폰을 $dir 으로 돌려주세요")
            lastSpeakAt = System.currentTimeMillis()
        }

    }
    fun stopGuidanceLoop() {
        guidanceJob?.cancel()
        guidanceJob = null
    }


    override fun onCleared() {
        super.onCleared()
        stopAlignmentLoop()     // 방향 맞추는 반복 끔
        stopGuidanceLoop()      // 안내용 반복 끔
        stopCompassTracking()   // 나침반 센서 끔
        stopTrackingLocation()  // GPS 끔
    }

    private fun toLocation(p: Coordinates.Point) = Location("").apply {
        latitude = p.lat
        longitude = p.lon
    }
    private fun nextPointByIndex(): Feature? {
        val points = stateViewModel.routePointFeatures.value ?: return null
        val nextIdx = lastSpokenIndex + 1
        return points.firstOrNull { (it.properties.pointIndex ?: -1) >= nextIdx }
    }


    private fun nextPointByIndexSkipNear(curr: Location, minDistM: Float = 8f): Feature? {
        val points = stateViewModel.routePointFeatures.value ?: return null
        val nextIdx = lastSpokenIndex + 1
        return points
            .filter { (it.properties.pointIndex ?: -1) >= nextIdx }
            .firstOrNull { f ->
                val p = f.geometry.coordinates as? Coordinates.Point ?: return@firstOrNull false
                val loc = toLocation(p)
                curr.distanceTo(loc) >= minDistM
            }
    }



}

package com.example.capstone_map.feature.navigation.viewmodel
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import android.os.Vibrator
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
import com.example.capstone_map.common.sharedVM.SharedNavigationViewModel
import com.example.capstone_map.common.voice.STTManager
import com.example.capstone_map.common.voice.TTSManager
import com.example.capstone_map.feature.navigation.GeometryDeserializer
import com.example.capstone_map.feature.navigation.sensor.NewCompassManager
import com.example.capstone_map.feature.navigation.state.AligningDirection
import com.example.capstone_map.feature.navigation.state.GuidingNavigation
import com.example.capstone_map.feature.navigation.state.NavigationError
import com.example.capstone_map.feature.navigation.state.NavigationFinished
import com.example.capstone_map.feature.navigation.state.NavigationState
import com.example.capstone_map.feature.navigation.state.RouteDataParsing
import com.example.capstone_map.feature.navigation.state.RouteSearching
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject


import android.os.VibrationEffect

import android.os.VibratorManager
import kotlin.math.sqrt


class NavigationViewModel(

    private val context: Context, // ✅ 추가
    private val stateViewModel: SharedNavigationViewModel,
    private val ttsManager: TTSManager,
    private val sttManager: STTManager



) : ViewModel() {


    @Volatile private var isSpeaking = false

    private var lastDirection: String? = null  // "오른쪽" or "왼쪽"

    // 완료 관련
    private var isAlignmentCompleted = false
    private var alignmentStableCount = 0
    private val REQUIRED_STABLE_CHECKS = 3  // 0.15초 × 3 = 0.45초

    private var isTransitioningToGuidance = false  // ← 추가




    private var locationTracker: LocationTracker? = null
    private var isTrackingLocation = false // 현재 추적 중인지 상태 저장
    val navigationState = MutableLiveData<NavigationState>()
    private val candidates = mutableListOf<String>() // 예시: 실제로는 POI 모델을 써야 함
    private var currentIndex = 0
    private var lastSpokenIndex = -1 // 중복 안내 방지용
    // private val compassManager = CompassManager(context)

    //  안전한 Vibrator 초기화
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }



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


    /**   첫 포인트와 방향 일치 여부 검사 함수
     *
     *
     * */

    fun checkAndGuideDirectionAlignment() {

        if (isTransitioningToGuidance) return

        val azimuth = stateViewModel.currentAzimuth.value ?: return
        val curr    = stateViewModel.currentLocation.value ?: return

        val next = nextPointByIndex() ?: return
        val p = next.geometry.coordinates as? Coordinates.Point ?: return
        val target = toLocation(p)

        val bearing = ((curr.bearingTo(target) + 360) % 360)
        val diff = ((bearing - azimuth + 540) % 360) - 180  // -180..+180 (최소 회전경로)
        val absDiff = kotlin.math.abs(diff)
        val threshold = 20f


        // 문제4 완료 후 재검증


        if (absDiff > threshold) { //방향이 아직 달라
            alignmentStableCount = 0  // 카운터 리셋
            Log.i("ALIGN_CHECK", "❌ 정렬 안됨 (absDiff > $threshold)")


            val currentDirection = if (diff > 0) "오른쪽" else "왼쪽"

            //  진동으로 거리 피드백
            val vibrationDuration = when {
                absDiff > 90f -> 50L    // 매우 틀림: 매우 약함
                absDiff > 60f -> 100L   // 많이 틀림: 약함
                absDiff > 40f -> 150L   // 중간: 중약
                absDiff > 25f -> 250L   // 조금 틀림: 중간
                else -> 350L            // 거의 맞음: 강함 (10~25도)
            }

            vibrate(vibrationDuration)


            if (currentDirection != lastDirection) {
                // 방향 변경 감지 → 즉시 알림
                Log.i("ALIGN_CHECK", "🔄 방향 변경: $lastDirection → $currentDirection (강제 발화)")
                forceSpeak(" $currentDirection 으로")
                lastDirection = currentDirection  //  현재 방향 저장
            } else {
                // 같은 방향 + 말 안 하는 중 → 알림
                speak(" $currentDirection 으로 ")
                // lastDirection은 이미 currentDirection과 같으므로 업데이트 불필요
            }

        // else: 같은 방향
        } else {

            Log.i("ALIGN_CHECK", "✅ 정렬됨! (absDiff ≤ $threshold)")


            //문제 4번  카운트
            alignmentStableCount++

            // 카운트증가



            if (alignmentStableCount >= REQUIRED_STABLE_CHECKS) {

                isTransitioningToGuidance = true  // ← 플래그 설정

                isAlignmentCompleted = true //정렬 완료 -> 정렬된 상태에서 forceSpeak그만

                // 완료 패턴
                vibratePattern(longArrayOf(0, 300, 150, 300, 150, 300))

                forceSpeak("정렬 완료") {
                    Log.i("ALIGN_CHECK", "TTS 완료 콜백 → GuidingNavigation 전환")


                    updateState(GuidingNavigation)


                    // 전환 완료 후 초기화
                    //isTransitioningToGuidance = false

                }
                alignmentStableCount = 0
                lastDirection = null
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
                    Log.d("TRACKING", " 위치 갱신됨 → ${location.latitude}, ${location.longitude}")
                    stateViewModel.currentLocation.postValue(location)
                    checkAndSpeakNextPoint(location)
                }

                override fun onLocationAccuracyChanged(accuracy: Float) {
                    Log.d("TRACKING", " 정확도 변경됨 → $accuracy")
                }

                override fun onGPSSignalWeak() {
                    Log.w("TRACKING", "️ GPS 신호 약함")
                }

                override fun onGPSSignalRestored() {
                    Log.d("TRACKING", " GPS 신호 정상 복구")
                }
            })
        }

        locationTracker?.startTracking()
        Log.i("TRACKING", " 위치 추적 시작됨")
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

        Log.d("NAVIGATION", " 현재 위치: (${location.latitude}, ${location.longitude})")

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

            if (distance < 7f) {
                val description = feature.properties.description
                if (!description.isNullOrBlank()) {
                    Log.i("NAVIGATION", " 안내 시작: $description" + isSpeaking)
                    lastSpokenIndex = index

                    speak("잠시후" + description) {
                        Log.i("NAVIGATION", " 안내 완료: index $index")
                        //lastSpokenIndex = index

                        //  도착 지점인지 확인
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

        Log.d("TTS", "speak() 호출: \"$text\"")
        Log.d("TTS", "isSpeaking = $isSpeaking")

        if (isSpeaking) return  // ← 말중이면 return

        isSpeaking = true
        ttsManager.speak(text, object : TTSManager.OnSpeakCallback {
            override fun onStart() {}
            override fun onDone() {
                isSpeaking = false
                onDone?.invoke()
            }
            override fun onError() {
                isSpeaking = false
            }
        })
    }

    // 강제 발화 (isSpeaking 체크 없음!)
    private fun forceSpeak(text: String, onComplete: (() -> Unit)? = null) {
        ttsManager.stop()  // 기존 중단
        isSpeaking = false  // 플래그 초기화

        // 새로 시작 (speak() 호출하면 이제 통과됨)
        speak(text, onComplete)
    }


    private fun handleArrival() {
        Log.i("NAVIGATION", " 목적지 도착 처리 시작")
        speak("목적지 주변에 도착했습니다. 안내를 종료합니다.") {
            updateState(NavigationFinished)
        }
    }




    fun startAlignmentLoop(intervalMs: Long = 300L) {
        Log.e("LOOP_DEBUG", "========== startAlignmentLoop 시작 ==========")

        if (alignmentJob?.isActive == true) {
            Log.e("LOOP_DEBUG", "이미 실행 중")
            return

        }
        alignmentJob = viewModelScope.launch(Dispatchers.Main) {

            while (isActive && navigationState.value is AligningDirection && !isTransitioningToGuidance) {
                Log.d("LOOP_DEBUG", "체크 함수 호출 | isActive=$isActive | state=${navigationState.value?.javaClass?.simpleName}")
                isAlignmentCompleted
                checkAndGuideDirectionAlignment()   // ← 아래 함수가 말해줌
                delay(intervalMs) // delay는 왜있는가 -> 센서 갱신보다 빠른 Check는 의미없이
                //리소스만 잡아먹기 때문
            }
            Log.e("LOOP_DEBUG", "루프 종료")
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
        val threshold = 30f


        // ✅ 먼저 체크
        if (isSpeaking) {
            Log.d("각도측정", "TTS 중이므로 스킵")
            return
        }

        // ✅ 시간 체크
        val now = System.currentTimeMillis()
        if (now - lastSpeakAt < speakIntervalMs) {
            Log.d("각도측정", "TTS 쿨타임 중 (${now - lastSpeakAt}ms)")
            return
        }

        if (isSpeaking) return

        // TTS 남발 방지(네 코드에 already 존재)
        if (System.currentTimeMillis() - lastSpeakAt < speakIntervalMs) return

        Log.d("각도측정", "absdiff = $absDiff" )


        if (absDiff > threshold) {


//            val dir = if (diff > 0) "오른쪽" else "왼쪽"
//            speak("휴대폰을 $dir 으로 돌려주세요")
//            lastSpeakAt = System.currentTimeMillis()
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



    private fun vibrate(duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun vibratePattern(pattern: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }





    class KalmanLatLong(private val qMetresPerSecond: Float) {

        private val minAccuracy = 1f
        private var timeStampMillis: Long = 0
        private var lat: Double = 0.0
        private var lng: Double = 0.0
        private var variance: Float = -1f

        val latitude: Double get() = lat
        val longitude: Double get() = lng
        val accuracy: Float get() = sqrt(variance)

        fun setState(lat: Double, lng: Double, accuracy: Float, timeStamp: Long) {
            this.lat = lat
            this.lng = lng
            this.variance = maxOf(accuracy, minAccuracy) * maxOf(accuracy, minAccuracy)
            this.timeStampMillis = timeStamp
        }

        fun process(latMeasurement: Double, lngMeasurement: Double, accuracy: Float, timeStamp: Long) {
            val accuracyClamped = maxOf(accuracy, minAccuracy)

            if (variance < 0) {
                // 초기화
                setState(latMeasurement, lngMeasurement, accuracyClamped, timeStamp)
                return
            }

            val timeInc = timeStamp - timeStampMillis
            if (timeInc > 0) {
                // 시간에 따른 불확실성 증가
                variance += timeInc * qMetresPerSecond * qMetresPerSecond / 1000f
                timeStampMillis = timeStamp
            }

            // Kalman gain
            val k = variance / (variance + accuracyClamped * accuracyClamped)

            // 업데이트
            lat += k * (latMeasurement - lat)
            lng += k * (lngMeasurement - lng)

            // 공분산 업데이트
            variance = (1 - k) * variance
        }
    }



}

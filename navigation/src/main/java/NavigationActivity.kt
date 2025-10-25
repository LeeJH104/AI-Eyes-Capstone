//package com.example.capstone_map.feature
//
//import android.os.Bundle
//import android.widget.Toast
//import androidx.activity.result.ActivityResultLauncher
//import androidx.appcompat.app.AppCompatActivity
//import com.example.capstone_map.feature.destination.state.AwaitingDestinationInput
//import com.example.capstone_map.common.permission.PermissionHelper
//import com.example.capstone_map.common.di.NavigationAssembler
//import com.example.capstone_map.common.input.NavigationGestureBinder
//import com.example.capstone_map.common.map.TMapInitializer
//import com.example.capstone_map.common.permission.micAndGpsPermissions
//import com.example.capstone_map.common.permission.registerMicAndGpsPermissionLauncher
//import com.example.capstone_map.common.route.MapRouteDisplayer
//import com.example.capstone_map.common.stateChecker.renderNavigationState
//import com.example.capstone_map.common.voice.STTManager
//import com.example.capstone_map.common.voice.TTSManager
////import com.example.capstone_map.databinding.ActivityNavigationBinding
//import com.example.capstone_map.feature.destination.viewmodel.DestinationViewModel
//import com.example.capstone_map.feature.navigation.viewmodel.NavigationViewModel
//
//import com.skt.Tmap.TMapView
//class NavigationActivity : AppCompatActivity() {
//
//    private var firstFix = false   // 첫 GPS 수신 때만 지도 중심 이동
//
//    private lateinit var ttsManager: TTSManager
//    private lateinit var sttManager: STTManager
//    private var tMapView: TMapView? = null
//
//    private lateinit var assembler: NavigationAssembler
//    private lateinit var destinationViewModel: DestinationViewModel // 타입 명시 필요
//    lateinit var multiPermissionLauncher: ActivityResultLauncher<Array<String>>
//    private lateinit var binding: ActivityNavigationBinding
//
//
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        //1. 화면 바인딩
//        binding = ActivityNavigationBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        //setContentView(R.layout.activity_main)
//
//
//        //2. tmap 초기화
//
//        tMapView = TMapInitializer.setupTMapView(this, binding.linearLayoutTmap)
//
//        tMapView?.apply {
//            setIconVisibility(true)   // 내 위치 아이콘 보이게
//            setTrackingMode(true)     // 지도 자동 따라오기
//            setZoomLevel(17)          // (선택) 적당한 확대 레벨
//        }
//
//        // 3.  mapdisplayer 만들기 -> tmap에 경로 시각화
//        val displayer = MapRouteDisplayer(tMapView!!)
//
//
//        //4. 어셈블러 생성 + StateViewModel(sharedViewmodel)에서 route 위치값을 주시하고있다가
//        // 바뀌면 point들을 가지고 tmap에 경로 그려주는 함수 실행
//        assembler = NavigationAssembler(this, this)
//
//        val stateVM = assembler.sharedNavigationViewModel
//        stateVM.routePointFeatures.observe(this) { points ->
//            if (!points.isNullOrEmpty()) {
//                displayer.displayFromPointFeatures(points)
//            }
//        }
//        destinationViewModel = assembler.destinationViewModel
//
//
//        // 5. sharedViewModel에서 현재 위치가 바뀔 때마다 지도 업데이트
//        stateVM.currentLocation.observe(this) { loc ->
//            //  TMap은 보통 (lon, lat) 순서
//            tMapView?.setLocationPoint(loc.longitude, loc.latitude)
//
//            // 첫 수신 때만 지도 중심 이동
//            if (!firstFix) {
//                tMapView?.setCenterPoint(loc.longitude, loc.latitude)
//                firstFix = true
//            }
//        }
//
//
//
//        // 6.(선택) 네비 상태에 따라 지도 트래킹 on/off
//        stateVM.navState.observe(this) { state ->
//            val trackingOn = state is com.example.capstone_map.feature.navigation.state.AligningDirection ||
//                    state is com.example.capstone_map.feature.navigation.state.GuidingNavigation
//            tMapView?.setTrackingMode(trackingOn)
//        }
//
//
//        // 7. 권한 런처 등록 및 요청
//        //간단하게처리
//        multiPermissionLauncher = registerMicAndGpsPermissionLauncher { micGranted, gpsGranted ->
//            if (micGranted && gpsGranted) {
//                startApp()
//            } else {
//                Toast.makeText(this, "권한이 필요합니다. 앱을 종료합니다.", Toast.LENGTH_LONG).show()
//                finish()
//            }
//        }
//
//        multiPermissionLauncher.launch(micAndGpsPermissions)
//
//
//
//        // 현재 state textview에 적기
//        val stateViewModel = assembler.sharedNavigationViewModel
//        stateViewModel.navState.observe(this) { state ->
//            renderNavigationState(binding.resultText, state)
//        }
//
//
//
//    }
//
//
//    private fun startApp() {
//        ttsManager = assembler.ttsManager
//        sttManager = assembler.sttManager
//
//
//        //1. naviViewmodel을 가져와서 gps추적 시작
//        val navViewModel = assembler.getViewModel(NavigationViewModel::class)
//        navViewModel.startTrackingLocation()
//
//
//        // 2.  DestinationVM 의 상태를 AwaitingDestinationInput 으로 변경 -> VM순환의 시작
//        destinationViewModel.updateState(AwaitingDestinationInput)
//
//        // 3.제스처 스와이프로 바인더 연결 (오른쪽=primary, 왼쪽=secondary, 아래=tertiary)
//        NavigationGestureBinder(
//            activity = this,
//            targetView = binding.gestureOverlay, // 전체 화면에 제스처 적용 (원하면 특정 뷰로 교체 가능)
//            stateProvider = { assembler.sharedNavigationViewModel.navState.value },
//            desViewModel = assembler.destinationViewModel,
//            poiViewModel = assembler.poiSearchViewModel,
//            navViewModel = assembler.navigationViewModel
//        )
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        ttsManager.shutdown()
//        sttManager.destroy()
//    }
//
//
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int, permissions: Array<String>, grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        PermissionHelper.handlePermissionResult(this, requestCode, permissions, grantResults)
//    }
//}

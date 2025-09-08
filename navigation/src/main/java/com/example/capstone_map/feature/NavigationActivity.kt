package com.example.capstone_map.feature

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.example.capstone_map.common.input.NavigationInputBinder
import com.example.capstone_map.feature.destination.state.AwaitingDestinationInput
import com.example.capstone_map.common.permission.PermissionHelper
import com.example.capstone_map.common.di.NavigationAssembler
import com.example.capstone_map.common.map.TMapInitializer
import com.example.capstone_map.common.permission.micAndGpsPermissions
import com.example.capstone_map.common.permission.registerMicAndGpsPermissionLauncher
import com.example.capstone_map.common.route.MapRouteDisplayer
import com.example.capstone_map.common.stateChecker.renderNavigationState
import com.example.capstone_map.common.voice.STTManager
import com.example.capstone_map.common.voice.TTSManager
import com.example.capstone_map.databinding.ActivityNavigationBinding
import com.example.capstone_map.feature.destination.viewmodel.DestinationViewModel
import com.example.capstone_map.feature.navigation.viewmodel.NavigationViewModel

import com.skt.Tmap.TMapView
class NavigationActivity : AppCompatActivity() {

    private var firstFix = false   // 첫 GPS 수신 때만 지도 중심 이동

    private lateinit var ttsManager: TTSManager
    private lateinit var sttManager: STTManager
    private var tMapView: TMapView? = null

    private lateinit var assembler: NavigationAssembler
    private lateinit var destinationViewModel: DestinationViewModel // 타입 명시 필요
    lateinit var multiPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var binding: ActivityNavigationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setContentView(R.layout.activity_main)


        //tmap 초기화
//        val tmapContainer: LinearLayout = findViewById(R.id.linearLayoutTmap)
        tMapView = TMapInitializer.setupTMapView(this, binding.linearLayoutTmap)

        tMapView?.apply {
            setIconVisibility(true)   // 내 위치 아이콘 보이게
            setTrackingMode(true)     // 지도 자동 따라오기
            setZoomLevel(17)          // (선택) 적당한 확대 레벨
        }

        val displayer = MapRouteDisplayer(tMapView!!)



//        tMapView = TMapInitializer.setupTMapView(this, tmapContainer)

        // 어셈블러 및 뷰모델 초기화
        assembler = NavigationAssembler(this, this)
        assembler.stateViewModel.routePointFeatures.observe(this) { points ->
            if (!points.isNullOrEmpty()) {
                displayer.displayFromPointFeatures(points)
            }
        }
        destinationViewModel = assembler.destinationViewModel

        val stateVM = assembler.stateViewModel
        // 현재 위치가 바뀔 때마다 지도 업데이트
        stateVM.currentLocation.observe(this) { loc ->
            // ⚠️ TMap은 보통 (lon, lat) 순서
            tMapView?.setLocationPoint(loc.longitude, loc.latitude)

            // 첫 수신 때만 지도 중심 이동
            if (!firstFix) {
                tMapView?.setCenterPoint(loc.longitude, loc.latitude)
                firstFix = true
            }
        }

        // (선택) 네비 상태에 따라 지도 트래킹 on/off
        stateVM.navState.observe(this) { state ->
            val trackingOn = state is com.example.capstone_map.feature.navigation.state.AligningDirection ||
                    state is com.example.capstone_map.feature.navigation.state.GuidingNavigation
            tMapView?.setTrackingMode(trackingOn)
        }


        // 권한 런처 등록 및 요청
        //간단하게처리
        multiPermissionLauncher = registerMicAndGpsPermissionLauncher { micGranted, gpsGranted ->
            if (micGranted && gpsGranted) {
                startApp()
            } else {
                Toast.makeText(this, "권한이 필요합니다. 앱을 종료합니다.", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        multiPermissionLauncher.launch(micAndGpsPermissions)



        // 현재 state textview에 적기
        val stateViewModel = assembler.stateViewModel
        stateViewModel.navState.observe(this) { state ->
            renderNavigationState(binding.resultText, state)
        }



    }


    private fun startApp() {
        ttsManager = assembler.ttsManager
        sttManager = assembler.sttManager

        val navViewModel = assembler.getViewModel(NavigationViewModel::class)
        navViewModel.startTrackingLocation()


        destinationViewModel.updateState(AwaitingDestinationInput)

        NavigationInputBinder(
            activity = this,
            desViewModel = assembler.destinationViewModel,
            poiViewModel = assembler.poiSearchViewModel,
            navViewModel = assembler.navigationViewModel,
            stateProvider = { assembler.stateViewModel.navState.value },
            primary = binding.gesture1,
            secondary = binding.gesture2,
            tertiary = binding.gesture3
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        sttManager.destroy()
    }



    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.handlePermissionResult(this, requestCode, permissions, grantResults)
    }
}

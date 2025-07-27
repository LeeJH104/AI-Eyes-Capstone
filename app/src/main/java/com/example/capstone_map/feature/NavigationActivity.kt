package com.example.capstone_map.feature
import android.Manifest
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.capstone_map.R
import com.example.capstone_map.common.input.NavigationInputBinder
import com.example.capstone_map.feature.destination.state.AwaitingDestinationInput
import com.example.capstone_map.common.permission.PermissionHelper
import com.example.capstone_map.common.di.NavigationAssembler
import com.example.capstone_map.common.map.TMapInitializer
import com.example.capstone_map.common.permission.micAndGpsPermissions
import com.example.capstone_map.common.permission.registerMicAndGpsPermissionLauncher
import com.example.capstone_map.common.permission.registerPermissionLauncher
import com.example.capstone_map.common.voice.STTManager
import com.example.capstone_map.common.voice.TTSManager
import com.example.capstone_map.feature.destination.viewmodel.DestinationViewModel
import com.example.capstone_map.feature.navigation.viewmodel.NavigationViewModel
import com.skt.Tmap.TMapView
class NavigationActivity : AppCompatActivity() {

    private lateinit var ttsManager: TTSManager
    private lateinit var sttManager: STTManager
    private var tMapView: TMapView? = null

    private lateinit var assembler: NavigationAssembler
    private lateinit var destinationViewModel: DestinationViewModel // 타입 명시 필요
    lateinit var multiPermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //tmap 초기화
        val tmapContainer: LinearLayout = findViewById(R.id.linearLayoutTmap)
        tMapView = TMapInitializer.setupTMapView(this, tmapContainer)

        // 어셈블러 및 뷰모델 초기화
        assembler = NavigationAssembler(this, this)
        destinationViewModel = assembler.destinationViewModel

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
    }


    private fun startApp() {
        ttsManager = assembler.ttsManager
        sttManager = assembler.sttManager

        val navViewModel = assembler.getViewModel(NavigationViewModel::class)
        navViewModel.startTrackingLocation()


        destinationViewModel.updateState(AwaitingDestinationInput)

        NavigationInputBinder(
            activity = this,
            desViewModel = destinationViewModel,
            poiViewModel = assembler.poiSearchViewModel,
            stateProvider = { assembler.stateViewModel.navState.value },
            primaryButtonId = R.id.gesture1,
            secondaryButtonId = R.id.gesture2,
            tertiaryButtonId = R.id.gesture3
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

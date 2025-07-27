package com.example.capstone_map.common.di


import android.app.Activity


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.capstone_map.feature.destination.viewmodel.DestinationViewModel
import com.example.capstone_map.feature.destination.viewmodel.factory.DestinationViewModelFactory
import com.example.capstone_map.feature.poisearch.viewmodel.POISearchViewModel
import com.example.capstone_map.feature.poisearch.viewmodel.factory.POISearchViewModelFactory
import com.example.capstone_map.common.location.LocationFetcher
import com.example.capstone_map.common.viewmodel.NavigationStateViewModel
import com.example.capstone_map.common.voice.STTManager
import com.example.capstone_map.common.voice.TTSManager
import com.google.android.gms.location.LocationServices
import kotlin.reflect.KClass

class NavigationAssembler(
    private val activity: Activity,
    private val owner: ViewModelStoreOwner
) {



    // 공통 상태를 가지는 뷰모델
    val stateViewModel: NavigationStateViewModel by lazy {
        ViewModelProvider(owner)[NavigationStateViewModel::class.java]
    }

    private val _ttsManager: TTSManager by lazy {
        TTSManager(
            activity
        )
    }
    private val _sttManager: STTManager by lazy {
        STTManager(
            activity
        )
    }

    val ttsManager: TTSManager get() = _ttsManager
    val sttManager: STTManager get() = _sttManager

    private val _locationFetcher: LocationFetcher by lazy {
        LocationFetcher(activity, LocationServices.getFusedLocationProviderClient(activity))
    }


    /**
     * ("이코드가 뭔지 나중에 제대로 공부하자 ")
     */
    // ViewModel 팩토리 모음 (확장 쉽게)
    private val factoryMap: Map<KClass<out ViewModel>, ViewModelProvider.Factory> = mapOf(

        POISearchViewModel::class to POISearchViewModelFactory(stateViewModel, _locationFetcher,ttsManager,sttManager),

        // 그 다음 DestinationViewModel에 주입
        DestinationViewModel::class to DestinationViewModelFactory(
            stateViewModel,
            getViewModel(POISearchViewModel::class),
            ttsManager,
            sttManager
        )



    )


    // generic하게 ViewModel을 가져오는 함수
    @Suppress("UNCHECKED_CAST")
    fun <T : ViewModel> getViewModel(vmClass: KClass<T>): T {
        //("이코드가 뭔지 나중에 제대로 공부하자 ")
        val factory = factoryMap[vmClass] ?: throw IllegalArgumentException("Factory not found")
        return ViewModelProvider(owner, factory)[vmClass.java]
    }




    // 편의상 개별 접근자도 지원 (필요 시)
    val destinationViewModel: DestinationViewModel by lazy {
        getViewModel(DestinationViewModel::class)
    }

    val poiSearchViewModel: POISearchViewModel by lazy {
        getViewModel(POISearchViewModel::class)
    }


//    val confirmationViewModel: ConfirmationViewModel by lazy {
//        getViewModel(ConfirmationViewModel::class)
//    }
    
    
    
}


//
//여러 개의 객체(TTS, STT, LocationFetcher, ViewModel 등)를 한 번에 초기화.
//
//stateViewModel을 공통 상태로 관리하여 다른 ViewModel들이 공유하도록 연결.
//
//ViewModel을 팩토리로 생성해서 DI 구조 유지.
//
//외부에서는 assembler.destinationViewModel, assembler.poiSearchViewModel처럼 간단하게 사용 가능.
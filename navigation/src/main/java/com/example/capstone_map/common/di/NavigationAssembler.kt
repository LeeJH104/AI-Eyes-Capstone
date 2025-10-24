package com.example.capstone_map.common.di


import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.capstone_map.feature.destination.viewmodel.DestinationViewModel
import com.example.capstone_map.feature.destination.viewmodel.factory.DestinationViewModelFactory
import com.example.capstone_map.feature.poisearch.viewmodel.POISearchViewModel
import com.example.capstone_map.feature.poisearch.viewmodel.factory.POISearchViewModelFactory
import com.example.capstone_map.common.location.oncefetcher.LocationFetcher
import com.example.capstone_map.common.sharedVM.SharedNavigationViewModel
import com.example.capstone_map.common.voice.STTManager
import com.example.capstone_map.common.voice.TTSManager
import com.example.capstone_map.feature.navigation.viewmodel.NavigationViewModel
import com.example.capstone_map.feature.navigation.viewmodel.factory.NavigationViewModelFactory
import com.google.android.gms.location.LocationServices
import kotlin.reflect.KClass

class NavigationAssembler(
    private val activity: Activity,
    private val owner: ViewModelStoreOwner
) {



    //1. shanredViewmodel 모든 viewmodel이 공유하는 데이터 저장소
    val sharedNavigationViewModel: SharedNavigationViewModel by lazy {
        ViewModelProvider(owner)[SharedNavigationViewModel::class.java]
    }

    //2. 필요한 인스턴스 생성
    private val _ttsManager by lazy { TTSManager(activity) }
    private val _sttManager by lazy { STTManager(activity) }

    private val _locationFetcher by lazy {
        LocationFetcher(activity, LocationServices.getFusedLocationProviderClient(activity))
    }
    val ttsManager get() = _ttsManager
    val sttManager get() = _sttManager



    //  3. 뷰모델 "제공자" 맵: 호출 시점에 생성
    private val providers: Map<KClass<out ViewModel>, () -> ViewModel> = mapOf(

        NavigationViewModel::class to {
            ViewModelProvider(
                owner,
                NavigationViewModelFactory(
                    context = activity,
                    stateViewModel = sharedNavigationViewModel,
                    ttsManager = ttsManager,
                    sttManager = sttManager
                )
            )[NavigationViewModel::class.java]
        },

        POISearchViewModel::class to {
            val navVM = getViewModel(NavigationViewModel::class)   // ← 이제 안전 (지연 호출)
            ViewModelProvider(
                owner,
                POISearchViewModelFactory(
                    sharedNavigationViewModel,
                    navVM,
                    _locationFetcher,
                    ttsManager,
                    sttManager
                )
            )[POISearchViewModel::class.java]
        },

        DestinationViewModel::class to {
            val poiVM = getViewModel(POISearchViewModel::class)
            ViewModelProvider(
                owner,
                DestinationViewModelFactory(
                    sharedNavigationViewModel,
                    poiVM,
                    ttsManager,
                    sttManager
                )
            )[DestinationViewModel::class.java]
        }
    )




    //4
    @Suppress("UNCHECKED_CAST")
    fun <T : ViewModel> getViewModel(vmClass: KClass<T>): T {
        val provider = providers[vmClass]
            ?: error("No provider for $vmClass. Did you register it in providers?")
        return provider.invoke() as T
    }


    //by lazy = “정말 필요할 때 처음으로 만들고, 그 다음부터는 이미 만든 걸 계속 써라”
    val destinationViewModel by lazy { getViewModel(DestinationViewModel::class) }
    val poiSearchViewModel  by lazy { getViewModel(POISearchViewModel::class) }

    val navigationViewModel by lazy { getViewModel(NavigationViewModel::class) }


}

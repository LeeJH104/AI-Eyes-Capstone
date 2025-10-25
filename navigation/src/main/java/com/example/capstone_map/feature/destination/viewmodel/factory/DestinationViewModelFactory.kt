package com.example.capstone_map.feature.destination.viewmodel.factory


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.capstone_map.feature.destination.viewmodel.DestinationViewModel
import com.example.capstone_map.common.sharedVM.SharedNavigationViewModel
import com.example.capstone_map.feature.poisearch.viewmodel.POISearchViewModel
import com.example.capstone_map.common.voice.STTManager
import com.example.capstone_map.common.voice.TTSManager

class DestinationViewModelFactory(
    private val stateViewModel: SharedNavigationViewModel,
    private val poiSearchViewModel: POISearchViewModel, // ✅ 이거 추가
    private val ttsManager: TTSManager,
    private val sttManager: STTManager,


    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DestinationViewModel(stateViewModel,poiSearchViewModel, ttsManager, sttManager) as T
    }
}

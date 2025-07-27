package com.example.capstone_map.feature.poisearch.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.capstone_map.feature.poisearch.viewmodel.POISearchViewModel
import com.example.capstone_map.common.location.LocationFetcher
import com.example.capstone_map.common.viewmodel.NavigationStateViewModel
import com.example.capstone_map.common.voice.STTManager
import com.example.capstone_map.common.voice.TTSManager

class POISearchViewModelFactory(
    private val stateViewModel: NavigationStateViewModel,
    private val locationFetcher: LocationFetcher,
    private val ttsManager: TTSManager,
    private val sttManager: STTManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return POISearchViewModel(stateViewModel,locationFetcher,ttsManager,sttManager) as T
    }
}
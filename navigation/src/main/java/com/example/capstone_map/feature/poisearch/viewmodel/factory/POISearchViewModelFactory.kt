package com.example.capstone_map.feature.poisearch.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.capstone_map.feature.poisearch.viewmodel.POISearchViewModel
import com.example.capstone_map.common.location.oncefetcher.LocationFetcher
import com.example.capstone_map.common.sharedVM.SharedNavigationViewModel
import com.example.capstone_map.common.voice.STTManager
import com.example.capstone_map.common.voice.TTSManager
import com.example.capstone_map.feature.navigation.viewmodel.NavigationViewModel

class POISearchViewModelFactory(
    private val stateViewModel: SharedNavigationViewModel,
    private val navigationViewModel : NavigationViewModel,
    private val locationFetcher: LocationFetcher,
    private val ttsManager: TTSManager,
    private val sttManager: STTManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return POISearchViewModel(stateViewModel,navigationViewModel,locationFetcher,ttsManager,sttManager) as T
    }
}
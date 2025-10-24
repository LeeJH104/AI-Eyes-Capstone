package com.example.capstone_map.feature.navigation.viewmodel.factory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.capstone_map.common.sharedVM.SharedNavigationViewModel
import com.example.capstone_map.common.voice.STTManager
import com.example.capstone_map.common.voice.TTSManager
import com.example.capstone_map.feature.navigation.viewmodel.NavigationViewModel

class NavigationViewModelFactory(
    private val context: Context,
    private val stateViewModel: SharedNavigationViewModel,
    private val ttsManager: TTSManager,
    private val sttManager: STTManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NavigationViewModel(context, stateViewModel, ttsManager, sttManager) as T
    }
}

package com.example.capstone_map.feature.destination.viewmodel

import androidx.lifecycle.ViewModel
import com.example.capstone_map.feature.destination.state.AskingDestinationConfirmation
import com.example.capstone_map.feature.destination.state.AwaitingDestinationInput
import com.example.capstone_map.feature.destination.state.DestinationRight
import com.example.capstone_map.feature.destination.state.DestinationWrong
import com.example.capstone_map.feature.destination.state.ListeningForDestination
import com.example.capstone_map.feature.destination.state.DestinationState
import com.example.capstone_map.feature.destination.state.SearchingDestination
import com.example.capstone_map.feature.poisearch.state.POISearchState
import com.example.capstone_map.feature.poisearch.state.StartingSearch

import com.example.capstone_map.common.voice.STTManager
import com.example.capstone_map.common.voice.TTSManager

import com.example.capstone_map.common.sharedVM.SharedNavigationViewModel
import com.example.capstone_map.feature.poisearch.viewmodel.POISearchViewModel

class DestinationViewModel(
    private val stateViewModel: SharedNavigationViewModel,
    private val poiSearchViewModel: POISearchViewModel, // ✅ 이거 추가
    private val ttsManager: TTSManager,
    private val sttManager: STTManager
) : ViewModel() {


    fun updateState(state: DestinationState) {
        stateViewModel.navState.value = state
        stateViewModel.setNavState("DEST", state)

        state.handle(this)
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) { //함수 넘겨도되고 안 넘겨도돼
        ttsManager.speak(text, object : TTSManager.OnSpeakCallback {
            override fun onStart() {}
            override fun onDone() {
                onDone?.invoke()
            }
        })
    }


    fun getCurrentPOISearchState(): POISearchState? {
        return stateViewModel.navState.value as? POISearchState
    }




    //행동함수
    fun getDestinationText(): String {
        return stateViewModel.destinationText.value ?: "알 수 없음"
    }

    fun startListeningForDestination() {
        updateState(ListeningForDestination)
    }

    fun listenToDestination() {
        sttManager.listen(object : STTManager.OnResultCallback {
            override fun onResult(result: String) { //목적지를 들은후 text저장 + 목적지 확인하는 상태로 업데이트
                stateViewModel.destinationText.value = result
                updateState(AskingDestinationConfirmation)
            }

            override fun onError(errorMessage: String) {
                speak("죄송합니다. 문제가 발생했습니다.") { //(제스쳐 다시실행해주세요)
                    updateState(AwaitingDestinationInput)
                }
            }
        })
    }


//3단계 목적지 확인
    fun confirmDestination() {

        updateState(DestinationRight)
    }

    fun denyDestination() {
        stateViewModel.destinationText.postValue("")  // 이게맞냐? 애매한 백그라운드 처리
        updateState(DestinationWrong)
    }

//

    fun fetchingCandidates () {
        updateState(SearchingDestination)

    }


    fun restartDestinationInput() {
        updateState(ListeningForDestination)
    }


    // 목적지 검색을위한

    fun fetchCurrentLocation() {
        poiSearchViewModel.updateState(StartingSearch) // 👈 여기가 핵심
    }



}

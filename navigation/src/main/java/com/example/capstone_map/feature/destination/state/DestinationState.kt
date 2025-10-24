package com.example.capstone_map.feature.destination.state

import com.example.capstone_map.feature.destination.viewmodel.DestinationViewModel
import com.example.capstone_map.common.state.BaseState

sealed interface DestinationState : BaseState<DestinationViewModel>


// 1단계: 목적지 요청
object AwaitingDestinationInput : DestinationState {
    override fun handle(viewModel: DestinationViewModel) {
        viewModel.speak("스와이프를 오른쪽으로 넘긴후 목적지를 이야기해주세요")
    }

    override fun onPrimaryInput(viewModel: DestinationViewModel) {
        //listening상태로 바꾸는 함수로
        viewModel.startListeningForDestination()
    }
}


// 2단계: 목적지 듣기
object ListeningForDestination : DestinationState {
    override fun handle(viewModel: DestinationViewModel) {
        viewModel.listenToDestination() //목적지를 들어
        //STT 실행후 목적지 text를 stateViewmodel에 저장
    }
}

// 3단계: 확인 요청
object AskingDestinationConfirmation : DestinationState {
    override fun handle(viewModel: DestinationViewModel) {
        val destination = viewModel.getDestinationText()
        viewModel.speak("말씀하신 목적지는 $destination 맞나요? 맞다면 오른쪽 , 아니라면 왼쪽 스와이프")
        // 이 후 판단은 버튼 누름에 따라 수행
    }
    override fun onPrimaryInput(viewModel: DestinationViewModel) { //해당 목적지가 맞아요
        viewModel.confirmDestination()
        //상태를 confirm에서 확정(Right)으로 바꾸고
    }

    override fun onSecondaryInput(viewModel: DestinationViewModel) { //해당 목적지가 아니에요
        viewModel.denyDestination()
        // 저장되어있는 목적지를 지우고
        //상태를 wrong으로 바꿈
    }
}

//4-1 해당 목적지가 맞아요

object DestinationRight : DestinationState {
    override fun handle(viewModel: DestinationViewModel) {
        //상태를 후보지검색으로 변경하고 , 목적지를 확정함
        //함수적는곳
        viewModel.fetchingCandidates()
    }


}


// 4-2 목적지 틀렸을 경우
object DestinationWrong : DestinationState {
    override fun handle(viewModel: DestinationViewModel) {
        viewModel.speak("다시 말씀해주세요.") {
            //함수적는곳
            viewModel.restartDestinationInput()
        }
    }
}

// 2 . 목적지 검색
//
object SearchingDestination : DestinationState {
    override fun handle(viewModel: DestinationViewModel){
        viewModel.fetchCurrentLocation()
    }


}

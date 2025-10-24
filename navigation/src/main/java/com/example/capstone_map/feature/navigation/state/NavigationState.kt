package com.example.capstone_map.feature.navigation.state

import androidx.lifecycle.viewModelScope
import com.example.capstone_map.common.state.BaseState
import com.example.capstone_map.feature.navigation.viewmodel.NavigationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 길안내(네비게이션) 관련 상태를 표현하는 sealed class
sealed interface NavigationState : BaseState<NavigationViewModel>



/** 1.길안내 시작 준비: 목적지/경로가 확정된 뒤 안내 시작 직전 */
object StartNavigationPreparation : NavigationState {
    override fun handle(viewModel: NavigationViewModel) {
        viewModel.prepareNavigation()

    }
}

/** 2.경로 검색중 */
object RouteSearching : NavigationState{
    override fun handle(viewModel: NavigationViewModel) {
        viewModel.requestRouteToDestination()
    }
}

/**3. 데이터 파싱중 */
object RouteDataParsing : NavigationState{
    override fun handle(viewModel: NavigationViewModel) {
        viewModel.parseRawJson()
    }
}


/** 3. 올바른 방향(방위) 정렬 유도 중 (예: "휴대폰을 북쪽으로 돌려주세요") */

object AligningDirection : NavigationState {
    override fun handle(viewModel: NavigationViewModel) {

        viewModel.startTrackingLocation()  // tracking 시작



        viewModel.startCompassTracking() //1. 나침반센서 시작
        viewModel.startAlignmentLoop(150L) //2. 0.3초마다 방향 체크

// 중복호출
//        viewModel.viewModelScope.launch(Dispatchers.Main) {
//            delay(500) // 위치/방위값 들어올 시간 살짝 기다림
//            viewModel.checkAndGuideDirectionAlignment()
//        }

    }
}
///** 4. 경로 요약(거리/시간/주요 경유지 등) 음성 안내 중 */
//data class AnnounceRouteSummary(
//    val summaryText: String // 예: "총 거리 500m, 예상 시간 8분입니다."
//) : NavigationState{}

/** 실제 길안내 진행 중 (경로 안내, 턴 바이 턴 등) */
object GuidingNavigation : NavigationState {
    override fun handle(viewModel: NavigationViewModel) {

        viewModel.stopAlignmentLoop()
        viewModel.startGuidanceLoop(500L)   // ← 도착할 때까지 계속
    }
}

/** 길안내 종료 또는 중단 */
object NavigationFinished : NavigationState {
    override fun handle(viewModel: NavigationViewModel) {
        viewModel.stopTrackingLocation()
        viewModel.stopAlignmentLoop()
        viewModel.stopGuidanceLoop()
        viewModel.stopCompassTracking()
        viewModel.speak("목적지에 도착했습니다. 안내를 종료합니다.")
    }
}
/** 에러 발생 시 */
data class NavigationError(val message: String) : NavigationState {}

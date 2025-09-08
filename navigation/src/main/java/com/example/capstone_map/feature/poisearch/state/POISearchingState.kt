package com.example.capstone_map.feature.poisearch.state


import android.util.Log
import com.example.capstone_map.common.state.BaseState
import com.example.capstone_map.feature.poisearch.viewmodel.POISearchViewModel


sealed interface POISearchState : BaseState<POISearchViewModel>


// 1. 장소 검색 시작 (내 위치 요청 및 상태 전이 준비)
object StartingSearch : POISearchState {
    override fun handle(viewModel: POISearchViewModel) {
        viewModel.fetchCurrentLocation() // 내위치 받아오기

    }

}


// 2. 장소 검색 중 (POI 검색 API 호출 중)
object Searching : POISearchState {
    // 검색 중
    override fun handle(viewModel: POISearchViewModel) {
        viewModel.fetchCandidatesFromAPI() // 내위치 + keyword로 검색하기
    }
}

// 3. 장소 검색 완료 (검색 결과 받아옴)
// 파싱해야됨
object SearchCompleted : POISearchState {
    override fun handle(viewModel: POISearchViewModel) {
        //검색 완로했구요

        // 추후 제스쳐나 동작에따라 다른방식으로 파싱할 수 있게
        //추후 분기를 대비한 상태 유지

        viewModel.SearchingComplete();

    }
}
// 검색 완료

// 4. 결과 JSON 파싱 하기 (GeoJSON → POIDTO로 변환)
object Parsing : POISearchState     {
    override fun handle(viewModel: POISearchViewModel) {
        viewModel.parseGeoJson()
    }

    // 파싱 중

}

// 5 파싱완료 상태
object ParsingCompleted : POISearchState     {
    override fun handle(viewModel: POISearchViewModel) {
        viewModel.showNextCandidate()
    }

    // 파싱 중

}


// 5. 후보지 나열 상태 (후보지들 중 하나씩 보여주거나 안내)
object ListingCandidates : POISearchState {
    override fun handle(viewModel: POISearchViewModel) {
        viewModel.readCurrentPoi() // 첫 번째 후보지 읽기
        Log.d("STATE", "ListingCandidates.handle()")

    }

    override fun onPrimaryInput(viewModel: POISearchViewModel) { //다음 후보지 읽기
        Log.d("STATE", "onPrimaryInput() in ListingCandidates")

        viewModel.nextPoiIndex() //index +1 후
        viewModel.readCurrentPoi() //읽어버리기
        }

    override fun onSecondaryInput(viewModel: POISearchViewModel) { //후보지 선택
        super.onSecondaryInput(viewModel)
        Log.d("STATE", "onSecondaryInput() in ListingCandidates")

        viewModel.confirmCandidate()
    }

    override fun onTertiaryInput(viewModel: POISearchViewModel) { // 후보없음
        super.onTertiaryInput(viewModel)
        Log.d("STATE", "onTertiaryInput() in ListingCandidates")
        viewModel.speak("후보 없음")

    }
}





/**
 * =======================
    예외/실패 상태들
   =======================
 **/

// 위치를 받아오는 데 실패함
object LocationError : POISearchState{ // 위치 가져오기 실패
    override fun handle(viewModel: POISearchViewModel) {
        viewModel.speak("위치 재시도는 1번 gesture " +
                " ")
    }

    override fun onPrimaryInput(viewModel: POISearchViewModel) {
        viewModel.fetchCurrentLocation() //재시도

    }

    override fun onSecondaryInput(viewModel: POISearchViewModel) {
        super.onSecondaryInput(viewModel) //구현해야함
    }

    override fun onTertiaryInput(viewModel: POISearchViewModel) {
        super.onTertiaryInput(viewModel) //구현해야한다.
    }
}

// 장소 검색 자체 실패 (API 오류 등)
object SearchFailed : POISearchState    // 장소 검색 실패

// GeoJSON 파싱 실패
object ParsingFailed : POISearchState   // 파싱 실패

// 후보지 중 아무것도 선택되지 않음
object CandidateRejected : POISearchState // 후보지 거절됨

//


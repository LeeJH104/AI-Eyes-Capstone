package com.example.capstone_map.common.stateChecker

import android.widget.TextView
import com.example.capstone_map.common.state.BaseState
import com.example.capstone_map.feature.destination.state.AskingDestinationConfirmation
import com.example.capstone_map.feature.destination.state.AwaitingDestinationInput
import com.example.capstone_map.feature.destination.state.DestinationRight
import com.example.capstone_map.feature.destination.state.DestinationWrong
import com.example.capstone_map.feature.destination.state.ListeningForDestination
import com.example.capstone_map.feature.destination.state.SearchingDestination
import com.example.capstone_map.feature.navigation.state.AligningDirection
import com.example.capstone_map.feature.navigation.state.GuidingNavigation
import com.example.capstone_map.feature.navigation.state.NavigationError
import com.example.capstone_map.feature.navigation.state.NavigationFinished
import com.example.capstone_map.feature.navigation.state.RouteDataParsing
import com.example.capstone_map.feature.navigation.state.RouteSearching
import com.example.capstone_map.feature.navigation.state.StartNavigationPreparation
import com.example.capstone_map.feature.poisearch.state.ListingCandidates
import com.example.capstone_map.feature.poisearch.state.Parsing
import com.example.capstone_map.feature.poisearch.state.ParsingCompleted
import com.example.capstone_map.feature.poisearch.state.SearchCompleted
import com.example.capstone_map.feature.poisearch.state.Searching
import com.example.capstone_map.feature.poisearch.state.StartingSearch

fun renderNavigationState(textView: TextView, state: BaseState<*>) {
    val result = when (state) {
        is AwaitingDestinationInput -> "🎤 목적지를 말씀해주세요"
        is ListeningForDestination -> "👂 듣고 있습니다..."
        is AskingDestinationConfirmation -> "❓ 이 맞나요?"
        is DestinationRight -> "✅ 목적지가 확인되었습니다"
        is DestinationWrong -> "❌ 다시 말씀해주세요"
        is SearchingDestination -> "🔍 목적지를 검색 중..."
        is StartingSearch -> "📍 현재 위치 확인 중..."
        is Searching -> "🔎 장소를 검색 중..."
        is SearchCompleted -> "✅ 검색 완료"
        is Parsing -> "🗂️ 검색 결과 파싱 중..."
        is ParsingCompleted -> "📄 후보지를 준비 중..."
        is ListingCandidates -> "📢 후보지를 안내 중입니다"
        is StartNavigationPreparation -> "🛠️ 네비게이션 준비 중..."
        is RouteSearching -> "🧭 경로를 검색 중..."
        is RouteDataParsing -> "🧩 경로 데이터를 파싱 중..."
        is AligningDirection -> "🧭 방향 정렬 중..."
        is GuidingNavigation -> "🚶 길안내 중입니다"
        is NavigationFinished -> "🎉 목적지에 도착했습니다"
        is NavigationError -> "⚠️ 네비게이션 중 오류가 발생했습니다"
        else -> "ℹ️ 알 수 없는 상태입니다"
    }

    textView.text = result
}

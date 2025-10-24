package com.example.capstone_map.common.input

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.example.capstone_map.common.state.BaseState
import com.example.capstone_map.feature.destination.state.DestinationState
import com.example.capstone_map.feature.destination.viewmodel.DestinationViewModel
import com.example.capstone_map.feature.navigation.state.NavigationState
import com.example.capstone_map.feature.navigation.viewmodel.NavigationViewModel
import com.example.capstone_map.feature.poisearch.state.POISearchState
import com.example.capstone_map.feature.poisearch.viewmodel.POISearchViewModel
import kotlin.math.abs


fun NavigationGestureBinder(
    activity: AppCompatActivity,
    targetView: View, // 제스처 적용할 뷰 (ex: 전체 화면 root)
        stateProvider: () -> BaseState<*>?,
    desViewModel: DestinationViewModel,
    poiViewModel: POISearchViewModel,
    navViewModel: NavigationViewModel
) {

    fun handlePrimary() { //  = 오른쪽 스와이프
        when (val state = stateProvider()) {
            is DestinationState -> state.onPrimaryInput(desViewModel)
            is POISearchState   -> state.onPrimaryInput(poiViewModel)
            is NavigationState  -> state.onPrimaryInput(navViewModel)
        }
    }

    fun handleSecondary() { //  = 왼쪽 스와이프
        when (val state = stateProvider()) {
            is DestinationState -> state.onSecondaryInput(desViewModel)
            is POISearchState   -> state.onSecondaryInput(poiViewModel)
            is NavigationState  -> state.onSecondaryInput(navViewModel)
        }
    }

    fun handleTertiary() { //  = 아래로 스와이프
        when (val state = stateProvider()) {
            is DestinationState -> state.onTertiaryInput(desViewModel)
            is POISearchState   -> state.onTertiaryInput(poiViewModel)
            is NavigationState  -> state.onTertiaryInput(navViewModel)
        }
    }




    // 2. 제스쳐 감지기
    val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,   // nullable 처리
            e2: MotionEvent,    //  그대로 사용
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val dx = (e2.x - (e1?.x ?: e2.x)) //  e1가 null일 때 대비
            val dy = (e2.y - (e1?.y ?: e2.y))
            val absDx = abs(dx)
            val absDy = abs(dy)
            val minDist = 120
            val minVel = 200

            if (absDx > absDy && absDx > minDist && abs(velocityX) > minVel) {
                if (dx > 0) {
                    handlePrimary() // 오른쪽
                } else {
                    handleSecondary() // 왼쪽
                }
                return true
            }

            if (dy > minDist && abs(velocityY) > minVel) {
                handleTertiary() // 아래
                return true
            }
            return false
        }

    }

    val detector = GestureDetectorCompat(activity, gestureListener)

    targetView.setOnTouchListener { _, event ->
        detector.onTouchEvent(event)
        true // 제스처 전용으로 쓰겠다면 true 유지
    }
}
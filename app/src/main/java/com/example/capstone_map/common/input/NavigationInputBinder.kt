package com.example.capstone_map.common.input

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.capstone_map.common.state.BaseState
import com.example.capstone_map.feature.destination.state.NavigationState
import com.example.capstone_map.feature.destination.viewmodel.DestinationViewModel
import com.example.capstone_map.feature.poisearch.state.POISearchState
import com.example.capstone_map.feature.poisearch.viewmodel.POISearchViewModel

fun NavigationInputBinder(
    activity: AppCompatActivity,
    stateProvider: () -> BaseState<*>?,
    desViewModel: DestinationViewModel,
    poiViewModel: POISearchViewModel,
    primaryButtonId: Int,
    secondaryButtonId: Int,
    tertiaryButtonId: Int
) {
    val primary = activity.findViewById<View>(primaryButtonId)
    val secondary = activity.findViewById<View>(secondaryButtonId)
    val tertiary = activity.findViewById<View>(tertiaryButtonId)

    primary.setOnClickListener {
        val state = stateProvider()
        when (state) {
            is NavigationState -> state.onPrimaryInput(desViewModel)
            is POISearchState -> state.onPrimaryInput(poiViewModel)
        }
    }

    secondary.setOnClickListener {
        val state = stateProvider()
        when (state) {
            is NavigationState -> state.onSecondaryInput(desViewModel)
            is POISearchState -> state.onSecondaryInput(poiViewModel)
        }
    }

    tertiary.setOnClickListener {
        val state = stateProvider()
        when (state) {
            is NavigationState -> state.onTertiaryInput(desViewModel)
            is POISearchState -> state.onTertiaryInput(poiViewModel)
        }
    }
}

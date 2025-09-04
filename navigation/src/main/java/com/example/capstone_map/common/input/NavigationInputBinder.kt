package com.example.capstone_map.common.input

import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.capstone_map.common.state.BaseState
import com.example.capstone_map.feature.destination.state.DestinationState
import com.example.capstone_map.feature.destination.viewmodel.DestinationViewModel
import com.example.capstone_map.feature.navigation.state.NavigationState
import com.example.capstone_map.feature.navigation.viewmodel.NavigationViewModel
import com.example.capstone_map.feature.poisearch.state.POISearchState
import com.example.capstone_map.feature.poisearch.viewmodel.POISearchViewModel

fun NavigationInputBinder(
    activity: AppCompatActivity,
    stateProvider: () -> BaseState<*>?,
    desViewModel: DestinationViewModel,
    poiViewModel: POISearchViewModel,
    navViewModel : NavigationViewModel,
    primary: Button,
    secondary: Button,
    tertiary: Button
) {


    primary.setOnClickListener {
        val state = stateProvider()
        when (state) {
            is DestinationState -> state.onPrimaryInput(desViewModel)
            is POISearchState -> state.onPrimaryInput(poiViewModel)
            is NavigationState -> state.onPrimaryInput(navViewModel)
        }
    }

    secondary.setOnClickListener {
        val state = stateProvider()
        when (state) {
            is DestinationState -> state.onSecondaryInput(desViewModel)
            is POISearchState -> state.onSecondaryInput(poiViewModel)
            is NavigationState -> state.onSecondaryInput(navViewModel)

        }
    }

    tertiary.setOnClickListener {
        val state = stateProvider()
        when (state) {
            is DestinationState -> state.onTertiaryInput(desViewModel)
            is POISearchState -> state.onTertiaryInput(poiViewModel)
            is NavigationState -> state.onTertiaryInput(navViewModel)

        }
    }
    val s = stateProvider() ?: return
}

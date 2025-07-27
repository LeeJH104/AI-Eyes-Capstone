package com.example.capstone_map.common.viewmodel

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.capstone_map.common.poi.Poi
import com.example.capstone_map.common.state.BaseState


class NavigationStateViewModel : ViewModel() {


    //val = 개념 핵심: val은 참조(주소)를 바꿀 수 없다는 뜻이지,
    val geoJsonData = MutableLiveData<String>() //
    val currentLocation = MutableLiveData<Location>()  // ✅ 올바르게 수정됨
    val navState = MutableLiveData<BaseState<*>>() // 혹은 BaseState<out ViewModel>
    val destinationText = MutableLiveData<String>()
    // POI 후보 리스트
    val poiList = MutableLiveData<List<Poi>>()
    // POI 후보리스트의  현재 index
    val currentPoiIndex = MutableLiveData<Int>()

    // 후보지들중 결정된 목적지 POI값
    val decidedDestinationPOI = MutableLiveData<Poi>()
}
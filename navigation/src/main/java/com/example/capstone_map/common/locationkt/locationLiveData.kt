package com.example.capstone_map.common.locationkt

// 📁 LocationLiveData.kt

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.LiveData
import com.google.android.gms.location.*

class LocationLiveData(private val app: Application) : LiveData<Location>() {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(app)

    private val locationRequest = LocationRequest.create().apply {
        interval = 3000 // 3초마다 요청
        fastestInterval = 2000
        priority = Priority.PRIORITY_HIGH_ACCURACY
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { value = it }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onActive() {
        // observe 시작되면 위치 추적 시작
        fusedClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    override fun onInactive() {
        // observe 없으면 추적 중단
        fusedClient.removeLocationUpdates(locationCallback)
    }
}

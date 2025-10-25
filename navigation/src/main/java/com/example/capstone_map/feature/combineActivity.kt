package com.example.capstone_map.feature

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.capstone_map.R
import com.example.capstone_map.feature.ObstacleDetectionFragment

class CombinedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_combined)

        if (savedInstanceState == null) {

            supportFragmentManager.beginTransaction()
                .replace(R.id.camera_container, ObstacleDetectionFragment()) // ✅ CameraFragment -> ObstacleDetectionFragment
                .commit()

            supportFragmentManager.beginTransaction()
                .replace(R.id.map_container, MapFragment())
                .commit()
        }
    }
}
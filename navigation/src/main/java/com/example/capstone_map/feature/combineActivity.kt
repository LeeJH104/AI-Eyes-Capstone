package com.example.capstone_map.feature



import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.capstone_map.R


// import com.example.aieyes.feature.CameraFragment  // ✅ CameraFragment import

class CombinedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_combined)

//        // ✅ 카메라 Fragment 추가 (상단)
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.camera_container, CameraFragment())
//            .commit()

        // ✅ 맵 Fragment 추가 (하단)
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, MapFragment())
            .commit()
    }
}

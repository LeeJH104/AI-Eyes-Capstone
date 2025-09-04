// LocationTracker.java
package com.example.capstone_map.common.location.tracker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.capstone_map.common.location.PermissionChecker;

public class LocationTracker implements LocationListener { //LocationListener를통해 위치가 변경되면 자동으로 알림받을 수 있다
    //LocationListener는 안드로이드에서 제공하는 인터페이스로,
    //"위치 정보가 바뀌었을 때 내가 알려줄게!" 라는 역할을 해요.

    private static final String TAG = "LocationTracker";


    private static final int LOCATION_UPDATE_INTERVAL = 2000;
    private static final float LOCATION_UPDATE_DISTANCE = 1.0f;
    //2초마다, 또는 1m 이상 움직이면 위치 정보를 받겠다는 설정

    private final Context context;
    private final LocationUpdateCallback callback;
    private final LocationManager locationManager;
    private boolean isTracking = false;

    /**
     * context: 앱 환경 정보 (시스템 서비스 사용하려고 필요해요)
     *
     * callback: 위치가 바뀌었을 때 알려주는 인터페이스
     *
     * locationManager: GPS나 네트워크 위치 제공자 사용을 도와주는 안드로이드 시스템 객체 (GPS를 쓰려면 필수!)
     *
     * isTracking: 현재 위치 추적 중인지 여부 (on/off)
    * */


    public LocationTracker(Context context, LocationUpdateCallback callback) {
        this.context = context;
        this.callback = callback;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        //locationManager도 여기서 세팅합니다 (GPS를 쓰려면 필수)
    }

    public void startTracking() {
        if (isTracking || !PermissionChecker.hasLocationPermission(context)) return;
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return;
        //이미 추적 중이거나, 권한이 없다면 그냥 종료
        //GPS 기능 자체가 꺼져 있으면 추적하지 않아요.



        // 위치 권한이 있는지 다시 한 번 확인
        if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

       /**
        * GPS 리스너 등록
        *
        * GPS와 네트워크 둘 다 이용해서 위치를 받아오기 시작합니다.
        *
        * 설정한 간격(2초, 1m 이상 움직임)에 따라 onLocationChanged()가 호출됩니다.
        *
        * “GPS 쓰고 싶고요,
        * 1초마다 또는 1m 이상 움직이면 알려주세요!
        * 알려줄 땐 이 클래스(this)의 onLocationChanged()를 자동으로 호출해주세요~”
       * */

       //
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_INTERVAL, LOCATION_UPDATE_DISTANCE, this);

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    LOCATION_UPDATE_INTERVAL, LOCATION_UPDATE_DISTANCE, this);
        }



        isTracking = true;
        //추적 상태를 true로 바꿔서 나중에 stop할 수 있게 합니다.
    }

    public void stopTracking() {
        if (!isTracking) return;
        locationManager.removeUpdates(this);
        isTracking = false;
    }

    //추적 멈추기

    public boolean isTracking() {
        return isTracking;
    }


    //GPS 켜져 있는지 확인: isGPSEnabled()
    public boolean isGPSEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }



    @Override
    public void onLocationChanged(Location location) { //사용자위치가 바뀔때마다 자동으로호출됨 LocationListener 오버라이드인가?
        if (location == null) return;


        //여기서 위치 정확도(accuracy)를 체크해서,
        //
        //20m 이상이면 GPS 약해요
        //
        //10m 이하면 GPS 좋아요

        Log.d("LocationTracker", "onLocationChanged 호출됨: " + location.getLatitude() + ", " + location.getLongitude());

        float accuracy = location.getAccuracy();

        Log.d("LocationTracker", "정확도: " + accuracy);


        if (accuracy > 20.0f) callback.onGPSSignalWeak();

        else if (accuracy <= 10.0f) callback.onGPSSignalRestored();


        callback.onLocationChanged(location);
        callback.onLocationAccuracyChanged(accuracy);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {} //구버전 사용방식 거의안씀
    @Override
    public void onProviderEnabled(String provider) {} //사용자가 GPS 같은 기능을 켜면 자동으로 호출됨
    @Override
    public void onProviderDisabled(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) callback.onGPSSignalWeak(); //사용자가 GPS를 껐을 때 자동 호출됨

    }
}

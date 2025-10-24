//package com.example.capstone_map.common.location.oncefetcher;
//
//import android.Manifest;
//import android.app.Activity;
//import android.content.pm.PackageManager;
//import android.location.Location;
//
//import com.example.capstone_map.common.location.LocationResultListener;
//import com.google.android.gms.location.LocationRequest;
//
//import android.os.Looper;
//import android.util.Log;
//
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationCallback;
//import com.google.android.gms.location.LocationResult;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.location.Priority;
//
//import androidx.activity.result.ActivityResultLauncher;
//import androidx.activity.result.contract.ActivityResultContracts;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//
//public class CurrentLocationProvider {
//    private final Activity activity;
//    private final FusedLocationProviderClient fusedLocationClient;
//    private LocationResultListener userCallback;
//    private final ActivityResultLauncher<String> permissionLauncher;
//
//
//
//
//    public CurrentLocationProvider(Activity activity, ActivityResultLauncher<String> permissionLauncher) {
//        this.activity = activity;
//        this.permissionLauncher = permissionLauncher;
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
//    }
//
//    public void getCurrentLocation(LocationResultListener listener) {
//        this.userCallback = listener;
//
//        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
//        } else {
//            fetchLocation();
//        }
//    }
//
//
//    void fetchLocation() {
//        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        fusedLocationClient.getLastLocation()
//                .addOnSuccessListener(activity, location -> {
//                    if (location != null) {
//                        userCallback.onLocationResult(location);
//                    } else {
//                        requestSingleLocation();
//                    }
//                });
//    }
//
//    private void requestSingleLocation() {
//        LocationRequest locationRequest = LocationRequest.create()
//                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
//                .setNumUpdates(1)
//                .setInterval(0);
//
//        LocationCallback locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult result) {
//                Location location = result.getLastLocation();
//                if (location != null) {
//                    userCallback.onLocationResult(location);
//                }
//                fusedLocationClient.removeLocationUpdates(this);
//            }
//        };
//
//        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
//    }
//
//    private void requestPermission() {
//        if (!(activity instanceof androidx.activity.ComponentActivity)) {
//            throw new IllegalStateException("Activity must extend ComponentActivity to use ActivityResultLauncher.");
//        }
//
//        ActivityResultLauncher<String> permissionLauncher = ((androidx.activity.ComponentActivity) activity)
//                .registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
//                    if (isGranted) {
//                        fetchLocation();
//                    } else {
//                        Log.w("LOCATION", "사용자가 위치 권한을 거부했습니다.");
//                    }
//                });
//
//        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
//    }
//}

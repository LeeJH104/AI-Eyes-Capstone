package com.example.capstone_map.common.map;

// TMapInitializer.java


import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.skt.Tmap.TMapView;

public class TMapInitializer {

    public static TMapView setupTMapView(Context context, LinearLayout containerLayout) {
        TMapView tMapView = new TMapView(context);
        tMapView.setSKTMapApiKey("MUUgFleM6h4uFPz6yYOW03Gbzskx5Gci1rdtifFf");

        tMapView.setCenterPoint(127.0, 37.0);
        tMapView.setZoomLevel(15);

        // ✨ TMap 터치 비활성화 (Java 문법)
        tMapView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;  // 모든 터치 이벤트 소비 (TMap이 처리 안 함)
            }
        });



        containerLayout.addView(tMapView);
        return tMapView;
    }
}

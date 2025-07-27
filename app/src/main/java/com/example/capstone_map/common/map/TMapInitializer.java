package com.example.capstone_map.common.map;

// TMapInitializer.java


import android.content.Context;
import android.widget.LinearLayout;

import com.skt.Tmap.TMapView;

public class TMapInitializer {

    public static TMapView setupTMapView(Context context, LinearLayout containerLayout) {
        TMapView tMapView = new TMapView(context);
        tMapView.setSKTMapApiKey("발급받은_키");

        tMapView.setCenterPoint(127.0, 37.0);
        tMapView.setZoomLevel(15);

        containerLayout.addView(tMapView);
        return tMapView;
    }
}

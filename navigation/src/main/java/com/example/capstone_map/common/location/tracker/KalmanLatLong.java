// KalmanLatLong.java
package com.example.capstone_map.common.location.tracker;

public class KalmanLatLong {

    private final float MIN_ACCURACY = 1.0f;
    private final float Q_METRES_PER_SECOND;

    private long timeStampMillis;
    private double lat;
    private double lng;
    private float variance; // -1이면 초기화 안 됨

    public KalmanLatLong(float qMetresPerSecond) {
        this.Q_METRES_PER_SECOND = qMetresPerSecond;
        this.variance = -1;
    }

    // Getters
    public long getTimeStamp() { return timeStampMillis; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public float getAccuracy() { return (float) Math.sqrt(variance); }

    /**
     * 초기 상태 설정
     */
    public void setState(double lat, double lng, float accuracy, long timeStamp) {
        this.lat = lat;
        this.lng = lng;
        this.variance = accuracy * accuracy;
        this.timeStampMillis = timeStamp;
    }

    /**
     * Kalman Filter 처리
     */
    public void process(double latMeasurement, double lngMeasurement, float accuracy, long timeStamp) {
        // 최소 정확도 보장
        if (accuracy < MIN_ACCURACY) {
            accuracy = MIN_ACCURACY;
        }

        // 초기화 안 됐으면 초기화
        if (variance < 0) {
            setState(latMeasurement, lngMeasurement, accuracy, timeStamp);
            return;
        }

        // 시간 경과에 따른 불확실성 증가
        long timeInc = timeStamp - this.timeStampMillis;
        if (timeInc > 0) {
            variance += timeInc * Q_METRES_PER_SECOND * Q_METRES_PER_SECOND / 1000.0f;
            this.timeStampMillis = timeStamp;
        }

        // Kalman Gain 계산
        float K = variance / (variance + accuracy * accuracy);

        // 위치 업데이트
        this.lat += K * (latMeasurement - this.lat);
        this.lng += K * (lngMeasurement - this.lng);

        // 공분산 업데이트
        this.variance = (1 - K) * variance;
    }

    /**
     * 필터 리셋
     */
    public void reset() {
        this.variance = -1;
    }
}
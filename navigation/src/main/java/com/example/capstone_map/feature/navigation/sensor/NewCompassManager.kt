package com.example.capstone_map.feature.navigation.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import kotlin.math.abs

/**
 * 스마트폰 헤딩(방위각, 0~360°)을 안정적으로 제공하는 Compass Manager.
 * - TYPE_ROTATION_VECTOR 우선 사용(자이로/가속도/자기장 융합)
 * - 백업: 가속도 + 자기장 → getRotationMatrix
 * - 저역통과 스무딩(360° 래핑 보정 포함)
 * - 화면 회전에 따른 좌표계 리매핑 지원
 */
class NewCompassManager(
    context: Context,
    private val onHeadingChanged: (Float) -> Unit // 0..360 degrees
) : SensorEventListener {

    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVector = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer  = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer   = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // 백업 경로용 버퍼
    private val accVals = FloatArray(3)
    private val magVals = FloatArray(3)
    private var hasAcc = false
    private var hasMag = false

    // 스무딩 상태
    private var lastDeg: Float = Float.NaN

    /** 현재 방위각(0~360). 스무딩된 값 */
    var currentAzimuth: Float = 0f
        private set

    /** 스무딩 강도 (0.0~1.0). 낮을수록 더 부드럽게. 기본 0.15 */
    var alpha: Float = 0.15f

    /** 급격한 튐(스파이크) 방지용 최대 허용 변화량(도). 0이면 비활성화. */
    var spikeThresholdDeg: Float = 0f

    /** 화면 회전(가로/세로)에 맞춘 리매핑을 위해 Activity/Fragment에서 설정 (Surface.ROTATION_*) */
    var displayRotation: Int = Surface.ROTATION_0

    fun start() {
        if (rotationVector != null) {
            sm.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)
        } else {
            sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sm.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sm.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> handleRotationVector(event.values)
            Sensor.TYPE_ACCELEROMETER -> {
                // 내부 버퍼에 복사 저장
                System.arraycopy(event.values, 0, accVals, 0, 3)
                hasAcc = true
                // acc만으로는 계산하지 않음
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magVals, 0, 3)
                hasMag = true
                // mag만으로는 계산하지 않음
            }
            else -> return
        }

        // 회전 벡터가 없는 기기에서는 acc+mag가 모두 들어온 다음 계산
        if (rotationVector == null && hasAcc && hasMag) {
            handleAccMagFusion()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 필요 시 정확도 낮은 상태(UNRELIABLE)에서 업데이트 무시 등의 로직을 넣을 수 있음.
        // if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD &&
        //     accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) { ... }
    }

    // --- 내부 구현 ---

    private fun handleRotationVector(values: FloatArray) {
        val R = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(R, values)
        val remapped = remapForDisplay(R)
        val orient = FloatArray(3)
        SensorManager.getOrientation(remapped, orient)
        val deg = to0_360(Math.toDegrees(orient[0].toDouble()).toFloat())
        updateHeading(deg)
    }

    private fun handleAccMagFusion() {
        val R = FloatArray(9)
        val I = FloatArray(9)
        if (SensorManager.getRotationMatrix(R, I, accVals, magVals)) {
            val remapped = remapForDisplay(R)
            val orient = FloatArray(3)
            SensorManager.getOrientation(remapped, orient)
            val deg = to0_360(Math.toDegrees(orient[0].toDouble()).toFloat())
            updateHeading(deg)
        }
    }

    /** 화면 회전에 따라 축을 재매핑해 일관된 헤딩을 얻는다. */
    private fun remapForDisplay(Rin: FloatArray): FloatArray {
        val out = FloatArray(9)
        when (displayRotation) {
            Surface.ROTATION_0 -> {
                SensorManager.remapCoordinateSystem(
                    Rin,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z,
                    out
                )
            }
            Surface.ROTATION_90 -> {
                SensorManager.remapCoordinateSystem(
                    Rin,
                    SensorManager.AXIS_Z,
                    SensorManager.AXIS_MINUS_X,
                    out
                )
            }
            Surface.ROTATION_180 -> {
                SensorManager.remapCoordinateSystem(
                    Rin,
                    SensorManager.AXIS_MINUS_X,
                    SensorManager.AXIS_MINUS_Z,
                    out
                )
            }
            Surface.ROTATION_270 -> {
                SensorManager.remapCoordinateSystem(
                    Rin,
                    SensorManager.AXIS_MINUS_Z,
                    SensorManager.AXIS_X,
                    out
                )
            }
            else -> System.arraycopy(Rin, 0, out, 0, 9)
        }
        return out
    }

    private fun updateHeading(rawDeg: Float) {
        // 스파이크 필터(옵션)
        if (!lastDeg.isNaN() && spikeThresholdDeg > 0f) {
            val delta = shortestDelta(rawDeg, lastDeg)
            if (abs(delta) > spikeThresholdDeg) {
                // 급격한 튐으로 판단 → 무시
                return
            }
        }

        // 저역통과 스무딩 (360 래핑 고려)
        val smooth = lowPassDegrees(rawDeg, lastDeg, alpha)
        lastDeg = smooth
        currentAzimuth = smooth
        onHeadingChanged(smooth)
    }

    /** -180..+180 내에서의 최소 회전 변화량 */
    private fun shortestDelta(a: Float, b: Float): Float {
        var diff = a - b
        diff = ((diff + 540f) % 360f) - 180f
        return diff
    }

    /** 0..360 정규화 */
    private fun to0_360(d: Float): Float = ((d + 360f) % 360f)

    /** 각도 전용 저역통과: 래핑을 고려해 최소 경로로 보간 */
    private fun lowPassDegrees(newDeg: Float, lastDeg: Float, alpha: Float): Float {
        if (lastDeg.isNaN()) return newDeg
        var diff = newDeg - lastDeg
        diff = ((diff + 540f) % 360f) - 180f // -180..+180
        return (lastDeg + alpha * diff + 360f) % 360f
    }
}

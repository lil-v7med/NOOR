package com.example.qibla

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class CompassSensorReading(
    val magneticAzimuthDeg: Float = 0f,
    val trueAzimuthDeg: Float = 0f,
    val accuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE,
    val hasAccelerometer: Boolean = true,
    val hasMagnetometer: Boolean = true,
    val isListening: Boolean = false
)

class QiblaSensorController(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _reading = MutableStateFlow(
        CompassSensorReading(
            hasAccelerometer = accelerometer != null,
            hasMagnetometer = magnetometer != null
        )
    )
    val reading: StateFlow<CompassSensorReading> = _reading.asStateFlow()

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private val rotationMatrix = FloatArray(9)
    private val remappedRotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var smoothSin = 0f
    private var smoothCos = 1f
    private var isSmoothingInitialized = false

    @Volatile
    private var declinationDeg: Float = 0f

    fun updateLocationForDeclination(latitude: Double, longitude: Double, altitude: Double = 0.0) {
        val field = GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            altitude.toFloat(),
            System.currentTimeMillis()
        )
        declinationDeg = field.declination
    }

    fun start() {
        val hasAcc = accelerometer != null
        val hasMag = magnetometer != null
        _reading.update {
            it.copy(
                hasAccelerometer = hasAcc,
                hasMagnetometer = hasMag,
                isListening = hasAcc && hasMag
            )
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        _reading.update { it.copy(isListening = false) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            _reading.update { it.copy(accuracy = accuracy) }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val alpha = 0.16f
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                if (!hasGravity) {
                    event.values.copyInto(gravity, endIndex = 3)
                    hasGravity = true
                } else {
                    for (i in 0..2) {
                        gravity[i] = gravity[i] + alpha * (event.values[i] - gravity[i])
                    }
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                if (!hasGeomagnetic) {
                    event.values.copyInto(geomagnetic, endIndex = 3)
                    hasGeomagnetic = true
                } else {
                    for (i in 0..2) {
                        geomagnetic[i] = geomagnetic[i] + alpha * (event.values[i] - geomagnetic[i])
                    }
                }
            }
        }

        if (!hasGravity || !hasGeomagnetic) return

        val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
        if (!success) return

        val displayRotation = getDisplayRotation()
        val (axisX, axisY) = when (displayRotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }

        val matrixToUse = if (displayRotation == Surface.ROTATION_0) {
            rotationMatrix
        } else {
            SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedRotationMatrix)
            remappedRotationMatrix
        }

        SensorManager.getOrientation(matrixToUse, orientationAngles)
        val rawAzimuthRad = orientationAngles[0].toDouble()

        // Circular exponential smoothing to prevent 0/360 jumpiness
        val s = sin(rawAzimuthRad).toFloat()
        val c = cos(rawAzimuthRad).toFloat()
        if (!isSmoothingInitialized) {
            smoothSin = s
            smoothCos = c
            isSmoothingInitialized = true
        } else {
            val angleAlpha = 0.22f
            smoothSin += angleAlpha * (s - smoothSin)
            smoothCos += angleAlpha * (c - smoothCos)
        }

        val smoothedDeg = ((Math.toDegrees(atan2(smoothSin.toDouble(), smoothCos.toDouble())) + 360.0) % 360.0).toFloat()
        val trueNorthDeg = ((smoothedDeg + declinationDeg + 360f) % 360f)

        _reading.update {
            it.copy(
                magneticAzimuthDeg = smoothedDeg,
                trueAzimuthDeg = trueNorthDeg
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun getDisplayRotation(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.display?.rotation ?: Surface.ROTATION_0
            } catch (_: Exception) {
                Surface.ROTATION_0
            }
        } else {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            wm?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        }
    }
}

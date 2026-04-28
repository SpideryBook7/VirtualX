package dev.vpad.controller.input

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.MotionEvent

class GyroManager(
    context: Context,
    private val inputProcessor: InputProcessor
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    var isEnabled = false
        set(value) {
            if (field == value) return
            field = value
            if (value && gyroSensor != null) {
                sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
            } else {
                sensorManager.unregisterListener(this)
                // Reset axes when disabled
                inputProcessor.updateAxes(mapOf(MotionEvent.AXIS_Z to 0f, MotionEvent.AXIS_RZ to 0f))
            }
        }

    var sensitivity = 1.0f
    var invertY = false

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isEnabled || event == null) return

        // Angular speed in rad/s
        // event.values[0] = x-axis (pitch forward/back)
        // event.values[1] = y-axis (roll left/right)
        // event.values[2] = z-axis (yaw)

        // Assuming device is primarily held in landscape for gaming:
        // Tilting left/right (steering) rotates around the short edge (Pitch/X-axis in portrait, Roll/Y-axis in landscape usually depending on exact orientation).
        // Let's map X to right-stick X, and Y to right-stick Y.
        // It may require tuning based on actual physical orientation.
        // Usually, X rotation is pitch (up/down). Y rotation is roll (left/right).
        
        var rawX = event.values[1] // Roll (tilting left/right)
        var rawY = event.values[0] // Pitch (tilting forward/back)

        // Apply orientation logic if needed, but standard gamepads use:
        // rawX -> AXIS_Z
        // rawY -> AXIS_RZ
        
        if (invertY) rawY = -rawY

        // Sensor values are typically small rad/s (e.g. 0.5 to 3.0 rad/s). 
        // We multiply by our user sensitivity and clamp it to standard joystick range [-1.0, 1.0].
        // 1 rad/s is about 57 degrees per second.
        val multiplier = sensitivity * 0.5f 

        val zAxis = (rawX * multiplier).coerceIn(-1.0f, 1.0f)
        val rzAxis = (rawY * multiplier).coerceIn(-1.0f, 1.0f)

        // Inject all current axes state together
        inputProcessor.updateAxes(mapOf(MotionEvent.AXIS_Z to zAxis, MotionEvent.AXIS_RZ to rzAxis), isHardwareGyro = true)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed
    }
}

package dev.vpad.controller.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

/**
 * Pure Input Processor (Phase 17).
 * COMPLETELY REMOVED all mouse/touch emulation to avoid conflicts with GeForce Now native touch.
 * Only handles Keyboard/Gamepad button injection.
 */
class InputProcessor {

    companion object {
        const val KEYCODE_RM = 10001
        
        val DefaultPcKeyMap = mapOf(
            KeyEvent.KEYCODE_BUTTON_A to KeyEvent.KEYCODE_SPACE, 
            KeyEvent.KEYCODE_BUTTON_B to KeyEvent.KEYCODE_ESCAPE,
            KeyEvent.KEYCODE_BUTTON_X to KeyEvent.KEYCODE_R,     
            KeyEvent.KEYCODE_BUTTON_Y to KeyEvent.KEYCODE_F,     
            KeyEvent.KEYCODE_BUTTON_L1 to KeyEvent.KEYCODE_Q,
            KeyEvent.KEYCODE_BUTTON_R1 to KeyEvent.KEYCODE_E,
            KeyEvent.KEYCODE_BUTTON_L2 to KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_BUTTON_R2 to KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_BUTTON_SELECT to KeyEvent.KEYCODE_TAB,
            KeyEvent.KEYCODE_BUTTON_START to KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_UP to KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN to KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT to KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT to KeyEvent.KEYCODE_DPAD_RIGHT
        )
    }

    var inputMode: Int = 1 // Default to PC Mode
    var deadZone: Float = 0.08f
    var sensitivityCurve: Float = 1.4f
    var customPcKeyMap: Map<Int, Int> = emptyMap()

    private val pcStickState = mutableSetOf<Int>()

    fun updateAxes(axes: Map<Int, Float>, isHardwareGyro: Boolean = false) {
        if (inputMode == 1) {
            handlePcAxes(axes, isHardwareGyro)
        } else {
            // Native Gamepad fallback
            val processedAxes = axes.mapValues { (_, value) -> applyCurves(value, isHardwareGyro) }
            if (processedAxes.isNotEmpty()) {
                // To ensure Maximum GFN Gamepad compatibility, use Gamepad | Joystick source
                val source = InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK
                VirtualDeviceManager.injectAxisEvent(processedAxes, source = source)
            }
        }
    }

    private fun applyCurves(value: Float, isHardwareGyro: Boolean): Float {
        // Gyroscope angular speeds are extremely small, don't kill them with the 8% analog stick deadzone.
        // Use a tiny 1% flat deadzone to prevent standing noise drift instead.
        val actualDeadZone = if (isHardwareGyro) 0.01f else deadZone
        
        val magnitude = abs(value)
        if (magnitude < actualDeadZone) return 0f
        val normalized = (magnitude - actualDeadZone) / (1f - actualDeadZone)
        return (normalized.pow(sensitivityCurve) * sign(value)).coerceIn(-1f, 1f)
    }

    private fun handlePcAxes(axes: Map<Int, Float>, isHardwareGyro: Boolean) {
        // Left Stick -> WASD
        if (axes.containsKey(MotionEvent.AXIS_X) || axes.containsKey(MotionEvent.AXIS_Y)) {
            val x = axes[MotionEvent.AXIS_X] ?: 0f
            val y = axes[MotionEvent.AXIS_Y] ?: 0f
            val threshold = 0.45f
            updatePcKey(KeyEvent.KEYCODE_W, y < -threshold)
            updatePcKey(KeyEvent.KEYCODE_S, y > threshold)
            updatePcKey(KeyEvent.KEYCODE_A, x < -threshold)
            updatePcKey(KeyEvent.KEYCODE_D, x > threshold)
        }

        // Right Stick / Gyroscope -> Relative Mouse Look
        if (axes.containsKey(MotionEvent.AXIS_Z) || axes.containsKey(MotionEvent.AXIS_RZ)) {
            val z = axes[MotionEvent.AXIS_Z] ?: 0f
            val rz = axes[MotionEvent.AXIS_RZ] ?: 0f
            
            // Map the processed analog curve to mouse pixel deltas directly. 
            // 40.0f is our base multiplier for "full stick" turning speed per tick.
            val processedZ = applyCurves(z, isHardwareGyro)
            val processedRz = applyCurves(rz, isHardwareGyro)
            
            val dx = processedZ * 40f
            val dy = processedRz * 40f
            
            if (dx != 0f || dy != 0f) {
                VirtualDeviceManager.injectMouseEvent(dx, dy)
            }
        }
    }

    fun stopTrackpad() {
        // No-op - Native GFN touch handles the camera now.
    }

    private fun updatePcKey(keyCode: Int, isPressed: Boolean) {
        val wasPressed = pcStickState.contains(keyCode)
        if (isPressed && !wasPressed) {
            pcStickState.add(keyCode)
            VirtualDeviceManager.injectKeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        } else if (!isPressed && wasPressed) {
            pcStickState.remove(keyCode)
            VirtualDeviceManager.injectKeyEvent(KeyEvent.ACTION_UP, keyCode)
        }
    }

    fun updateAxis(axis: Int, value: Float) {
        updateAxes(mapOf(axis to value))
    }

    fun updateButton(keyCode: Int, isPressed: Boolean) {
        val action = if (isPressed) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
        
        if (inputMode == 1) { // PC Mode
            val targetCode = customPcKeyMap[keyCode] ?: DefaultPcKeyMap[keyCode] ?: keyCode
            VirtualDeviceManager.injectKeyEvent(action, targetCode)
        } else {
            // Native Gamepad fallback
            VirtualDeviceManager.injectKeyEvent(action, keyCode, source = InputDevice.SOURCE_GAMEPAD)
        }
    }
}

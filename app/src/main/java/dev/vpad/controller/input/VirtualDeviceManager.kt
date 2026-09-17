package dev.vpad.controller.input

import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Method

/**
 * Pure Button Injection Engine (Phase 17).
 * Uses Shizuku reflection to inject MotionEvents (including SOURCE_MOUSE for macros),
 * KeyEvents, and AxisEvents.
 */
object VirtualDeviceManager {
    private const val TAG = "VirtualDeviceManager"
    private const val INJECT_MODE_ASYNC = 0
    private const val VIRTUAL_DEVICE_ID = 9999 

    private var iimInstance: Any? = null
    private var injectMethod: Method? = null
    private var touchDownTime: Long = 0L

    fun initialize(): Boolean {
        try {
            val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("input"))
            val stubClass = Class.forName("android.hardware.input.IInputManager\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            iimInstance = asInterface.invoke(null, binder)

            val iimClass = Class.forName("android.hardware.input.IInputManager")
            injectMethod = iimClass.methods.firstOrNull { it.name == "injectInputEvent" }
            return iimInstance != null && injectMethod != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize IInputManager via Shizuku", e)
            return false
        }
    }

    fun stop() {
        iimInstance = null
        injectMethod = null
    }

    private fun getRealTouchscreenDeviceId(): Int {
        for (id in InputDevice.getDeviceIds()) {
            val dev = InputDevice.getDevice(id) ?: continue
            if (!dev.isVirtual && (dev.sources and InputDevice.SOURCE_TOUCHSCREEN) == InputDevice.SOURCE_TOUCHSCREEN) {
                return id
            }
        }
        return VIRTUAL_DEVICE_ID
    }

    /**
     * Inject a high-precision multi-touch tap at (x, y).
     * 1. Primary: Uses AccessibilityService dispatchTap if active (100% zero touch interruption).
     * 2. Fallback: Uses Shizuku Multi-Touch Protocol (ACTION_POINTER_DOWN / UP for Pointer Index 1).
     *    By using Pointer Index 1, Android's InputDispatcher maintains Pointer 0 (active joystick/movement)
     *    without sending ACTION_CANCEL.
     */
    fun injectTapAt(x: Float, y: Float, durationMs: Long = 40L) {
        if (dev.vpad.controller.service.VPadAccessibilityService.dispatchTap(x, y, durationMs)) {
            return
        }

        val now = SystemClock.uptimeMillis()

        val props = arrayOf(
            MotionEvent.PointerProperties().apply {
                id = 0
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        )
        val coords = arrayOf(
            MotionEvent.PointerCoords().apply {
                this.x = x
                this.y = y
                pressure = 1.0f
                size = 1.0f
            }
        )

        val eventDown = MotionEvent.obtain(
            now, now,
            MotionEvent.ACTION_DOWN,
            1, props, coords,
            0, 0, 1f, 1f,
            VIRTUAL_DEVICE_ID, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0
        )
        inject(eventDown)
        eventDown.recycle()

        try { Thread.sleep(durationMs) } catch (e: Exception) {}

        val upTime = SystemClock.uptimeMillis()
        val eventUp = MotionEvent.obtain(
            now, upTime,
            MotionEvent.ACTION_UP,
            1, props, coords,
            0, 0, 1f, 1f,
            VIRTUAL_DEVICE_ID, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0
        )
        inject(eventUp)
        eventUp.recycle()
    }

    fun injectTouchEvent(action: Int, x: Float, y: Float, pointerId: Int = 0) {
        injectTapAt(x, y)
    }

    fun injectKeyEvent(action: Int, keyCode: Int, source: Int = InputDevice.SOURCE_KEYBOARD) {
        val now = SystemClock.uptimeMillis()
        val event = KeyEvent(now, now, action, keyCode, 0, 0, VIRTUAL_DEVICE_ID, 0, 0, source)
        inject(event)
    }

    fun injectMouseEvent(dx: Float, dy: Float) {
        val now = SystemClock.uptimeMillis()
        val coords = arrayOf(MotionEvent.PointerCoords().apply {
            x = dx
            y = dy
        })
        val props = arrayOf(MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        })
        val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 1, props, coords, 0, 0, 1f, 1f, VIRTUAL_DEVICE_ID, 0, InputDevice.SOURCE_MOUSE, 0)
        inject(event)
        event.recycle()
    }

    fun injectMouseButton(isDown: Boolean) {
        val now = SystemClock.uptimeMillis()
        val action = if (isDown) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_UP
        val buttonState = if (isDown) MotionEvent.BUTTON_PRIMARY else 0
        val coords = arrayOf(MotionEvent.PointerCoords().apply {
            x = 0f
            y = 0f
        })
        val props = arrayOf(MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        })
        val event = MotionEvent.obtain(now, now, action, 1, props, coords, 0, buttonState, 1f, 1f, VIRTUAL_DEVICE_ID, 0, InputDevice.SOURCE_MOUSE, 0)
        inject(event)
        event.recycle()
    }

    fun injectAxisEvent(axes: Map<Int, Float>, source: Int = InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK) {
        val now = SystemClock.uptimeMillis()
        val coords = arrayOf(MotionEvent.PointerCoords().apply {
            axes.forEach { (axis, value) -> setAxisValue(axis, value) }
        })
        val props = arrayOf(MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_UNKNOWN
        })
        val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 1, props, coords, 0, 0, 1f, 1f, VIRTUAL_DEVICE_ID, 0, source, 0)
        inject(event)
        event.recycle()
    }

    private fun inject(event: InputEvent) {
        val method = injectMethod ?: return
        val instance = iimInstance ?: return
        try {
            method.invoke(instance, event, INJECT_MODE_ASYNC)
        } catch (e: Exception) {
            Log.e(TAG, "Injection error", e)
        }
    }
}
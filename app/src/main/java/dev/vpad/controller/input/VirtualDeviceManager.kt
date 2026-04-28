package dev.vpad.controller.input

import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * Pure Button Injection Engine (Phase 17).
 * Removed all Mouse/Trackpad injection logic to ensure 100% GFN synergy.
 */
object VirtualDeviceManager {
    private const val TAG = "VirtualDeviceManager"
    private const val INJECT_MODE_ASYNC = 0
    private const val VIRTUAL_DEVICE_ID = 9999 

    private var iimInstance: Any? = null
    private var injectMethod: java.lang.reflect.Method? = null

    fun initialize(packageName: String? = null): Boolean {
        if (!Shizuku.pingBinder()) {
            Log.e(TAG, "Shizuku not reachable.")
            return false
        }
        
        return try {
            val rawBinder: IBinder = SystemServiceHelper.getSystemService("input")
                ?: error("null binder for 'input'")
            val wrapped = ShizukuBinderWrapper(rawBinder)
            val stubClass = Class.forName("android.hardware.input.IInputManager\$Stub")
            iimInstance = stubClass
                .getDeclaredMethod("asInterface", IBinder::class.java)
                .also { it.isAccessible = true }
                .invoke(null, wrapped)
            
            injectMethod = iimInstance!!.javaClass
                .getMethod("injectInputEvent", InputEvent::class.java, Int::class.java)
                .also { it.isAccessible = true }
            
            Log.i(TAG, "Pure Injection Engine ready.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Input engine init failed", e)
            false
        }
    }

    fun stop() {}

    fun injectKeyEvent(action: Int, keyCode: Int, metaState: Int = 0, source: Int = InputDevice.SOURCE_KEYBOARD) {
        val now = SystemClock.uptimeMillis()
        val event = KeyEvent(now, now, action, keyCode, 0, metaState, VIRTUAL_DEVICE_ID, 0, KeyEvent.FLAG_FROM_SYSTEM, source)
        inject(event)
    }

    fun injectAxisEvent(axisMap: Map<Int, Float>, source: Int = InputDevice.SOURCE_JOYSTICK, buttonState: Int = 0) {
        val now = SystemClock.uptimeMillis()
        val coords = arrayOf(MotionEvent.PointerCoords().apply {
            axisMap.forEach { (axis, value) -> setAxisValue(axis, value) }
        })
        val props = arrayOf(MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_UNKNOWN })
        val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 1, props, coords, 0, buttonState, 1f, 1f, VIRTUAL_DEVICE_ID, 0, source, 0)
        inject(event)
        event.recycle()
    }

    fun injectMouseEvent(dx: Float, dy: Float, buttonState: Int = 0) {
        val now = SystemClock.uptimeMillis()
        val coords = arrayOf(MotionEvent.PointerCoords().apply {
            setAxisValue(MotionEvent.AXIS_RELATIVE_X, dx)
            setAxisValue(MotionEvent.AXIS_RELATIVE_Y, dy)
        })
        val props = arrayOf(MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_MOUSE })
        
        // SOURCE_MOUSE_RELATIVE (Added in API 26) ensures relative deltas are dispatched cleanly to games like GeForce Now without screen-jumping
        // Value of InputDevice.SOURCE_MOUSE_RELATIVE is 0x00020004
        val source = 131076
        
        val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 1, props, coords, 0, buttonState, 1f, 1f, VIRTUAL_DEVICE_ID, 0, source, 0)
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

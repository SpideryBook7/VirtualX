package dev.vpad.controller.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import dev.vpad.controller.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class VPadAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var repo: SettingsRepository? = null

    companion object {
        @Volatile
        var instance: VPadAccessibilityService? = null
            private set

        @Volatile
        var volumeMacrosEnabled: Boolean = true

        fun isEnabled(): Boolean = instance != null

        fun dispatchTap(x: Float, y: Float, durationMs: Long = 50L): Boolean {
            val service = instance ?: return false
            val path = Path().apply {
                moveTo(x, y)
                lineTo(x + 1f, y + 1f)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs, false)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            return service.dispatchGesture(gesture, null, null)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        repo = SettingsRepository(applicationContext)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (volumeMacrosEnabled && event.action == KeyEvent.ACTION_DOWN) {
            val repository = repo
            if (repository != null) {
                if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    scope.launch { triggerGlooMacro(repository) }
                    return true
                } else if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    scope.launch { triggerAwmMacro(repository) }
                    return true
                }
            }
        }
        return super.onKeyEvent(event)
    }

    private suspend fun triggerGlooMacro(repo: SettingsRepository) {
        val settings = repo.settings.firstOrNull() ?: return
        val dm = resources.displayMetrics
        val screenW = dm.widthPixels.toFloat()
        val screenH = dm.heightPixels.toFloat()
        val targetRadius = 24f * settings.buttonScale * dm.density

        val glooOffset = settings.layoutOffsets["target_gloo"] ?: Pair(screenW * 0.18f, screenH * 0.60f)
        val glooX = glooOffset.first + targetRadius
        val glooY = glooOffset.second + targetRadius

        val crouchOffset = settings.layoutOffsets["target_crouch"] ?: Pair(screenW * 0.88f, screenH * 0.82f)
        val crouchX = crouchOffset.first + targetRadius
        val crouchY = crouchOffset.second + targetRadius

        dev.vpad.controller.input.VirtualDeviceManager.injectTapAt(glooX, glooY, 40L)
        delay(140L)
        dev.vpad.controller.input.VirtualDeviceManager.injectTapAt(crouchX, crouchY, 40L)
    }

    private suspend fun triggerAwmMacro(repo: SettingsRepository) {
        val settings = repo.settings.firstOrNull() ?: return
        val dm = resources.displayMetrics
        val screenW = dm.widthPixels.toFloat()
        val screenH = dm.heightPixels.toFloat()
        val targetRadius = 24f * settings.buttonScale * dm.density

        val wep1Offset = settings.layoutOffsets["target_wep1"] ?: Pair(screenW * 0.72f, screenH * 0.08f)
        val wep1X = wep1Offset.first + targetRadius
        val wep1Y = wep1Offset.second + targetRadius

        val wep2Offset = settings.layoutOffsets["target_wep2"] ?: Pair(screenW * 0.84f, screenH * 0.08f)
        val wep2X = wep2Offset.first + targetRadius
        val wep2Y = wep2Offset.second + targetRadius

        dev.vpad.controller.input.VirtualDeviceManager.injectTapAt(wep1X, wep1Y, 40L)
        delay(140L)
        dev.vpad.controller.input.VirtualDeviceManager.injectTapAt(wep2X, wep2Y, 40L)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}

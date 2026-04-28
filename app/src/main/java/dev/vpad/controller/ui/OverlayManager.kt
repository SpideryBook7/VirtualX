package dev.vpad.controller.ui

import android.content.Context
import android.graphics.Point
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.vpad.controller.data.SettingsRepository
import dev.vpad.controller.data.VPadSettings
import dev.vpad.controller.input.InputProcessor
import dev.vpad.controller.input.GyroManager
import dev.vpad.controller.ui.compose.AtomicControl
import dev.vpad.controller.ui.compose.TogglePill
import dev.vpad.controller.ui.theme.VPadTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Unified Absolute Layout (Phase 20 - Direct Drag & Default Hidden).
 * Solves the UI thread lockup on Meizu right-half drags by skipping Datastore in real-time.
 */
class OverlayManager(
    private val context: Context,
    private val inputProcessor: InputProcessor
) : LifecycleOwner, SavedStateRegistryOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val activeWindows = mutableMapOf<String, ComposeView>()
    private var pillView: ComposeView? = null

    private val controlsVisible = mutableStateOf(false)
    private val currentSettings = mutableStateOf(VPadSettings())
    private var lastScreenWidth = 0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val repo  = SettingsRepository(context)
    private val gyroManager = GyroManager(context, inputProcessor)

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val CONTROL_IDS = listOf(
        "analog_left", "analog_right", "dpad_up", "dpad_down", "dpad_left", "dpad_right",
        "btn_a", "btn_b", "btn_x", "btn_y",
        "btn_l1", "btn_l2", "btn_r1", "btn_r2", "btn_rm",
        "btn_select", "btn_start"
    )

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun showOverlay() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        scope.launch {
            repo.settings.collectLatest { settings ->
                currentSettings.value = settings
                
                gyroManager.sensitivity = settings.gyroSensitivity
                gyroManager.invertY = settings.gyroInvertY
                gyroManager.isEnabled = settings.gyroEnabled && (controlsVisible.value || settings.editMode)
                
                if (activeWindows.isEmpty()) {
                    if (controlsVisible.value || settings.editMode) createAllWindows()
                } else {
                    updateAllWindowPositions()
                }
            }
        }

        if (pillView == null) createPill()
    }

    private fun getRealScreenSize(): Point {
        val point = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            point.x = metrics.bounds.width()
            point.y = metrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(point)
        }
        return point
    }

    private fun createAllWindows() {
        CONTROL_IDS.forEach { id ->
            if (activeWindows.containsKey(id)) return@forEach
            
            val offset = getSafeOffset(id, currentSettings.value)
            
            val view = composeView {
                VPadTheme {
                    AtomicControl(
                        id = id,
                        initialX = offset.first,
                        initialY = offset.second,
                        inputProcessor = inputProcessor,
                        settings = currentSettings.value,
                        onDrag = { finalX, finalY ->
                            // Update WindowManager DIRECTLY without touching Datastore or forcing recompositions
                            val currentView = activeWindows[id] ?: return@AtomicControl
                            (currentView.layoutParams as? WindowManager.LayoutParams)?.let { p ->
                                p.x = finalX.toInt()
                                p.y = finalY.toInt()
                                windowManager.updateViewLayout(currentView, p)
                            }
                        },
                        onDragEnd = { endOffset ->
                            // ONLY save to DB when touch is released
                            scope.launch { repo.updateComponentOffset(id, endOffset) }
                        }
                    )
                }
            }
            activeWindows[id] = view
            
            val params = buildParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                id
            ).apply {
                x = offset.first.toInt()
                y = offset.second.toInt()
            }

            try {
                windowManager.addView(view, params)
            } catch (e: Exception) { Log.e("VPad", "Add $id failed", e) }
        }
    }

    private fun updateAllWindowPositions() {
        val settings = currentSettings.value
        if (!controlsVisible.value && !settings.editMode) {
            removeAllControls()
            return
        }

        val screen = getRealScreenSize()
        val rotated = lastScreenWidth != 0 && lastScreenWidth != screen.x
        lastScreenWidth = screen.x

        if (activeWindows.isEmpty()) {
            createAllWindows()
            return
        }

        activeWindows.forEach { (id, view) ->
            val params = view.layoutParams as? WindowManager.LayoutParams ?: return@forEach
            val offset = getSafeOffset(id, settings)
            params.x = offset.first.toInt()
            params.y = offset.second.toInt()
            params.alpha = settings.overlayOpacity.coerceIn(0.1f, 1.0f)
            
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {}
        }

        pillView?.let { pill ->
            val params = pill.layoutParams as? WindowManager.LayoutParams
            if (params != null) {
                if (rotated) {
                    params.x = screen.x / 2 - 150
                    params.y = 50
                    scope.launch { repo.updatePillPosition(params.x, params.y) }
                } else {
                    params.x = params.x.coerceIn(0, screen.x - 350)
                    params.y = params.y.coerceIn(0, screen.y - 150)
                }
                try { windowManager.updateViewLayout(pill, params) } catch (e: Exception) {}
            }
        }
    }

    private fun getSafeOffset(id: String, settings: VPadSettings): Pair<Float, Float> {
        val saved = settings.layoutOffsets[id]
        val screen = getRealScreenSize()
        val isRightSideBtn = id.startsWith("btn_a") || id.startsWith("btn_b") || id.startsWith("btn_x") || id.startsWith("btn_y") || id.contains("_r")
        val isClumped = saved != null && isRightSideBtn && saved.first < (screen.x * 0.25f)
        
        return if (saved == null || isClumped || saved.first < 1f) {
            getInitialPos(id)
        } else {
            saved
        }
    }

    private fun removeAllControls() {
        activeWindows.forEach { (_, view) -> try { windowManager.removeView(view) } catch (e: Exception) {} }
        activeWindows.clear()
    }

    private fun createPill() {
        pillView = composeView {
            VPadTheme {
                TogglePill(
                    isVisible = controlsVisible.value,
                    settings  = currentSettings.value,
                    onToggleVisibility = { 
                        controlsVisible.value = it
                        gyroManager.isEnabled = currentSettings.value.gyroEnabled && it
                        updateAllWindowPositions() 
                    },
                    onToggleEditMode = { scope.launch { repo.toggleEditMode(it) } },
                    onDrag = { drag ->
                        (pillView?.layoutParams as? WindowManager.LayoutParams)?.let { p ->
                            p.x += drag.x.toInt(); p.y += drag.y.toInt()
                            windowManager.updateViewLayout(pillView, p)
                        }
                    },
                    onDragEnd = { (pillView?.layoutParams as? WindowManager.LayoutParams)?.let { p ->
                        scope.launch { repo.updatePillPosition(p.x, p.y) }
                    } },
                    onConfigChange = { updateAllWindowPositions() }
                )
            }
        }
        windowManager.addView(pillView, buildParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, "pill").apply {
            val screen = getRealScreenSize()
            val savedX = currentSettings.value.pillX
            val savedY = currentSettings.value.pillY
            
            x = if (savedX < 0) screen.x / 2 - 150 else savedX.coerceIn(0, screen.x - 350)
            y = if (savedY < 0) 50 else savedY.coerceIn(0, screen.y - 150)
        })
    }

    fun hideOverlay() {
        gyroManager.isEnabled = false
        removeAllControls()
        pillView?.let { try { windowManager.removeView(it) } catch (e: Exception) {} }
        pillView = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    private fun getInitialPos(id: String): Pair<Float, Float> {
        val screen = getRealScreenSize()
        val w = screen.x.toFloat(); val h = screen.y.toFloat()
        
        return when (id) {
            "analog_left" -> Pair(w * 0.08f, h * 0.70f)
            "analog_right"-> Pair(w * 0.75f, h * 0.70f)
            
            "dpad_up"     -> Pair(w * 0.14f, h * 0.45f)
            "dpad_down"   -> Pair(w * 0.14f, h * 0.65f)
            "dpad_left"   -> Pair(w * 0.08f, h * 0.55f)
            "dpad_right"  -> Pair(w * 0.20f, h * 0.55f)
            
            "btn_a"       -> Pair(w * 0.85f, h * 0.75f)
            "btn_b"       -> Pair(w * 0.92f, h * 0.65f)
            "btn_x"       -> Pair(w * 0.78f, h * 0.65f)
            "btn_y"       -> Pair(w * 0.85f, h * 0.55f)
            
            "btn_l1"      -> Pair(w * 0.05f, h * 0.15f)
            "btn_r1"      -> Pair(w * 0.88f, h * 0.15f)
            "btn_l2"      -> Pair(w * 0.05f, h * 0.05f)
            "btn_r2"      -> Pair(w * 0.88f, h * 0.05f)
            
            "btn_rm"      -> Pair(w * 0.75f, h * 0.05f)
            "btn_select"  -> Pair(w * 0.42f, h * 0.05f)
            "btn_start"   -> Pair(w * 0.58f, h * 0.05f)
            else -> Pair(w / 2f, h / 2f)
        }
    }

    private fun composeView(body: @androidx.compose.runtime.Composable () -> Unit): ComposeView =
        ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@OverlayManager)
            setViewTreeSavedStateRegistryOwner(this@OverlayManager)
            setContent { body() }
        }

    private fun buildParams(w: Int, h: Int, id: String): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            w, h,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            dimAmount = 0.0f
        }
    }
}

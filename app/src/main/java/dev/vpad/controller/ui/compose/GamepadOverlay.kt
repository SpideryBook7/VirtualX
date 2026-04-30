package dev.vpad.controller.ui.compose

import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vpad.controller.data.VPadSettings
import dev.vpad.controller.input.InputProcessor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Atomic Floating Widget (Phase 20 - Direct Drag Optimization).
 * Real-time drag coordinates are passed directly to WindowManager to eliminate Datastore IO lag.
 */
@Composable
fun AtomicControl(
    id: String,
    initialX: Float,
    initialY: Float,
    inputProcessor: InputProcessor,
    settings: VPadSettings,
    onDrag: (Float, Float) -> Unit = {_,_ -> },
    onDragEnd: (Pair<Float, Float>) -> Unit = {},
    onRemove: () -> Unit = {}
) {
    val scale = settings.buttonScale
    val alpha = settings.overlayOpacity
    val editMode = settings.editMode
    
    var dragPos by remember(initialX, initialY) { mutableStateOf(Offset(initialX, initialY)) }

    // Synchronize dragPos when a new profile is loaded from Settings
    val storedOffset = settings.layoutOffsets[id]
    LaunchedEffect(storedOffset) {
        if (storedOffset != null) {
            val dx = kotlin.math.abs(storedOffset.first - dragPos.x)
            val dy = kotlin.math.abs(storedOffset.second - dragPos.y)
            // If the settings offset differs significantly from the current drag pos (Profile load), override
            if (dx > 5f || dy > 5f) {
                dragPos = Offset(storedOffset.first, storedOffset.second)
            }
        }
    }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .then(
                if (editMode) {
                    Modifier
                        .background(Color.Yellow.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .pointerInput(id) {
                            detectDragGestures(
                                onDragEnd = { onDragEnd(Pair(dragPos.x, dragPos.y)) }
                            ) { change, dragAmount ->
                                change.consume()
                                dragPos += dragAmount
                                onDrag(dragPos.x, dragPos.y)
                            }
                        }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        when (id) {
            "analog_left" -> AnalogStick(MotionEvent.AXIS_X, MotionEvent.AXIS_Y, inputProcessor, scale, alpha, editMode)
            "analog_right"-> AnalogStick(MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ, inputProcessor, scale, alpha, editMode)
            "trackpad"    -> Trackpad(MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ, inputProcessor, scale, alpha, editMode)
            
            "dpad_up"     -> GameButton("↑", KeyEvent.KEYCODE_DPAD_UP, inputProcessor, Color(0xFF4A4A5A), alpha, scale, editMode, settings.hapticsEnabled)
            "dpad_down"   -> GameButton("↓", KeyEvent.KEYCODE_DPAD_DOWN, inputProcessor, Color(0xFF4A4A5A), alpha, scale, editMode, settings.hapticsEnabled)
            "dpad_left"   -> GameButton("←", KeyEvent.KEYCODE_DPAD_LEFT, inputProcessor, Color(0xFF4A4A5A), alpha, scale, editMode, settings.hapticsEnabled)
            "dpad_right"  -> GameButton("→", KeyEvent.KEYCODE_DPAD_RIGHT, inputProcessor, Color(0xFF4A4A5A), alpha, scale, editMode, settings.hapticsEnabled)
            
            "btn_a"       -> GameButton("A", KeyEvent.KEYCODE_BUTTON_A, inputProcessor, Color(0xFF3CB371), alpha, scale, editMode, settings.hapticsEnabled)
            "btn_b"       -> GameButton("B", KeyEvent.KEYCODE_BUTTON_B, inputProcessor, Color(0xFFDC3545), alpha, scale, editMode, settings.hapticsEnabled)
            "btn_x"       -> GameButton("X", KeyEvent.KEYCODE_BUTTON_X, inputProcessor, Color(0xFF4169E1), alpha, scale, editMode, settings.hapticsEnabled)
            "btn_y"       -> GameButton("Y", KeyEvent.KEYCODE_BUTTON_Y, inputProcessor, Color(0xFFDAA520), alpha, scale, editMode, settings.hapticsEnabled)
            
            "btn_l1"      -> GameButton("LB", KeyEvent.KEYCODE_BUTTON_L1, inputProcessor, Color(0xFF3A3A4A), alpha, scale, editMode, settings.hapticsEnabled)
            "btn_r1"      -> GameButton("RB", KeyEvent.KEYCODE_BUTTON_R1, inputProcessor, Color(0xFF3A3A4A), alpha, scale, editMode, settings.hapticsEnabled)
            "btn_l2"      -> TriggerButton("LT", MotionEvent.AXIS_LTRIGGER, KeyEvent.KEYCODE_BUTTON_L2, inputProcessor, alpha, scale, editMode, settings.hapticsEnabled)
            "btn_r2"      -> TriggerButton("RT", MotionEvent.AXIS_RTRIGGER, KeyEvent.KEYCODE_BUTTON_R2, inputProcessor, alpha, scale, editMode, settings.hapticsEnabled)
            
            "btn_rm"      -> GameButton("RM", InputProcessor.KEYCODE_RM, inputProcessor, Color(0xFF6A5ACD), alpha, scale * 0.9f, editMode, settings.hapticsEnabled)
            "btn_select"  -> GameButton("⊟", KeyEvent.KEYCODE_BUTTON_SELECT, inputProcessor, Color(0xFF2A2A3A), alpha, scale, editMode, settings.hapticsEnabled)
            "btn_start"   -> GameButton("⊞", KeyEvent.KEYCODE_BUTTON_START, inputProcessor, Color(0xFF2A2A3A), alpha, scale, editMode, settings.hapticsEnabled)
        }
        
        if (editMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .pointerInput(Unit) {
                        detectTapGestures { onRemove() }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("×", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TogglePill(
    isVisible: Boolean,
    settings: VPadSettings,
    onToggleVisibility: (Boolean) -> Unit,
    onToggleEditMode: (Boolean) -> Unit,
    onUpdateInputMode: (Int) -> Unit,
    onAddControl: (String) -> Unit = {},
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onConfigChange: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    LaunchedEffect(configuration) {
        onConfigChange()
    }

    val editMode = settings.editMode
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (editMode) Color(0xDD222200) else Color(0xDA000000))
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd = onDragEnd) { change, dragAmount ->
                    change.consume()
                    showMenu = false
                    onDrag(dragAmount)
                }
            }
            .pointerInput(editMode, isVisible) {
                detectTapGestures(onTap = { if (!editMode) showMenu = !showMenu })
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (editMode) {
            val allControls = listOf("analog_left", "trackpad", "dpad_up", "dpad_down", "dpad_left", "dpad_right", "btn_a", "btn_b", "btn_x", "btn_y", "btn_l1", "btn_l2", "btn_r1", "btn_r2", "btn_rm", "btn_select", "btn_start")
            val currentActive = if (settings.activeControls.isEmpty()) allControls else settings.activeControls
            val missing = allControls.filter { !currentActive.contains(it) }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("✥ Mueve Menu", color = Color.Yellow, fontSize = 13.sp)
                    Button(onClick = { onToggleEditMode(false) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4FF))) {
                        Text("Guardar", fontSize = 12.sp)
                    }
                }
                if (missing.isNotEmpty()) {
                    Divider(color = Color.DarkGray, modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 4.dp))
                    Text("Añadir control:", color = Color.Gray, fontSize = 10.sp)
                    // limit to 4 to prevent pill from getting too big
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        missing.take(4).forEach { id ->
                            Button(onClick = { onAddControl(id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                                Text(id.replace("btn_", "").replace("dpad_", ""), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        } else if (showMenu) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MODE SELECT", color = Color.Gray, fontSize = 10.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = { onUpdateInputMode(0) }) {
                        Text("🎮 X-INPUT", color = if (settings.inputMode == 0) Color(0xFF00FF00) else Color.White, fontSize = 12.sp)
                    }
                    TextButton(onClick = { onUpdateInputMode(1) }) {
                        Text("⌨️ PC", color = if (settings.inputMode == 1) Color(0xFF00FF00) else Color.White, fontSize = 12.sp)
                    }
                }
                Divider(color = Color.DarkGray, modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { showMenu = false; onToggleVisibility(!isVisible) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text(if (isVisible) "👁️ Ocultar" else "👁️ Mostrar", color = Color.White, fontSize = 13.sp)
                    }
                    TextButton(onClick = { showMenu = false; onToggleEditMode(true) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("✏️ Editar", color = Color(0xFF00D4FF), fontSize = 13.sp)
                    }
                }
            }
        } else {
            Text("🎮 V-PAD", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AnalogStick(axisX: Int, axisY: Int, inputProcessor: InputProcessor, scale: Float, alpha: Float, isEditMode: Boolean) {
    val baseDp     = (100 * scale).dp
    val thumbDp    = (40 * scale).dp
    val density    = LocalDensity.current
    val baseRadius = with(density) { (baseDp / 2).toPx() }
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(baseDp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
            .pointerInput(isEditMode) {
                if (!isEditMode) {
                    detectDragGestures(
                        onDragStart  = { thumbOffset = Offset.Zero },
                        onDragEnd    = { 
                            thumbOffset = Offset.Zero
                            inputProcessor.updateAxes(mapOf(axisX to 0f, axisY to 0f))
                        },
                        onDragCancel = { 
                            thumbOffset = Offset.Zero
                            inputProcessor.updateAxes(mapOf(axisX to 0f, axisY to 0f))
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val raw  = thumbOffset + dragAmount
                            val dist = sqrt(raw.x.pow(2) + raw.y.pow(2))
                            thumbOffset = if (dist <= baseRadius) raw else raw * (baseRadius / dist)
                            val normX = (thumbOffset.x / baseRadius).coerceIn(-1.05f, 1.05f)
                            val normY = (thumbOffset.y / baseRadius).coerceIn(-1.05f, 1.05f)
                            inputProcessor.updateAxes(mapOf(axisX to normX, axisY to normY))
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(thumbDp).offset { IntOffset(thumbOffset.x.roundToInt(), thumbOffset.y.roundToInt()) }.clip(CircleShape).background(Color.White.copy(alpha=0.85f)))
    }
}

@Composable
fun TriggerButton(label: String, axisCode: Int, keyCode: Int, inputProcessor: InputProcessor, overlayAlpha: Float, scale: Float, isEditMode: Boolean, hapticsEnabled: Boolean) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width((64 * scale).dp)
            .height((28 * scale).dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF222233).copy(alpha = 0.8f))
            .pointerInput(isEditMode) {
                if (!isEditMode) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val down  = event.changes.any { it.pressed }
                            if (down != isPressed) {
                                isPressed = down
                                inputProcessor.updateAxis(axisCode, if (down) 1.0f else 0.0f)
                                inputProcessor.updateButton(keyCode, down)
                                if (down && hapticsEnabled) {
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (12 * scale).sp)
    }
}

@Composable
fun GameButton(label: String, keyCode: Int, inputProcessor: InputProcessor, color: Color, overlayAlpha: Float, scale: Float, isEditMode: Boolean, hapticsEnabled: Boolean) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size((50 * scale).dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.8f))
            .pointerInput(isEditMode) {
                if (!isEditMode) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val down  = event.changes.any { it.pressed }
                            if (down != isPressed) {
                                isPressed = down
                                inputProcessor.updateButton(keyCode, down)
                                if (down && hapticsEnabled) {
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center) { Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (13 * scale).sp, textAlign = TextAlign.Center) }
}

@Composable
fun Trackpad(
    axisX: Int, axisY: Int,
    inputProcessor: InputProcessor,
    scale: Float, alpha: Float, editMode: Boolean
) {
    val sizeW = 180.dp * scale
    val sizeH = 100.dp * scale
    
    var pointerOffset by remember { mutableStateOf(Offset.Zero) }
    
    Box(
        modifier = Modifier
            .size(sizeW, sizeH)
            .alpha(alpha)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x33000000)) // slightly lighter than pure black to see swipe zone
            .pointerInput(editMode) {
                if (editMode) return@pointerInput
                detectDragGestures(
                    onDragStart = { pointerOffset = Offset.Zero },
                    onDragEnd = {
                        pointerOffset = Offset.Zero
                        inputProcessor.updateAxes(mapOf(axisX to 0f, axisY to 0f))
                    },
                    onDragCancel = {
                        pointerOffset = Offset.Zero
                        inputProcessor.updateAxes(mapOf(axisX to 0f, axisY to 0f))
                    }
                ) { change, dragAmount ->
                    change.consume()
                    pointerOffset += dragAmount
                    val maxRadiusX = sizeW.toPx() / 2f
                    val maxRadiusY = sizeH.toPx() / 2f
                    val nx = (pointerOffset.x / maxRadiusX).coerceIn(-1f, 1f)
                    val ny = (pointerOffset.y / maxRadiusY).coerceIn(-1f, 1f)
                    inputProcessor.updateAxes(mapOf(axisX to nx, axisY to ny))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Trackpad", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp * scale)
            Text("←↕→", color = Color.White.copy(alpha = 0.3f), fontSize = 16.sp * scale)
        }
    }
}

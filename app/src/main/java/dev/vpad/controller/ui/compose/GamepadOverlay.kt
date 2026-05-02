package dev.vpad.controller.ui.compose

import android.content.Context
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
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

    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

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
            "analog_left" -> AnalogStick(MotionEvent.AXIS_X, MotionEvent.AXIS_Y, inputProcessor, scale, alpha, editMode, settings.selectedSkin)
            "analog_right"-> AnalogStick(MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ, inputProcessor, scale, alpha, editMode, settings.selectedSkin)
            "trackpad"    -> Trackpad(MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ, inputProcessor, scale, alpha, editMode, settings.inputMode, settings.selectedSkin)
            
            "dpad_up"     -> GameButton("↑", KeyEvent.KEYCODE_DPAD_UP, inputProcessor, Color(0xFF4A4A5A), alpha, scale, editMode, settings.hapticsEnabled, vibrator, skin = settings.selectedSkin)
            "dpad_down"   -> GameButton("↓", KeyEvent.KEYCODE_DPAD_DOWN, inputProcessor, Color(0xFF4A4A5A), alpha, scale, editMode, settings.hapticsEnabled, vibrator, skin = settings.selectedSkin)
            "dpad_left"   -> GameButton("←", KeyEvent.KEYCODE_DPAD_LEFT, inputProcessor, Color(0xFF4A4A5A), alpha, scale, editMode, settings.hapticsEnabled, vibrator, skin = settings.selectedSkin)
            "dpad_right"  -> GameButton("→", KeyEvent.KEYCODE_DPAD_RIGHT, inputProcessor, Color(0xFF4A4A5A), alpha, scale, editMode, settings.hapticsEnabled, vibrator, skin = settings.selectedSkin)
            
            "btn_a"       -> GameButton("A", KeyEvent.KEYCODE_BUTTON_A, inputProcessor, Color(0xFF32CD32), alpha, scale, editMode, settings.hapticsEnabled, vibrator, skin = settings.selectedSkin)
            "btn_b"       -> GameButton("B", KeyEvent.KEYCODE_BUTTON_B, inputProcessor, Color(0xFFFF4500), alpha, scale, editMode, settings.hapticsEnabled, vibrator, skin = settings.selectedSkin)
            "btn_x"       -> GameButton("X", KeyEvent.KEYCODE_BUTTON_X, inputProcessor, Color(0xFF1E90FF), alpha, scale, editMode, settings.hapticsEnabled, vibrator, skin = settings.selectedSkin)
            "btn_y"       -> GameButton("Y", KeyEvent.KEYCODE_BUTTON_Y, inputProcessor, Color(0xFFFFD700), alpha, scale, editMode, settings.hapticsEnabled, vibrator, skin = settings.selectedSkin)
            
            "btn_l1"      -> GameButton("L1", KeyEvent.KEYCODE_BUTTON_L1, inputProcessor, Color(0xFF4A4A5A), alpha, scale, editMode, settings.hapticsEnabled, vibrator, isBumper = true, skin = settings.selectedSkin)
            "btn_l2"      -> GameButton("L2", KeyEvent.KEYCODE_BUTTON_L2, inputProcessor, Color(0xFF4A4A5A), alpha, scale, editMode, settings.hapticsEnabled, vibrator, isBumper = true, skin = settings.selectedSkin)
            "btn_r1"      -> GameButton("R1", KeyEvent.KEYCODE_BUTTON_R1, inputProcessor, Color(0xFF4A4A5A), alpha, scale, editMode, settings.hapticsEnabled, vibrator, isBumper = true, skin = settings.selectedSkin)
            "btn_r2"      -> GameButton("R2", KeyEvent.KEYCODE_BUTTON_R2, inputProcessor, Color(0xFF4A4A5A), alpha, scale, editMode, settings.hapticsEnabled, vibrator, isBumper = true, skin = settings.selectedSkin)
            
            "btn_rm"      -> GameButton("RM", 10001, inputProcessor, Color(0xFF666666), alpha, scale, editMode, settings.hapticsEnabled, vibrator, isBumper = true, skin = settings.selectedSkin)
            
            "btn_select"  -> GameButton("⊟", KeyEvent.KEYCODE_BUTTON_SELECT, inputProcessor, Color(0xFF2A2A3A), alpha, scale, editMode, settings.hapticsEnabled, vibrator, skin = settings.selectedSkin)
            "btn_start"   -> GameButton("⊞", KeyEvent.KEYCODE_BUTTON_START, inputProcessor, Color(0xFF2A2A3A), alpha, scale, editMode, settings.hapticsEnabled, vibrator, skin = settings.selectedSkin)
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
            .width(if (showMenu || editMode) 220.dp else 120.dp)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp, topStart = 8.dp, topEnd = 8.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xE612121A), // Sleek dark metallic
                        Color(0xDD1A1A24)
                    )
                )
            )
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (editMode) {
            val allControls = listOf("analog_left", "trackpad", "dpad_up", "dpad_down", "dpad_left", "dpad_right", "btn_a", "btn_b", "btn_x", "btn_y", "btn_l1", "btn_l2", "btn_r1", "btn_r2", "btn_rm", "btn_select", "btn_start")
            val currentActive = if (settings.activeControls.isEmpty()) allControls else settings.activeControls
            val missing = allControls.filter { !currentActive.contains(it) }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("✥ MOVER", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = { onToggleEditMode(false) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4FF)), modifier = Modifier.height(30.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                        Text("Guardar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
                if (missing.isNotEmpty()) {
                    HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth())
                    Text("AÑADIR CONTROL", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        missing.take(3).forEach { id ->
                            Button(onClick = { onAddControl(id) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333344)), modifier = Modifier.height(28.dp).weight(1f), contentPadding = PaddingValues(0.dp)) {
                                Text(id.replace("btn_", "").replace("dpad_", ""), fontSize = 9.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        } else if (showMenu) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MODE SELECT", color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { onUpdateInputMode(0) }) {
                        Text("🎮 X-INPUT", color = if (settings.inputMode == 0) Color(0xFF00FFCC) else Color.White, fontSize = 12.sp, fontWeight = if (settings.inputMode == 0) FontWeight.Bold else FontWeight.Normal)
                    }
                    TextButton(onClick = { onUpdateInputMode(1) }) {
                        Text("⌨️ PC", color = if (settings.inputMode == 1) Color(0xFF00FFCC) else Color.White, fontSize = 12.sp, fontWeight = if (settings.inputMode == 1) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { showMenu = false; onToggleVisibility(!isVisible) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text(if (isVisible) "👁️ Ocultar" else "👁️ Mostrar", color = Color.White, fontSize = 12.sp)
                    }
                    TextButton(onClick = { showMenu = false; onToggleEditMode(true) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("✏️ Editar", color = Color(0xFF00D4FF), fontSize = 12.sp)
                    }
                }
            }
        } else {
            Text("V-PAD", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

@Composable
fun GameButton(
    label: String, keyCode: Int, inputProcessor: InputProcessor, 
    color: Color, alpha: Float, scale: Float, isEditMode: Boolean, hapticsEnabled: Boolean, vibrator: Vibrator, isBumper: Boolean = false, skin: String = "Default"
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val width = if (isBumper) (60 * scale).dp else (45 * scale).dp
    val height = if (isBumper) (35 * scale).dp else (45 * scale).dp
    
    val isNeon = skin == "Neon Cyberpunk"
    val bgColor = if (isNeon) Color(0xFF0F0F1A) else color
    val borderColor = if (isNeon) (if (isPressed) Color(0xFFFF00FF) else Color(0xFF00FFFF)) else Color.Transparent
    val borderWidth = if (isNeon) 2.dp else 0.dp
    val textColor = if (isNeon) borderColor else Color.White
    
    Box(
        modifier = Modifier
            .size(width, height)
            .alpha(if (isPressed) 1f else alpha)
            .clip(if (isBumper) RoundedCornerShape(8.dp) else CircleShape)
            .background(bgColor)
            .then(if (isNeon) Modifier.border(borderWidth, borderColor, if (isBumper) RoundedCornerShape(8.dp) else CircleShape) else Modifier)
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
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(15)
                                    }
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center) { Text(label, color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * scale).sp, textAlign = TextAlign.Center) }
}

@Composable
fun AnalogStick(axisX: Int, axisY: Int, inputProcessor: InputProcessor, scale: Float, alpha: Float, isEditMode: Boolean, skin: String = "Default") {
    val baseDp     = (100 * scale).dp
    val thumbDp    = (40 * scale).dp
    val density    = LocalDensity.current
    val baseRadius = with(density) { (baseDp / 2).toPx() }
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    val isNeon = skin == "Neon Cyberpunk"
    val baseBrush = if (isNeon) Brush.radialGradient(listOf(Color(0x2200FFFF), Color(0x0500FFFF))) else Brush.radialGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF)))
    val borderColor = if (isNeon) Color(0x8800FFFF) else Color.Transparent

    Box(
        modifier = Modifier
            .size(baseDp)
            .clip(CircleShape)
            .background(baseBrush)
            .then(if (isNeon) Modifier.border(1.dp, borderColor, CircleShape) else Modifier)
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
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.x.toInt(), thumbOffset.y.toInt()) }
                .size(thumbDp)
                .clip(CircleShape)
                .background(if (isNeon) Color(0xFFFF00FF) else Color(0x88FFFFFF))
                .then(if (isNeon) Modifier.border(2.dp, Color(0xFF00FFFF), CircleShape) else Modifier)
        )
    }
}

@Composable
fun Trackpad(
    axisX: Int, axisY: Int,
    inputProcessor: InputProcessor,
    scale: Float, alpha: Float, editMode: Boolean, inputMode: Int, skin: String = "Default"
) {
    val sizeW = 180.dp * scale
    val sizeH = 100.dp * scale
    
    var pointerOffset by remember { mutableStateOf(Offset.Zero) }
    
    val isNeon = skin == "Neon Cyberpunk"
    val bgColor = if (isNeon) Color(0xAA0A0A1A) else Color(0x33000000)
    val borderColor = if (isNeon) Color(0xFF00FFFF) else Color.Transparent
    
    Box(
        modifier = Modifier
            .size(sizeW, sizeH)
            .alpha(alpha)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .then(if (isNeon) Modifier.border(2.dp, borderColor, RoundedCornerShape(16.dp)) else Modifier)
            .pointerInput(editMode) {
                if (editMode) return@pointerInput
                detectDragGestures(
                    onDragStart = { pointerOffset = Offset.Zero },
                    onDragEnd = {
                        pointerOffset = Offset.Zero
                        if (inputMode == 0) inputProcessor.updateAxes(mapOf(axisX to 0f, axisY to 0f))
                    },
                    onDragCancel = {
                        pointerOffset = Offset.Zero
                        if (inputMode == 0) inputProcessor.updateAxes(mapOf(axisX to 0f, axisY to 0f))
                    }
                ) { change, dragAmount ->
                    change.consume()
                    if (inputMode == 1) {
                        inputProcessor.injectRelativeMouse(dragAmount.x, dragAmount.y)
                    } else {
                        pointerOffset += dragAmount
                        val maxRadiusX = sizeW.toPx() / 2f
                        val maxRadiusY = sizeH.toPx() / 2f
                        val nx = (pointerOffset.x / maxRadiusX).coerceIn(-1f, 1f)
                        val ny = (pointerOffset.y / maxRadiusY).coerceIn(-1f, 1f)
                        inputProcessor.updateAxes(mapOf(axisX to nx, axisY to ny))
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isNeon) {
            // Cool Neon Grid Line
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x4400FFFF)))
            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color(0x4400FFFF)))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Trackpad", color = if (isNeon) Color(0xFF00FFFF) else Color.White.copy(alpha = 0.5f), fontSize = 12.sp * scale, fontWeight = if (isNeon) FontWeight.Bold else FontWeight.Normal)
            Text("←↕→", color = if (isNeon) Color(0xFFFF00FF) else Color.White.copy(alpha = 0.3f), fontSize = 16.sp * scale)
        }
    }
}

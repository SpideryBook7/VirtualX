package dev.vpad.controller.ui.screens

import android.content.Intent
import android.provider.Settings
import android.net.Uri
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.vpad.controller.data.SettingsRepository
import dev.vpad.controller.data.VPadSettings
import dev.vpad.controller.input.InputProcessor
import dev.vpad.controller.service.VPadService
import dev.vpad.controller.ui.theme.*
import kotlinx.coroutines.launch

val availablePcKeys = listOf(
    KeyEvent.KEYCODE_SPACE to "Space",
    KeyEvent.KEYCODE_ENTER to "Enter",
    KeyEvent.KEYCODE_ESCAPE to "Esc",
    KeyEvent.KEYCODE_TAB to "Tab",
    KeyEvent.KEYCODE_SHIFT_LEFT to "L-Shift",
    KeyEvent.KEYCODE_CTRL_LEFT to "L-Ctrl",
    KeyEvent.KEYCODE_ALT_LEFT to "L-Alt",
    KeyEvent.KEYCODE_DEL to "Backspace",
    KeyEvent.KEYCODE_Q to "Q",
    KeyEvent.KEYCODE_W to "W",
    KeyEvent.KEYCODE_E to "E",
    KeyEvent.KEYCODE_R to "R",
    KeyEvent.KEYCODE_T to "T",
    KeyEvent.KEYCODE_Y to "Y",
    KeyEvent.KEYCODE_U to "U",
    KeyEvent.KEYCODE_I to "I",
    KeyEvent.KEYCODE_O to "O",
    KeyEvent.KEYCODE_P to "P",
    KeyEvent.KEYCODE_A to "A",
    KeyEvent.KEYCODE_S to "S",
    KeyEvent.KEYCODE_D to "D",
    KeyEvent.KEYCODE_F to "F",
    KeyEvent.KEYCODE_G to "G",
    KeyEvent.KEYCODE_H to "H",
    KeyEvent.KEYCODE_J to "J",
    KeyEvent.KEYCODE_K to "K",
    KeyEvent.KEYCODE_L to "L",
    KeyEvent.KEYCODE_Z to "Z",
    KeyEvent.KEYCODE_X to "X",
    KeyEvent.KEYCODE_C to "C",
    KeyEvent.KEYCODE_V to "V",
    KeyEvent.KEYCODE_B to "B",
    KeyEvent.KEYCODE_N to "N",
    KeyEvent.KEYCODE_M to "M",
    KeyEvent.KEYCODE_1 to "1",
    KeyEvent.KEYCODE_2 to "2",
    KeyEvent.KEYCODE_3 to "3",
    KeyEvent.KEYCODE_4 to "4",
    KeyEvent.KEYCODE_DPAD_UP to "Arrow Up",
    KeyEvent.KEYCODE_DPAD_DOWN to "Arrow Down",
    KeyEvent.KEYCODE_DPAD_LEFT to "Arrow Left",
    KeyEvent.KEYCODE_DPAD_RIGHT to "Arrow Right",
    InputProcessor.KEYCODE_RM to "Mouse Right-Click"
)

fun pcKeyName(code: Int): String = availablePcKeys.find { it.first == code }?.second ?: "Key $code"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    repo: SettingsRepository
) {
    val context = LocalContext.current
    val settings by repo.settings.collectAsState(initial = VPadSettings())
    val scope = rememberCoroutineScope()
    var showKeyPickerFor by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VPadBackground)
            )
        },
        containerColor = VPadBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsSection(title = "Controls & Physics") {
                SettingsSlider(
                    label = "Stick Sensitivity",
                    value = settings.sensitivity,
                    range = 0.5f..2.0f,
                    formatted = { "%.1fx".format(it) },
                    onChanged = { scope.launch { repo.updateSensitivity(it) } }
                )
                SettingsSlider(
                    label = "Analog Deadzone",
                    value = settings.deadZone,
                    range = 0.0f..0.3f,
                    formatted = { "%.0f%%".format(it * 100) },
                    onChanged = { scope.launch { repo.updateDeadZone(it) } }
                )
                SettingsSlider(
                    label = "Response Curve",
                    value = settings.curveExponent,
                    range = 1.0f..3.0f,
                    formatted = { "%.1f".format(it) },
                    onChanged = { scope.launch { repo.updateCurveExponent(it) } }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Haptic Feedback", color = VPadOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Vibrate on button press", color = VPadOnSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.hapticsEnabled,
                        onCheckedChange = { scope.launch { repo.updateHapticsEnabled(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = VPadPrimary, checkedTrackColor = VPadPrimary.copy(alpha = 0.3f))
                    )
                }
            }

            SettingsSection(title = "Overlay Appearance") {
                SettingsSlider(
                    label = "Opacity",
                    value = settings.overlayOpacity,
                    range = 0.1f..1.0f,
                    formatted = { "%.0f%%".format(it * 100) },
                    onChanged = { scope.launch { repo.updateOpacity(it) } }
                )
                SettingsSlider(
                    label = "Button Size",
                    value = settings.buttonScale,
                    range = 0.6f..1.4f,
                    formatted = { "%.0f%%".format(it * 100) },
                    onChanged = { scope.launch { repo.updateButtonScale(it) } }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Input Profile", color = VPadOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(if (settings.inputMode == 0) "Gamepad (X-Input)" else "PC (Keyboard & Mouse)", color = VPadOnSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.inputMode == 1,
                        onCheckedChange = { scope.launch { repo.updateInputMode(if (it) 1 else 0) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = VPadPrimary, checkedTrackColor = VPadPrimary.copy(alpha = 0.3f))
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Edit Layout Mode", color = VPadOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Drag buttons to move them", color = VPadOnSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.editMode,
                        onCheckedChange = { scope.launch { repo.toggleEditMode(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = VPadPrimary, checkedTrackColor = VPadPrimary.copy(alpha = 0.3f))
                    )
                }
            }

            SettingsSection(title = "Gyroscope Aiming") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Enable Gyroscope", color = VPadOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Steer camera by tilting device", color = VPadOnSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.gyroEnabled,
                        onCheckedChange = { scope.launch { repo.updateGyroEnabled(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = VPadPrimary, checkedTrackColor = VPadPrimary.copy(alpha = 0.3f))
                    )
                }

                if (settings.gyroEnabled) {
                    SettingsSlider(
                        label = "Gyro Sensitivity",
                        value = settings.gyroSensitivity,
                        range = 0.1f..4.0f,
                        formatted = { "%.1fx".format(it) },
                        onChanged = { scope.launch { repo.updateGyroSensitivity(it) } }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Invert Y-Axis", color = VPadOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Reverse up/down tilt", color = VPadOnSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.gyroInvertY,
                            onCheckedChange = { scope.launch { repo.updateGyroInvertY(it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = VPadPrimary, checkedTrackColor = VPadPrimary.copy(alpha = 0.3f))
                        )
                    }
                }
            }

            SettingsSection(title = "Layout Profiles") {
                var newProfileName by remember { mutableStateOf("") }
                var showNewProfileDialog by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Active Profile", color = VPadOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(settings.activeProfileName, color = VPadPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { showNewProfileDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = VPadPrimary)) {
                        Text("Save As New", fontSize = 12.sp)
                    }
                }

                if (settings.savedProfiles.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFF2A2A3A).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    settings.savedProfiles.keys.forEach { profileName ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(profileName, color = VPadOnSurface, fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { scope.launch { repo.loadProfile(profileName) } }) {
                                    Text("Load", color = VPadPrimary)
                                }
                                TextButton(onClick = { scope.launch { repo.deleteProfile(profileName) } }) {
                                    Text("Delete", color = Color(0xFFDC3545))
                                }
                            }
                        }
                    }
                }

                if (showNewProfileDialog) {
                    Dialog(onDismissRequest = { showNewProfileDialog = false }) {
                        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF2A2A3A)) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("New Profile Name", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                OutlinedTextField(
                                    value = newProfileName,
                                    onValueChange = { newProfileName = it },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = VPadPrimary,
                                        unfocusedBorderColor = VPadOnSurface.copy(alpha=0.5f)
                                    )
                                )
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { showNewProfileDialog = false }) { Text("Cancel", color = VPadOnSurface) }
                                    TextButton(onClick = { 
                                        val nameToSave = newProfileName.trim()
                                        if (nameToSave.isNotBlank()) {
                                            scope.launch { repo.saveCurrentLayoutAsProfile(nameToSave) }
                                            showNewProfileDialog = false
                                            newProfileName = ""
                                        }
                                    }) { Text("Save", color = VPadPrimary) }
                                }
                            }
                        }
                    }
                }
            }

            if (settings.inputMode == 1) {
                SettingsSection(title = "PC Key Bindings") {
                    val gamepadKeys = listOf(
                        KeyEvent.KEYCODE_BUTTON_A to "Button A",
                        KeyEvent.KEYCODE_BUTTON_B to "Button B",
                        KeyEvent.KEYCODE_BUTTON_X to "Button X",
                        KeyEvent.KEYCODE_BUTTON_Y to "Button Y",
                        KeyEvent.KEYCODE_BUTTON_L1 to "Left Bumper (LB)",
                        KeyEvent.KEYCODE_BUTTON_R1 to "Right Bumper (RB)",
                        KeyEvent.KEYCODE_BUTTON_L2 to "Left Trigger (LT)",
                        KeyEvent.KEYCODE_BUTTON_R2 to "Right Trigger (RT)",
                        KeyEvent.KEYCODE_BUTTON_SELECT to "Select",
                        KeyEvent.KEYCODE_BUTTON_START to "Start",
                        KeyEvent.KEYCODE_DPAD_UP to "D-Pad Up",
                        KeyEvent.KEYCODE_DPAD_DOWN to "D-Pad Down",
                        KeyEvent.KEYCODE_DPAD_LEFT to "D-Pad Left",
                        KeyEvent.KEYCODE_DPAD_RIGHT to "D-Pad Right",
                        InputProcessor.KEYCODE_RM to "Right Mouse (RM)"
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        gamepadKeys.forEach { (gameKey, label) ->
                            val currentMapped = settings.pcKeyMap[gameKey] ?: InputProcessor.DefaultPcKeyMap[gameKey] ?: gameKey
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showKeyPickerFor = gameKey }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, color = VPadOnSurface, fontSize = 15.sp)
                                Text(pcKeyName(currentMapped), color = VPadPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color(0xFF2A2A3A).copy(alpha = 0.5f))
                        }
                    }
                }
            }

            SettingsSection(title = "About") {
                Text("Version 1.0.0", color = VPadOnSurface.copy(alpha = 0.7f), fontSize = 13.sp)
                Text("Virtual Gamepad Controller", color = VPadOnSurface.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
    }

    if (showKeyPickerFor != null) {
        Dialog(onDismissRequest = { showKeyPickerFor = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2A2A3A)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Select PC Key Mapping", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                    LazyColumn(modifier = Modifier.weight(1f, fill=false).heightIn(max=400.dp)) {
                        items(availablePcKeys) { (keyCode, keyName) ->
                            Text(
                                text = keyName,
                                color = VPadOnSurface,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        val selectedKey = showKeyPickerFor
                                        if (selectedKey != null) {
                                            scope.launch { repo.updatePcKeyMapping(selectedKey, keyCode) }
                                            showKeyPickerFor = null 
                                        }
                                    }
                                    .padding(vertical = 14.dp, horizontal = 12.dp)
                            )
                        }
                    }
                    TextButton(onClick = { showKeyPickerFor = null }, modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) {
                        Text("Cancel", color = VPadPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2A2A3A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title, color = VPadPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        content()
    }
}

@Composable
fun SettingsSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    formatted: (Float) -> String,
    onChanged: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = VPadOnSurface, fontSize = 14.sp)
            Text(formatted(value), color = VPadOnSurface.copy(alpha = 0.7f), fontSize = 13.sp)
        }
        Slider(
            value = value,
            onValueChange = onChanged,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = VPadPrimary,
                activeTrackColor = VPadPrimary,
                inactiveTrackColor = VPadOnSurface.copy(alpha = 0.2f)
            )
        )
    }
}

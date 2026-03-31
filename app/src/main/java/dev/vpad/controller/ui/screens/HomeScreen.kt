package dev.vpad.controller.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vpad.controller.ui.theme.*
import rikka.shizuku.Shizuku

enum class ShizukuStatus { NOT_RUNNING, NO_PERMISSION, GRANTED }

fun getShizukuStatus(): ShizukuStatus = try {
    if (!Shizuku.pingBinder()) ShizukuStatus.NOT_RUNNING
    else if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) ShizukuStatus.GRANTED
    else ShizukuStatus.NO_PERMISSION
} catch (e: Exception) { ShizukuStatus.NOT_RUNNING }

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val context = LocalContext.current
    var shizukuStatus by remember { mutableStateOf(getShizukuStatus()) }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val allReady = shizukuStatus == ShizukuStatus.GRANTED && overlayGranted

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1525), VPadBackground)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Logo / Header ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(VPadPrimary.copy(alpha = 0.25f), Color.Transparent))
                    )
                    .border(2.dp, VPadPrimary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🎮", fontSize = 36.sp)
            }

            Text(
                "V-PAD",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = VPadPrimary
            )
            Text(
                "Virtual Gamepad Bridge",
                fontSize = 14.sp,
                color = VPadOnSurface.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // ── Permission cards ─────────────────────────────────────────────
            PermCard(
                step = "1",
                title = "Shizuku Permission",
                description = when (shizukuStatus) {
                    ShizukuStatus.NOT_RUNNING    -> "Shizuku is not running. Open Shizuku → Start via Wireless ADB."
                    ShizukuStatus.NO_PERMISSION  -> "Shizuku is active — tap Grant to allow gamepad injection."
                    ShizukuStatus.GRANTED        -> "Connected — privileged input injection ready."
                },
                isGranted = shizukuStatus == ShizukuStatus.GRANTED,
                actionLabel = if (shizukuStatus == ShizukuStatus.NO_PERMISSION) "Grant" else "Refresh",
                onAction = {
                    if (shizukuStatus == ShizukuStatus.NO_PERMISSION) {
                        val listener = object : Shizuku.OnRequestPermissionResultListener {
                            override fun onRequestPermissionResult(code: Int, result: Int) {
                                Shizuku.removeRequestPermissionResultListener(this)
                                shizukuStatus = getShizukuStatus()
                            }
                        }
                        Shizuku.addRequestPermissionResultListener(listener)
                        Shizuku.requestPermission(1001)
                    } else {
                        shizukuStatus = getShizukuStatus()
                    }
                }
            )

            PermCard(
                step = "2",
                title = "Draw Over Apps",
                description = if (overlayGranted) "Granted — overlay can appear over games."
                              else "Required to display the gamepad over other apps.",
                isGranted = overlayGranted,
                actionLabel = "Open Settings",
                onAction = {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"))
                    )
                    overlayGranted = Settings.canDrawOverlays(context)
                }
            )

            Spacer(Modifier.height(8.dp))

            // ── Launch button ────────────────────────────────────────────────
            Button(
                onClick = onStartService,
                enabled = allReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VPadPrimary,
                    contentColor   = Color(0xFF001F26),
                    disabledContainerColor = VPadSurface2
                )
            ) {
                Text("▶  Launch Overlay", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            OutlinedButton(
                onClick = onStopService,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VPadOnSurface.copy(alpha = 0.7f))
            ) {
                Text("■  Stop Overlay")
            }

            // ── Settings link ────────────────────────────────────────────────
            TextButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = VPadPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sensitivity & Layout Settings", color = VPadPrimary, fontSize = 13.sp)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Requires Shizuku  •  No Root  •  Gamepad injection mode",
                fontSize = 11.sp,
                color = VPadOnSurface.copy(alpha = 0.3f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PermCard(
    step: String,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    val borderColor = if (isGranted) VPadPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
    val accentColor = if (isGranted) VPadPrimary else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VPadSurface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isGranted) "✓" else step, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = VPadOnSurface, fontSize = 14.sp)
                Text(description, color = VPadOnSurface.copy(alpha = 0.55f), fontSize = 12.sp)
            }

            if (!isGranted) {
                FilledTonalButton(
                    onClick = onAction,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = accentColor.copy(alpha = 0.15f), contentColor = accentColor)
                ) {
                    Text(actionLabel, fontSize = 12.sp)
                }
            }
        }
    }
}

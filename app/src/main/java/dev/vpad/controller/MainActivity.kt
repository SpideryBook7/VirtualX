package dev.vpad.controller

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.vpad.controller.data.SettingsRepository
import dev.vpad.controller.service.VPadService
import dev.vpad.controller.ui.screens.HomeScreen
import dev.vpad.controller.ui.screens.SettingsScreen
import dev.vpad.controller.ui.theme.VPadTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable True Immersive Fullscreen (Hide Navigation/Status Bars)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())

        enableEdgeToEdge()
        setContent {
            VPadTheme {
                VPadApp()
            }
        }
    }
}

@Composable
fun VPadApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { fadeIn(tween(800)) },
        exitTransition  = { fadeOut(tween(800)) },
        popEnterTransition = { fadeIn(tween(600)) },
        popExitTransition  = { fadeOut(tween(600)) }
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onStartService = { context.startForegroundService(Intent(context, VPadService::class.java)) },
                onStopService  = { context.stopService(Intent(context, VPadService::class.java)) }
            )
        }
        composable("settings") {
            SettingsScreen(
                repo = repo,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
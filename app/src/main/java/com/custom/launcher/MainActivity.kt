package com.custom.launcher

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel

enum class Screen { Home, Drawer, Settings }

class MainActivity : ComponentActivity() {
    private var goHome: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: LauncherViewModel = viewModel()
            val s by vm.settings.collectAsState()
            var screen by remember { mutableStateOf(Screen.Home) }
            goHome = { screen = Screen.Home }

            // Real wallpaper blur behind the drawer/settings (Android 12+, customizable radius)
            LaunchedEffect(screen, s.blur) { applyBlur(if (screen == Screen.Home) 0 else s.blur) }

            when (screen) {
                Screen.Home -> HomeScreen(vm, { screen = Screen.Drawer }, { screen = Screen.Settings })
                Screen.Drawer -> DrawerScreen(vm) { screen = Screen.Home }
                Screen.Settings -> SettingsScreen(vm) { screen = Screen.Home }
            }
        }
    }

    private fun applyBlur(radius: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.also { it.blurBehindRadius = radius }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) goHome?.invoke()
    }
}

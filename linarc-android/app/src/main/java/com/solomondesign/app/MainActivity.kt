package com.solomondesign.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.images.CapturedMediaStore
import com.solomondesign.app.ui.navigation.AppNavHost
import com.solomondesign.app.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Capture metadata is process-scoped demo state, so photo files from a previous process
        // are orphans — wipe them once per process (survives Activity recreation untouched).
        CapturedMediaStore.ensureFreshProcess(this)
        setContent {
            val darkTheme = DemoProjectRepository.darkTheme
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.Black.toArgb())
                    } else {
                        SystemBarStyle.light(Color.White.toArgb(), Color.White.toArgb())
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.Black.toArgb())
                    } else {
                        SystemBarStyle.light(Color.White.toArgb(), Color.White.toArgb())
                    },
                )
            }
            AppTheme(darkTheme = darkTheme) {
                AppNavHost(playLaunchSplash = true, showProjectPicker = true)
            }
        }
    }
}

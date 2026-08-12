package com.solomondesign.punchlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.solomondesign.punchlist.ui.navigation.PunchlistNavHost
import com.solomondesign.punchlist.ui.theme.PunchlistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PunchlistTheme {
                PunchlistNavHost()
            }
        }
    }
}

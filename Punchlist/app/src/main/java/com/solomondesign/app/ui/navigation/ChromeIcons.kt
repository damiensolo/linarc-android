package com.solomondesign.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Compose boundary for [ChromeIcon]. Kept in its own file so `AppChrome.kt` stays free of
 * Compose imports and therefore JVM-unit-testable.
 */
fun ChromeIcon.asImageVector(): ImageVector = when (this) {
    ChromeIcon.ADD -> Icons.Filled.Add
    ChromeIcon.ADD_A_PHOTO -> Icons.Filled.AddAPhoto
    ChromeIcon.MORE_TIME -> Icons.Filled.MoreTime
}

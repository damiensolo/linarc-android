package com.solomondesign.punchlist.ui.punchhome

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solomondesign.punchlist.ui.designsystem.PunchlistButton
import com.solomondesign.punchlist.ui.designsystem.PunchlistButtonSize
import com.solomondesign.punchlist.ui.designsystem.PunchlistButtonType
import com.solomondesign.punchlist.ui.theme.PunchlistTheme

private const val TAG = "PunchHomeScreen"

/**
 * Recreates the Figma "AndroidButtonDark" button sheet (node 5923:938) row-for-row
 * so it can be visually diffed against the Figma screenshot. Proof-of-concept for
 * the Figma-to-Compose pipeline: no ViewModel/state — every button here is static,
 * real, and interactive (press the primary button to see its pressed-color swap).
 */
@Composable
fun PunchHomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Figma To Code", style = MaterialTheme.typography.headlineSmall)

        PunchlistButton(
            text = "Label",
            onClick = { Log.d(TAG, "Primary large clicked") },
        )
        PunchlistButton(
            text = "Label",
            onClick = { Log.d(TAG, "Primary large disabled clicked (should not fire)") },
            enabled = false,
        )
        PunchlistButton(
            text = "Label",
            onClick = { Log.d(TAG, "Secondary large clicked") },
            type = PunchlistButtonType.Secondary,
        )
        PunchlistButton(
            text = "Label",
            onClick = { Log.d(TAG, "Error large clicked") },
            type = PunchlistButtonType.Error,
        )
        PunchlistButton(
            text = "Label",
            onClick = { Log.d(TAG, "Success large clicked") },
            type = PunchlistButtonType.Success,
        )
        PunchlistButton(
            text = "Label",
            onClick = { Log.d(TAG, "Primary small clicked") },
            size = PunchlistButtonSize.Small,
        )
        PunchlistButton(
            text = "Label",
            onClick = { Log.d(TAG, "Secondary small clicked") },
            type = PunchlistButtonType.Secondary,
            size = PunchlistButtonSize.Small,
        )
        PunchlistButton(
            text = "Label",
            onClick = { Log.d(TAG, "Error small clicked") },
            type = PunchlistButtonType.Error,
            size = PunchlistButtonSize.Small,
        )
        PunchlistButton(
            text = "Label",
            onClick = { Log.d(TAG, "Success small clicked") },
            type = PunchlistButtonType.Success,
            size = PunchlistButtonSize.Small,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PunchHomeScreenPreview() {
    PunchlistTheme {
        PunchHomeScreen()
    }
}

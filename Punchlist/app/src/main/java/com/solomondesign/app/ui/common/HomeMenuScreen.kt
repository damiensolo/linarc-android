package com.solomondesign.app.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solomondesign.app.ui.designsystem.AppButton
import com.solomondesign.app.ui.designsystem.AppButtonType

/** A single navigable row on a [HomeMenuScreen]. */
data class MenuItem(
    val label: String,
    val onClick: () -> Unit,
)

/** A primary/secondary call-to-action shown below a [HomeMenuScreen]'s menu rows. */
data class CtaSpec(
    val label: String,
    val type: AppButtonType,
    val onClick: () -> Unit,
)

/**
 * Generic "menu of destinations" screen: a title, a list of tappable rows (one per
 * [items] entry, standard Material [ListItem] rows rather than buttons — this is a
 * navigation menu, not a set of calls to action), and an optional CTA row using the
 * [AppButton] design-system component. Used for every tab's home screen and
 * other menu-shaped nodes in the information architecture.
 */
@Composable
fun HomeMenuScreen(
    title: String,
    items: List<MenuItem>,
    modifier: Modifier = Modifier,
    ctas: List<CtaSpec> = emptyList(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
        )

        items.forEach { menuItem ->
            ListItem(
                headlineContent = { Text(menuItem.label) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable(onClick = menuItem.onClick),
            )
            HorizontalDivider()
        }

        if (ctas.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ctas.forEach { cta ->
                    AppButton(text = cta.label, onClick = cta.onClick, type = cta.type)
                }
            }
        }
    }
}

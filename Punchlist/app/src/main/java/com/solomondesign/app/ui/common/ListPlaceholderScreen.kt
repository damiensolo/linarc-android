package com.solomondesign.app.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Generic fake-data list screen: a title followed by tappable rows. Used for every
 * "List" node in the IA (Task List, Issues & RFIs List, OAC Report List, Projects
 * portfolio) until each is backed by a real repository.
 *
 * Pass [onBack] when this list is reached by drilling down from another screen (it
 * then gets a top bar with a back action); omit it when the list itself is a
 * bottom-nav tab root (e.g. Projects Home), which relies on the bottom nav only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListPlaceholderScreen(
    title: String,
    rows: List<String>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val content = @Composable { padding: Modifier ->
        LazyColumn(modifier = padding.fillMaxSize()) {
            if (onBack == null) {
                item {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(rows) { label ->
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable { onItemClick(label) },
                )
                HorizontalDivider()
            }
        }
    }

    if (onBack == null) {
        content(modifier)
    } else {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { padding -> content(Modifier.padding(padding)) }
    }
}

package com.solomondesign.app.ui.projects

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val projectSpaceTabs = listOf("Overview", "Field", "Issues/RFIs", "Docs")

private fun descriptionFor(tab: String): String = when (tab) {
    "Overview" -> "Project status, milestones, key contacts."
    "Field" -> "Daily logs, time, and photos for this project."
    "Issues/RFIs" -> "Open issues and RFIs for this project."
    else -> "Plans, specs, contracts, and the OAC archive."
}

/**
 * "Project Space" — nested a level deeper than the other IA sections, so it gets its
 * own internal [TabRow] (Overview / Field / Issues-RFIs / Docs) instead of the shared
 * [com.solomondesign.app.ui.common.HomeMenuScreen] pattern used elsewhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSpaceScreen(projectName: String, onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(projectName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                projectSpaceTabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label) },
                    )
                }
            }
            Text(
                text = descriptionFor(projectSpaceTabs[selectedTab]),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

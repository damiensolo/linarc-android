package com.solomondesign.app.ui.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolCategory { FIELD, PROJECT, DOCUMENTATION, FINANCIALS, QUALITY }

data class PlatformTool(
    val id: String,
    val label: String,
    val subtitle: String,
    val category: ToolCategory,
    val icon: ImageVector,
    val canQuickCreate: Boolean,
    val quickCreateUsesPhoto: Boolean = false,
    val placeholderRows: List<String>,
)

object PlatformTools {
    val catalog: List<PlatformTool> = listOf(
        tool(
            id = "field_task",
            label = "Field task",
            subtitle = "Assign and track field work",
            category = ToolCategory.FIELD,
            icon = Icons.AutoMirrored.Filled.Assignment,
        ),
        tool(
            id = "time_card",
            label = "Time card",
            subtitle = "Hours on site",
            category = ToolCategory.FIELD,
            icon = Icons.Filled.Schedule,
        ),
        tool(
            id = "crew",
            label = "Crew",
            subtitle = "Who's on this job",
            category = ToolCategory.FIELD,
            icon = Icons.Filled.Groups,
        ),
        tool(
            id = "collaboration",
            label = "Collaboration",
            subtitle = "Messages and updates",
            category = ToolCategory.PROJECT,
            icon = Icons.AutoMirrored.Filled.Send,
            canQuickCreate = true,
        ),
        tool(
            id = "images",
            label = "Images",
            subtitle = "Photos from the field",
            category = ToolCategory.FIELD,
            icon = Icons.Filled.PhotoLibrary,
            canQuickCreate = true,
            quickCreateUsesPhoto = true,
        ),
        tool(
            id = "plans",
            label = "Plans",
            subtitle = "Plan sheets for this area",
            category = ToolCategory.DOCUMENTATION,
            icon = Icons.Filled.Map,
        ),
        tool(
            id = "rfis",
            label = "RFIs",
            subtitle = "Requests for information",
            category = ToolCategory.DOCUMENTATION,
            icon = Icons.Filled.Description,
            canQuickCreate = true,
        ),
        tool(
            id = "punch_list",
            label = "Punch list",
            subtitle = "Closeout items",
            category = ToolCategory.QUALITY,
            icon = Icons.AutoMirrored.Filled.PlaylistAddCheck,
            canQuickCreate = true,
        ),
        tool(
            id = "incidents",
            label = "Incidents",
            subtitle = "Safety incidents",
            category = ToolCategory.FIELD,
            icon = Icons.Filled.HealthAndSafety,
            canQuickCreate = true,
        ),
        tool(
            id = "issues",
            label = "Issues",
            subtitle = "Field issues",
            category = ToolCategory.QUALITY,
            icon = Icons.Filled.ReportProblem,
            canQuickCreate = true,
        ),
        tool(
            id = "t_and_m",
            label = "T & M",
            subtitle = "Time and material",
            category = ToolCategory.FINANCIALS,
            icon = Icons.Filled.MoreTime,
        ),
        tool(
            id = "checklist",
            label = "Checklist",
            subtitle = "Inspections and checks",
            category = ToolCategory.QUALITY,
            icon = Icons.Filled.Checklist,
        ),
        tool(
            id = "drive",
            label = "Drive",
            subtitle = "Project files",
            category = ToolCategory.DOCUMENTATION,
            icon = Icons.Filled.Cloud,
        ),
        tool(
            id = "toolbox_talks",
            label = "Toolbox Talks",
            subtitle = "Safety talks",
            category = ToolCategory.FIELD,
            icon = Icons.Filled.Shield,
            canQuickCreate = true,
        ),
        tool(
            id = "scan",
            label = "Scan",
            subtitle = "Scan a document or code",
            category = ToolCategory.FIELD,
            icon = Icons.Filled.DocumentScanner,
        ),
    )

    fun byId(id: String): PlatformTool? = catalog.firstOrNull { it.id == id }

    private fun tool(
        id: String,
        label: String,
        subtitle: String,
        category: ToolCategory,
        icon: ImageVector,
        canQuickCreate: Boolean = false,
        quickCreateUsesPhoto: Boolean = false,
    ) = PlatformTool(
        id = id,
        label = label,
        subtitle = subtitle,
        category = category,
        icon = icon,
        canQuickCreate = canQuickCreate,
        quickCreateUsesPhoto = quickCreateUsesPhoto,
        placeholderRows = listOf(
            "$label · sample 1",
            "$label · sample 2",
            "$label · sample 3",
        ),
    )
}

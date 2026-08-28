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
import com.solomondesign.app.ui.navigation.AppRoutes
import com.solomondesign.app.ui.persona.FieldPersona
import com.solomondesign.app.ui.records.RecordCategory

enum class ToolCategory { FIELD, PROJECT, DOCUMENTATION, FINANCIALS, QUALITY }

data class PlatformTool(
    val id: String,
    val label: String,
    val subtitle: String,
    val category: ToolCategory,
    val icon: ImageVector,
    val canQuickCreate: Boolean,
    val quickCreateUsesPhoto: Boolean = false,
    /**
     * Real destination for tools that have a built screen. Null keeps the generic
     * `tool/{id}` placeholder. Mirrors [quickCreateUsesPhoto], which is already a per-tool
     * routing override resolved at the navigation call site.
     */
    val homeRoute: String? = null,
    /** Real destination for the card's quick-create `+`. Null keeps the create placeholder. */
    val quickCreateRoute: String? = null,
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
            homeRoute = AppRoutes.FIELD_TASK_LIST,
        ),
        tool(
            id = "time_card",
            label = "Time card",
            subtitle = "Hours on site",
            category = ToolCategory.FIELD,
            icon = Icons.Filled.Schedule,
            homeRoute = AppRoutes.TIME_CARD_LIST,
        ),
        tool(
            id = "crew",
            label = "Crew",
            subtitle = "Who's on this job",
            category = ToolCategory.FIELD,
            icon = Icons.Filled.Groups,
            homeRoute = AppRoutes.CREW_LIST,
        ),
        tool(
            id = "collaboration",
            label = "Collaboration",
            subtitle = "Messages and updates",
            category = ToolCategory.PROJECT,
            icon = Icons.AutoMirrored.Filled.Send,
            canQuickCreate = true,
            homeRoute = AppRoutes.COLLAB_TOPIC_LIST,
        ),
        tool(
            id = "images",
            label = "Images",
            subtitle = "Photos from the field",
            category = ToolCategory.FIELD,
            icon = Icons.Filled.PhotoLibrary,
            canQuickCreate = true,
            quickCreateUsesPhoto = true,
            homeRoute = AppRoutes.IMAGE_GRID,
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
            homeRoute = AppRoutes.RECORD_LIST_PUNCH,
            quickCreateRoute = AppRoutes.recordCreate(RecordCategory.PUNCH.routeId),
        ),
        tool(
            id = "incidents",
            label = "Incidents",
            subtitle = "Safety incidents",
            category = ToolCategory.FIELD,
            icon = Icons.Filled.HealthAndSafety,
            canQuickCreate = true,
            homeRoute = AppRoutes.RECORD_LIST_INCIDENTS,
            quickCreateRoute = AppRoutes.recordCreate(RecordCategory.INCIDENT.routeId),
        ),
        tool(
            id = "issues",
            label = "Issues",
            subtitle = "Field issues",
            category = ToolCategory.QUALITY,
            icon = Icons.Filled.ReportProblem,
            canQuickCreate = true,
            homeRoute = AppRoutes.RECORD_LIST_ISSUES,
            quickCreateRoute = AppRoutes.recordCreate(RecordCategory.ISSUE.routeId),
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

    /**
     * Persona-first ids for the Crew view of the catalog: what a crew member reaches for —
     * their tasks, their hours, photos, and safety — floats to the top; the rest keeps
     * catalog order below. Ids, not indices, so [catalogFor] survives catalog reordering.
     */
    private val crewLeadIds = listOf("field_task", "time_card", "images", "toolbox_talks", "checklist")

    /** Superintendent-first ids: quality and safety oversight — open records, then the rest. */
    private val superintendentLeadIds =
        listOf("issues", "punch_list", "incidents", "rfis", "checklist")

    /** Project-manager-first ids: RFIs, decision threads, issues, cost, and project files. */
    private val projectManagerLeadIds =
        listOf("rfis", "collaboration", "issues", "t_and_m", "drive")

    /** Owner-first ids: progress and decisions — photos, plans, threads, files. */
    private val ownerLeadIds = listOf("images", "plans", "collaboration", "drive", "rfis")

    /** Subcontractor-first ids: assigned work and getting it signed off. */
    private val subcontractorLeadIds =
        listOf("field_task", "checklist", "punch_list", "images", "rfis")

    /**
     * The catalog as one persona sees it — same tools reordered (the spec's iteration-2
     * rule: same objects reorder by persona), with ONE sanctioned removal: the Owner drops
     * Time card, because the spec's persona table says Owner sees "no time cards or voice
     * log" — labor hours are internal, not an owner surface. No other persona removes
     * anything. Personas without a dedicated ordering keep the canonical [catalog] order.
     */
    fun catalogFor(persona: FieldPersona): List<PlatformTool> = when (persona) {
        FieldPersona.CREW -> leadWith(crewLeadIds)
        FieldPersona.SUPERINTENDENT -> leadWith(superintendentLeadIds)
        FieldPersona.PROJECT_MANAGER -> leadWith(projectManagerLeadIds)
        FieldPersona.OWNER -> leadWith(ownerLeadIds).filterNot { it.id == "time_card" }
        FieldPersona.SUBCONTRACTOR -> leadWith(subcontractorLeadIds)
        else -> catalog
    }

    private fun leadWith(leadIds: List<String>): List<PlatformTool> =
        leadIds.mapNotNull(::byId) + catalog.filterNot { it.id in leadIds }

    private fun tool(
        id: String,
        label: String,
        subtitle: String,
        category: ToolCategory,
        icon: ImageVector,
        canQuickCreate: Boolean = false,
        quickCreateUsesPhoto: Boolean = false,
        homeRoute: String? = null,
        quickCreateRoute: String? = null,
    ) = PlatformTool(
        id = id,
        label = label,
        subtitle = subtitle,
        category = category,
        icon = icon,
        canQuickCreate = canQuickCreate,
        quickCreateUsesPhoto = quickCreateUsesPhoto,
        homeRoute = homeRoute,
        quickCreateRoute = quickCreateRoute,
        placeholderRows = listOf(
            "$label · sample 1",
            "$label · sample 2",
            "$label · sample 3",
        ),
    )
}

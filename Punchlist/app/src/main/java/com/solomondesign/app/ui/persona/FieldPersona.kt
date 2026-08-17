package com.solomondesign.app.ui.persona

/**
 * Field roles the prototype can present. Only [FOREMAN] is live in this build.
 * Others appear in Tools → Demo: view as as visible placeholders for iteration 2.
 */
enum class FieldPersona(
    val displayName: String,
    val isLive: Boolean,
    val nextFocus: String,
) {
    FOREMAN(
        displayName = "Foreman",
        isLive = true,
        nextFocus = "Crew today, blockers, Start My Day, Voice Daily",
    ),
    SUPERINTENDENT(
        displayName = "Superintendent",
        isLive = false,
        nextFocus = "Open issues and inspections first; Plan is the power view",
    ),
    CREW(
        displayName = "Crew",
        isLive = false,
        nextFocus = "My assignment, start/end shift, take a photo",
    ),
    PROJECT_MANAGER(
        displayName = "Project manager",
        isLive = false,
        nextFocus = "Aging RFIs, delays, decisions",
    ),
    OWNER(
        displayName = "Owner",
        isLive = false,
        nextFocus = "Progress photos and decisions; no time cards or voice log",
    ),
    SUBCONTRACTOR(
        displayName = "Subcontractor",
        isLive = false,
        nextFocus = "Assigned work plus Request Inspection",
    ),
}

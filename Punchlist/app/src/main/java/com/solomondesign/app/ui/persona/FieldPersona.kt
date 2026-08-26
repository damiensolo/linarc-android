package com.solomondesign.app.ui.persona

/**
 * Field roles the prototype can present. Every persona is live in this build (the
 * non-Foreman personas went live 2026-08-25 via Demo: view as — same three tabs, same
 * objects, only Today focus, tool ordering, and Plan emphasis change). [OWNER] is the one
 * persona allowed to drop surfaces, per the spec's "no time cards or voice log".
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
        isLive = true,
        nextFocus = "Open issues and inspections first; Plan is the power view",
    ),
    CREW(
        displayName = "Crew",
        isLive = true,
        nextFocus = "My assignment, start/end shift, take a photo",
    ),
    PROJECT_MANAGER(
        displayName = "Project manager",
        isLive = true,
        nextFocus = "Aging RFIs, delays, decisions",
    ),
    OWNER(
        displayName = "Owner",
        isLive = true,
        nextFocus = "Progress photos and decisions; no time cards or voice log",
    ),
    SUBCONTRACTOR(
        displayName = "Subcontractor",
        isLive = true,
        nextFocus = "Assigned work plus Request Inspection",
    ),
}

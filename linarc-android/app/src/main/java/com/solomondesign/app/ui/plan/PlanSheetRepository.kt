package com.solomondesign.app.ui.plan

import com.solomondesign.app.R

/**
 * Seeded plan set for the field prototype. The rasters are public-domain HABS/HAER measured
 * drawings (Library of Congress) so the demo reads as a real, stamped construction set rather
 * than placeholder shapes.
 *
 * Sheets are ordered by discipline section (the Plans list order), which is also the paging
 * order in the viewer, so swiping matches what the list promises.
 */
object PlanSheetRepository {

    val sheets: List<PlanSheet> = listOf(
        // ---- Architectural ----
        PlanSheet(
            id = "a-101",
            number = "A-101",
            title = "Level 1 Floor Plan",
            discipline = PlanDiscipline.ARCHITECTURAL,
            revision = "Rev 4",
            updatedLabel = "Updated Aug 12",
            drawableRes = R.drawable.plan_a_level1,
        ),
        PlanSheet(
            id = "a-102",
            number = "A-102",
            title = "Level 2 Floor Plan",
            discipline = PlanDiscipline.ARCHITECTURAL,
            revision = "Rev 4",
            updatedLabel = "Updated Aug 12",
            drawableRes = R.drawable.plan_a_level2,
            isPinSheet = true,
            demoMarkup = listOf(
                DemoMarkup(
                    kind = MarkupKind.RECT,
                    points = listOf(0.52f to 0.30f, 0.68f to 0.44f),
                    color = MarkupColor.RED,
                ),
            ),
        ),
        PlanSheet(
            id = "a-201",
            number = "A-201",
            title = "Exterior Elevations",
            discipline = PlanDiscipline.ARCHITECTURAL,
            revision = "Rev 2",
            updatedLabel = "Updated Jul 30",
            drawableRes = R.drawable.plan_a_elevations,
        ),

        // ---- Civil ----
        PlanSheet(
            id = "c-101",
            number = "C-101",
            title = "Site Plan",
            discipline = PlanDiscipline.CIVIL,
            revision = "Rev 3",
            updatedLabel = "Updated Aug 5",
            drawableRes = R.drawable.plan_c_site,
        ),
        PlanSheet(
            id = "c-102",
            number = "C-102",
            title = "Grading & Drainage Plan",
            discipline = PlanDiscipline.CIVIL,
            revision = "Rev 1",
            updatedLabel = "Updated Jun 18",
            drawableRes = R.drawable.plan_c_grading,
        ),

        // ---- Concrete ----
        PlanSheet(
            id = "co-101",
            number = "CO-101",
            title = "Foundation Plan & Sections",
            discipline = PlanDiscipline.CONCRETE,
            revision = "Rev 2",
            updatedLabel = "Updated Jul 22",
            drawableRes = R.drawable.plan_co_foundation,
        ),
        PlanSheet(
            id = "co-102",
            number = "CO-102",
            title = "Foundation Sections & Details",
            discipline = PlanDiscipline.CONCRETE,
            revision = "Rev 1",
            updatedLabel = "Updated Jul 22",
            drawableRes = R.drawable.plan_co_details,
        ),

        // ---- Electrical ----
        PlanSheet(
            id = "e-401",
            number = "E-401",
            title = "Pump Control Panel Wiring",
            discipline = PlanDiscipline.ELECTRICAL,
            revision = "Rev 5",
            updatedLabel = "Updated Aug 19",
            drawableRes = R.drawable.plan_e_wiring,
            demoMarkup = listOf(
                DemoMarkup(
                    kind = MarkupKind.RECT,
                    points = listOf(0.62f to 0.55f, 0.76f to 0.72f),
                    color = MarkupColor.BLUE,
                ),
            ),
        ),
        PlanSheet(
            id = "e-402",
            number = "E-402",
            title = "Power & Controls Diagram",
            discipline = PlanDiscipline.ELECTRICAL,
            revision = "Rev 2",
            updatedLabel = "Updated Aug 1",
            drawableRes = R.drawable.plan_e_bascule,
        ),

        // ---- General ----
        PlanSheet(
            id = "g-001",
            number = "G-001",
            title = "Cover Sheet & Site Plan",
            discipline = PlanDiscipline.GENERAL,
            revision = "Rev 1",
            updatedLabel = "Updated May 2",
            drawableRes = R.drawable.plan_g_cover,
        ),
        PlanSheet(
            id = "g-002",
            number = "G-002",
            title = "General Arrangement",
            discipline = PlanDiscipline.GENERAL,
            revision = "Rev 1",
            updatedLabel = "Updated May 2",
            drawableRes = R.drawable.plan_g_arrangement,
        ),

        // ---- Structural ----
        PlanSheet(
            id = "s-201",
            number = "S-201",
            title = "Framing Plan – Girder Spans",
            discipline = PlanDiscipline.STRUCTURAL,
            revision = "Rev 3",
            updatedLabel = "Updated Aug 8",
            drawableRes = R.drawable.plan_s_girder,
            demoMarkup = listOf(
                DemoMarkup(
                    kind = MarkupKind.POLYLINE,
                    points = listOf(
                        0.30f to 0.62f,
                        0.36f to 0.55f,
                        0.43f to 0.60f,
                        0.50f to 0.52f,
                    ),
                    color = MarkupColor.AMBER,
                ),
            ),
        ),
        PlanSheet(
            id = "s-202",
            number = "S-202",
            title = "Framing & Deck Plans",
            discipline = PlanDiscipline.STRUCTURAL,
            revision = "Rev 2",
            updatedLabel = "Updated Jul 15",
            drawableRes = R.drawable.plan_s_trestle,
        ),
    )

    fun byId(id: String): PlanSheet? = sheets.firstOrNull { it.id == id }

    fun indexOf(id: String): Int = sheets.indexOfFirst { it.id == id }
}

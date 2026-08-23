package com.solomondesign.app.ui.plan

/**
 * Plan-sheet domain model for the Plans tab.
 *
 * IMPORTANT: this file must stay free of `androidx.compose` imports so the filter logic and the
 * seeded sheet set can be exercised by `./gradlew testDebugUnitTest` (JVM only).
 */

/** Disciplines shown as collapsible sections on the Plans list, in display order. */
enum class PlanDiscipline(val label: String) {
    ARCHITECTURAL("Architectural"),
    CIVIL("Civil"),
    CONCRETE("Concrete"),
    ELECTRICAL("Electrical"),
    GENERAL("General"),
    STRUCTURAL("Structural"),
}

/** A single markup shape drawn over a sheet for demo purposes. Points are image-fractions. */
data class DemoMarkup(
    val kind: MarkupKind,
    val points: List<Pair<Float, Float>>,
    val color: MarkupColor,
)

enum class MarkupKind { RECT, POLYLINE }

enum class MarkupColor { RED, BLUE, AMBER }

data class PlanSheet(
    val id: String,
    /** Sheet number as printed in the title block, e.g. "A-102". */
    val number: String,
    val title: String,
    val discipline: PlanDiscipline,
    val revision: String,
    val updatedLabel: String,
    /** Raster drawable resource for the sheet (drawable-nodpi). */
    val drawableRes: Int,
    /**
     * True on the single sheet that receives live [com.solomondesign.app.ui.demo.PlanPin]s from
     * Voice-to-Log and photo capture (the Level 2 floor plan, matching the project area).
     */
    val isPinSheet: Boolean = false,
    val demoMarkup: List<DemoMarkup> = emptyList(),
)

/**
 * Case-insensitive search over number and title, optionally restricted to one discipline.
 * Pure function so the demo search/filter behaviour is unit-testable.
 */
fun filterPlanSheets(
    sheets: List<PlanSheet>,
    query: String,
    discipline: PlanDiscipline?,
): List<PlanSheet> {
    val trimmed = query.trim()
    return sheets.filter { sheet ->
        (discipline == null || sheet.discipline == discipline) &&
            (
                trimmed.isEmpty() ||
                    sheet.number.contains(trimmed, ignoreCase = true) ||
                    sheet.title.contains(trimmed, ignoreCase = true)
                )
    }
}

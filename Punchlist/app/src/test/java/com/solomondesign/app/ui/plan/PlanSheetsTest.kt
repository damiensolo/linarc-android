package com.solomondesign.app.ui.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for the Plans demo data and the search/filter logic behind the Plans list. */
class PlanSheetsTest {

    private fun sheet(
        id: String,
        number: String,
        title: String,
        discipline: PlanDiscipline,
    ) = PlanSheet(
        id = id,
        number = number,
        title = title,
        discipline = discipline,
        revision = "Rev 1",
        updatedLabel = "Updated",
        drawableRes = 0,
    )

    private val sheets = listOf(
        sheet("a-101", "A-101", "Level 1 Floor Plan", PlanDiscipline.ARCHITECTURAL),
        sheet("a-102", "A-102", "Level 2 Floor Plan", PlanDiscipline.ARCHITECTURAL),
        sheet("e-401", "E-401", "Pump Control Panel Wiring", PlanDiscipline.ELECTRICAL),
        sheet("s-201", "S-201", "Framing Plan", PlanDiscipline.STRUCTURAL),
    )

    @Test
    fun blankQueryAndNoDisciplineReturnsEverything() {
        assertEquals(sheets, filterPlanSheets(sheets, "", null))
        assertEquals(sheets, filterPlanSheets(sheets, "   ", null))
    }

    @Test
    fun queryMatchesSheetNumberCaseInsensitively() {
        val hits = filterPlanSheets(sheets, "a-1", null)
        assertEquals(listOf("a-101", "a-102"), hits.map { it.id })
    }

    @Test
    fun queryMatchesTitleCaseInsensitively() {
        val hits = filterPlanSheets(sheets, "floor plan", null)
        assertEquals(listOf("a-101", "a-102"), hits.map { it.id })
    }

    @Test
    fun disciplineFilterRestrictsResults() {
        val hits = filterPlanSheets(sheets, "", PlanDiscipline.ELECTRICAL)
        assertEquals(listOf("e-401"), hits.map { it.id })
    }

    @Test
    fun queryAndDisciplineCombine() {
        assertEquals(
            emptyList<PlanSheet>(),
            filterPlanSheets(sheets, "floor", PlanDiscipline.STRUCTURAL),
        )
        assertEquals(
            listOf("s-201"),
            filterPlanSheets(sheets, "framing", PlanDiscipline.STRUCTURAL).map { it.id },
        )
    }

    @Test
    fun unmatchedQueryReturnsEmpty() {
        assertTrue(filterPlanSheets(sheets, "zzz", null).isEmpty())
    }

    // ---- Seeded repository integrity ----

    @Test
    fun everyDisciplineSectionHasAtLeastOneSheet() {
        val covered = PlanSheetRepository.sheets.map { it.discipline }.toSet()
        assertEquals(PlanDiscipline.entries.toSet(), covered)
    }

    @Test
    fun sheetIdsAndNumbersAreUnique() {
        val ids = PlanSheetRepository.sheets.map { it.id }
        val numbers = PlanSheetRepository.sheets.map { it.number }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(numbers.size, numbers.toSet().size)
    }

    /** Voice-to-Log pins land on exactly one sheet — the Level 2 floor plan. */
    @Test
    fun exactlyOneSheetReceivesLivePins() {
        val pinSheets = PlanSheetRepository.sheets.filter { it.isPinSheet }
        assertEquals(1, pinSheets.size)
        assertEquals("A-102", pinSheets.single().number)
    }

    @Test
    fun repositoryOrderGroupsSheetsByDisciplineSectionOrder() {
        val disciplineOrder = PlanSheetRepository.sheets.map { it.discipline }
        val sorted = disciplineOrder.sortedBy { it.ordinal }
        assertEquals(
            "viewer paging order must match the list's discipline sections",
            sorted,
            disciplineOrder,
        )
    }

    @Test
    fun byIdAndIndexOfAgree() {
        val sheet = PlanSheetRepository.sheets.last()
        assertEquals(sheet, PlanSheetRepository.byId(sheet.id))
        assertEquals(PlanSheetRepository.sheets.lastIndex, PlanSheetRepository.indexOf(sheet.id))
        assertEquals(null, PlanSheetRepository.byId("nope"))
        assertEquals(-1, PlanSheetRepository.indexOf("nope"))
    }

    @Test
    fun rectMarkupAlwaysCarriesTwoPoints() {
        PlanSheetRepository.sheets
            .flatMap { it.demoMarkup }
            .filter { it.kind == MarkupKind.RECT }
            .forEach { assertEquals(2, it.points.size) }
    }
}

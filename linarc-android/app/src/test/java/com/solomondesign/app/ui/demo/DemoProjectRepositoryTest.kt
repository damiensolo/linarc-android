package com.solomondesign.app.ui.demo

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoProjectRepositoryTest {

    @After
    fun resetStore() {
        DemoProjectRepository.clear()
    }

    @Test
    fun darkTheme_defaultsOff_togglesAndClearResetsToLight() {
        assertFalse(DemoProjectRepository.darkTheme)

        DemoProjectRepository.darkTheme = true
        assertTrue(DemoProjectRepository.darkTheme)

        DemoProjectRepository.clear()
        assertFalse(DemoProjectRepository.darkTheme)
    }

    @Test
    fun speakOnForms_defaultsOff_andClearResetsIt() {
        assertFalse(DemoProjectRepository.speakOnForms)

        DemoProjectRepository.speakOnForms = true
        assertTrue(DemoProjectRepository.speakOnForms)

        DemoProjectRepository.clear()
        assertFalse(DemoProjectRepository.speakOnForms)
    }

    @Test
    fun projects_seedsRiversideMedicalFirstWithUniqueIds() {
        val projects = DemoProjectRepository.projects

        assertTrue(projects.isNotEmpty())
        assertEquals(DemoProjectRepository.PROJECT_NAME, projects.first().name)
        assertEquals(projects.size, projects.map { it.id }.distinct().size)
    }
}

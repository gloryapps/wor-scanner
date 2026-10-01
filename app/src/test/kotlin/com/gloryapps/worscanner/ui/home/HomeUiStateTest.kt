package com.gloryapps.worscanner.ui.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeUiStateTest {
    @Test
    fun `a scan can start without notifications, which are still asked for`() {
        val state = HomeUiState(granted = setOf(Permission.ACCESSIBILITY, Permission.OVERLAY))

        assertTrue(state.ready)
        assertEquals(listOf(Permission.NOTIFICATIONS), state.missing)
    }

    @Test
    fun `a scan cannot start while the accessibility service is off`() {
        val state = HomeUiState(granted = setOf(Permission.OVERLAY, Permission.NOTIFICATIONS))

        assertFalse(state.ready)
        assertEquals(listOf(Permission.ACCESSIBILITY), state.missing)
    }

    @Test
    fun `the missing grants are asked for in one order, whatever order they were given in`() {
        val state = HomeUiState(granted = emptySet())

        assertEquals(listOf(Permission.ACCESSIBILITY, Permission.OVERLAY, Permission.NOTIFICATIONS), state.missing)
    }

    @Test
    fun `nothing is missing once every grant is on`() {
        assertEquals(emptyList(), HomeUiState(granted = Permission.entries.toSet()).missing)
    }
}

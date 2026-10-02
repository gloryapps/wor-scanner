package com.gloryapps.worscanner.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GrantsTest {
    @Test
    fun `a scan can start without notifications, which are still asked for`() {
        val grants = Grants.on(Permission.ACCESSIBILITY, Permission.OVERLAY)

        assertTrue(grants.canScan)
        assertEquals(listOf(Permission.NOTIFICATIONS), grants.missing)
    }

    @Test
    fun `a scan cannot start while the accessibility service is off`() {
        val grants = Grants.on(Permission.OVERLAY, Permission.NOTIFICATIONS)

        assertFalse(grants.canScan)
        assertEquals(listOf(Permission.ACCESSIBILITY), grants.missing)
    }

    @Test
    fun `a scan cannot start while the accessibility service is on in the settings but stopped, which is asked for`() {
        val grants = Grants(mapOf(Permission.ACCESSIBILITY to Standing.STALLED, Permission.OVERLAY to Standing.ON, Permission.NOTIFICATIONS to Standing.ON))

        assertFalse(grants.canScan)
        assertEquals(listOf(Permission.ACCESSIBILITY), grants.missing)
    }

    @Test
    fun `the missing grants are asked for in one order, whatever order they were given in`() {
        assertEquals(listOf(Permission.ACCESSIBILITY, Permission.OVERLAY, Permission.NOTIFICATIONS), Grants().missing)
    }

    @Test
    fun `nothing is missing once every grant is on`() {
        assertEquals(emptyList(), Grants.ALL_ON.missing)
    }
}

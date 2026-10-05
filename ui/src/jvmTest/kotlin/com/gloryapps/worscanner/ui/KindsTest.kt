package com.gloryapps.worscanner.ui

import com.gloryapps.worscanner.scanner.kinds.Kind
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KindsTest {
    @Test
    fun `every kind is named, with its steps, its note and its reminder`() = runBlocking {
        Kind.entries.forEach { kind ->
            (listOf(kind.label, kind.note, kind.reminder) + kind.steps).forEach { assertTrue(getString(it).isNotBlank(), "$kind says nothing") }
        }
    }

    @Test
    fun `a kind's tiles are counted in its own word`() = runBlocking {
        assertEquals("Gear", getString(Kind.GEAR.label))
        assertEquals(listOf("piece", "heroes"), listOf(getPluralString(Kind.GEAR.pieces, 1), getPluralString(Kind.HEROES.pieces, 2)))
    }
}

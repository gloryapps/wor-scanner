package com.gloryapps.worscanner.scanner.lua

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LuaHeapTest {
    private val memory = LaidOutHeap()
    private val heap = LuaHeap(memory)

    @Test
    fun `a table's fields read as Lua holds them, its array part under 1, 2, 3`() {
        val attribute = memory.table("iAttrId" to 7)
        val piece = memory.table("iItemUid" to 9_000_000_000L, "bLocked" to true, "fScale" to 1.5, "sName" to "Sigil", "vAttr" to attribute, array = listOf(4, 5))

        assertEquals(
            mapOf(
                LuaValue.Integer(1) to LuaValue.Integer(4),
                LuaValue.Integer(2) to LuaValue.Integer(5),
                LuaValue.Text("iItemUid") to LuaValue.Integer(9_000_000_000L),
                LuaValue.Text("bLocked") to LuaValue.Bool(true),
                LuaValue.Text("fScale") to LuaValue.Real(1.5),
                LuaValue.Text("sName") to LuaValue.Text("Sigil"),
                LuaValue.Text("vAttr") to LuaValue.Table(attribute.address),
            ),
            heap.fields(piece.address),
        )
    }

    @Test
    fun `a field holding nil is not a field`() {
        val table = memory.table("iHeroId" to 3, "vEquipSlot" to null)

        assertEquals(setOf(LuaValue.Text("iHeroId")), heap.fields(table.address)?.keys)
    }

    @Test
    fun `an address that holds a string is no table`() {
        assertNull(heap.fields(memory.string("equips")))
    }

    @Test
    fun `the tables holding a name are found by it, and no table without it`() = runTest {
        val first = memory.table("m_CharactorDatas" to memory.table(), "iLimit" to 200)
        memory.gap(4096)
        val second = memory.table("iLimit" to 100, "m_CharactorDatas" to memory.table())
        val other = memory.table("m_vArtifacts" to memory.table())
        memory.table("iLimit" to 50)

        assertEquals(
            mapOf("m_CharactorDatas" to setOf(first.address, second.address), "m_vArtifacts" to setOf(other.address)),
            heap.holders(setOf("m_CharactorDatas", "m_vArtifacts", "suitId2EquipIdExt")),
        )
    }

    @Test
    fun `a name held as a value, not as a key, holds nothing`() = runTest {
        memory.table("sKey" to "m_vArtifacts", array = listOf("m_vArtifacts"))

        assertEquals(emptyMap(), heap.holders(setOf("m_vArtifacts")))
    }
}

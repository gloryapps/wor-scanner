package com.gloryapps.worscanner.scanner.client

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Clavicus's row as the client's table of units holds it, but for most of its fields. */
class GameTableTest {
    private val fields = listOf(
        ClassField("m_bError", null),
        ClassField("m_ID", Column.INT),
        ClassField("m_Type", Column.INT),
        ClassField("m_mName", Column.STRING),
        ClassField("m_Quality", Column.INT),
        ClassField("m_Speedlevel", Column.FLOAT),
        ClassField("m_HeroTalent", Column.INT_LISTS),
        ClassField("m_AdvancedUnlock", Column.STRING),
        ClassField("m_OnShowFactionMember", Column.INTS),
    )
    private val row = numbers(1, 5, 1.5f.toRawBits()) + byteArrayOf(2) + byteArrayOf(2) + numbers(0, 290330) + byteArrayOf(1) + numbers(2) +
        numbers(0) + byteArrayOf(1) + numbers(103)
    private val table = mufTable(listOf("1,1,290305;3,1,290307"), mapOf(2901 to numbers(1, 3, 0) + byteArrayOf(0) + numbers(0) + byteArrayOf(0), 2903 to row))

    @Test
    fun `a row reads as its class's fields after the key, but for the texts the language's tables hold`() {
        assertEquals(
            Json.parseToJsonElement(
                """{"m_Type":1,"m_Quality":5,"m_Speedlevel":1.5,"m_HeroTalent":[[0,290330],[2]],"m_AdvancedUnlock":"1,1,290305;3,1,290307","m_OnShowFactionMember":[103]}""",
            ).jsonObject,
            GameTable(table, fields).row(2903),
        )
    }

    @Test
    fun `a key the table does not hold has no row`() {
        assertNull(GameTable(table, fields).row(2902))
    }

    @Test
    fun `a row that does not read whole is the class and the table disagreeing`() {
        assertFailsWith<IllegalStateException> { GameTable(table, fields.dropLast(1)).row(2903) }
    }

    @Test
    fun `every key the table holds is one of its rows`() {
        assertEquals(setOf(2901L, 2903L), GameTable(table, fields).keys().toSet())
    }
}

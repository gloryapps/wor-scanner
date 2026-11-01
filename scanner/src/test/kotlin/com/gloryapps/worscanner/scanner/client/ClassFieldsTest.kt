package com.gloryapps.worscanner.scanner.client

import com.gloryapps.worscanner.scanner.senses.Memory
import com.gloryapps.worscanner.scanner.senses.Span
import kotlinx.coroutines.test.runTest
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class ClassFieldsTest {
    /** Classes laid out as il2cpp lays them out: their field names in the metadata the game maps, their types and fields' records where it allocates. */
    private class LaidOutClasses : Memory {
        private val names = ByteBuffer.allocate(SIZE).order(ByteOrder.LITTLE_ENDIAN).position(1)
        private val heap = ByteBuffer.allocate(SIZE).order(ByteOrder.LITTLE_ENDIAN).position(64)

        fun type(kind: Int, on: Long = 0, static: Boolean = false): Long = place(heap, HEAP) { heap.putLong(on).putInt((kind shl 16) or (if (static) 0x10 else 0)).putInt(0) }

        /** The records of one class's fields, one after the other, each a name and the type `types` gives it. */
        fun fields(parent: Long, vararg fields: Pair<String, Long>) = fields.forEach { (name, type) ->
            val named = place(names, NAMES) { names.put(name.encodeToByteArray()).put(0) }
            place(heap, HEAP) { heap.putLong(named).putLong(type).putLong(parent).putInt(0).putInt(0) }
        }

        private fun place(into: ByteBuffer, start: Long, lay: () -> Unit): Long = (start + into.position()).also { lay() }

        override fun spans() = listOf(Span(HEAP, SIZE.toLong()))

        override fun mapped() = listOf(Span(NAMES, SIZE.toLong()))

        override fun read(address: Long, size: Int): ByteArray? = listOf(NAMES to names, HEAP to heap).firstNotNullOfOrNull { (start, bytes) ->
            (address - start).takeIf { it >= 0 && it + size <= SIZE }?.let { at -> ByteArray(size).also { bytes.get(at.toInt(), it) } }
        }

        companion object {
            const val NAMES = 0x1_0000_0000L
            const val HEAP = 0x2_0000_0000L
            const val SIZE = 4096
        }
    }

    @Test
    fun `the fields of the class declaring a field are read in the order it declares them, its statics left out`() = runTest {
        val memory = LaidOutClasses()
        val int = memory.type(0x08)
        memory.fields(parent = 1, "m_SkinId" to int)
        memory.fields(
            parent = 2,
            "m_bError" to memory.type(0x02),
            "nullObj" to memory.type(0x12, static = true),
            "m_ID" to int,
            "m_mName" to memory.type(0x0E),
            "m_Speedlevel" to memory.type(0x0C),
            "m_HeroSkillLevelupList" to memory.type(0x1D, on = int),
            "m_HeroTalent" to memory.type(0x1D, on = memory.type(0x1D, on = int)),
        )
        memory.fields(parent = 3, "m_Quality" to int)

        assertEquals(
            listOf(
                ClassField("m_bError", null),
                ClassField("m_ID", Column.INT),
                ClassField("m_mName", Column.STRING),
                ClassField("m_Speedlevel", Column.FLOAT),
                ClassField("m_HeroSkillLevelupList", Column.INTS),
                ClassField("m_HeroTalent", Column.INT_LISTS),
            ),
            classFields(memory, "m_HeroSkillLevelupList"),
        )
    }

    @Test
    fun `no class declaring the field is no fields`() = runTest {
        assertEquals(emptyList(), classFields(LaidOutClasses(), "m_HeroSkillLevelupList"))
    }
}

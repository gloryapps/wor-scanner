package com.gloryapps.worscanner.scanner.il2cpp

import com.gloryapps.worscanner.scanner.senses.Memory
import com.gloryapps.worscanner.scanner.senses.Span
import kotlinx.coroutines.test.runTest
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class Il2CppHeapTest {
    private val memory = LaidOutRuntime()

    @Test
    fun `a class is found by its name in the metadata, its fields read off the runtime`() = runTest {
        val klass = memory.klass("AngerComponent", "Battle", fields = listOf(Triple("m_iAnger", 0x10, 0x08), Triple("m_bFull", 0x14, 0x02)))

        val found = Il2CppHeap(memory).classes(setOf("AngerComponent"))

        assertEquals(
            listOf(Il2CppClass(klass, "AngerComponent", "Battle", 0L, 0L, listOf(Il2CppField("m_iAnger", 0x10, 0x08, 0x0006, 0L), Il2CppField("m_bFull", 0x14, 0x02, 0x0006, 0L)))),
            found,
        )
    }

    @Test
    fun `an object is a word pointing at its class`() = runTest {
        val klass = memory.klass("BattleBuff", "Battle", fields = listOf(Triple("m_iLayer", 0x10, 0x08)))
        val buff = memory.instance(klass, 0x10 to 3)
        val heap = Il2CppHeap(memory)

        val classes = heap.classes(setOf("BattleBuff"))

        assertEquals(mapOf(klass to listOf(buff)), heap.instances(classes, most = 10))
        assertEquals("BattleBuff", heap.classOf(buff))
    }

    @Test
    fun `a class named only like another is not it`() = runTest {
        memory.klass("BattleBuffCache", "Battle", fields = emptyList())

        assertEquals(emptyList(), Il2CppHeap(memory).classes(setOf("BattleBuff")))
    }
}

/** An IL2CPP runtime laid out by hand as a 64-bit Unity 2019.4 game's would be: the metadata's names read-only, classes and objects writable. */
private class LaidOutRuntime : Memory {
    private val names = Region(0x1_0000_0000L)
    private val heap = Region(0x2_0000_0000L)

    /** A class with `fields`, each a name, an offset and a type code; its address. */
    fun klass(name: String, namespace: String, fields: List<Triple<String, Int, Int>>): Long {
        val nameAt = names.cString(name)
        val namespaceAt = names.cString(namespace)
        val types = fields.map { (_, _, type) -> heap.place(16) { at -> heap.bytes.putInt(at + 8, (type shl 16) or 0x0006) } }
        val klass = heap.reserve(0x120)
        val fieldInfos = heap.place(32 * fields.size) { at ->
            fields.forEachIndexed { index, (field, offset, _) ->
                val info = at + index * 32
                heap.bytes.putLong(info, names.cString(field))
                heap.bytes.putLong(info + 8, types[index])
                heap.bytes.putLong(info + 16, klass)
                heap.bytes.putInt(info + 24, offset)
            }
        }
        heap.at(klass) { at ->
            heap.bytes.putLong(at + 0x10, nameAt)
            heap.bytes.putLong(at + 0x18, namespaceAt)
            heap.bytes.putLong(at + 0x80, if (fields.isEmpty()) 0L else fieldInfos)
        }

        return klass
    }

    /** An object of `klass` holding `ints` at their offsets; its address. */
    fun instance(klass: Long, vararg ints: Pair<Int, Int>): Long = heap.place(0x40) { at ->
        heap.bytes.putLong(at, klass)
        ints.forEach { (offset, value) -> heap.bytes.putInt(at + offset, value) }
    }

    override fun spans() = listOf(heap.span())

    override fun readOnly() = listOf(names.span())

    override fun read(address: Long, size: Int): ByteArray? = listOf(names, heap).firstNotNullOfOrNull { it.read(address, size) }

    private class Region(private val start: Long) {
        val bytes: ByteBuffer = ByteBuffer.allocate(1 shl 16).order(ByteOrder.LITTLE_ENDIAN)
        private var next = 16

        /** `text` as the metadata keeps a name: its bytes, then a NUL. */
        fun cString(text: String): Long = place(text.length + 1) { at -> bytes.put(at, text.encodeToByteArray()) }

        fun place(size: Int, lay: (Int) -> Unit): Long = reserve(size).also { at(it, lay) }

        fun reserve(size: Int): Long = (start + next).also { next = (next + size + 16) / 16 * 16 }

        fun at(address: Long, lay: (Int) -> Unit) = lay((address - start).toInt())

        fun span() = Span(start, next.toLong())

        fun read(address: Long, size: Int): ByteArray? {
            val at = address - start
            if (at < 0 || at + size > next) return null

            return ByteArray(size).also { bytes.get(at.toInt(), it) }
        }
    }
}

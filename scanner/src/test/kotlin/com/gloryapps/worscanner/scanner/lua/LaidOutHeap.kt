package com.gloryapps.worscanner.scanner.lua

import com.gloryapps.worscanner.scanner.senses.Memory
import com.gloryapps.worscanner.scanner.senses.Span
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A Lua 5.3 heap laid out by hand as a 64-bit game's would be, one object after the next, short strings kept once. */
internal class LaidOutHeap(private val start: Long = 0x1_2340_0000L) : Memory {
    private val bytes = ByteBuffer.allocate(SIZE).order(ByteOrder.LITTLE_ENDIAN)
    private val interned = mutableMapOf<String, Long>()
    private var next = 64

    /** A table whose array part holds `array` and whose nodes hold `fields`; a String value or key is a Lua string, an Int or Long an integer, a [Ref] a table. */
    fun table(vararg fields: Pair<Any, Any?>, array: List<Any?> = emptyList()): Ref {
        val log = generateSequence(0) { it + 1 }.first { (1 shl it) >= fields.size }
        val values = array.map(::value)
        val keyed = fields.map { (key, field) -> value(key) to value(field) }
        val arrayAt = place(values.size * VALUE) { at -> values.forEachIndexed { index, it -> write(at + index * VALUE, it) } }
        val nodesAt = place(NODE shl log) { at -> keyed.forEachIndexed { index, (key, field) -> write(at + index * NODE, field); write(at + index * NODE + VALUE, key) } }
        val table = place(TABLE) { at ->
            bytes.put(at + 8, 0x05)
            bytes.put(at + 11, log.toByte())
            bytes.putInt(at + 12, values.size)
            bytes.putLong(at + 16, start + arrayAt)
            bytes.putLong(at + 24, start + nodesAt)
        }

        return Ref(start + table)
    }

    fun string(text: String): Long = interned.getOrPut(text) {
        val encoded = text.encodeToByteArray()
        start + place(STRING + encoded.size + 1) { at ->
            bytes.put(at + 8, 0x04)
            bytes.put(at + 11, encoded.size.toByte())
            bytes.put(at + STRING, encoded)
        }
    }

    /** `count` bytes of zeros: memory between objects, or memory the game freed. */
    fun gap(count: Int) {
        place(count) {}
    }

    override fun spans() = listOf(Span(start, next.toLong()))

    override fun read(address: Long, size: Int): ByteArray? {
        val at = address - start
        if (at < 0 || at + size > next) return null

        return ByteArray(size).also { bytes.get(at.toInt(), it) }
    }

    private fun place(size: Int, lay: (Int) -> Unit): Int {
        val at = next
        lay(at)
        next = (at + size + 15) / 16 * 16

        return at
    }

    private fun value(of: Any?): Pair<Long, Int> = when (of) {
        null -> 0L to 0x00
        is Boolean -> (if (of) 1L else 0L) to 0x01
        is Int -> of.toLong() to 0x13
        is Long -> of to 0x13
        is Double -> of.toRawBits() to 0x03
        is String -> string(of) to 0x44
        is Ref -> of.address to 0x45
        else -> error("no Lua value for $of")
    }

    private fun write(at: Int, value: Pair<Long, Int>) {
        bytes.putLong(at, value.first)
        bytes.putInt(at + 8, value.second)
    }

    data class Ref(val address: Long)

    private companion object {
        const val SIZE = 4 shl 20
        const val STRING = 24
        const val TABLE = 56
        const val VALUE = 16
        const val NODE = 32
    }
}

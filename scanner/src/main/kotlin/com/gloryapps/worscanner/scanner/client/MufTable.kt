package com.gloryapps.worscanner.scanner.client

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A `$muf_table_1`, how the client writes a table: a pool of strings, each byte XORed with its place's low byte, then each row's key beside where the row starts. */
internal class MufTable(private val bytes: ByteArray) {
    private val numbers = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    /** Where each string of the pool starts, at its length. */
    private val pool: IntArray
    /** Where the keys start, each beside where its row does. */
    private val keysAt: Int
    private val count: Int

    init {
        require(bytes.startsWith(TABLE)) { "not a table of the client's" }
        var at = TABLE.size + Int.SIZE_BYTES
        pool = IntArray(numbers.getInt(TABLE.size)) {
            val start = at
            val (length, text) = varint(start)
            at = text + length
            start
        }
        val index = numbers.getInt(at + KEYS_AT)
        count = numbers.getInt(index + Int.SIZE_BYTES)
        keysAt = index + KEY
    }

    /** Where each row starts, by its key. */
    private val starts: Map<Int, Int> by lazy { (0 until count).associate { numbers.getInt(keysAt + KEY * it) to numbers.getInt(keysAt + KEY * it + Int.SIZE_BYTES) } }
    private val sorted: IntArray by lazy { starts.values.toIntArray().apply { sort() } }

    /** Every row's key, in no order. */
    fun keys(): Set<Int> = starts.keys

    /** Where the row under `key` starts; null where there is none. */
    fun start(key: Int): Int? = starts[key]

    /** Where the row starting at `start` ends: where the next one starts, or where the keys do after the last. */
    fun end(start: Int): Int = sorted.getOrNull(sorted.binarySearch(start) + 1) ?: (keysAt - KEY)

    fun int(at: Int): Int = numbers.getInt(at)

    /** The number written seven bits a byte from `at`, least first, and where what it counts begins. */
    fun varint(at: Int): Pair<Int, Int> {
        var number = 0
        var read = at
        var shift = 0
        do {
            val byte = bytes[read++].toInt() and 0xFF
            number = number or (byte and 0x7F shl shift)
            shift += 7
        } while (byte >= 0x80)

        return number to read
    }

    /** The string in place `place` of the pool. */
    fun string(place: Int): String {
        val (length, at) = varint(pool[place])

        return ByteArray(length) { (bytes[at + it].toInt() xor it).toByte() }.decodeToString()
    }

    private companion object {
        /** After the pool, a byte and two numbers come before where the keys are. */
        const val KEYS_AT = 1 + 2 * Int.SIZE_BYTES
        const val KEY = 2 * Int.SIZE_BYTES
    }
}

internal val TABLE = "\$muf_table_1\u0000".encodeToByteArray()

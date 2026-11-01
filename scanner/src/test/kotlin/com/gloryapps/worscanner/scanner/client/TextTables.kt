package com.gloryapps.worscanner.scanner.client

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A `$muf_table_1` of `pool` and `rows` by key, laid out as the client lays one out: the pool, the rows, then where each starts. */
internal fun mufTable(pool: List<String>, rows: Map<Int, ByteArray>): ByteArray {
    val strings = ByteArrayOutputStream()
    pool.map { it.encodeToByteArray() }.forEach { text ->
        var length = text.size
        while (length >= 0x80) {
            strings.write(length and 0x7F or 0x80)
            length = length ushr 7
        }
        strings.write(length)
        strings.write(ByteArray(text.size) { (text[it].toInt() xor it).toByte() })
    }
    val first = TABLE.size + Int.SIZE_BYTES + strings.size() + 1 + 3 * Int.SIZE_BYTES
    val index = first + rows.values.sumOf { it.size }

    return ByteBuffer.allocate(index + 2 * Int.SIZE_BYTES * (1 + rows.size)).order(ByteOrder.LITTLE_ENDIAN).apply {
        put(TABLE).putInt(pool.size).put(strings.toByteArray()).put(2).putInt(rows.size).putInt(0).putInt(index)
        rows.values.forEach { put(it) }
        putInt(0).putInt(rows.size)
        var start = first
        rows.forEach { (key, row) -> putInt(key).putInt(start).also { start += row.size } }
    }.array()
}

/** A row's bytes as the client writes its numbers: four little-endian bytes each. */
internal fun numbers(vararg numbers: Int): ByteArray =
    ByteBuffer.allocate(Int.SIZE_BYTES * numbers.size).order(ByteOrder.LITTLE_ENDIAN).apply { numbers.forEach { putInt(it) } }.array()

/** A table of texts by their keys, each row the place of its text in the pool. */
internal fun textTable(texts: Map<String, String>): ByteArray =
    mufTable(texts.values.toList(), texts.keys.withIndex().associate { (place, key) -> hash(key) to numbers(place) })

/** A Unity 2019.4 bundle of one LZ4 block, holding `assets` as a serialized file keeps text assets: each name's length, the name filling whole words, then its own length and bytes. */
internal fun bundleOf(vararg assets: Pair<String, ByteArray>): ByteArray {
    val files = ByteArrayOutputStream().apply {
        write(ByteArray(32) { 7 })
        assets.forEach { (name, asset) ->
            val named = name.encodeToByteArray()
            write(numbers(named.size))
            write(named.copyOf((named.size + 3) / 4 * 4))
            write(numbers(asset.size))
            write(asset)
            write(ByteArray(12) { 9 })
        }
    }.toByteArray()
    val block = ByteArrayOutputStream().apply {
        write(0xF0)
        var left = files.size - 15
        while (left >= 0xFF) {
            write(0xFF)
            left -= 0xFF
        }
        write(left)
        write(files)
    }.toByteArray()
    val info = ByteBuffer.allocate(16 + Int.SIZE_BYTES + 10).put(ByteArray(16)).putInt(1).putInt(files.size).putInt(block.size).putShort(2).array()
    val head = ByteArrayOutputStream().apply {
        write("UnityFS\u0000".encodeToByteArray())
        write(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(7).array())
        write("5.x.x\u00002019.4.40f1\u0000".encodeToByteArray())
        write(ByteBuffer.allocate(20).putLong(0).putInt(info.size).putInt(info.size).putInt(0x40).array())
        while (size() % 16 != 0) write(0)
    }

    return head.toByteArray() + info + block
}

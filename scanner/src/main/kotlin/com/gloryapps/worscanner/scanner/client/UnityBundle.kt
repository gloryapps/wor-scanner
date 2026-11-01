package com.gloryapps.worscanner.scanner.client

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** The table `name` a Unity bundle (`UnityFS`) holds as a text asset, found where its serialized file keeps one: its name's length, the name filling whole words, then its own length and bytes. */
internal fun assetIn(bundle: ByteArray, name: String): ByteArray {
    val files = inflated(bundle)
    val named = name.encodeToByteArray()
    val record = ByteBuffer.allocate(Int.SIZE_BYTES + named.size.aligned(WORD)).order(ByteOrder.LITTLE_ENDIAN).putInt(named.size).put(named).array()
    var at = files.indexOf(record, from = 0)
    while (at >= 0 && !files.startsWith(TABLE, at = at + record.size + Int.SIZE_BYTES)) at = files.indexOf(record, from = at + 1)
    require(at >= 0) { "the bundle holds no table $name" }
    val length = ByteBuffer.wrap(files, at + record.size, Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).int
    val start = at + record.size + Int.SIZE_BYTES

    return files.copyOfRange(start, start + length)
}

/** A bundle's files, its blocks inflated one after the other; its numbers are big-endian. */
private fun inflated(bundle: ByteArray): ByteArray {
    require(bundle.startsWith(SIGNATURE)) { "not a UnityFS bundle" }
    val head = ByteBuffer.wrap(bundle).position(SIGNATURE.size)
    val format = head.int
    repeat(2) { while (head.get() != END) Unit }
    head.long
    val packedInfo = head.int
    val infoSize = head.int
    val flags = head.int
    if (format >= ALIGNED_FROM) head.position(head.position().aligned(BLOCK))
    val atEnd = flags and INFO_AT_END != 0
    val infoAt = if (atEnd) bundle.size - packedInfo else head.position()
    val info = ByteBuffer.wrap(unpacked(bundle.copyOfRange(infoAt, infoAt + packedInfo), infoSize, flags)).position(HASH)
    var at = if (atEnd) head.position() else infoAt + packedInfo
    if (flags and PADDED != 0) at = at.aligned(BLOCK)

    val files = ByteArrayOutputStream()
    repeat(info.int) {
        val size = info.int
        val packed = info.int
        files.write(unpacked(bundle.copyOfRange(at, at + packed), size, info.short.toInt()))
        at += packed
    }

    return files.toByteArray()
}

private fun unpacked(bytes: ByteArray, size: Int, flags: Int): ByteArray = when (val compression = flags and COMPRESSION) {
    NONE -> bytes
    LZ4, LZ4_HC -> lz4(bytes, size)
    else -> error("a block compressed the Unity way $compression")
}

/** An LZ4 block inflated to `size` bytes: runs of literals, each but the last followed by a copy of what is already out, which may overlap itself. */
internal fun lz4(packed: ByteArray, size: Int): ByteArray {
    val out = ByteArray(size)
    var read = 0
    var written = 0
    fun length(first: Int): Int {
        var total = first
        if (first == LONG) do {
            val more = packed[read++].toInt() and 0xFF
            total += more
        } while (more == 0xFF)
        return total
    }
    while (read < packed.size) {
        val token = packed[read++].toInt() and 0xFF
        val literals = length(token ushr 4)
        packed.copyInto(out, written, read, read + literals)
        read += literals
        written += literals
        if (read >= packed.size) break
        val offset = (packed[read].toInt() and 0xFF) or (packed[read + 1].toInt() and 0xFF shl 8)
        read += 2
        repeat(length(token and LONG) + MIN_MATCH) { out[written] = out[written - offset]; written++ }
    }
    check(written == size) { "the block inflated to $written bytes, not $size" }

    return out
}

internal fun ByteArray.startsWith(prefix: ByteArray, at: Int = 0): Boolean = at >= 0 && size - at >= prefix.size && prefix.indices.all { this[at + it] == prefix[it] }

private fun ByteArray.indexOf(part: ByteArray, from: Int): Int {
    for (at in from..size - part.size) if (startsWith(part, at)) return at
    return -1
}

private fun Int.aligned(to: Int): Int = (this + to - 1) and (to - 1).inv()

private val SIGNATURE = "UnityFS\u0000".encodeToByteArray()
private const val END: Byte = 0
/** Bundles of format 7 on align their block list to 16 bytes. */
private const val ALIGNED_FROM = 7
private const val BLOCK = 16
/** A serialized file aligns what follows a name to whole words. */
private const val WORD = 4
private const val HASH = 16
private const val COMPRESSION = 0x3F
private const val INFO_AT_END = 0x80
private const val PADDED = 0x200
private const val NONE = 0
private const val LZ4 = 2
private const val LZ4_HC = 3
private const val LONG = 15
private const val MIN_MATCH = 4

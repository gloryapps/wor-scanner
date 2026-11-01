package com.gloryapps.worscanner.scanner.client

import com.gloryapps.worscanner.scanner.senses.Memory
import com.gloryapps.worscanner.scanner.senses.sweep
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap

/** How the client's tables write a field of one of its C# classes. */
enum class Column { INT, FLOAT, STRING, INTS, INT_LISTS }

/** A field of one of the game's C# classes, by name, with how its tables write it; none for a type they were never read with. */
data class ClassField(val name: String, val type: Column?)

/**
 * The instance fields of the C# class declaring the field `anchor`, in the order it declares them, as
 * il2cpp keeps them in the game's memory: the names in its metadata, which the game maps, and each
 * field's record pointing at its name, which the game allocates beside its class's others.
 */
suspend fun classFields(memory: Memory, anchor: String): List<ClassField> {
    val named = "\u0000$anchor\u0000".encodeToByteArray()
    val names = ConcurrentHashMap.newKeySet<Long>()
    memory.sweep(memory.mapped()) { at, bytes, looked ->
        for (offset in 0 until minOf(looked, bytes.limit() - named.size)) {
            if (named.indices.all { bytes.get(offset + it) == named[it] }) names += at + offset + 1
        }
    }
    val wanted = names.sorted().toLongArray()
    val records = ConcurrentHashMap.newKeySet<Long>()
    memory.sweep(memory.spans()) { at, bytes, looked ->
        for (offset in 0 until minOf(looked, bytes.limit() - Long.SIZE_BYTES) step Long.SIZE_BYTES) {
            if (wanted.binarySearch(bytes.getLong(offset)) >= 0) records += at + offset
        }
    }
    val record = records.minOrNull() ?: return emptyList()
    val parent = memory.long(record + PARENT) ?: return emptyList()
    val first = generateSequence(record) { it - FIELD }.takeWhile { memory.long(it + PARENT) == parent }.last()

    return generateSequence(first) { it + FIELD }.takeWhile { memory.long(it + PARENT) == parent }.mapNotNull(memory::field).toList()
}

/* A field's record: its name, its type, its class; a type: what it is built on, then its attributes and its kind. */
private fun Memory.field(record: Long): ClassField? {
    val type = long(record + TYPE) ?: return null
    if ((int(type + BITS) ?: return null) and STATIC != 0) return null

    return ClassField(name = text(long(record) ?: return null) ?: return null, type = column(type))
}

private fun Memory.column(type: Long): Column? = when (kind(type)) {
    I4 -> Column.INT
    R4 -> Column.FLOAT
    STRING -> Column.STRING
    SZARRAY -> long(type)?.let { element ->
        when (kind(element)) {
            I4 -> Column.INTS
            SZARRAY -> Column.INT_LISTS.takeIf { long(element)?.let { inner -> kind(inner) } == I4 }
            else -> null
        }
    }
    else -> null
}

private fun Memory.kind(type: Long): Int? = int(type + BITS)?.let { it ushr KIND_SHIFT and 0xFF }

private fun Memory.long(address: Long): Long? = read(address, Long.SIZE_BYTES)?.let { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).long }

private fun Memory.int(address: Long): Int? = read(address, Int.SIZE_BYTES)?.let { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).int }

/** The C string at `address`, read a piece at a time so as not to read far past what the game maps. */
private fun Memory.text(address: Long): String? {
    var read = ByteArray(0)
    for (from in 0 until MAX_NAME step PIECE) {
        val piece = read(address + from, PIECE) ?: return null
        val end = piece.indexOf(0)
        if (end >= 0) return (read + piece.copyOf(end)).decodeToString()
        read += piece
    }

    return null
}

/* A field's record, as il2cpp lays it out. */
private const val FIELD = 32L
private const val TYPE = 8
private const val PARENT = 16
/* A type's attributes in the low half of the number after its pointer, then its kind. */
private const val BITS = 8
private const val KIND_SHIFT = 16
private const val STATIC = 0x10
/* The kinds of type the client's tables hold, as il2cpp numbers them. */
private const val I4 = 0x08
private const val R4 = 0x0C
private const val STRING = 0x0E
private const val SZARRAY = 0x1D
private const val PIECE = 16
private const val MAX_NAME = 256

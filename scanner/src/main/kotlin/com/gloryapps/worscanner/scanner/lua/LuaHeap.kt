package com.gloryapps.worscanner.scanner.lua

import com.gloryapps.worscanner.scanner.senses.Memory
import com.gloryapps.worscanner.scanner.senses.Span
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/** Lua 5.3's strings and tables in the game's memory, as a 64-bit build lays them out (`lobject.h`). */
class LuaHeap(private val memory: Memory) {
    private val texts = ConcurrentHashMap<Long, String>()

    /** Every table holding a field under one of `names`, by the name: three sweeps, for the strings, the nodes keyed by them, and the tables those nodes are in. */
    suspend fun holders(names: Set<String>): Map<String, Set<Long>> {
        val strings = strings(names)
        val keys = keys(strings.keys)
        val nodes = keys.keys.sorted().toLongArray()
        val holders = ConcurrentHashMap<String, MutableSet<Long>>()
        sweep { at, bytes, looked ->
            for (offset in 0 until minOf(looked, bytes.limit() - TABLE_SIZE) step ALIGNMENT) {
                val log = bytes.nodeLog(offset) ?: continue
                val first = bytes.getLong(offset + NODES)
                val end = first + (NODE_SIZE.toLong() shl log)
                var hit = nodes.binarySearch(first).let { if (it < 0) -it - 1 else it }
                while (hit < nodes.size && nodes[hit] < end) {
                    if ((nodes[hit] - first) % NODE_SIZE == 0L) {
                        holders.getOrPut(strings.getValue(keys.getValue(nodes[hit]))) { ConcurrentHashMap.newKeySet() } += at + offset
                    }
                    hit++
                }
            }
        }

        return holders
    }

    /** Every table in the game's memory, by its address, lowest first: a head whose last free node lies outside its own nodes is no table's. */
    suspend fun tables(): LongArray {
        val found = ConcurrentLinkedQueue<LongArray>()
        sweep { at, bytes, looked ->
            val here = mutableListOf<Long>()
            for (offset in 0 until minOf(looked, bytes.limit() - TABLE_SIZE) step ALIGNMENT) {
                val log = bytes.nodeLog(offset) ?: continue
                val nodes = bytes.getLong(offset + NODES)
                val free = bytes.getLong(offset + LAST_FREE)
                /* Lua 5.3 marks a table with no hash part by no last free node, its nodes the one dummy node. */
                val held = if (free == 0L) log == 0 else free - nodes in 0..(NODE_SIZE.toLong() shl log) && (free - nodes) % NODE_SIZE == 0L
                if (held && nodes != 0L) here += at + offset
            }
            found += here.toLongArray()
        }

        return found.flatMap { it.asList() }.toLongArray().apply { sort() }
    }

    /** The table's fields, its array part's under 1, 2, 3…; null where `address` holds no table. */
    fun fields(address: Long): Map<LuaValue, LuaValue>? = table(address)?.let { table ->
        buildMap {
            table.array.forEachIndexed { index, value -> if (value != null) put(LuaValue.Integer(index + 1L), value) }
            putAll(table.hash)
        }
    }

    /** The table at `address` as it lies, each part read to its first `most` values; null where `address` holds no table. */
    fun table(address: Long, most: Int = Int.MAX_VALUE): LuaTable? {
        val head = memory.read(address, TABLE_SIZE)?.let(::littleEndian) ?: return null
        val log = head.nodeLog(0)
        val length = head.getInt(ARRAY_LENGTH).toUInt().toLong()
        if (log == null || length > MAX_ARRAY) return null

        val read = minOf(length.toInt(), most)
        val array = if (read == 0) null else memory.read(head.getLong(ARRAY), read * VALUE_SIZE)?.let(::littleEndian)?.let { array -> List(read) { valueAt(array, it * VALUE_SIZE) } }
        val hash = LinkedHashMap<LuaValue, LuaValue>()
        memory.read(head.getLong(NODES), NODE_SIZE shl log)?.let(::littleEndian)?.let { nodes ->
            for (node in 0 until (1 shl log)) {
                if (hash.size == most) break
                val key = valueAt(nodes, node * NODE_SIZE + KEY)?.takeIf { it is LuaValue.Text || it is LuaValue.Integer } ?: continue
                hash[key] = valueAt(nodes, node * NODE_SIZE) ?: continue
            }
        }

        return LuaTable(length.toInt(), array.orEmpty(), hash)
    }

    fun field(address: Long, name: String): LuaValue? = fields(address)?.get(LuaValue.Text(name))

    /** The string at `address`; null where none lives there. */
    fun text(address: Long): String? = texts[address] ?: readText(address)?.also { texts[address] = it }

    private fun readText(address: Long): String? {
        val head = memory.read(address, STRING_HEAD)?.let(::littleEndian) ?: return null
        val length = when (head.get(TAG)) {
            SHORT_STRING -> head.get(SHORT_LENGTH).toInt() and 0xFF
            LONG_STRING -> head.getLong(LONG_LENGTH).takeIf { it in 0..MAX_TEXT }?.toInt() ?: return null
            else -> return null
        }

        return memory.read(address + STRING_HEAD, length)?.decodeToString()
    }

    private fun valueAt(bytes: ByteBuffer, at: Int): LuaValue? {
        val raw = bytes.getLong(at)

        return when (val tag = bytes.getInt(at + TAG)) {
            NIL -> null
            BOOLEAN -> LuaValue.Bool(raw.toInt() != 0)
            INTEGER -> LuaValue.Integer(raw)
            REAL -> LuaValue.Real(Double.fromBits(raw))
            SHORT_STRING_VALUE, LONG_STRING_VALUE -> text(raw)?.let(LuaValue::Text) ?: LuaValue.Other(tag)
            TABLE_VALUE -> LuaValue.Table(raw)
            else -> LuaValue.Other(tag)
        }
    }

    /** Where each of `names` lives as a short string, by the address. */
    private suspend fun strings(names: Set<String>): Map<Long, String> {
        val wanted = names.map { it.encodeToByteArray() }.filter { it.size <= MAX_SHORT }.groupBy { it.size }
        val found = ConcurrentHashMap<Long, String>()
        sweep { at, bytes, looked ->
            for (offset in 0 until minOf(looked, bytes.limit() - STRING_HEAD) step ALIGNMENT) {
                if (bytes.get(offset + TAG) != SHORT_STRING) continue
                val length = bytes.get(offset + SHORT_LENGTH).toInt() and 0xFF
                val text = offset + STRING_HEAD
                if (text + length >= bytes.limit() || bytes.get(text + length) != END) continue
                val name = wanted[length]?.firstOrNull { name -> name.indices.all { bytes.get(text + it) == name[it] } } ?: continue
                found[at + offset] = name.decodeToString()
            }
        }

        return found
    }

    /** The nodes keyed by one of `strings`, each to its string. */
    private suspend fun keys(strings: Set<Long>): Map<Long, Long> {
        val found = ConcurrentHashMap<Long, Long>()
        sweep { at, bytes, looked ->
            for (offset in 0 until minOf(looked, bytes.limit() - VALUE_SIZE) step ALIGNMENT) {
                if (bytes.getInt(offset + TAG) != SHORT_STRING_VALUE) continue
                val string = bytes.getLong(offset)
                if (string in strings) found[at + offset - KEY] = string
            }
        }

        return found
    }

    /** Hands each chunk of the game's memory to `look`, side by side: its address, its bytes, and how many it starts objects in, the rest only read past. */
    private suspend fun sweep(look: (at: Long, bytes: ByteBuffer, looked: Int) -> Unit) = coroutineScope {
        memory.spans().flatMap(::chunks).map { chunk ->
            launch(Dispatchers.Default) { memory.read(chunk.at, chunk.size)?.let { look(chunk.at, littleEndian(it), chunk.looked) } }
        }.joinAll()
    }

    private fun chunks(span: Span): List<Chunk> = (0 until span.size step CHUNK.toLong()).map { from ->
        val left = span.size - from

        Chunk(span.start + from, looked = minOf(CHUNK.toLong(), left).toInt(), size = minOf(CHUNK.toLong() + PAST, left).toInt())
    }

    private data class Chunk(val at: Long, val looked: Int, val size: Int)
}

private fun littleEndian(bytes: ByteArray): ByteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

/** The hash part's size, as a power of two, of a table whose head lies at `offset`; null where no table's head can. */
private fun ByteBuffer.nodeLog(offset: Int): Int? =
    (get(offset + NODE_LOG).toInt() and 0xFF).takeIf { get(offset + TAG) == TABLE && it <= MAX_NODE_LOG }

/* Where an object's fields sit, from its start. */
private const val TAG = 8
private const val SHORT_LENGTH = 11
private const val LONG_LENGTH = 16
private const val STRING_HEAD = 24
private const val NODE_LOG = 11
private const val ARRAY_LENGTH = 12
private const val ARRAY = 16
private const val NODES = 24
private const val LAST_FREE = 32
private const val KEY = 16
private const val TABLE_SIZE = 56
private const val VALUE_SIZE = 16
private const val NODE_SIZE = 32

/* An object's tag, then a value's, which marks a collected object's with 0x40. */
private const val SHORT_STRING: Byte = 0x04
private const val LONG_STRING: Byte = 0x14
private const val TABLE: Byte = 0x05
private const val NIL = 0x00
private const val BOOLEAN = 0x01
private const val REAL = 0x03
private const val INTEGER = 0x13
private const val SHORT_STRING_VALUE = 0x44
private const val LONG_STRING_VALUE = 0x54
private const val TABLE_VALUE = 0x45

private const val END: Byte = 0
private const val ALIGNMENT = 8
private const val MAX_SHORT = 40
private const val MAX_TEXT = 65_536L
private const val MAX_NODE_LOG = 20
private const val MAX_ARRAY = 1_000_000L
private const val CHUNK = 8 shl 20
private const val PAST = 128

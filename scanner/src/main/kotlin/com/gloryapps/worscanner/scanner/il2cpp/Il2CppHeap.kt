package com.gloryapps.worscanner.scanner.il2cpp

import com.gloryapps.worscanner.scanner.senses.Memory
import com.gloryapps.worscanner.scanner.senses.littleEndian
import com.gloryapps.worscanner.scanner.senses.sweep
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/** A class as the game's IL2CPP runtime holds it: where, its names, its parent's address, its static block and its own fields. */
data class Il2CppClass(val address: Long, val name: String, val namespace: String, val parent: Long, val statics: Long, val fields: List<Il2CppField>)

/** A field as the class describes it: its name, where it lies from the object's start (or the static block's), its type and its attributes. */
data class Il2CppField(val name: String, val offset: Int, val type: Int, val attributes: Int, val typeData: Long) {
    val static get() = attributes and STATIC != 0

    /** A constant has no storage, and a thread's own static none this reader reaches. */
    val stored get() = attributes and LITERAL == 0 && offset >= 0
}

/** IL2CPP's classes and objects in the game's memory, as Unity 2019.4 (metadata 24) lays them out on 64 bits (`il2cpp-class-internals.h`). */
class Il2CppHeap(private val memory: Memory) {
    private val classes = ConcurrentHashMap<Long, Il2CppClass>()
    private val texts = ConcurrentHashMap<Long, String>()

    /** Every class named one of `names`: their names in the read-only metadata, then the classes pointing at those names. */
    suspend fun classes(names: Set<String>): List<Il2CppClass> {
        val strings = names(names)
        val wanted = strings.keys.sorted().toLongArray()
        val found = ConcurrentHashMap.newKeySet<Long>()
        memory.sweep { at, bytes, looked ->
            for (offset in 0 until minOf(looked, bytes.limit() - NAME - 8) step ALIGNMENT) {
                if (wanted.binarySearch(bytes.getLong(offset + NAME)) >= 0) found += at + offset
            }
        }

        return found.sorted().mapNotNull(::klass).filter { it.name in names }
    }

    /** The class at `address`, its fields read off the runtime; null where none lies there. */
    fun klass(address: Long): Il2CppClass? = classes[address] ?: readClass(address)?.also { classes[address] = it }

    /** Every field an object of `klass` holds, its ancestors' first. */
    fun fieldsOf(klass: Il2CppClass): List<Il2CppField> =
        generateSequence(klass) { it.parent.takeIf { parent -> parent != 0L }?.let(::klass) }.toList().asReversed().flatMap { it.fields }

    /** Where objects of each of `classes` lie, by the class: a word pointing at the class, then a monitor that is none or a pointer; at most `most` each. */
    suspend fun instances(classes: Collection<Il2CppClass>, most: Int): Map<Long, List<Long>> {
        val wanted = classes.map { it.address }.sorted().toLongArray()
        val found = ConcurrentHashMap<Long, MutableSet<Long>>()
        memory.sweep { at, bytes, looked ->
            for (offset in 0 until minOf(looked, bytes.limit() - 16) step ALIGNMENT) {
                val klass = bytes.getLong(offset)
                if (wanted.binarySearch(klass) < 0) continue
                val monitor = bytes.getLong(offset + MONITOR)
                if (monitor != 0L && (monitor < LOWEST_POINTER || monitor % ALIGNMENT != 0L)) continue
                val objects = found.getOrPut(klass) { ConcurrentHashMap.newKeySet() }
                if (objects.size < most) objects += at + offset
            }
        }

        return found.mapValues { (_, objects) -> objects.sorted() }
    }

    /** The name of the class of the object at `address`; null where no class's object lies there. */
    fun classOf(address: Long): String? = memory.read(address, 8)?.let(::littleEndian)?.getLong(0)?.let(::klass)?.name

    /** The C string at `address`, at most [MOST_NAME] bytes. */
    fun text(address: Long): String? = if (address == 0L) null else texts[address] ?: readText(address)?.also { texts[address] = it }

    /** A System.String's characters; null where none lies at `address`. */
    fun string(address: Long): String? {
        val head = memory.read(address, STRING_CHARS)?.let(::littleEndian) ?: return null
        val length = head.getInt(STRING_LENGTH).takeIf { it in 0..MOST_STRING } ?: return null

        return memory.read(address + STRING_CHARS, length * 2)?.let { String(it, Charsets.UTF_16LE) }
    }

    fun read(address: Long, size: Int): ByteBuffer? = memory.read(address, size)?.let(::littleEndian)

    private fun readClass(address: Long): Il2CppClass? {
        val head = read(address, CLASS_HEAD) ?: return null
        val name = text(head.getLong(NAME))?.takeIf(::isName) ?: return null
        val namespace = text(head.getLong(NAMESPACE)) ?: return null

        return Il2CppClass(address, name, namespace, head.getLong(PARENT), head.getLong(STATICS), fieldsAt(address, head))
    }

    /** The class's own fields: the first of the runtime's candidate places whose entries name it as their parent. */
    private fun fieldsAt(address: Long, head: ByteBuffer): List<Il2CppField> = FIELDS.asSequence()
        .map { at -> fields(address, head.getLong(at)) }
        .firstOrNull { it.isNotEmpty() }
        .orEmpty()

    private fun fields(klass: Long, first: Long): List<Il2CppField> {
        if (first == 0L) return emptyList()
        val fields = mutableListOf<Il2CppField>()
        for (index in 0 until MOST_FIELDS) {
            val info = read(first + index.toLong() * FIELD_SIZE, FIELD_SIZE) ?: break
            if (info.getLong(FIELD_PARENT) != klass) break
            val name = text(info.getLong(FIELD_NAME))?.takeIf(::isName) ?: break
            val type = read(info.getLong(FIELD_TYPE), TYPE_SIZE) ?: break
            val bits = type.getInt(TYPE_BITS)
            fields += Il2CppField(name, info.getInt(FIELD_OFFSET), (bits ushr 16) and 0xFF, bits and 0xFFFF, type.getLong(0))
        }

        return fields
    }

    /** Where each of `names` lies as a C string, in the metadata: preceded and followed by a NUL. */
    private suspend fun names(names: Set<String>): Map<Long, String> {
        val wanted = names.map { (it + '\u0000').encodeToByteArray() }
        val firsts = BooleanArray(256).also { first -> wanted.forEach { first[it[0].toInt() and 0xFF] = true } }
        val found = ConcurrentHashMap<Long, String>()
        memory.sweep(memory.readOnly() + memory.spans()) { at, bytes, looked ->
            for (offset in 1 until minOf(looked, bytes.limit())) {
                if (!firsts[bytes.get(offset).toInt() and 0xFF] || bytes.get(offset - 1) != END) continue
                val name = wanted.firstOrNull { name -> offset + name.size <= bytes.limit() && name.indices.all { bytes.get(offset + it) == name[it] } } ?: continue
                found[at + offset] = name.decodeToString().dropLast(1)
            }
        }

        return found
    }

    /* Read a step at a time, so a name close to its region's end still reads. */
    private fun readText(address: Long): String? {
        val text = java.io.ByteArrayOutputStream()
        while (text.size() < MOST_NAME) {
            val step = memory.read(address + text.size(), TEXT_STEP) ?: return null
            val end = step.indexOf(END)
            if (end >= 0) return (text.toByteArray() + step.copyOf(end)).decodeToString()
            text.write(step)
        }

        return null
    }

    private fun isName(text: String) = text.isNotEmpty() && text.all { it.code in 0x20..0x7E }
}

/* Il2CppClass: the image, the GC descriptor, then these; the field list's place moved between runtime versions, so each of these is tried. */
private const val NAME = 0x10
private const val NAMESPACE = 0x18
private const val PARENT = 0x58
private val FIELDS = listOf(0x80, 0x78, 0x88, 0x70, 0x90)
private const val STATICS = 0xB8
private const val CLASS_HEAD = 0x120

/* FieldInfo: its name, its Il2CppType, its parent class, its offset and its token. */
private const val FIELD_NAME = 0
private const val FIELD_TYPE = 8
private const val FIELD_PARENT = 16
private const val FIELD_OFFSET = 24
private const val FIELD_SIZE = 32

/* Il2CppType: its data, then a word of attributes (16 bits) and the type's code (8 bits). */
private const val TYPE_BITS = 8
private const val TYPE_SIZE = 16

/* System.String: its class, its monitor, its length, then its UTF-16 characters. */
private const val STRING_LENGTH = 16
private const val STRING_CHARS = 20

private const val MONITOR = 8
private const val LOWEST_POINTER = 0x10000L
private const val STATIC = 0x10
private const val LITERAL = 0x40
private const val ALIGNMENT = 8
private const val MOST_FIELDS = 512
private const val MOST_NAME = 128
private const val TEXT_STEP = 16
private const val MOST_STRING = 4_096
private const val END: Byte = 0

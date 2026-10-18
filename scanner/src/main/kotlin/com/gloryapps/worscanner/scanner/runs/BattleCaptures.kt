package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.account.Account
import com.gloryapps.worscanner.scanner.account.readAccount
import com.gloryapps.worscanner.scanner.il2cpp.Il2CppClass
import com.gloryapps.worscanner.scanner.il2cpp.Il2CppField
import com.gloryapps.worscanner.scanner.il2cpp.Il2CppHeap
import com.gloryapps.worscanner.scanner.senses.Memory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.time.LocalDateTime
import java.util.zip.GZIPOutputStream

/** A fight's state read off the game's memory for a diagnosis: a gzipped file of JSON lines in `battles`, named by the stamp it started at. */
class BattleCaptures(private val root: File) {
    /**
     * Captures `memory` while `going` says so, `seconds` at most: a first line naming each battle class found and its
     * fields, then a line every `everyMs` with each object of those classes and its fields' values, the objects found again
     * every [FIND_EVERY] lines, and a last line with the account's entries for the heroes the fight held: each hero, the
     * pieces and the artifact it wears, read as the fight ends.
     */
    suspend fun take(
        memory: Memory,
        going: () -> Boolean = { true },
        seconds: Int = SECONDS,
        everyMs: Long = EVERY_MS,
        names: Set<String> = BATTLE_CLASSES,
    ): File = withContext(Dispatchers.IO) {
        val startedAt = LocalDateTime.now().format(STAMP)
        val heap = Il2CppHeap(memory)
        val classes = heap.classes(names)
        val layouts = classes.associate { it.address to heap.fieldsOf(it) }
        val file = File(root, "battles/$startedAt.jsonl.gz").apply { parentFile.mkdirs() }

        GZIPOutputStream(file.outputStream()).bufferedWriter().use { out ->
            val heads = classes.map { klass -> heap.read(klass.address, CLASS_HEAD)?.let { buffer -> ByteArray(CLASS_HEAD).also { buffer.get(0, it) }.joinToString("") { "%02x".format(it) } } }
            out.appendLine(JsonObject(mapOf("startedAt" to JsonPrimitive(startedAt), "classes" to JsonArray(classes.mapIndexed { index, it -> it.json(layouts.getValue(it.address), heads[index]) }))).toString())
            val start = System.nanoTime()
            var objects = emptyMap<Long, List<Long>>()
            var reached = emptySet<Long>()
            var findMs = 0L
            val units = mutableSetOf<Long>()
            /* The heroes the fight held: the units it fields, and the ones its statistics name, as a Lord on the bench. */
            val named = listOf(UNIT_ROW to "m_ID", STATISTICS_ROW to "iBaseID").mapNotNull { (name, id) ->
                classes.firstOrNull { it.name == name }?.let { klass -> layouts.getValue(klass.address).firstOrNull { it.name == id }?.let { klass to it } }
            }
            for (tick in 0 until (seconds * 1000 / everyMs).toInt()) {
                if (!going()) break
                if (tick % FIND_EVERY == 0) {
                    val finding = System.nanoTime()
                    val scanned = heap.instances(classes.filter { it.name !in REACHED_ONLY && !it.name.startsWith(TABLE_ROW) }, MOST_OBJECTS)
                    val roots = listOfNotNull(heap.fight(classes, layouts)) + classes.filter { it.name in REACH_ROOTS }.flatMap { scanned[it.address].orEmpty() }
                    val found = heap.reached(roots, classes)
                    reached = found.values.flatten().toSet()
                    for ((klass, id) in named) found[klass.address].orEmpty().mapNotNullTo(units) { heap.read(it + id.offset, 4)?.getInt(0)?.toLong()?.takeIf { unit -> unit > 0 } }
                    objects = (scanned.keys + found.keys).associateWith { (scanned[it].orEmpty() + found[it].orEmpty()).distinct().sorted() }
                    findMs = (System.nanoTime() - finding) / 1_000_000
                }
                val at = (System.nanoTime() - start) / 1_000_000
                out.appendLine(heap.line(at, classes, layouts, objects, reached, findMs).toString())
                delay((everyMs * (tick + 1) - (System.nanoTime() - start) / 1_000_000).coerceAtLeast(0))
            }
            val account = runCatching { readAccount(memory, startedAt).ofHeroes(units) }.getOrElse { JsonNull }
            out.appendLine(JsonObject(mapOf("account" to account)).toString())
        }

        file
    }
}

/** The account's entries for the heroes among `units`: each hero, and the pieces and artifacts naming it as their wearer (`iHeroId`, its id × [HERO_ID_SCALE]). */
private fun Account.ofHeroes(units: Set<Long>): JsonObject {
    fun field(entry: JsonElement, name: String) = (entry as? JsonObject)?.get(name)?.jsonPrimitive?.longOrNull
    fun worn(entry: JsonElement) = field(entry, "iHeroId")?.let { it / HERO_ID_SCALE in units } == true

    return JsonObject(
        mapOf(
            "heroes" to JsonArray(heroes.filter { field(it, "iBaseId") in units }),
            "gear" to JsonArray(gear.filter(::worn)),
            "artifacts" to JsonArray(artifacts.filter(::worn)),
        ),
    )
}

/** A class as the first line names it: where, its names, its runtime head's bytes to check the layout read, and each field's name, offset, type code and whether it is static. */
private fun Il2CppClass.json(fields: List<Il2CppField>, head: String?) = JsonObject(
    mapOf(
        "at" to JsonPrimitive(address),
        "head" to (head?.let(::JsonPrimitive) ?: JsonNull),
        "name" to JsonPrimitive(name),
        "namespace" to JsonPrimitive(namespace),
        "parent" to JsonPrimitive(parent),
        "fields" to JsonArray(fields.map { JsonArray(listOf(JsonPrimitive(it.name), JsonPrimitive(it.offset), JsonPrimitive(it.type), JsonPrimitive(it.static))) }),
    ),
)

/**
 * One moment: its milliseconds from the capture's start, the fight it is in, each class's static fields, and each object's instance
 * fields; `r` marks an object reached from the fight or a [REACH_ROOTS] object, which the heap scan's look-alikes never are, and
 * `findMs` is how long the last finding of the objects took.
 */
private fun Il2CppHeap.line(at: Long, classes: List<Il2CppClass>, layouts: Map<Long, List<Il2CppField>>, objects: Map<Long, List<Long>>, reached: Set<Long>, findMs: Long) = JsonObject(
    mapOf(
        "t" to JsonPrimitive(at),
        "findMs" to JsonPrimitive(findMs),
        "fight" to (fight(classes, layouts)?.let(::JsonPrimitive) ?: JsonNull),
        "statics" to JsonObject(
            classes.filter { it.statics != 0L }.associate { klass ->
                "${klass.name}@${klass.address}" to fieldsJson(klass.statics, klass.fields.filter { it.static && it.stored })
            },
        ),
        "objects" to JsonArray(
            classes.flatMap { klass ->
                val fields = layouts.getValue(klass.address).filter { !it.static && it.stored }
                objects[klass.address].orEmpty().map { address ->
                    JsonObject(
                        buildMap {
                            put("c", JsonPrimitive(klass.name))
                            put("at", JsonPrimitive(address))
                            if (address in reached) put("r", JsonPrimitive(true))
                            put("f", fieldsJson(address, fields))
                        },
                    )
                }
            },
        ),
    ),
)

/** The simulation `BattleManager.instance` runs now: a new one each time the fight starts again. */
private fun Il2CppHeap.fight(classes: List<Il2CppClass>, layouts: Map<Long, List<Il2CppField>>): Long? {
    val manager = classes.firstOrNull { it.name == "BattleManager" && it.statics != 0L } ?: return null
    val instance = manager.fields.firstOrNull { it.name == "instance" && it.static }?.let { read(manager.statics + it.offset, 8)?.getLong(0) }?.takeIf { it != 0L } ?: return null
    val simulation = layouts.getValue(manager.address).firstOrNull { it.name == "m_simulation" && !it.static } ?: return null

    return read(instance + simulation.offset, 8)?.getLong(0)
}

/**
 * Objects of `classes` reached from `roots` through their references, any object, array or collection in between, [REACH_DEPTH]
 * hops at most: what the heap scan misses or cannot tell from a look-alike, as the statistics a component keeps in dictionaries.
 * A game table's row is kept but not gone through, as it leads to the whole table; neither are the game's caches and data managers,
 * nor the pools that keep a finished fight's objects for reuse.
 */
private fun Il2CppHeap.reached(roots: Collection<Long>, classes: List<Il2CppClass>): Map<Long, List<Long>> {
    val wanted = classes.map { it.address }.toSet()
    val found = mutableMapOf<Long, MutableSet<Long>>()
    val seen = HashSet<Long>()
    var layer: Collection<Long> = roots.toSet()
    repeat(REACH_DEPTH) {
        val next = LinkedHashSet<Long>()
        for (address in layer) {
            if (seen.size >= MOST_REACHED || !seen.add(address)) continue
            val klassAddress = read(address, 8)?.getLong(0) ?: continue
            val klass = klass(klassAddress) ?: continue
            if (klassAddress in wanted) found.getOrPut(klassAddress) { mutableSetOf() } += address
            if (klass.name.startsWith(TABLE_ROW) || klass.name in NOT_WALKED) continue
            pointersOf(address, klass).filterTo(next) { it !in seen }
        }
        layer = next
    }

    return found.mapValues { (_, objects) -> objects.sorted() }
}

/**
 * The words an object points through: its reference fields, every word of its struct fields, or an array's items, every word of
 * them, as a struct holds its references among other values.
 */
private fun Il2CppHeap.pointersOf(address: Long, klass: Il2CppClass): List<Long> {
    if (!klass.name.endsWith("[]")) {
        val fields = fieldsOf(klass).filter { !it.static && it.stored }.sortedBy { it.offset }

        return fields.flatMapIndexed { index, field ->
            when (field.type) {
                in REFERENCES -> listOfNotNull(read(address + field.offset, 8)?.getLong(0))
                VALUE_TYPE -> {
                    val room = ((fields.getOrNull(index + 1)?.offset ?: (field.offset + 8)) - field.offset).coerceIn(8, MOST_STRUCT)
                    read(address + field.offset, room / 8 * 8)?.let { words -> (0 until words.limit() / 8).map { words.getLong(it * 8) } }.orEmpty()
                }
                else -> emptyList()
            }
        }.filter { it >= LOWEST_POINTER && it % 8 == 0L }
    }
    val count = read(address + ARRAY_LENGTH, 8)?.getLong(0)?.takeIf { it in 0..MOST_COUNT } ?: return emptyList()
    val words = read(address + ARRAY_ITEMS, (minOf(count * WORDS_PER_ITEM, MOST_WORDS.toLong()) * 8).toInt()) ?: return emptyList()

    return (0 until words.limit() / 8).map { words.getLong(it * 8) }.filter { it >= LOWEST_POINTER && it % 8 == 0L }
}

/** Each field's value at `base`, a struct's bytes as far as the next field. */
private fun Il2CppHeap.fieldsJson(base: Long, fields: List<Il2CppField>): JsonObject {
    val sorted = fields.sortedBy { it.offset }

    return JsonObject(
        sorted.mapIndexed { index, field ->
            val room = (sorted.getOrNull(index + 1)?.offset ?: (field.offset + MOST_STRUCT)) - field.offset
            field.name to value(base + field.offset, field.type, room.coerceIn(1, MOST_STRUCT))
        }.toMap(),
    )
}

/** A value as its type code says: numbers as themselves, a string's text, a struct's bytes, a reference as where and what it points at. */
private fun Il2CppHeap.value(at: Long, type: Int, room: Int): JsonElement {
    val bytes = read(at, if (type == VALUE_TYPE) room else 8) ?: return JsonNull

    return when (type) {
        BOOLEAN -> JsonPrimitive(bytes.get(0).toInt() != 0)
        CHAR, U2 -> JsonPrimitive(bytes.getShort(0).toInt() and 0xFFFF)
        I1 -> JsonPrimitive(bytes.get(0).toInt())
        U1 -> JsonPrimitive(bytes.get(0).toInt() and 0xFF)
        I2 -> JsonPrimitive(bytes.getShort(0).toInt())
        I4 -> JsonPrimitive(bytes.getInt(0))
        U4 -> JsonPrimitive(bytes.getInt(0).toUInt().toLong())
        I8, U8, POINTER, NATIVE_INT, NATIVE_UINT -> JsonPrimitive(bytes.getLong(0))
        R4 -> bytes.getFloat(0).let { if (it.isFinite()) JsonPrimitive(it) else JsonNull }
        R8 -> bytes.getDouble(0).let { if (it.isFinite()) JsonPrimitive(it) else JsonNull }
        STRING -> string(bytes.getLong(0))?.let(::JsonPrimitive) ?: JsonNull
        VALUE_TYPE -> JsonObject(
            mapOf(
                "raw" to JsonPrimitive(ByteArray(room).also { bytes.get(0, it) }.joinToString("") { "%02x".format(it) }),
                "i32" to JsonPrimitive(if (room >= 4) bytes.getInt(0) else 0),
                "i64" to JsonPrimitive(if (room >= 8) bytes.getLong(0) else 0L),
            ),
        )
        in REFERENCES -> reference(bytes.getLong(0))
        else -> JsonObject(mapOf("type" to JsonPrimitive(type)))
    }
}

/** What a reference points at: nothing, or where and its class; a list's or an array's count and first words, each the class it points at where it is an object. */
private fun Il2CppHeap.reference(address: Long): JsonElement {
    if (address == 0L) return JsonNull
    val name = classOf(address) ?: return JsonObject(mapOf("at" to JsonPrimitive(address)))
    val (count, items) = when {
        name.endsWith("[]") -> read(address + ARRAY_LENGTH, 8)?.getLong(0) to address + ARRAY_ITEMS
        name == "List`1" -> read(address + LIST_SIZE, 4)?.getInt(0)?.toLong() to read(address + LIST_ITEMS, 8)?.getLong(0)?.plus(ARRAY_ITEMS)
        else -> null to null
    }
    val words = if (count != null && items != null && count in 0..MOST_COUNT) read(items, (minOf(count, MOST_ITEMS.toLong()) * 8).toInt()) else null

    return JsonObject(
        buildMap {
            put("at", JsonPrimitive(address))
            put("class", JsonPrimitive(name))
            count?.let { put("count", JsonPrimitive(it)) }
            words?.let { buffer ->
                put("items", JsonArray((0 until buffer.limit() / 8).map { index -> buffer.getLong(index * 8).let { word -> JsonArray(listOf(JsonPrimitive(word), classOf(word)?.let(::JsonPrimitive) ?: JsonNull)) } }))
            }
        },
    )
}

/**
 * The battle's classes, by the names the client gives them: entities and their components, buffs, the damage statistics the Stats
 * screen shows (each hero's damage dealt and taken and healing, each skill's damage, kills), the player's operations (Ultimate
 * presses), the dead's records, where each unit stands (its cell, for ranges such as an Inspiration's), the fight itself (its
 * logic clock), each entity, the skill each unit is using and the table row naming each unit.
 */
val BATTLE_CLASSES = setOf(
    "EntityWorld", "SubjectEntity", "AttributeComponent", "BattleAttributeData", "SpecialAttributeComponent", "BuffComponent",
    "BattleBuff", "BuffRuntime", "AngerComponent", "SkillComponent", "SkillCDData", "HPChangeComponent", "CounterComponent",
    "StatisticsComponent", "DamageStatistics", "DamageStatisticsData", "DamageStepStatisticsData", "SkillDamageStatisticsData",
    "DpsData", "BattleDpsView", "BattleManager", "BattleFrame", "BattleOperData", "PlayerOperData", "DeadSystemData",
    "DeadAttackHpData", "PositionComponent", "TowerSignComponent", "BattleAttributeAddition", "BattleAttributeSole",
    "BattleAttributeListAddtion", "TDGameSimulation", "Entity", "CData_Unit_Element", "DeadComponent", "OwnerRelationComponent",
    "CardComponent", "UseSkill",
)

/** The objects the statistics hang from, whose references are followed to the records they keep. */
private val REACH_ROOTS = setOf("StatisticsComponent", "DamageStatistics")

/** The table row naming a unit and the statistics naming a hero, whose ids tell which heroes the fight held. */
private const val UNIT_ROW = "CData_Unit_Element"
private const val STATISTICS_ROW = "DamageStatisticsData"
private const val HERO_ID_SCALE = 100_000L

/** Classes captured only as reached from the fight: the heap holds many look-alikes and other fights' copies of them. */
private val REACHED_ONLY = setOf("TDGameSimulation", "Entity", "UseSkill")

/** What the walk from the fight does not go through: text, the game's tables, caches and data managers, and the pools of reused objects. */
private val NOT_WALKED = setOf(
    "String", "GameDataManager", "StateScriptCache", "BattleBuffCache", "AIRuntimeCache", "BattleUtilityCache", "AgentsManager",
    "NotifyMgr", "Stack`1", "Queue`1",
)

/* Il2CppTypeEnum's codes. */
private const val BOOLEAN = 0x02
private const val CHAR = 0x03
private const val I1 = 0x04
private const val U1 = 0x05
private const val I2 = 0x06
private const val U2 = 0x07
private const val I4 = 0x08
private const val U4 = 0x09
private const val I8 = 0x0A
private const val U8 = 0x0B
private const val R4 = 0x0C
private const val R8 = 0x0D
private const val STRING = 0x0E
private const val POINTER = 0x0F
private const val VALUE_TYPE = 0x11
private const val NATIVE_INT = 0x18
private const val NATIVE_UINT = 0x19
private val REFERENCES = setOf(0x12, 0x14, 0x15, 0x1C, 0x1D)

/* Il2CppArray: class, monitor, bounds, then its length and its items; List`1: class, monitor, its items' array, then its size. */
private const val ARRAY_LENGTH = 0x18L
private const val ARRAY_ITEMS = 0x20L
private const val LIST_ITEMS = 0x10L
private const val LIST_SIZE = 0x18L

private const val SECONDS = 360
private const val CLASS_HEAD = 0x140
private const val EVERY_MS = 500L
private const val FIND_EVERY = 10
private const val MOST_OBJECTS = 3_000
private const val MOST_STRUCT = 1_024
private const val MOST_ITEMS = 64
private const val MOST_COUNT = 1_000_000L
private const val REACH_DEPTH = 16
private const val MOST_REACHED = 300_000
private const val TABLE_ROW = "CData_"
private const val WORDS_PER_ITEM = 4
private const val MOST_WORDS = 8_192
private const val LOWEST_POINTER = 0x10000L

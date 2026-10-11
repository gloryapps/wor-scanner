package com.gloryapps.worscanner.scanner.account

import com.gloryapps.worscanner.scanner.lua.LuaHeap
import com.gloryapps.worscanner.scanner.lua.LuaValue
import com.gloryapps.worscanner.scanner.senses.Memory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.TimeSource

/** The account as the running game holds it, each entry in the game's own fields: the file the lab imports, told from a scan by its `kind`. */
@Serializable
data class Account(
    @EncodeDefault val kind: String = "account",
    val startedAt: String,
    val heroes: List<JsonElement>,
    val gear: List<JsonElement>,
    val artifacts: List<JsonElement>,
    /** Every hero the account has held, by the client's base id: what the game's Collection Progress counts. */
    val collected: List<Long>,
    /** The rules of the game's plan, both sides'. */
    val rules: List<JsonElement>,
    /** How many entries every table holding each list had: the largest is the one read, the others copies the game let go. */
    val found: Map<Holding, List<Int>>,
    val seconds: Double,
)

/** Where the game keeps a list: under `list`, in the table holding `anchor`, a field only that table has, or `grouped` in tables `list` holds; `shared` are fields holding the game's row for every entry of a kind, and `keyed` fields mapping the client's ids to such rows, kept as the ids. */
@Serializable
enum class Holding(val anchor: String, val list: String, val shared: Set<String>, val keyed: Set<String> = emptySet(), val grouped: Boolean = false) {
    @SerialName("heroes") HEROES(anchor = "m_CharactorDatas", list = "m_CharactorDatas", shared = setOf("config")),
    @SerialName("gear") GEAR(anchor = "suitId2EquipIdExt", list = "equips", shared = setOf("vConfig", "vItemConfig", "vSuitConfig")),
    @SerialName("artifacts") ARTIFACTS(anchor = "m_vArtifacts", list = "m_vArtifacts", shared = setOf("m_AfCfg", "m_ItemCfg")),
    /* The plan as the game holds it once read, a list of rules by side, since what the server sent it is let go. */
    @SerialName("rules") RULES(
        anchor = "m_EquipRecommendRulesMap",
        list = "m_EquipRecommendRulesMap",
        shared = setOf("SuitList", "MainAttrList", "SubAttrList", "MustAttrList"),
        keyed = setOf("SuitMap", "MainAttrMap", "SubAttrMap", "MustAttrMap"),
        grouped = true,
    ),
}

/** Reads every list the game holds, from whichever of its holders holds the most; `startedAt` is the stamp the read is kept under. */
suspend fun readAccount(memory: Memory, startedAt: String): Account = withContext(Dispatchers.Default) {
    val started = TimeSource.Monotonic.markNow()
    val heap = LuaHeap(memory)
    val holders = heap.holders(Holding.entries.map { it.anchor }.toSet())
    /* Each list's holders with what each holds, the one holding most first. */
    val lists = Holding.entries.associateWith { holding ->
        holders[holding.anchor].orEmpty().map { it to heap.entries(it, holding) }.sortedByDescending { (_, list) -> list.size }
    }
    val read = lists.mapValues { (holding, candidates) -> candidates.firstOrNull()?.second.orEmpty().mapNotNull { heap.entry(it, holding) } }

    Account(
        startedAt = startedAt,
        heroes = read.getValue(Holding.HEROES),
        gear = read.getValue(Holding.GEAR),
        artifacts = read.getValue(Holding.ARTIFACTS),
        collected = lists.getValue(Holding.HEROES).firstOrNull()?.let { (holder, _) -> heap.values<LuaValue.Integer>(heap.field(holder, COLLECTED)).map { it.value } }.orEmpty(),
        rules = read.getValue(Holding.RULES),
        found = lists.mapValues { (_, candidates) -> candidates.map { (_, list) -> list.size } },
        seconds = started.elapsedNow().inWholeMilliseconds / 1000.0,
    )
}

/** Where the heroes' holder keeps every hero the account has held, by base id. */
private const val COLLECTED = "m_OwenHeroBaseId"

/** The values of type `V` a table holds; none for what is no table. */
private inline fun <reified V : LuaValue> LuaHeap.values(table: LuaValue?): List<V> =
    (table as? LuaValue.Table)?.let { fields(it.address) }?.values?.filterIsInstance<V>().orEmpty()

/** The tables the field `list` of the table at `holder` holds, or the tables each of those holds where the holding is `grouped`. */
private fun LuaHeap.entries(holder: Long, holding: Holding): List<LuaValue.Table> {
    val held = values<LuaValue.Table>(field(holder, holding.list))

    return if (holding.grouped) held.flatMap { values<LuaValue.Table>(it) } else held
}

/** An entry as JSON without its `shared` fields, read to an attribute in a piece's attribute list, and with its `keyed` fields as their ids. */
private fun LuaHeap.entry(entry: LuaValue.Table, holding: Holding): JsonElement? {
    val (keyed, own) = fields(entry.address)?.entries?.filter { it.key.name !in holding.shared }?.partition { it.key.name in holding.keyed } ?: return null
    val read = json(own.associate { it.toPair() }, depth = 2)

    return if (keyed.isEmpty()) read else JsonObject((read as? JsonObject).orEmpty() + keyed.associate { (key, field) -> key.name.orEmpty() to ids(field) })
}

/** The integer keys of a table, smallest first. */
private fun LuaHeap.ids(table: LuaValue): JsonArray =
    JsonArray((table as? LuaValue.Table)?.let { fields(it.address) }?.keys?.mapNotNull { (it as? LuaValue.Integer)?.value }.orEmpty().sorted().map(::JsonPrimitive))

private val LuaValue.name: String? get() = (this as? LuaValue.Text)?.value

/** A value as JSON, its tables read `depth` deep; null for what is not data. */
private fun LuaHeap.json(value: LuaValue, depth: Int): JsonElement? = when (value) {
    is LuaValue.Bool -> JsonPrimitive(value.value)
    is LuaValue.Integer -> JsonPrimitive(value.value)
    is LuaValue.Real -> value.value.takeIf { it.isFinite() }?.let(::JsonPrimitive)
    is LuaValue.Text -> JsonPrimitive(value.value)
    is LuaValue.Other -> null
    is LuaValue.Table -> if (depth == 0) null else fields(value.address)?.let { json(it, depth - 1) }
}

/** A table keyed 1 to n is an array, in that order; any other an object. */
private fun LuaHeap.json(fields: Map<LuaValue, LuaValue>, depth: Int): JsonElement {
    val values = fields.mapNotNull { (key, field) -> json(field, depth)?.let { key to it } }
    val indices = values.map { (key, _) -> (key as? LuaValue.Integer)?.value }

    return if (indices.toSet() == (1L..values.size).toSet()) {
        JsonArray(values.sortedBy { (key, _) -> (key as LuaValue.Integer).value }.map { it.second })
    } else {
        JsonObject(values.associate { (key, field) -> ((key as? LuaValue.Text)?.value ?: (key as LuaValue.Integer).value.toString()) to field })
    }
}

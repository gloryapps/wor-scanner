package com.gloryapps.worscanner.scanner.account

import com.gloryapps.worscanner.scanner.client.Client
import com.gloryapps.worscanner.scanner.client.unitName
import com.gloryapps.worscanner.scanner.lua.LuaHeap
import com.gloryapps.worscanner.scanner.lua.LuaValue
import com.gloryapps.worscanner.scanner.senses.Memory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/** The squads a squad screen holds for the stage it opens on, each in its slots' order. */
@Serializable
data class Squads(val stageId: Long, val squads: List<List<SquadHero>>)

/** A hero in a squad: `heroId` is the account's own (a read's `iHeroId`), `baseId` and `name` the client's, null where the account or the client lacks them. */
@Serializable
data class SquadHero(val heroId: Long, val baseId: Long?, val name: String?, val lord: Boolean)

/** The squads of every squad screen the game holds, which is only while one is open: the Guild Boss's are nowhere else. */
suspend fun readSquads(memory: Memory, client: Client?): List<Squads> = withContext(Dispatchers.Default) {
    val heap = LuaHeap(memory)
    val holders = heap.holders(setOf(TEAMS, Holding.HEROES.anchor))
    val heroes = holders[Holding.HEROES.anchor].orEmpty().map { heap.entries(it, Holding.HEROES) }.maxByOrNull { it.size }.orEmpty()
    val bases = heroes.mapNotNull { hero -> heap.fields(hero.address)?.let { (it.integer(HERO_ID) ?: return@let null) to it.integer(BASE_ID) } }.toMap()

    holders[TEAMS].orEmpty().mapNotNull { screen ->
        val fields = heap.fields(screen) ?: return@mapNotNull null
        val stage = fields.integer(STAGE) ?: return@mapNotNull null
        val squads = heap.inOrder(fields[LuaValue.Text(TEAMS)]).map { squad ->
            heap.inOrder(squad).mapNotNull { slot ->
                val held = heap.fields(slot.address) ?: return@mapNotNull null
                val id = held.integer(SLOT_HERO)?.takeIf { it != EMPTY } ?: return@mapNotNull null
                val base = bases[id]
                SquadHero(id, base, base?.let { client?.unitName(it) }, lord = held[LuaValue.Text(LORD)].let { it != null && it != LuaValue.Bool(false) })
            }
        }
        Squads(stage, squads)
    }.distinct()
}

/** The tables a table holds, by their integer keys in order: a table keyed 1 to n may keep some in its nodes. */
private fun LuaHeap.inOrder(table: LuaValue?): List<LuaValue.Table> =
    (table as? LuaValue.Table)?.let { fields(it.address) }?.entries
        ?.mapNotNull { (key, value) -> ((key as? LuaValue.Integer)?.value ?: return@mapNotNull null) to (value as? LuaValue.Table ?: return@mapNotNull null) }
        ?.sortedBy { it.first }?.map { it.second }.orEmpty()

private fun Map<LuaValue, LuaValue>.integer(name: String): Long? = (get(LuaValue.Text(name)) as? LuaValue.Integer)?.value

/* The squad screen's own fields, and its slots'; a Lord's flag is a number the game gives him, anyone else's false. */
private const val TEAMS = "m_TeamDataList"
private const val STAGE = "m_BattleStageId"
private const val SLOT_HERO = "m_iHeroID"
private const val LORD = "m_IsLord"
private const val EMPTY = -1L

/* A hero's own fields, as the account's heroes hold them. */
private const val HERO_ID = "iHeroId"
private const val BASE_ID = "iBaseId"

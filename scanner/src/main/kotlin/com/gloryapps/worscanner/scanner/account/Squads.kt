package com.gloryapps.worscanner.scanner.account

import com.gloryapps.worscanner.scanner.game.ReadAttribute
import com.gloryapps.worscanner.scanner.kinds.artifact.ScannedArtifact
import com.gloryapps.worscanner.scanner.kinds.gear.Slot
import kotlinx.serialization.Serializable

/** The squads a squad screen holds for the stage it opens on, each in its slots' order, and how the player plays them. */
@Serializable
data class Squads(val stageId: Long, val squads: List<List<SquadHero>>, val play: Play = Play())

/** What the player says of how he plays a comp, which no screen holds: his heroes by name, as the squads name them. */
@Serializable
data class Play(
    val deployOrder: List<String> = emptyList(),
    val healers: List<String> = emptyList(),
    val inspiration: List<String> = emptyList(),
    /** A hero's stance, and when he casts his Ultimate, in the player's words; a hero left out plays as the game would. */
    val stances: Map<String, String> = emptyMap(),
    val ultimates: Map<String, String> = emptyMap(),
)

/** A hero in a squad: `heroId` is the account's own (a read's `iHeroId`), `baseId` and `name` the client's, null where the account or the client lacks them; `pieces` and `artifact` are what he wears. */
@Serializable
data class SquadHero(
    val heroId: Long,
    val baseId: Long?,
    val name: String?,
    val lord: Boolean,
    val pieces: List<SquadPiece>,
    val artifact: ScannedArtifact?,
)

/** How the player and his play name a hero: by the client's name, by his id where the client has none. */
val SquadHero.called: String get() = name ?: "#$heroId"

/** A piece a squad's hero wears, its main attribute `primary` and the others `secondary`, as a gear scan's card reads them. */
@Serializable
data class SquadPiece(
    val set: String?,
    val slot: Slot?,
    val ancient: Boolean,
    val variant: String?,
    val variantEffect: String?,
    val variantAttributes: List<ReadAttribute>,
    val exclusive: String?,
    val primary: ReadAttribute?,
    val secondary: List<ReadAttribute>,
)

/* A squad screen's own fields: its stage, and its squads of slots. */
internal const val SQUAD_STAGE = "m_BattleStageId"
internal const val SQUAD_TEAMS = "m_TeamDataList"

/* A slot's: its hero, -1 where it is empty, and a Lord's flag, a number the game gives him and anyone else's false. */
internal const val SLOT_HERO = "m_iHeroID"
internal const val SLOT_LORD = "m_IsLord"
internal const val EMPTY_SLOT = -1L

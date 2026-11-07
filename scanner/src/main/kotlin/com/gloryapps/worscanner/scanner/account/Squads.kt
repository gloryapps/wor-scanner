package com.gloryapps.worscanner.scanner.account

import com.gloryapps.worscanner.scanner.kinds.artifact.ScannedArtifact
import com.gloryapps.worscanner.scanner.kinds.gear.ScannedGear
import kotlinx.serialization.Serializable

/** The squads a squad screen holds for the stage it opens on, each in its slots' order. */
@Serializable
data class Squads(val stageId: Long, val squads: List<List<SquadHero>>)

/** A hero in a squad: `heroId` is the account's own (a read's `iHeroId`), `baseId` and `name` the client's, null where the account or the client lacks them; `pieces` and `artifact` are what he wears. */
@Serializable
data class SquadHero(
    val heroId: Long,
    val baseId: Long?,
    val name: String?,
    val lord: Boolean,
    val pieces: List<ScannedGear>,
    val artifact: ScannedArtifact?,
)

/* A squad screen's own fields: its stage, and its squads of slots. */
internal const val SQUAD_STAGE = "m_BattleStageId"
internal const val SQUAD_TEAMS = "m_TeamDataList"

/* A slot's: its hero, -1 where it is empty, and a Lord's flag, a number the game gives him and anyone else's false. */
internal const val SLOT_HERO = "m_iHeroID"
internal const val SLOT_LORD = "m_IsLord"
internal const val EMPTY_SLOT = -1L

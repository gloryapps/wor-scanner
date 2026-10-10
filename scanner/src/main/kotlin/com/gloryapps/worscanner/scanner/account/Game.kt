package com.gloryapps.worscanner.scanner.account

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What turns the client's ids in an account into a card's words, for one version of the game: written by wor-extract's `export_scanner.py`. */
@Serializable
data class Game(
    val version: String,
    val attributes: Map<Int, GameAttribute>,
    val gear: Map<Long, GamePiece>,
    /** An exclusive effect's hero or faction, by name. */
    val exclusives: Map<Long, String>,
    /** A variant's slug, by its effect's group and its index in the group. */
    val variants: Map<Long, Map<Long, String>>,
    val heroes: Map<Long, GameHero>,
    val artifacts: Map<Long, GameArtifact>,
    /** HP and ATK by level, first level first, by an artifact's `levels`. */
    val artifactLevels: Map<Int, List<List<Int>>>,
) {
    companion object {
        /** The version the scanner ships, read once. */
        val shipped: Game by lazy {
            Json.decodeFromString(serializer(), Game::class.java.getResource("/game.json")!!.readText())
        }
    }
}

/** An attribute's word, and how the client keeps its value: in hundredths of what is printed, and printed as a percentage or flat. */
@Serializable
data class GameAttribute(val word: String, val hundredths: Boolean, val percentage: Boolean)

/** A piece's set, as the lab's gear sets name it, and its slot's word; a set the game names in no English has none. */
@Serializable
data class GamePiece(val set: String?, val slot: String)

/** A hero's name, rarity (4 epic, 5 legendary), and the skills after the ultimate in the lab's order. */
@Serializable
data class GameHero(val name: String, val rarity: Int, val row: List<GameSkill>)

/** Where a skill's level sits in the account's levels, none where it is never raised, and the promotion that unlocks it. */
@Serializable
data class GameSkill(val at: Int?, val from: Int)

@Serializable
data class GameArtifact(val name: String, val exclusive: String?, val levels: Int)

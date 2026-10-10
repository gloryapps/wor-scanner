package com.gloryapps.worscanner.scanner.kinds.gear

import com.gloryapps.worscanner.scanner.game.ReadAttribute
import kotlinx.serialization.Serializable

/** A card as the reader saw it; what it could not name is null, and the raw rows travel with it. */
@Serializable
data class ScannedGear(
    /** A `GEAR_SETS` id, or null where the block at the foot did not read. */
    val set: String?,
    val slot: Slot?,
    val ancient: Boolean,
    /** The variant's slug as wor-api's gear variants name it; null on a piece that is none, or whose panel does not name it. */
    val variant: String?,
    /** What the card printed above the word Exclusive, as read; who that is belongs to the lab. */
    val exclusive: String?,
    /** The primary first, in the order the card prints them. */
    val attributes: List<ReadAttribute>,
)

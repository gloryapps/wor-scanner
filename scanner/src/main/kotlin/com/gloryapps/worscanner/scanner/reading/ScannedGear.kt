package com.gloryapps.worscanner.scanner.reading

import com.gloryapps.worscanner.scanner.catalogue.Attribute
import com.gloryapps.worscanner.scanner.catalogue.Slot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ValueUnit {
    @SerialName("percentage") PERCENTAGE,
    @SerialName("flat") FLAT,
}

/** One attribute of a piece: its word, the number beside it, and the green extra the first one carries. */
@Serializable
data class ReadAttribute(val name: Attribute, val value: Double, val unit: ValueUnit, val bonus: Double?)

/** A card as the reader saw it; what it could not name is null, and the raw rows travel with it. */
@Serializable
data class ScannedGear(
    /** A `GEAR_SETS` id, or null where the block at the foot did not read. */
    val set: String?,
    val slot: Slot?,
    val ancient: Boolean,
    val variant: Boolean,
    /** What the card printed above the word Exclusive, as read; who that is belongs to the lab. */
    val exclusive: String?,
    /** The primary first, in the order the card prints them. */
    val attributes: List<ReadAttribute>,
)

package com.gloryapps.worscanner.scanner.kinds.artifact

import com.gloryapps.worscanner.scanner.game.ReadAttribute
import kotlinx.serialization.Serializable

/** An artifact as its panel showed it; what the panel did not name is null, and which artifact it is belongs to the lab. */
@Serializable
data class ScannedArtifact(
    /** As the panel prints it. */
    val name: String?,
    /** The enhancement, the number before its cap. */
    val level: Int?,
    /** The level beside `Artifact Skills`, which the effect's numbers follow. */
    val skill: Int?,
    /** The hero an exclusive artifact names, as read; null on any other. */
    val exclusive: String?,
    /** In the order the panel prints them. */
    val attributes: List<ReadAttribute>,
)

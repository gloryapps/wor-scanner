package com.gloryapps.worscanner.scanner.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What a scan of the account sends beyond its heroes: gear in the bands of `enhancements`, and the artifacts `artifacts` keeps; and how the lab takes each scan, `arrival`. */
@Serializable
data class ScanChoices(
    val enhancements: Set<Enhancement> = setOf(Enhancement.AT_16),
    val artifacts: ArtifactsToScan = ArtifactsToScan.WORTH_WEARING,
    /** A scan of the account reads the whole of it, so the lab's lists become the scan's unless the player chooses otherwise. */
    val arrival: Arrival = Arrival.REPLACE,
) {
    /** One band in or out; the last one chosen stays, since a gear scan of none sends the lab nothing to keep. */
    fun toggled(band: Enhancement): ScanChoices {
        val next = if (band in enhancements) enhancements - band else enhancements + band

        return if (next.isEmpty()) this else copy(enhancements = next)
    }

    companion object {
        val EVERYTHING = ScanChoices(Enhancement.entries.toSet(), ArtifactsToScan.EVERY)
    }
}

/** Which artifacts a scan sends. */
@Serializable
enum class ArtifactsToScan {
    /** At +25, or an exclusive from +10: one below that never beats a +25, exclusive or not. */
    WORTH_WEARING {
        override fun keeps(level: Int, exclusive: Boolean) = level >= TOP || (exclusive && level >= EXCLUSIVE_FROM)
    },
    EVERY {
        override fun keeps(level: Int, exclusive: Boolean) = true
    },
    ;

    abstract fun keeps(level: Int, exclusive: Boolean): Boolean

    private companion object {
        const val TOP = 25
        const val EXCLUSIVE_FROM = 10
    }
}

/** How the lab takes a scan: its list of the scan's kind becomes the scan's, or the scan is laid over it. */
@Serializable
enum class Arrival {
    @SerialName("replace") REPLACE,
    @SerialName("add") ADD,
}

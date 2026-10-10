package com.gloryapps.worscanner.scanner.account

import kotlinx.serialization.Serializable

/** What a scan of the account sends beyond its heroes: gear in the bands of `enhancements`, and the artifacts `artifacts` keeps. */
@Serializable
data class ScanChoices(
    val enhancements: Set<Enhancement> = setOf(Enhancement.AT_16),
    val artifacts: ArtifactsToScan = ArtifactsToScan.WORTH_WEARING,
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

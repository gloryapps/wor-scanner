package com.gloryapps.worscanner.scanner.account

/** The bands a piece's enhancement falls in, as the game's own gear filter offers them. */
enum class Enhancement(val levels: IntRange, val word: String) {
    FROM_0(0..3, "+0~+3"),
    FROM_4(4..7, "+4~+7"),
    FROM_8(8..11, "+8~+11"),
    FROM_12(12..15, "+12~+15"),
    AT_16(16..16, "+16"),
    ;

    companion object {
        fun of(level: Int): Enhancement? = entries.firstOrNull { level in it.levels }
    }
}

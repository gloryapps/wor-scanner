package com.gloryapps.worscanner.ui

import androidx.compose.ui.graphics.Color

/**
 * The one place a colour has a value. Dark glass with an ice accent: the app and the strip over the
 * game are the same surface, so looking away from one and back at the other reads as one thing.
 */
object Colors {
    /** The screen itself, what is laid on it, and what is sunk into that. */
    /* `screen` is repeated in the app's `res/values/colors.xml`: Android paints the launch window before Compose draws. */
    val screen = Color(0xFF0B0D12)
    val raised = Color(0xFF14171F)
    val sunken = Color(0xFF0E1117)

    /** The chosen half of a segmented control, lifted off its track. */
    val lifted = Color(0xFF242A36)

    val accent = Color(0xFF9EC5FF)
    val onAccent = Color(0xFF0B0D12)

    /** Where the accent is a ground or an edge rather than a mark. */
    val accentWash = Color(0x149EC5FF)
    val accentEdge = Color(0x479EC5FF)

    /** What is read, what is said beside it, and what is only there for whoever looks for it. */
    val text = Color(0xFFE9EDF5)
    val muted = Color(0x9EE9EDF5)
    val faint = Color(0x73E9EDF5)

    /** A grant not given, a piece read badly: something to look at, not a failure. */
    val warning = Color(0xFFE8B34A)
    val failure = Color(0xFFE08585)

    /** The border of what failed, as `accentEdge` is the accent's. */
    val failureEdge = Color(0x66E08585)

    val hairline = Color(0x17FFFFFF)
    val edge = Color(0x24FFFFFF)

    /** The strip over the game: the same dark, thinned so the game stays legible under it; solid where a menu is read. */
    val glass = Color(0xDB0C0E14)
    val glassSolid = Color(0xEB0C0E14)
    val glassThin = Color(0xB80C0E14)
    val glassEdge = Color(0x21FFFFFF)
    val glassEdgeStrong = Color(0x38FFFFFF)

    /** The accent over the game: the idle capsule's edge, the ground of the row under way, the hairline's glow. */
    val glassAccentEdge = Color(0x669EC5FF)
    val glassWash = Color(0x249EC5FF)
    val accentGlow = Color(0xB39EC5FF)

    /** A JSON as the reading screen prints it. */
    object Json {
        val key = Colors.accent
        val string = Color(0xFFB7C7A8)
        val number = Colors.warning
        val punctuation = Colors.faint
    }
}

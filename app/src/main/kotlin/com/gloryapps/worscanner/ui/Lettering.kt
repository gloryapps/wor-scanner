package com.gloryapps.worscanner.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Two voices, both the device's own: the sans every Android app is written in, and a mono for what the app read or wrote. */
object Fonts {
    val sans = FontFamily.Default
    val mono = FontFamily.Monospace
}

/** The one place a text style has a size. A role says what the text is for, never how big it is. */
object Lettering {
    /** What a card or a screen is called: the weight carries it, since there is one family now. */
    val title = TextStyle(fontFamily = Fonts.sans, fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em)
    val subtitle = TextStyle(fontFamily = Fonts.sans, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium)

    /** The heading of a first-run screen. */
    val display = TextStyle(fontFamily = Fonts.sans, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em)

    /** A step's name, a grant's, what the site answered: `action`'s weight, set in lines. */
    val stepName = TextStyle(fontFamily = Fonts.sans, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)

    /** The app's own name in the header. */
    val brand = TextStyle(fontFamily = Fonts.sans, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.01).em)

    /** The caps line above a group: Permissions, Readings. */
    val section = TextStyle(fontFamily = Fonts.sans, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.08.em)

    val body = TextStyle(fontFamily = Fonts.sans, fontSize = 13.sp, lineHeight = 18.sp)
    val caption = TextStyle(fontFamily = Fonts.sans, fontSize = 11.sp, lineHeight = 15.sp)
    val action = TextStyle(fontFamily = Fonts.sans, fontSize = 13.sp, fontWeight = FontWeight.Medium)

    /** A grant's button and a strip's, where the action is small beside what it acts on. */
    val actionSmall = TextStyle(fontFamily = Fonts.sans, fontSize = 12.sp, fontWeight = FontWeight.Medium)

    /** A stamp, a count, an id: anything the app read or wrote rather than said. */
    val data = TextStyle(fontFamily = Fonts.mono, fontSize = 12.sp, lineHeight = 16.sp)
    val dataSmall = TextStyle(fontFamily = Fonts.mono, fontSize = 10.sp, lineHeight = 14.sp)

    /** How many a scan read, the code that links the scanner, and the number of a step. */
    val count = TextStyle(fontFamily = Fonts.mono, fontSize = 44.sp, lineHeight = 44.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.03).em)
    val code = TextStyle(fontFamily = Fonts.mono, fontSize = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.12.em)
    val numeral = TextStyle(fontFamily = Fonts.mono, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)

    /** A row of the menu over the game, and the one tracked word on its capsule. */
    val label = TextStyle(fontFamily = Fonts.mono, fontSize = 11.sp, lineHeight = 15.sp)
    val mark = TextStyle(fontFamily = Fonts.mono, fontSize = 9.sp, letterSpacing = 0.09.em)
}

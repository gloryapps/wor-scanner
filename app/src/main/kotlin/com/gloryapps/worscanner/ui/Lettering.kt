package com.gloryapps.worscanner.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gloryapps.worscanner.R

/** Three voices: a serif that names a thing, a sans that talks about it, a mono that shows data. */
object Fonts {
    val serif = FontFamily(Font(R.font.newsreader_regular))
    val sans = FontFamily(
        Font(R.font.space_grotesk_regular),
        Font(R.font.space_grotesk_medium, FontWeight.Medium),
        Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    )
    val mono = FontFamily(Font(R.font.dm_mono_regular), Font(R.font.dm_mono_medium, FontWeight.Medium))
}

/** The one place a text style has a size. A role says what the text is for, never how big it is. */
object Lettering {
    /** What a card or a screen is called, in the serif: the only place it is used. */
    val title = TextStyle(fontFamily = Fonts.serif, fontSize = 22.sp, lineHeight = 26.sp)
    val subtitle = TextStyle(fontFamily = Fonts.serif, fontSize = 19.sp, lineHeight = 23.sp)

    /** The app's own name in the header. */
    val brand = TextStyle(fontFamily = Fonts.sans, fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.01).em)

    /** The caps line above a group: Permissions, Readings, Format. */
    val section = TextStyle(fontFamily = Fonts.sans, fontSize = 11.sp, letterSpacing = 0.1.em)

    val body = TextStyle(fontFamily = Fonts.sans, fontSize = 13.sp, lineHeight = 18.sp)
    val caption = TextStyle(fontFamily = Fonts.sans, fontSize = 12.sp, lineHeight = 16.sp)
    val action = TextStyle(fontFamily = Fonts.sans, fontSize = 14.sp, fontWeight = FontWeight.Medium)

    /** A stamp, a count, an id: anything the app read or wrote rather than said. */
    val data = TextStyle(fontFamily = Fonts.mono, fontSize = 13.sp, lineHeight = 18.sp)
    val dataSmall = TextStyle(fontFamily = Fonts.mono, fontSize = 11.sp, lineHeight = 15.sp)
}

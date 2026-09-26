package com.gloryapps.worscanner.scanner.scan

import com.gloryapps.worscanner.scanner.senses.Colour
import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.text.Line
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private class Recording(val width: Int, val height: Int, val lines: List<Line>)

/** A frame the overlay's Read kept inside LDPlayer, read back from the tests' resources as the walk sees it, without its pixels. */
fun recorded(resource: String): Seen {
    val recording = Json { ignoreUnknownKeys = true }.decodeFromString<Recording>(checkNotNull(Recording::class.java.getResource("/$resource")).readText())

    return Seen(Blind(recording.width, recording.height), recording.lines)
}

private class Blind(override val width: Int, override val height: Int) : Frame {
    override fun colourAt(x: Int, y: Int) = Colour(0)
}

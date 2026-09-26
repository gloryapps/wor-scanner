package com.gloryapps.worscanner.scanner.kinds.artifact

import com.gloryapps.worscanner.scanner.game.attributesIn
import com.gloryapps.worscanner.scanner.game.exclusiveIn
import com.gloryapps.worscanner.scanner.game.headOf
import com.gloryapps.worscanner.scanner.scan.Read
import com.gloryapps.worscanner.scanner.scan.Scan
import com.gloryapps.worscanner.scanner.scan.Seen
import com.gloryapps.worscanner.scanner.scan.Tapped
import com.gloryapps.worscanner.scanner.scan.gridBox
import com.gloryapps.worscanner.scanner.scan.tileBox
import com.gloryapps.worscanner.scanner.text.holdsName
import com.gloryapps.worscanner.scanner.text.wordIn

/**
 * How mythic artifacts are scanned: a tile's panel prints its rarity, its name, an exclusive's hero,
 * its level, its attributes and its skill's level, all read off the frame the tap left; the first
 * panel whose rarity reads below Mythic ends the scan.
 */
object ArtifactScan : Scan<ScannedArtifact>() {
    override val layout = ARTIFACT_STORAGE
    override val serializer = ScannedArtifact.serializer()

    /** `+ 25/25`: the enhancement against its cap. */
    private val LEVEL = Regex("""\+\s*(\d{1,2})\s*/\s*\d{1,2}""")

    /** The skill's `Lvl. 6`, which ends its row. */
    private val SKILL = Regex("""L\S{0,3}\s*(\d{1,2})\s*$""")

    override suspend fun readTile(tapped: Tapped): Read<ScannedArtifact> {
        val rows = tapped.seen.rowsIn(layout.panel)
        if (rows.firstNotNullOfOrNull(::rarityIn)?.scanned == false) return Read.Beyond
        val artifact = read(rows)

        return Read.Card(artifact, rows, listOf(tapped.seen.frame), closed(artifact))
    }

    override fun readScreen(seen: Seen): ScannedArtifact = read(seen.rowsIn(layout.panel))

    /**
     * A tile by the colour of its face, red, gold or purple where an empty slot is grey: a tile prints
     * no word to find it by but its `+25`, and nothing where the artifact was never enhanced. Only
     * the part of the face inside the grid is looked at.
     */
    override fun tileAt(seen: Seen, column: Int, centreY: Int): Boolean {
        val grid = layout.gridBox(seen.frame)
        val tile = layout.tileBox(seen.frame, column, centreY)
        val across = (0 until FACE_SAMPLES).map { tile.left + (tile.right - tile.left) * (2 * it + 1) / (2 * FACE_SAMPLES) }
        val down = (0 until FACE_SAMPLES).map { tile.top + (tile.bottom - tile.top) * (2 * it + 1) / (2 * FACE_SAMPLES) }
        val face = across.flatMap { x -> down.filter { it in grid.top..grid.bottom }.map { y -> seen.frame.colourAt(x, y).saturation } }

        return face.isNotEmpty() && face.average() > FACE
    }

    internal fun read(rows: List<String>): ScannedArtifact {
        val skills = rows.indexOfFirst(::isSkillsRow)
        val own = if (skills < 0) rows else rows.subList(0, skills)

        return ScannedArtifact(
            name = nameOf(rows),
            level = headOf(own).firstNotNullOfOrNull { LEVEL.find(it) }?.groupValues?.get(1)?.toInt(),
            skill = rows.getOrNull(skills)?.let { SKILL.find(it) }?.groupValues?.get(1)?.toInt(),
            exclusive = exclusiveIn(own),
            attributes = attributesIn(own, extras = 2),
        )
    }

    /** Whether the record names what identifies it; one that does not keeps its panel as an image. */
    internal fun closed(artifact: ScannedArtifact): Boolean =
        artifact.name != null && artifact.level != null && artifact.skill != null && artifact.attributes.isNotEmpty()

    /** `Mythic Artifact` and its like, the panel's first row. */
    private fun rarityIn(row: String): Rarity? = wordIn(row, Rarity.entries) { it.word }

    /** The row under the rarity. */
    private fun nameOf(rows: List<String>): String? {
        val at = rows.indexOfFirst { rarityIn(it) != null }

        return if (at < 0) null else rows.getOrNull(at + 1)?.trim()
    }

    /** `Artifact Skills`, which the recogniser also reads `Artifact Skils`; the effect's text below it names attributes too. */
    private fun isSkillsRow(row: String): Boolean = holdsName(row, "Artifact Skills")

    private const val FACE_SAMPLES = 8
    /** Above this mean saturation a face is a tile's: 0.31 to 0.54 on the tiles measured, 0.16 to 0.18 on the empty slots. */
    private const val FACE = 0.25

    /** The rarities a panel prints, and whether the scan reads them. */
    private enum class Rarity(val word: String, val scanned: Boolean) { MYTHIC("Mythic", true), LEGENDARY("Legendary", false), EPIC("Epic", false) }
}

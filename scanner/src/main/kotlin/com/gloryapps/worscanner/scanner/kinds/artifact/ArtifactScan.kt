package com.gloryapps.worscanner.scanner.kinds.artifact

import com.gloryapps.worscanner.scanner.game.Attribute
import com.gloryapps.worscanner.scanner.game.attributesIn
import com.gloryapps.worscanner.scanner.game.exclusiveIn
import com.gloryapps.worscanner.scanner.game.headOf
import com.gloryapps.worscanner.scanner.scan.Read
import com.gloryapps.worscanner.scanner.scan.Scan
import com.gloryapps.worscanner.scanner.scan.Seen
import com.gloryapps.worscanner.scanner.scan.Tapped
import com.gloryapps.worscanner.scanner.scan.gridBox
import com.gloryapps.worscanner.scanner.scan.spread
import com.gloryapps.worscanner.scanner.scan.tileBox
import com.gloryapps.worscanner.scanner.text.holdsName
import com.gloryapps.worscanner.scanner.text.readsAsCapitals
import com.gloryapps.worscanner.scanner.text.wordIn

/**
 * How artifacts are scanned: a tile's panel prints its rarity, its name, an exclusive's hero, its
 * level, its attributes and its skill's level, all read off the frame the tap left. An artifact
 * never enhanced is skipped, and the scan ends where the tiles do.
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
        val artifact = read(rows)
        if (artifact.level == NEVER_ENHANCED) return Read.Skipped

        return Read.Card(artifact, rows, listOf(tapped.seen.frame), closed(artifact))
    }

    override fun readScreen(seen: Seen): ScannedArtifact = read(seen.rowsIn(layout.panel))

    override fun titleOf(rows: List<String>): String? = nameOf(rows)

    /**
     * A tile by the colour of its face, red, gold or purple where an empty slot is grey: a tile prints
     * no word to find it by but its `+25`, and nothing where the artifact was never enhanced. Only
     * the part of the face inside the grid is looked at.
     */
    override fun tileAt(seen: Seen, column: Int, centreY: Int): Boolean {
        val grid = layout.gridBox(seen.frame)
        val tile = layout.tileBox(seen.frame, column, centreY)
        val down = spread(tile.top, tile.bottom, FACE_SAMPLES).filter { it in grid.top..grid.bottom }
        val face = spread(tile.left, tile.right, FACE_SAMPLES).flatMap { x -> down.map { y -> seen.frame.colourAt(x, y).saturation } }

        return face.isNotEmpty() && face.average() > FACE
    }

    /** No word finds the first row: the `+25` reads as `Wt25` or not at all. The scan begins on the selected tile or not at all. */
    override fun firstRowCentre(seen: Seen): Int? = null

    internal fun read(rows: List<String>): ScannedArtifact {
        val skills = rows.indexOfFirst(::isSkillsRow)
        val own = if (skills < 0) rows else rows.subList(0, skills)
        val name = nameOf(own)

        return ScannedArtifact(
            name = name,
            level = headOf(own).firstNotNullOfOrNull { LEVEL.find(it) }?.groupValues?.get(1)?.toInt(),
            skill = rows.getOrNull(skills)?.let { SKILL.find(it) }?.groupValues?.get(1)?.toInt(),
            /* Below the name only, so a hero's name that did not read leaves no neighbour in its place. */
            exclusive = if (name == null) null else exclusiveIn(own.drop(2)),
            attributes = attributesIn(own, extras = 2),
        )
    }

    /** Whether the record names what identifies it, HP and ATK among the attributes; one that does not keeps its panel as an image. */
    internal fun closed(artifact: ScannedArtifact): Boolean =
        artifact.name != null && artifact.level != null && artifact.skill != null &&
            artifact.attributes.map { it.name }.containsAll(listOf(Attribute.HP, Attribute.ATK))

    private fun rarityIn(row: String): Rarity? = wordIn(row, Rarity.entries) { it.word }

    /**
     * The row under the rarity, which the panel prints first. A row that is the hero's name in
     * capitals, `Exclusive`, `Class-Limited` or the level is the name that did not read.
     */
    private fun nameOf(rows: List<String>): String? {
        if (rows.firstOrNull()?.let(::rarityIn) == null) return null
        val under = rows.getOrNull(1)?.trim() ?: return null
        val stranger = readsAsCapitals(under) || holdsName(under, "Exclusive") || holdsName(under, "Class-Limited") || LEVEL.containsMatchIn(under)

        return under.takeUnless { stranger }
    }

    /** `Artifact Skills`, which the recogniser also reads `Artifact Skils`; the effect's text below it names attributes too. */
    private fun isSkillsRow(row: String): Boolean = holdsName(row, "Artifact Skills")

    /** The level an artifact comes at, `+ 1/10`, whose tile prints no `+`. */
    private const val NEVER_ENHANCED = 1
    private const val FACE_SAMPLES = 8
    /** Above this mean saturation a face is a tile's: 0.31 to 0.54 on the tiles measured, 0.16 to 0.18 on the empty slots. */
    private const val FACE = 0.25

    /** The rarities a panel prints, which the wiki's Artifact page lists: none is lower than Epic. */
    private enum class Rarity(val word: String) { MYTHIC("Mythic"), LEGENDARY("Legendary"), EPIC("Epic") }
}

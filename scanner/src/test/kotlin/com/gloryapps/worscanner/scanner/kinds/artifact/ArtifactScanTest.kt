package com.gloryapps.worscanner.scanner.kinds.artifact

import com.gloryapps.worscanner.scanner.game.Attribute
import com.gloryapps.worscanner.scanner.game.ReadAttribute
import com.gloryapps.worscanner.scanner.scan.Read
import com.gloryapps.worscanner.scanner.scan.Seen
import com.gloryapps.worscanner.scanner.scan.Spot
import com.gloryapps.worscanner.scanner.scan.Tapped
import com.gloryapps.worscanner.scanner.senses.Colour
import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.Line
import com.gloryapps.worscanner.scanner.text.ValueUnit
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The panels' rows as the artifact tab printed them inside LDPlayer on 2026-09-26. */
class ArtifactScanTest {
    private val spearOfLeonidas = listOf(
        "Mythic Artifact", "Spear of Leonidas", "KASSANIDR", "Exclusive", "+ 25/25t", "HP 4650+1730", "ATK 1497+575", "ATK Bonus +4.7%",
        "Artifact Skills Lvl. 6", "Increases single-target DMG by", "30%. Reduces the number of Basic",
    )
    private val hatesContagion = listOf(
        "Mythic Artifact", "Hate's Contagion", "Class-Limited", "+ 25/25", "HP 4650+1790", "ATK 1497+625", "ATK Bonus +3.1%",
        "Artifact Skills Lvl. 6", "Increases AoE DMG by 20%. When", "dealing DMG to 3 or more enemies", "simultaneously, permanently",
    )
    private val perdition = listOf(
        "Mythic Artifact", "Perdition", "[EZEBELI.", "Exclusive", "+22/22", "HP 4300+1990", "ATK 1407+845", "7 ATK Bonus +4.5%",
        "Artifact Skills Lvl. 5", "Increases DMG by 32%. After Seeds", "of Malice detonate, enemies",
    )
    private val tomeOfGreed = listOf(
        "Mythic Artifact", "Tome of Greed", "GREED", "Exclusive", "+1/10", "HP 2200", "ATK 777", "Artifact Skills Lvl. 1",
        "Increases ATK by 10%. When", "deployed, immediately gains 3", "stack(s) of Soul Reaping. Each kill",
    )
    private val tearOfTwilight = listOf(
        "Mythic Artifact", "Tear of Twilight", "Class-Limited *", "+ 13/13", "HP 3400", "ATK 1137", "Artifact Skils Lvl. 2",
        "Increases ATK by 22% when", "deployed, reduced to 11% while HP", "is below 80%.",
    )
    private val ironbloomOfMercy = listOf(
        "Mythic Artifact", "Ironbloom of Mercy", "Class-Limited", "+ 1/10", "HP 2200", "ATK 777", "Artifact Skills Lvl. 1",
        "Increases Healing Effect by 15.", "When blocking a target, increases", "the hero's Ldx HP by 15%, 1",
    )
    private val tumultuousHorn = listOf(
        "Legendary Artifact", "Tumultuous Horn", "Class-Limited Â", "+ 25/25", "HP 1950", "ATK 623", "Artifact Skills Lvl. 6",
        "Increases Initial Rage by 50% when", "deployed.",
    )
    private val keenWisdom = listOf(
        "Epic Artifact", "Keen Wisdom", "+ 25/25", "2 HP 830+780", "ATK 260+420", "Artifact Skills Lvl. 6",
        "When there is any ally within 2", "tile(s), increases ATK by 16%.",
    )

    @Test
    fun `an exclusive mythic reads whole`() {
        val artifact = ArtifactScan.read(spearOfLeonidas)

        assertEquals(
            ScannedArtifact(
                name = "Spear of Leonidas",
                level = 25,
                skill = 6,
                exclusive = "KASSANIDR",
                attributes = listOf(
                    ReadAttribute(Attribute.HP, 4650.0, ValueUnit.FLAT, 1730.0),
                    ReadAttribute(Attribute.ATK, 1497.0, ValueUnit.FLAT, 575.0),
                    ReadAttribute(Attribute.ATK_BONUS, 4.7, ValueUnit.PERCENTAGE, null),
                ),
            ),
            artifact,
        )
        assertTrue(ArtifactScan.closed(artifact))
    }

    @Test
    fun `a class-limited mythic names no hero`() {
        val artifact = ArtifactScan.read(hatesContagion)

        assertEquals("Hate's Contagion", artifact.name)
        assertNull(artifact.exclusive)
    }

    @Test
    fun `the hero's name is kept as read past the portrait's noise`() {
        assertEquals("EZEBELI", ArtifactScan.read(perdition).exclusive)
        assertEquals("GREED", ArtifactScan.read(tomeOfGreed).exclusive)
    }

    @Test
    fun `the level is the enhancement before its cap`() {
        assertEquals(listOf(25, 22, 1, 13), listOf(spearOfLeonidas, perdition, tomeOfGreed, tearOfTwilight).map { ArtifactScan.read(it).level })
    }

    @Test
    fun `the skill's level is read however the recogniser spells Skills`() {
        assertEquals(listOf(6, 5, 1, 2), listOf(spearOfLeonidas, perdition, tomeOfGreed, tearOfTwilight).map { ArtifactScan.read(it).skill })
    }

    @Test
    fun `the icon before a third attribute is noise`() {
        assertEquals(ReadAttribute(Attribute.ATK_BONUS, 4.5, ValueUnit.PERCENTAGE, null), ArtifactScan.read(perdition).attributes.last())
    }

    @Test
    fun `the effect's text names no attribute of the artifact`() {
        assertEquals(listOf(Attribute.HP, Attribute.ATK), ArtifactScan.read(tomeOfGreed).attributes.map { it.name })
        assertEquals(listOf(Attribute.HP, Attribute.ATK), ArtifactScan.read(ironbloomOfMercy).attributes.map { it.name })
    }

    @Test
    fun `an artifact without enhancement prints no green extra`() {
        assertEquals(listOf(null, null), ArtifactScan.read(tomeOfGreed).attributes.map { it.bonus })
    }

    @Test
    fun `a panel whose skill did not read keeps its image`() {
        assertFalse(ArtifactScan.closed(ArtifactScan.read(spearOfLeonidas.take(8))))
    }

    @Test
    fun `an artifact is written with every field`() {
        val written = Json.encodeToString(ScannedArtifact.serializer(), ArtifactScan.read(tomeOfGreed))

        assertEquals(
            """{"name":"Tome of Greed","level":1,"skill":1,"exclusive":"GREED","attributes":[""" +
                """{"name":"HP","value":2200.0,"unit":"flat","bonus":null},{"name":"ATK","value":777.0,"unit":"flat","bonus":null}]}""",
            written,
        )
    }

    @Test
    fun `a mythic's tile is read off the frame the tap left`() = runTest {
        val read = ArtifactScan.readTile(Lent(panelOf(hatesContagion)))

        assertIs<Read.Card<ScannedArtifact>>(read)
        assertEquals("Hate's Contagion", read.card.name)
        assertEquals(hatesContagion, read.rows)
    }

    @Test
    fun `the first legendary or epic ends the scan`() = runTest {
        assertEquals(Read.Beyond, ArtifactScan.readTile(Lent(panelOf(tumultuousHorn))))
        assertEquals(Read.Beyond, ArtifactScan.readTile(Lent(panelOf(keenWisdom))))
    }

    @Test
    fun `a rarity that does not read ends nothing`() = runTest {
        assertIs<Read.Card<ScannedArtifact>>(ArtifactScan.readTile(Lent(panelOf(listOf("Mythc Artifac") + hatesContagion.drop(1)))))
    }

    @Test
    fun `the overlay's reading of a whole frame keeps to the panel`() {
        val sheet = listOf(Line("Open the app", Box(951, 167, 1069, 185)), Line("Close overlay", Box(951, 226, 1079, 243)))

        assertEquals("Hate's Contagion", ArtifactScan.readScreen(Seen(Blank, sheet + panelOf(hatesContagion).lines)).name)
    }

    @Test
    fun `a tile is told from an empty slot by the colour of its face`() {
        val seen = Seen(Painted(listOf(Box(152, 141, 236, 265) to MYTHIC_FACE, Box(261, 141, 345, 265) to EMPTY_SLOT)), emptyList())

        assertTrue(ArtifactScan.tileAt(seen, 0, 203))
        assertFalse(ArtifactScan.tileAt(seen, 1, 203))
    }

    @Test
    fun `only the part of a face inside the grid is looked at`() {
        val buttons = Seen(Painted(listOf(Box(140, 616, 910, 720) to MYTHIC_FACE)), emptyList())

        assertFalse(ArtifactScan.tileAt(buttons, 0, 660))
    }

    /** The rows laid down the panel, one under the other, as the recogniser boxes them. */
    private fun panelOf(rows: List<String>): Seen = Seen(Blank, rows.mapIndexed { at, row -> Line(row, Box(950, 300 + 25 * at, 1230, 318 + 25 * at)) })

    /** Squares of colour on the grey of the storage's background. */
    private class Painted(private val squares: List<Pair<Box, Colour>>) : Frame {
        override val width = 1280
        override val height = 720
        override fun colourAt(x: Int, y: Int) = squares.lastOrNull { (box, _) -> x in box.left..box.right && y in box.top..box.bottom }?.second ?: EMPTY_SLOT
    }

    private object Blank : Frame {
        override val width = 1280
        override val height = 720
        override fun colourAt(x: Int, y: Int) = Colour(0)
    }

    private companion object {
        /** The median pixel of Spear of Leonidas's face and of an empty slot's, off LDPlayer's screenshots. */
        val MYTHIC_FACE = Colour(0xFFA87F54.toInt())
        val EMPTY_SLOT = Colour(0xFF5D626F.toInt())
    }

    private class Lent(override val seen: Seen) : Tapped {
        override val tile = Box(152, 141, 236, 265)
        override suspend fun show(tab: Spot): Seen = error("an artifact's panel takes no tab")
        override suspend fun regrip(): Seen = error("an artifact's panel moves no grid")
    }
}

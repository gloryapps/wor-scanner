package com.gloryapps.worscanner.scanner.kinds.artifact

import com.gloryapps.worscanner.scanner.game.Attribute
import com.gloryapps.worscanner.scanner.game.ReadAttribute
import com.gloryapps.worscanner.scanner.scan.countIn
import com.gloryapps.worscanner.scanner.scan.recorded
import com.gloryapps.worscanner.scanner.text.ValueUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/** Storage's Artifact tab as ML Kit read it inside LDPlayer at 1280x720, Spear of Leonidas selected at the top of the grid. */
class LdPlayerArtifactReadingTest {
    private val seen = recorded("ldplayer-artifacts-1280x720.json")
    private val layout = ARTIFACT_STORAGE

    @Test
    fun `the header's count is read from its region`() {
        assertEquals(423, countIn(seen.rowsIn(layout.count)))
    }

    @Test
    fun `the panel region reads the selected artifact, the overlay's sheet above it left out`() {
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
            ArtifactScan.readScreen(seen),
        )
    }
}

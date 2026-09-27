package com.gloryapps.worscanner.scanner.kinds.hero

import com.gloryapps.worscanner.scanner.scan.Read
import com.gloryapps.worscanner.scanner.scan.Scan
import com.gloryapps.worscanner.scanner.scan.Seen
import com.gloryapps.worscanner.scanner.scan.Spot
import com.gloryapps.worscanner.scanner.scan.Tapped
import com.gloryapps.worscanner.scanner.scan.spread
import com.gloryapps.worscanner.scanner.senses.Colour
import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.flatten
import com.gloryapps.worscanner.scanner.text.holdsName
import com.gloryapps.worscanner.scanner.text.readsAs
import com.gloryapps.worscanner.scanner.text.readsAsCapitals
import com.gloryapps.worscanner.scanner.text.timesHeld
import com.gloryapps.worscanner.scanner.text.wordIn

/**
 * How legendary heroes are scanned: a tile's panel is read under Attributes, Skills and Awaken,
 * then left on Attributes, where the grid is found again; the first tile framed in an epic's
 * purple ends the scan. The panel's words are kept as printed and its skills by place: which hero
 * and which skill they are belongs to the lab.
 */
object HeroScan : Scan<ScannedHero>() {
    override val layout = HERO_ROSTER
    override val serializer = ScannedHero.serializer()

    /** `Lvl. 49/50` on Attributes: two characters of cap, which no skill's `Lvl. 2/5` has. The cap may read `5O`. */
    private val LEVEL = Regex("""L\S{0,3}\s*(\d{1,2})\s*/\s*[\dOo]{2}""")

    /** A skill's `Lvl. 2/5` as the recogniser keeps it through `LV 2/5` and `Lyl, 1/S`: a number, a slash, a top. */
    private val SKILL_LEVEL = Regex("""(\d{1,2})\s*/\s*[0-9SsOoIl]""")

    /** The power under the name, `73,634 X`: digits grouped by commas. */
    private val POWER = Regex("""\d{1,3}(,\d{3})+""")

    override suspend fun readTile(tapped: Tapped): Read<ScannedHero> {
        if (isEpic(tapped.seen.frame, tapped.tile)) return Read.Beyond
        val attributes = tapped.seen.takeIf { it.showsAttributes() } ?: tapped.show(ATTRIBUTES)
        val skills = tapped.show(SKILLS)
        val awaken = tapped.show(AWAKEN)
        tapped.show(ATTRIBUTES)
        tapped.regrip() ?: return Read.Lost("back on Attributes, the hero's tile is not framed")
        val hero = read(attributes, skills, awaken.takeIf { it.showsAwaken() })
        val shown = listOf(attributes, skills, awaken)

        return Read.Card(hero, shown.flatMap { it.rowsIn(layout.panel) }, shown.map { it.frame }, closed(hero, skills))
    }

    override fun readScreen(seen: Seen): ScannedHero {
        val rows = seen.rowsIn(layout.panel)

        return read(
            attributes = seen.takeIf { it.showsAttributes() },
            skills = seen.takeIf { rows.any(::isSkillsHeader) },
            awaken = seen.takeIf { it.showsAwaken() },
        )
    }

    /** The name, found among every tab's rows by the power or the level above which it is printed. */
    override fun titleOf(rows: List<String>): String? = nameOf(rows)

    /** The hero the tabs' frames show; a tab not given leaves its part null or empty. */
    internal fun read(attributes: Seen?, skills: Seen?, awaken: Seen?): ScannedHero {
        val rows = attributes?.rowsIn(layout.panel).orEmpty()
        val stars = attributes?.let { starsOf(it.frame) }

        return ScannedHero(
            name = nameOf(rows),
            level = levelOf(rows),
            stars = stars?.first,
            promotion = stars?.second,
            awakening = awaken?.let { awakeningOf(it.frame) },
            skills = skills?.let { skillsOf(it.rowsIn(layout.panel)).skills } ?: HeroSkills(),
        )
    }

    /** Whether the tile's frame is an epic's purple rather than a legendary's gold, by the lower half of its left edge: the upper half carries the tile's gold icons. */
    internal fun isEpic(frame: Frame, tile: Box): Boolean {
        val middle = (tile.top + tile.bottom) / 2
        val width = tile.right - tile.left
        val band = (tile.left + (width * BORDER_FROM).toInt() until tile.left + (width * BORDER_TO).toInt())
        val hues = spread(middle, middle + (tile.bottom - tile.top) / 2, EDGE_SAMPLES).map { y ->
            /* The strongest colour across the band is the border's. */
            band.map { x -> frame.colourAt(x, y) }.maxBy { it.saturation * maxOf(it.red, it.green, it.blue) }.hue
        }

        return hues.count { it in PURPLE } > hues.count { it in GOLD }
    }

    /** Whether the record names what identifies it, every skill label's level included; one that does not keeps its panels as an image. */
    private fun closed(hero: ScannedHero, skills: Seen): Boolean =
        hero.name != null && hero.level != null && (hero.stars ?: 0) > 0 && hero.awakening != null &&
            hero.skills.ultimate != null && hero.skills.row.isNotEmpty() && skillsOf(skills.rowsIn(layout.panel)).whole

    private fun Seen.showsAttributes(): Boolean = levelOf(rowsIn(layout.panel)) != null

    /** A tab that did not change leaves the one before it on screen: only Awaken prints `Awakened`. */
    private fun Seen.showsAwaken(): Boolean = rowsIn(layout.panel).any { holdsName(it, "Awakened") }

    private fun isSkillsHeader(row: String): Boolean = readsAs(flatten(row), "skills")

    /**
     * The name is the row mostly in capitals nearest above the power, or above the level where the
     * power did not read, written in capitals: it is printed in small capitals, which the
     * recogniser lowers a letter of here and there (`RoSALIA`, `Ezio AUDITORE`), and the icons
     * above the title read as capitals too (`AY`). The title is mostly lower case; a tag like
     * `AoE M. ATK` comes after the power.
     */
    private fun nameOf(rows: List<String>): String? {
        val anchor = rows.indexOfFirst { POWER.containsMatchIn(it) }.takeIf { it > 0 } ?: rows.indexOfFirst { LEVEL.containsMatchIn(it) }.takeIf { it > 0 } ?: return null

        return rows.subList(0, anchor).asReversed().firstOrNull(::readsAsName)?.trim()?.uppercase()
    }

    private fun readsAsName(row: String): Boolean = readsAsCapitals(row) && row.none(Char::isDigit) && !row.contains("ATK")

    private fun levelOf(rows: List<String>): Int? = rows.firstNotNullOfOrNull { LEVEL.find(it) }?.groupValues?.get(1)?.toInt()

    /** The filled slots and the purple among them; null where a slot is none of purple, gold and grey. */
    private fun starsOf(frame: Frame): Pair<Int, Int>? {
        var stars = 0
        var promotion = 0
        for (slot in STAR_SLOTS) {
            val colour = frame.colourAt(slot)
            when {
                colour.saturation < GREY -> continue
                colour.hue in PURPLE -> promotion++
                colour.hue !in GOLD -> return null
            }
            stars++
        }

        return stars to promotion
    }

    private fun awakeningOf(frame: Frame): Int = AWAKENING_NODES.count { frame.colourAt(it).saturation > LIT }

    /**
     * A label's level is the one level on the row right under it; a label followed by another
     * label, or by a row of several levels, read none, and the record stays open. Every other level
     * is the icon row's, left to right.
     */
    private fun skillsOf(rows: List<String>): SkillsRead {
        val header = rows.indexOfFirst(::isSkillsHeader)
        if (header < 0) return SkillsRead(HeroSkills(), whole = false)
        var skills = HeroSkills()
        var whole = true
        var labelled: Place? = null
        for (row in rows.drop(header + 1)) {
            val place = wordIn(row, Place.entries) { it.word }
            val levels = if (place == null) levelsIn(row) else emptyList()
            val label = labelled
            labelled = place
            if (label != null) {
                val own = levels.singleOrNull()
                if (own != null) {
                    skills = skills.at(label, own)
                    continue
                }
                whole = false
            }
            skills = skills.copy(row = skills.row + levels)
        }
        if (labelled != null) whole = false

        return SkillsRead(skills, whole)
    }

    private fun HeroSkills.at(place: Place, level: SkillLevel): HeroSkills = when (place) {
        Place.LORD -> copy(lord = level)
        Place.ULTIMATE -> copy(ultimate = level)
        Place.BOND -> copy(bonds = bonds + level)
    }

    /** The skills by place, and whether every label the tab printed read its level. */
    private class SkillsRead(val skills: HeroSkills, val whole: Boolean)

    /** The levels a row prints, left to right: its numbers, and the stretches around them read for `Max Level` or a bond to unlock. */
    private fun levelsIn(row: String): List<SkillLevel> {
        /* The tab's `MAX LEVEL` banner is the one row in capitals without a level's slash: `8MAX LEVEL` is the banner, `LV 2/5` a level. */
        if (row.none(Char::isLowerCase) && !SKILL_LEVEL.containsMatchIn(row)) return emptyList()
        val levels = mutableListOf<SkillLevel>()
        var from = 0
        for (found in SKILL_LEVEL.findAll(row)) {
            levels += topsIn(row.substring(from, found.range.first))
            levels += SkillLevel.Of(found.groupValues[1].toInt())
            from = found.range.last + 1
        }

        return levels + topsIn(row.substring(from))
    }

    /* A stretch between numbers: a bond still to unlock, or as many `Max Level` as it holds through the recogniser's slips. */
    private fun topsIn(stretch: String): List<SkillLevel> =
        if (holdsName(stretch, "unlock")) listOf(SkillLevel.Of(0)) else List(timesHeld(stretch, "Max Level")) { SkillLevel.Max }

    private fun Frame.colourAt(spot: Spot): Colour = spot.on(this).let { (x, y) -> colourAt(x, y) }

    /** The word that tells apart the labels the Skills tab prints over the skills it names: `Lord Skill` and `Bond Skill` are too close whole. */
    private enum class Place(val word: String) { LORD("Lord"), ULTIMATE("Ultimate"), BOND("Bond") }

    private const val EDGE_SAMPLES = 8
    /** Where the border runs in from the tile's left edge, past the selection frame drawn on it: 2 to 12 px of a tile 83 wide. */
    private const val BORDER_FROM = 0.025
    private const val BORDER_TO = 0.145
    /** Below this saturation a star slot is the grey of an empty one. */
    private const val GREY = 0.15
    /** Above this saturation a node is lit; a grey one sits near none. */
    private const val LIT = 0.3
    private val PURPLE = 240.0..300.0
    private val GOLD = 25.0..70.0
}

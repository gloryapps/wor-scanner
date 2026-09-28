package com.gloryapps.worscanner.scanner.kinds.hero

import com.gloryapps.worscanner.scanner.scan.GridLayout
import com.gloryapps.worscanner.scanner.scan.Read
import com.gloryapps.worscanner.scanner.scan.Scan
import com.gloryapps.worscanner.scanner.scan.Seen
import com.gloryapps.worscanner.scanner.scan.Spot
import com.gloryapps.worscanner.scanner.scan.Tapped
import com.gloryapps.worscanner.scanner.scan.spread
import com.gloryapps.worscanner.scanner.scan.tileBox
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
 * How legendary and epic heroes are scanned: a tile's panel is read under Attributes, Skills and
 * Awaken, then left on Attributes, where the grid is found again. A tile is one the scan reads where
 * its edge is a legendary's gold or an epic's purple, so the grid ends at the first hero below epic;
 * that is told before the tile is tapped, since a square's selection frame is drawn over its edge.
 * The panel's words are kept as printed and its skills by place: which hero and which skill they
 * are belongs to the lab.
 */
sealed class HeroScan private constructor(override val layout: GridLayout, private val edge: RankEdge) : Scan<ScannedHero>() {
    /** The roster as cards, each printing its level below. */
    data object Cards : HeroScan(HERO_CARDS, CARDS_EDGE)

    /** The roster as squares, which print no word: the scan begins on the selected hero or not at all. */
    data object Squares : HeroScan(HERO_SQUARES, SQUARES_EDGE) {
        override fun firstRowCentre(seen: Seen): Int? = null
    }

    override val serializer = ScannedHero.serializer()

    override fun viewOn(seen: Seen): HeroScan = if (litness(seen.frame, SQUARES_BUTTON) > litness(seen.frame, CARDS_BUTTON)) Squares else Cards

    override fun tileAt(seen: Seen, column: Int, centreY: Int): Boolean = isLegendaryOrEpic(seen.frame, layout.tileBox(seen.frame, column, centreY))

    override suspend fun readTile(tapped: Tapped): Read<ScannedHero> {
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

    /**
     * Whether the tile's edge is a legendary's gold or an epic's purple, where the view reads it. A
     * rare's blue, an uncommon's green and an empty slot's slate are other hues; a common's grey leans
     * gold but is dull.
     */
    internal fun isLegendaryOrEpic(frame: Frame, tile: Box): Boolean {
        val middle = (tile.top + tile.bottom) / 2
        val width = tile.right - tile.left
        val height = tile.bottom - tile.top
        val band = (tile.left + (width * edge.across.start).toInt() until tile.left + (width * edge.across.endInclusive).toInt())
        val border = spread(middle + (height * edge.down.start).toInt(), middle + (height * edge.down.endInclusive).toInt(), EDGE_SAMPLES).map { y ->
            /* The strongest colour across the band is the border's. */
            band.map { x -> frame.colourAt(x, y) }.maxBy { it.saturation * maxOf(it.red, it.green, it.blue) }
        }

        return border.count { it.saturation > edge.saturated && (it.hue in GOLD || it.hue in PURPLE) } * 2 > border.size
    }

    /* How bright a button's face is, the lit one of two the brighter. */
    private fun litness(frame: Frame, button: Spot): Int {
        val (x, y) = button.on(frame)
        val reachX = (frame.width * BUTTON_REACH).toInt()
        val reachY = (frame.height * BUTTON_REACH).toInt()

        return spread(x - reachX, x + reachX, BUTTON_SAMPLES).sumOf { sx ->
            spread(y - reachY, y + reachY, BUTTON_SAMPLES).sumOf { sy -> frame.colourAt(sx, sy).let { maxOf(it.red, it.green, it.blue) } }
        }
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

    private companion object {
        /** `Lvl. 49/50` on Attributes: two characters of cap, which no skill's `Lvl. 2/5` has. The cap may read `5O`. */
        val LEVEL = Regex("""L\S{0,3}\s*(\d{1,2})\s*/\s*[\dOo]{2}""")

        /** A skill's `Lvl. 2/5` as the recogniser keeps it through `LV 2/5` and `Lyl, 1/S`: a number, a slash, a top. */
        val SKILL_LEVEL = Regex("""(\d{1,2})\s*/\s*[0-9SsOoIl]""")

        /** The power under the name, `73,634 X`: digits grouped by commas. */
        val POWER = Regex("""\d{1,3}(,\d{3})+""")

        const val EDGE_SAMPLES = 8
        /** How far around a button's centre its face is taken, in shares of the display. */
        const val BUTTON_REACH = 0.008
        const val BUTTON_SAMPLES = 5
        /** Below this saturation a star slot is the grey of an empty one. */
        const val GREY = 0.15
        /** Above this saturation a node is lit; a grey one sits near none. */
        const val LIT = 0.3
        val PURPLE = 240.0..300.0
        val GOLD = 25.0..70.0
    }
}

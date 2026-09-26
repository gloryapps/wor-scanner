package com.gloryapps.worscanner.scanner.kinds.hero

import com.gloryapps.worscanner.scanner.scan.Read
import com.gloryapps.worscanner.scanner.scan.Scan
import com.gloryapps.worscanner.scanner.scan.Seen
import com.gloryapps.worscanner.scanner.scan.Spot
import com.gloryapps.worscanner.scanner.scan.Tapped
import com.gloryapps.worscanner.scanner.senses.Colour
import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.flatten
import com.gloryapps.worscanner.scanner.text.holdsName
import com.gloryapps.worscanner.scanner.text.readsAs
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
        val hero = read(attributes, skills, awaken)
        val shown = listOf(attributes, skills, awaken)

        return Read.Card(hero, shown.flatMap { it.rowsIn(layout.panel) }, shown.map { it.frame }, closed(hero))
    }

    override fun readScreen(seen: Seen): ScannedHero {
        val rows = seen.rowsIn(layout.panel)

        return read(
            attributes = seen.takeIf { it.showsAttributes() },
            skills = seen.takeIf { rows.any(::isSkillsHeader) },
            awaken = seen.takeIf { rows.any { row -> holdsName(row, "Awakened") } },
        )
    }

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
            skills = skills?.let { skillsOf(it.rowsIn(layout.panel)) } ?: HeroSkills(),
        )
    }

    /** Whether the tile's frame is an epic's purple rather than a legendary's gold, by the lower half of its left edge: the upper half carries the tile's gold icons. */
    internal fun isEpic(frame: Frame, tile: Box): Boolean {
        val height = tile.bottom - tile.top
        val hues = (0 until EDGE_SAMPLES).map { step ->
            val y = tile.top + height / 2 + height / 2 * (2 * step + 1) / (2 * EDGE_SAMPLES)
            /* The border is a few pixels in from the edge the selection frame is drawn on: the strongest colour across it is the border's. */
            (tile.left + 2 until tile.left + 12).map { x -> frame.colourAt(x, y) }.maxBy { it.saturation * maxOf(it.red, it.green, it.blue) }.hue
        }

        return hues.count { it in PURPLE } > hues.count { it in GOLD }
    }

    /** Whether the record names what identifies it; one that does not keeps its panels as an image. */
    private fun closed(hero: ScannedHero): Boolean =
        hero.name != null && hero.level != null && hero.stars != null && hero.awakening != null && hero.skills.ultimate != null && hero.skills.row.isNotEmpty()

    private fun Seen.showsAttributes(): Boolean = levelOf(rowsIn(layout.panel)) != null

    private fun isSkillsHeader(row: String): Boolean = readsAs(flatten(row), "skills")

    /**
     * The name is the row mostly in capitals nearest above the power, written in capitals: it is
     * printed in small capitals, which the recogniser lowers a letter of here and there (`RoSALIA`,
     * `Ezio AUDITORE`), and the icons above the title read as capitals too (`AY`). The title is
     * mostly lower case; a tag like `AoE M. ATK` comes after the power. Where no power reads, the
     * first such row from the top.
     */
    private fun nameOf(rows: List<String>): String? {
        val power = rows.indexOfFirst { POWER.containsMatchIn(it) }
        val above = if (power > 0) rows.subList(0, power).asReversed() else rows

        return above.firstOrNull(::readsAsName)?.trim()?.uppercase()
    }

    private fun readsAsName(row: String): Boolean {
        val letters = row.filter(Char::isLetter)

        return letters.length >= 2 && letters.count(Char::isUpperCase) * 3 >= letters.length * 2 && row.none(Char::isDigit) && !row.contains("ATK")
    }

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

    /** The labelled rows take the levels printed under them, in the order they are labelled; the levels left over are the icon row's, left to right. */
    private fun skillsOf(rows: List<String>): HeroSkills {
        val header = rows.indexOfFirst(::isSkillsHeader)
        if (header < 0) return HeroSkills()
        var skills = HeroSkills()
        val waiting = ArrayDeque<Place>()
        for (row in rows.drop(header + 1)) {
            val place = wordIn(row, Place.entries) { it.word }
            if (place != null) {
                waiting += place
                continue
            }
            for (level in levelsIn(row)) {
                skills = when (waiting.removeFirstOrNull()) {
                    Place.LORD -> skills.copy(lord = level)
                    Place.ULTIMATE -> skills.copy(ultimate = level)
                    Place.BOND -> skills.copy(bonds = skills.bonds + level)
                    null -> skills.copy(row = skills.row + level)
                }
            }
        }

        return skills
    }

    /** The levels a row prints, left to right: its numbers, and the stretches around them read for `Max Level` or a bond to unlock. */
    private fun levelsIn(row: String): List<SkillLevel> {
        /* The tab's `MAX LEVEL` banner is the one row printed all in capitals. */
        if (row.none(Char::isLowerCase)) return emptyList()
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
        if (stretch.contains("unlock", ignoreCase = true)) listOf(SkillLevel.Of(0)) else List(timesHeld(stretch, "Max Level")) { SkillLevel.Max }

    private fun Frame.colourAt(spot: Spot): Colour = spot.on(this).let { (x, y) -> colourAt(x, y) }

    /** The word that tells apart the labels the Skills tab prints over the skills it names: `Lord Skill` and `Bond Skill` are too close whole. */
    private enum class Place(val word: String) { LORD("Lord"), ULTIMATE("Ultimate"), BOND("Bond") }

    private const val EDGE_SAMPLES = 8
    /** Below this saturation a star slot is the grey of an empty one. */
    private const val GREY = 0.15
    /** Above this saturation a node is lit; a grey one sits near none. */
    private const val LIT = 0.3
    private val PURPLE = 240.0..300.0
    private val GOLD = 25.0..70.0
}

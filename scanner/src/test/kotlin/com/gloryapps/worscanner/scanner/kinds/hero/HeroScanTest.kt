package com.gloryapps.worscanner.scanner.kinds.hero

import com.gloryapps.worscanner.scanner.scan.Read
import com.gloryapps.worscanner.scanner.scan.Seen
import com.gloryapps.worscanner.scanner.scan.Spot
import com.gloryapps.worscanner.scanner.scan.Tapped
import com.gloryapps.worscanner.scanner.scan.tileBox
import com.gloryapps.worscanner.scanner.senses.Colour
import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.Line
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeroScanTest {
    private fun at(text: String, left: Int, top: Int, right: Int, bottom: Int) = Line(text, Box(left, top, right, bottom))

    /** A dark 1280x720 frame with squares painted in the colours the recordings measured. */
    private class Painted(private val squares: List<Pair<Box, Colour>> = emptyList()) : Frame {
        override val width = 1280
        override val height = 720

        override fun colourAt(x: Int, y: Int): Colour = squares.lastOrNull { (box, _) -> x in box.left..box.right && y in box.top..box.bottom }?.second ?: DARK
    }

    private fun square(spot: Spot, colour: Colour): Pair<Box, Colour> {
        val x = (spot.x * 1280).toInt()
        val y = (spot.y * 720).toInt()

        return Box(x - 3, y - 3, x + 3, y + 3) to colour
    }

    /** The Attributes frame, its six star slots painted: the purple first, then the gold, then the empty. */
    private fun starred(purple: Int, gold: Int) =
        Painted(STAR_SLOTS.mapIndexed { at, spot -> square(spot, if (at < purple) PURPLE else if (at < purple + gold) GOLD else EMPTY) })

    /** The Awaken frame, the first `lit` nodes lit and the rest grey. */
    private fun awakened(lit: Int) = Painted(AWAKENING_NODES.mapIndexed { at, spot -> square(spot, if (at < lit) LIT_NODE else GREY_NODE) })

    /** Ingrid on Attributes, as ML Kit read it on 2026-09-26. */
    private val ingridAttributes = listOf(
        at("Quick Star Up", 823, 674, 926, 690),
        at("Regalis Excelsa", 1031, 301, 1139, 318),
        at("INGRID", 1057, 327, 1141, 347),
        at("36,083", 1027, 360, 1106, 386),
        at("AOE M. ATK", 1044, 395, 1128, 411),
        at("Single-Target M. ATK", 975, 422, 1130, 438),
        at("Range Boost", 1040, 448, 1130, 463),
        at("Continuous DMG", 1005, 472, 1130, 484),
        at("Anti-Air", 1068, 497, 1130, 509),
        at("Lvl. 60/60.", 941, 522, 1076, 548),
        at("Max", 1039, 667, 1082, 682),
    )

    /** Ingrid on Skills, as ML Kit read it on 2026-09-26. */
    private val ingridSkills = listOf(
        at("DMG Type", 851, 37, 960, 62),
        at("Talent", 852, 81, 911, 96),
        at("When deployed, enters Solar Mode.", 852, 119, 1110, 134),
        at("Solar Mode: Launching 4 Basic ATK(S)", 852, 137, 1129, 154),
        at("turns the next Basic ATK into Solar", 851, 156, 1111, 169),
        at("Flare.", 852, 175, 892, 188),
        at("Skills", 853, 228, 909, 245),
        at("Magic", 1033, 40, 1092, 62),
        at("Stellar Mode: Laung4 Basic ATK(s)", 860, 192, 1141, 209),
        at("Lord Skill", 932, 284, 1004, 297),
        at("Max Level", 927, 311, 1006, 327),
        at("Basic ATK DMG", 1066, 79, 1144, 94),
        at("Manual Ultimate", 924, 356, 1065, 376),
        at("Max Level", 927, 385, 1006, 402),
        at("Max Level Max Level Max Level Max Level", 853, 496, 1149, 507),
        at("MAX LEVEL", 866, 624, 1145, 658),
    )

    /** Ingrid on Awaken, as ML Kit read it on 2026-09-26. */
    private val ingridAwaken = listOf(
        at("Awakened I", 959, 235, 1071, 250),
        at("Awakened II", 959, 330, 1077, 346),
        at("Awakened II", 959, 426, 1085, 441),
        at("Awakened IV", 959, 521, 1085, 537),
        at("Awakened V", 959, 617, 1075, 632),
    )

    /** Maw on Attributes, as ML Kit read it on 2026-09-26. */
    private val mawAttributes = listOf(
        at("Quick Star Up", 823, 674, 926, 688),
        at("Hhe Devourer", 1050, 377, 1141, 393),
        at("MAW", 1075, 398, 1139, 421),
        at("17", 1113, 352, 1130, 366),
        at("32,082 X", 1026, 433, 1137, 460),
        at("Single-Target P. ATK", 980, 472, 1130, 488),
        at("Continuous DMG", 1004, 497, 1130, 509),
        at("Lvl. 60/60", 941, 523, 1069, 544),
        at("Lvl. Max", 997, 666, 1082, 682),
    )

    /** Maw on Skills, as ML Kit read it on 2026-09-26. */
    private val mawSkills = listOf(
        at("DMG Type", 853, 36, 960, 63),
        at("Talent", 852, 81, 911, 96),
        at("Normal", 1033, 40, 1109, 57),
        at("Each attack has a(n) 25%", 853, 120, 1112, 142),
        at("chance to inflict Bleed on", 852, 147, 1122, 164),
        at("the target.", 851, 173, 961, 196),
        at("Skills", 853, 215, 909, 232),
        at("Auto Ultimate", 938, 268, 1069, 287),
        at("Max Level", 927, 299, 1009, 314),
        at("Passive Bond Skill", 929, 343, 1070, 362),
        at("Max Level", 927, 372, 1007, 389),
        at("Max LevelMax Level Max Level", 854, 480, 1071, 495),
        at("MAX LEVEL", 866, 620, 1144, 658),
    )

    /** Kigiri on Attributes, as ML Kit read it on 2026-09-26. */
    private val kigiriAttributes = listOf(
        at("Quick Star Up", 824, 676, 927, 690),
        at("The Undying Ronin", 999, 353, 1141, 369),
        at("KIGIRI", 1073, 375, 1148, 401),
        at("106,282", 1012, 410, 1105, 436),
        at("Single-Target P. ATK", 980, 447, 1130, 463),
        at("offensive Dispel", 1007, 469, 1129, 487),
        at("Continuous DMG", 1003, 497, 1130, 509),
        at("Lvl. 60/60", 941, 523, 1069, 544),
        at("Lvl. Max", 996, 665, 1081, 682),
    )

    /** Kigiri on Skills, as ML Kit read it on 2026-09-26. */
    private val kigiriSkills = listOf(
        at("DMG Type", 854, 37, 960, 61),
        at("Talent", 852, 81, 911, 96),
        at("A Normal", 1004, 39, 1109, 59),
        at("Can launch Sweeping Strike at the", 851, 118, 1105, 134),
        at("farthest enemy within the Ultimate's", 853, 137, 1130, 153),
        at("range every 40 sec. Each time Basic", 852, 157, 1116, 173),
        at("ATK deals DMG, reduçes the cooldown", 852, 175, 1133, 192),
        at("of Sweeping Strike1sec. The AoE", 858, 191, 1120, 210),
        at("Skills", 853, 228, 909, 246),
        at("Manual Ultimate", 928, 282, 1068, 301),
        at("Max Level", 927, 313, 1009, 327),
        at("Passive Bond Skill", 928, 352, 1072, 374),
        at("Obtain Yuri to unlock", 926, 386, 1082, 401),
        at("Max Level Max Level Max Level", 854, 496, 1071, 507),
        at("MAX LEVEL", 866, 625, 1145, 658),
    )

    /** Bayek on Attributes, as ML Kit read it on 2026-09-26. */
    private val bayekAttributes = listOf(
        at("Quick Star Up", 823, 672, 926, 690),
        at("Meday ofSiva", 1039, 347, 1143, 371),
        at("BAYEK", 1060, 373, 1138, 403),
        at("66", 1085, 404, 1109, 436),
        at("19", 1104, 328, 1132, 345),
        at("AoE P. ATK", 1049, 446, 1128, 460),
        at("Ally Buff", 1063, 468, 1129, 488),
        at("Anti-Air", 1068, 497, 1130, 509),
        at("Lvl. 2/50", 963, 520, 1070, 549),
        at("Upgrade", 994, 666, 1088, 687),
    )

    /** Bayek on Skills, as ML Kit read it on 2026-09-26. */
    private val bayekSkills = listOf(
        at("DMG Type", 854, 38, 960, 62),
        at("Talent", 852, 81, 911, 96),
        at("UP", 866, 137, 878, 152),
        at("to enhance it.", 896, 147, 1023, 162),
        at("Reach Promotion Grade V", 896, 123, 1139, 139),
        at("Piercing", 1032, 39, 1115, 61),
        at("Skills", 853, 228, 909, 245),
        at("Basic ATK DMG", 1065, 80, 1144, 93),
        at("Every 20 sec, sends Spnu out to hover", 852, 178, 1128, 194),
        at("for 10 sec, continuly inflicting Mark", 854, 196, 1149, 212),
        at("Max Level", 853, 422, 915, 432),
        at("Manual Ultimate", 928, 282, 1068, 300),
        at("Lvl. 1/5", 927, 313, 987, 327),
        at("Lvl. 2/5 Lvl. 2/5", 936, 421, 1067, 433),
        at("Random Upgrade", 894, 630, 1098, 656),
    )

    /** Laseer on Attributes, as ML Kit read it on 2026-09-26. */
    private val laseerAttributes = listOf(
        at("Quick Star Up", 823, 674, 926, 690),
        at("The Sandswept King", 991, 325, 1139, 344),
        at("17", 1113, 303, 1130, 317),
        at("LASEER", 1061, 354, 1136, 372),
        at("3,959 X", 1043, 382, 1137, 411),
        at("AoE M. ATK", 1045, 423, 1130, 435),
        at("Anti-Healing", 1033, 443, 1130, 463),
        at("Continuous DMG", 1003, 472, 1130, 484),
        at("Anti-Air", 1068, 497, 1130, 509),
        at("Lvl. 1/5O", 963, 523, 1068, 544),
        at("Upgrade", 991, 665, 1083, 686),
    )

    /** Laseer on Skills, as ML Kit read it on 2026-09-26. */
    private val laseerSkills = listOf(
        at("DMG Type", 853, 37, 958, 62),
        at("Talent", 852, 78, 909, 96),
        at("UP", 854, 138, 881, 151),
        at("Reach Promotion Grade V", 896, 123, 1140, 139),
        at("to enhance it.", 895, 144, 1020, 163),
        at("Basic ATK and Omingus Sandstorm", 852, 178, 1118, 195),
        at("inflict Sand Erosion the target,", 857, 198, 1102, 212),
        at("Skills", 853, 228, 909, 246),
        at("Lyl. 2/5", 858, 421, 910, 433),
        at("Magic", 1032, 38, 1091, 60),
        at("Manual Ultimate", 927, 280, 1067, 299),
        at("Lvl. 2/5", 927, 313, 987, 327),
        at("CA", 957, 360, 976, 396),
        at("Lvl. 4/5 Lvl. 2/5", 936, 421, 1067, 433),
        at("Random Upgrade", 883, 631, 1096, 657),
    )

    @Test
    fun `Ingrid reads whole, her name, her level, six promoted stars, three awakenings and every skill at its top`() {
        val hero = HeroScan.Cards.read(
            Seen(starred(purple = 6, gold = 0), ingridAttributes),
            Seen(Painted(), ingridSkills),
            Seen(awakened(3), ingridAwaken),
        )

        assertEquals("INGRID", hero.name)
        assertEquals(60, hero.level)
        assertEquals(6 to 6, hero.stars to hero.promotion)
        assertEquals(3, hero.awakening)
        assertEquals(HeroSkills(lord = SkillLevel.Max, ultimate = SkillLevel.Max, row = List(4) { SkillLevel.Max }), hero.skills)
    }

    @Test
    fun `the name is the row in capitals, past the title above it and a number read before it`() {
        assertEquals("MAW", HeroScan.Cards.read(Seen(Painted(), mawAttributes), null, null).name)
        assertEquals("BAYEK", HeroScan.Cards.read(Seen(Painted(), bayekAttributes), null, null).name)
        assertEquals("KIGIRI", HeroScan.Cards.read(Seen(Painted(), kigiriAttributes), null, null).name)
    }

    /** Rows as the recogniser read them down the Attributes panel, one line each. */
    private fun rowsDown(vararg rows: String) = rows.mapIndexed { at, text -> at(text, 950, 300 + 30 * at, 1140, 318 + 30 * at) }

    @Test
    fun `a name in small capitals reads whole in capitals, though the recogniser lowers a letter of it here and there`() {
        fun nameIn(vararg rows: String) = HeroScan.Cards.read(Seen(Painted(), rowsDown(*rows)), null, null).name

        assertEquals("ROSALIA", nameIn("20", "The Paintress", "RoSALIA", "73,634 X", "AoE M. ATK"))
        assertEquals("EZIO AUDITORE", nameIn("18", "Master Assassin", "Ezio AUDITORE", "32,910", "Single-Target P. ATK"))
        assertEquals("NYX", nameIn("22", "Daughter ofBlood", "NYx", "28,096", "AoE P. ATK"))
        assertEquals("SU YUE", nameIn("The Jadefrost Maiden", "Su YUE", "48,340", "AOE P. ATK"))
    }

    @Test
    fun `the name is the row nearest above the power, past icons that read as capitals above the title`() {
        assertEquals("PRAETUS", HeroScan.Cards.read(Seen(Painted(), rowsDown("AY", "The Sun Supreme", "PRAETUS", "31,887 X", "Single-Target M. ATK")), null, null).name)
    }

    @Test
    fun `a hero is called by the name its Attributes print, among every tab's rows`() {
        val rows = listOf(ingridAttributes, ingridSkills, ingridAwaken).flatMap { Seen(Painted(), it).rowsIn(HERO_CARDS.panel) }

        assertEquals("INGRID", HeroScan.Cards.titleOf(rows))
    }

    @Test
    fun `where the power did not read, the name is the row in capitals nearest above the level`() {
        val rows = rowsDown("AY", "The Sun Supreme", "PRAETUS", "31.887 X", "Single-Target M. ATK", "Lvl. 60/60")

        assertEquals("PRAETUS", HeroScan.Cards.read(Seen(Painted(), rows), null, null).name)
    }

    @Test
    fun `a skill's level reads through the recogniser's slips, each keeping its place in the row`() {
        fun skillsIn(vararg rows: String) = HeroScan.Cards.read(null, Seen(Painted(), rowsDown(*rows)), null).skills

        assertEquals(
            HeroSkills(ultimate = SkillLevel.Max, row = List(3) { SkillLevel.Max }),
            skillsIn("Skills", "Mahual Ultimate", "Mx Level", "Max Level Max Letel Max Level", "MAX LEVEL"),
        )
        assertEquals(
            HeroSkills(ultimate = SkillLevel.Of(4), row = listOf(SkillLevel.Of(2), SkillLevel.Of(1), SkillLevel.Of(3), SkillLevel.Max)),
            skillsIn("Skills", "Manual Ultimate", "Lvl. 4/5", "LV 2/5 Lvl. 1/5 Lvl. 3/5 Max Level", "Random Upgrade"),
        )
        assertEquals(
            HeroSkills(ultimate = SkillLevel.Of(3), row = listOf(SkillLevel.Of(2), SkillLevel.Of(1), SkillLevel.Of(4))),
            skillsIn("Skills", "Manual Ultimate", "Lvl. 3/5", "Lvl. 2/5 Lyl, 1/S Lvl 4/5", "Random Upgrade"),
        )
        assertEquals(
            HeroSkills(ultimate = SkillLevel.Of(2), row = listOf(SkillLevel.Of(4), SkillLevel.Of(1), SkillLevel.Max)),
            skillsIn("Skills", "Manual Ultimate", "Lvl. 2/5", "Lvl, 4/5 Lvl. 1/5Mextevel", "Random Upgrade"),
        )
    }

    @Test
    fun `a level read all in capitals is still a level, its label's`() {
        fun skillsIn(vararg rows: String) = HeroScan.Cards.read(null, Seen(Painted(), rowsDown(*rows)), null).skills

        assertEquals(
            HeroSkills(ultimate = SkillLevel.Of(2), row = listOf(SkillLevel.Of(1), SkillLevel.Of(3), SkillLevel.Max)),
            skillsIn("Skills", "Manual Ultimate", "LV 2/5", "Lvl. 1/5 Lvl. 3/5 Max Level", "MAX LEVEL"),
        )
        assertEquals(
            HeroSkills(ultimate = SkillLevel.Max, row = List(3) { SkillLevel.Max }),
            skillsIn("Skills", "Manual Ultimate", "Max Level", "Max Levet Max Level Max Level", "8MAX LEVEL"),
        )
    }

    @Test
    fun `a label whose level did not read takes none, and no level moves up a place`() {
        fun skillsIn(vararg rows: String) = HeroScan.Cards.read(null, Seen(Painted(), rowsDown(*rows)), null).skills

        assertEquals(
            HeroSkills(ultimate = SkillLevel.Of(2), row = List(2) { SkillLevel.Max }),
            skillsIn("Skills", "Lord Skill", "Manual Ultimate", "Lvl. 2/5", "Max Level Max Level"),
        )
        assertEquals(
            HeroSkills(row = List(3) { SkillLevel.Max }),
            skillsIn("Skills", "Manual Ultimate", "Max Level Max Level Max Level"),
        )
    }

    @Test
    fun `a bond still to unlock reads through a misread letter`() {
        val skills = HeroScan.Cards.read(null, Seen(Painted(), rowsDown("Skills", "Auto Ultimate", "Max Level", "Passive Bond Skill", "Obtain Yuri to unIock", "Max Level")), null).skills

        assertEquals(listOf(SkillLevel.Of(0)), skills.bonds)
    }

    @Test
    fun `a bond is a place of its own, earned or still to unlock`() {
        val maw = HeroScan.Cards.read(null, Seen(Painted(), mawSkills), null).skills
        val kigiri = HeroScan.Cards.read(null, Seen(Painted(), kigiriSkills), null).skills

        assertEquals(HeroSkills(ultimate = SkillLevel.Max, bonds = listOf(SkillLevel.Max), row = List(3) { SkillLevel.Max }), maw)
        assertEquals(HeroSkills(ultimate = SkillLevel.Max, bonds = listOf(SkillLevel.Of(0)), row = List(3) { SkillLevel.Max }), kigiri)
    }

    @Test
    fun `a skill below its top reads as its number, the icon row left to right`() {
        val bayek = HeroScan.Cards.read(null, Seen(Painted(), bayekSkills), null).skills
        val laseer = HeroScan.Cards.read(null, Seen(Painted(), laseerSkills), null).skills

        assertEquals(HeroSkills(ultimate = SkillLevel.Of(1), row = listOf(SkillLevel.Max, SkillLevel.Of(2), SkillLevel.Of(2))), bayek)
        assertEquals(HeroSkills(ultimate = SkillLevel.Of(2), row = listOf(SkillLevel.Of(2), SkillLevel.Of(4), SkillLevel.Of(2))), laseer)
    }

    @Test
    fun `the level is the number before the slash, a cap read as 5O included`() {
        assertEquals(2, HeroScan.Cards.read(Seen(Painted(), bayekAttributes), null, null).level)
        assertEquals(1, HeroScan.Cards.read(Seen(Painted(), laseerAttributes), null, null).level)
    }

    @Test
    fun `stars count the purple and the gold slots, promotion only the purple`() {
        val bayek = HeroScan.Cards.read(Seen(starred(purple = 2, gold = 3), bayekAttributes), null, null)
        val laseer = HeroScan.Cards.read(Seen(starred(purple = 0, gold = 5), laseerAttributes), null, null)

        assertEquals(5 to 2, bayek.stars to bayek.promotion)
        assertEquals(5 to 0, laseer.stars to laseer.promotion)
    }

    @Test
    fun `awakening counts the lit nodes, none lit at A0`() {
        assertEquals(5, HeroScan.Cards.read(null, null, Seen(awakened(5), emptyList())).awakening)
        assertEquals(0, HeroScan.Cards.read(null, null, Seen(awakened(0), emptyList())).awakening)
    }

    /** Column 0's left edge, a sixth of the tile wide, where the view puts that tile at [CENTRE]. */
    private fun edgeOf(scan: HeroScan): Box = scan.layout.tileBox(Painted(), 0, CENTRE).let { Box(it.left, it.top, it.left + (it.right - it.left) / 6, it.bottom) }

    @Test
    fun `as cards, a tile edged in a legendary's gold or an epic's purple is read, one in a rare's blue, an uncommon's green or a common's grey is not`() {
        fun edged(colour: Colour) = HeroScan.Cards.tileAt(Seen(Painted(listOf(edgeOf(HeroScan.Cards) to colour)), emptyList()), 0, CENTRE)

        assertTrue(edged(GOLD_FRAME))
        assertTrue(edged(PURPLE_FRAME))
        assertFalse(edged(BLUE_FRAME))
        assertFalse(edged(GREEN_FRAME))
        assertFalse(edged(GREY_FRAME))
    }

    @Test
    fun `as squares, the same ranks are told off their paler edges, and an empty slot is no tile`() {
        fun edged(colour: Colour) = HeroScan.Squares.tileAt(Seen(Painted(listOf(edgeOf(HeroScan.Squares) to colour)), emptyList()), 0, CENTRE)

        assertTrue(edged(SQUARE_GOLD))
        assertTrue(edged(SQUARE_PURPLE))
        assertFalse(edged(SQUARE_BLUE))
        assertFalse(edged(SQUARE_GREEN))
        assertFalse(edged(SQUARE_GREY))
        assertFalse(edged(EMPTY_SLOT))
    }

    @Test
    fun `the roster is scanned in the view whose button is lit`() {
        fun face(spot: Spot, colour: Colour) = square(spot, colour).let { (box, _) -> Box(box.left - 9, box.top - 9, box.right + 9, box.bottom + 9) to colour }
        val squares = Seen(Painted(listOf(face(SQUARES_BUTTON, LIT_BUTTON), face(CARDS_BUTTON, UNLIT_BUTTON))), emptyList())
        val cards = Seen(Painted(listOf(face(SQUARES_BUTTON, UNLIT_BUTTON), face(CARDS_BUTTON, LIT_BUTTON))), emptyList())

        assertEquals(HeroScan.Squares, HeroScan.Cards.viewOn(squares))
        assertEquals(HeroScan.Cards, HeroScan.Cards.viewOn(cards))
        assertEquals(HeroScan.Cards, HeroScan.Squares.viewOn(cards))
    }

    @Test
    fun `as squares, the scan begins on the selected hero, no word finding a row`() {
        assertNull(HeroScan.Squares.firstRowCentre(Seen(Painted(), ingridAttributes)))
    }

    @Test
    fun `a level goes on the wire as its number, and the top as max`() {
        val skills = HeroSkills(lord = SkillLevel.Max, ultimate = SkillLevel.Of(1), bonds = listOf(SkillLevel.Of(0)), row = listOf(SkillLevel.Max, SkillLevel.Of(2)))
        val written = Json.encodeToString(HeroSkills.serializer(), skills)

        assertEquals("""{"lord":"max","ultimate":1,"bonds":[0],"row":["max",2]}""", written)
        assertEquals(skills, Json.decodeFromString(HeroSkills.serializer(), written))
    }

    @Test
    fun `every place of the skills is written, a hero without a lord or a bond included`() {
        val written = Json.encodeToString(HeroSkills.serializer(), HeroSkills(ultimate = SkillLevel.Max))

        assertEquals("""{"lord":null,"ultimate":"max","bonds":[],"row":[]}""", written)
    }

    @Test
    fun `a whole frame of Awaken reads its lit nodes`() {
        assertEquals(3, HeroScan.Cards.readScreen(Seen(awakened(3), ingridAwaken)).awakening)
    }

    @Test
    fun `a whole frame reads what its tab shows, and nothing of the tabs it does not`() {
        val hero = HeroScan.Cards.readScreen(Seen(Painted(), ingridSkills))

        assertEquals(null, hero.name)
        assertEquals(null, hero.awakening)
        assertEquals(SkillLevel.Max, hero.skills.lord)
    }

    /** The walk's lending as the hero screen would answer it: each tab shows its frame, the grid found again or not. */
    private class Lent(override val seen: Seen, private val tabs: Map<Spot, Seen>, private val regrips: Boolean = true) : Tapped {
        override val tile = Box(28, 115, 111, 242)
        val shown = mutableListOf<Spot>()
        var regripped = false

        override suspend fun show(tab: Spot): Seen = tabs.getValue(tab).also { shown += tab }

        override suspend fun regrip(): Seen? = seen.takeIf { regrips }.also { regripped = true }
    }

    private val ingrid = mapOf(
        ATTRIBUTES to Seen(starred(purple = 6, gold = 0), ingridAttributes),
        SKILLS to Seen(Painted(), ingridSkills),
        AWAKEN to Seen(awakened(3), ingridAwaken),
    )

    @Test
    fun `a tile is read under Attributes, Skills and Awaken, then left on Attributes and the grid found again`() = runTest {
        val lent = Lent(ingrid.getValue(ATTRIBUTES), ingrid)

        val read = HeroScan.Cards.readTile(lent)

        assertIs<Read.Card<ScannedHero>>(read)
        assertEquals(listOf(SKILLS, AWAKEN, ATTRIBUTES), lent.shown)
        assertTrue(lent.regripped)
        assertEquals("INGRID", read.card.name)
        assertEquals(3, read.frames.size)
        assertTrue(read.closed)
    }

    @Test
    fun `a tile the game left on another tab is read from Attributes first`() = runTest {
        val lent = Lent(ingrid.getValue(SKILLS), ingrid)

        val read = HeroScan.Cards.readTile(lent)

        assertIs<Read.Card<ScannedHero>>(read)
        assertEquals(listOf(ATTRIBUTES, SKILLS, AWAKEN, ATTRIBUTES), lent.shown)
        assertEquals(60, read.card.level)
    }

    @Test
    fun `an Awaken tap that left another tab on screen reads no awakening, and the panels are kept`() = runTest {
        val read = HeroScan.Cards.readTile(Lent(ingrid.getValue(ATTRIBUTES), ingrid + (AWAKEN to ingrid.getValue(SKILLS))))

        assertIs<Read.Card<ScannedHero>>(read)
        assertEquals(null, read.card.awakening)
        assertFalse(read.closed)
    }

    @Test
    fun `a skill label whose level did not read keeps the panels`() = runTest {
        val skills = Seen(Painted(), ingridSkills.filterNot { it.box.top == 311 })

        val read = HeroScan.Cards.readTile(Lent(ingrid.getValue(ATTRIBUTES), ingrid + (SKILLS to skills)))

        assertIs<Read.Card<ScannedHero>>(read)
        assertEquals(null, read.card.skills.lord)
        assertEquals(SkillLevel.Max, read.card.skills.ultimate)
        assertFalse(read.closed)
    }

    @Test
    fun `a frame whose stars did not show keeps the panels`() = runTest {
        val read = HeroScan.Cards.readTile(Lent(Seen(Painted(), ingridAttributes), ingrid))

        assertIs<Read.Card<ScannedHero>>(read)
        assertFalse(read.closed)
    }

    @Test
    fun `a grid not found again once back on Attributes stops the scan`() = runTest {
        val lent = Lent(ingrid.getValue(ATTRIBUTES), ingrid, regrips = false)

        assertIs<Read.Lost>(HeroScan.Cards.readTile(lent))
    }

    private companion object {
        val DARK = Colour(0x101010)
        val PURPLE = Colour(0x854DFF)
        val GOLD = Colour(0xF6E67D)
        val EMPTY = Colour(0x7D7D7D)
        val LIT_NODE = Colour(0x604626)
        val GREY_NODE = Colour(0x222222)
        val GOLD_FRAME = Colour(0xC5AA55)
        val PURPLE_FRAME = Colour(0x4B236C)
        /* Measured inside LDPlayer on 2026-09-28 on the tiles' left edges, as cards, then as squares, then the view buttons' faces. */
        val BLUE_FRAME = Colour(0x2C3D63)
        val GREEN_FRAME = Colour(0x35502D)
        val GREY_FRAME = Colour(0x655441)
        val SQUARE_GOLD = Colour(0xAE8850)
        val SQUARE_PURPLE = Colour(0x7B63C3)
        val SQUARE_BLUE = Colour(0x5A688F)
        val SQUARE_GREEN = Colour(0x597D5C)
        val SQUARE_GREY = Colour(0x6E7078)
        val EMPTY_SLOT = Colour(0x404758)
        val LIT_BUTTON = Colour(0xB99F7C)
        val UNLIT_BUTTON = Colour(0x8E8069)
        /** Where the tile whose edge is painted sits, well inside either view's grid. */
        const val CENTRE = 300
    }
}

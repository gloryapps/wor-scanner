package com.gloryapps.worscanner.scanner.kinds.gear

import com.gloryapps.worscanner.scanner.game.ReadAttribute
import com.gloryapps.worscanner.scanner.game.attributesIn
import com.gloryapps.worscanner.scanner.game.exclusiveIn
import com.gloryapps.worscanner.scanner.game.headOf
import com.gloryapps.worscanner.scanner.scan.Read
import com.gloryapps.worscanner.scanner.scan.Scan
import com.gloryapps.worscanner.scanner.scan.Seen
import com.gloryapps.worscanner.scanner.scan.Tapped
import com.gloryapps.worscanner.scanner.text.ValueUnit
import com.gloryapps.worscanner.scanner.text.flatten
import com.gloryapps.worscanner.scanner.text.holdsName
import com.gloryapps.worscanner.scanner.text.numbersIn
import com.gloryapps.worscanner.scanner.text.wordIn

/**
 * How gear is scanned: its storage, its record, and the card a piece's tile draws in the panel, a
 * banner, a title, an exclusive's name, the attribute rows, and the set block at the foot. What
 * the card did not name is null.
 */
object GearScan : Scan<ScannedGear>() {
    override val layout = GEAR_STORAGE
    override val serializer = ScannedGear.serializer()

    /** Where the piece's own rows end: the set block below them talks about attributes too. The count reads `B` as often as `3`. */
    private val SET_BLOCK = Regex("\\(\\s*\\S{1,2}\\s*pieces?\\s*\\)", RegexOption.IGNORE_CASE)

    override suspend fun readTile(tapped: Tapped): Read<ScannedGear> {
        val rows = tapped.seen.rowsIn(layout.panel)
        val card = read(rows)

        return Read.Card(card, rows, listOf(tapped.seen.frame), closed(card))
    }

    override fun readScreen(seen: Seen): ScannedGear = read(seen.rowsIn(layout.panel))

    /** The title, `Variant: Vierna's Bangle`: the head's row that names the slot. */
    override fun titleOf(rows: List<String>): String? = headOf(rows).getOrNull(titleAt(rows))?.trim()

    internal fun read(rows: List<String>): ScannedGear {
        val set = setIn(rows)
        val read = attributesIn(ownRows(rows), extras = 1)
        val slot = slotIn(rows, set, read)
        val primary = primaryIn(rows, slot, read)

        return ScannedGear(
            set = set,
            slot = slot,
            ancient = bannerHas(rows, "ancient"),
            variant = bannerHas(rows, "variant"),
            exclusive = exclusiveIn(belowTitle(rows)),
            attributes = if (primary != null) listOf(primary) + read.map { it.copy(bonus = null) } else read,
        )
    }

    /** Whether the card names what identifies it; one that does not keeps its panel as an image. */
    internal fun closed(record: ScannedGear): Boolean = record.set != null && record.slot != null

    private fun setBlockAt(rows: List<String>): Int = rows.indexOfFirst { SET_BLOCK.containsMatchIn(it) }

    /** The piece's own rows, above the set block. */
    private fun ownRows(rows: List<String>): List<String> {
        val ends = setBlockAt(rows)

        return if (ends < 0) rows else rows.subList(0, ends)
    }

    /** The set, off the block at the foot of the card, which is the one part that always reads. */
    private fun setIn(rows: List<String>): String? {
        val at = setBlockAt(rows)
        if (at < 0) return null

        val block = rows.subList(maxOf(0, at - 2), at + 1).joinToString(" ")

        return GEAR_SETS.firstOrNull { holdsName(block, it.name) }?.id
    }

    private fun bannerHas(rows: List<String>, word: String): Boolean = headOf(rows).any { flatten(it).contains(word) }

    private fun titleAt(rows: List<String>): Int = headOf(rows).indexOfFirst { row -> wordIn(row, Slot.entries) { it.word } != null }

    /** The rows under the title: an exclusive's hero is printed below it, and a hero that did not read leaves the title no way in. */
    private fun belowTitle(rows: List<String>): List<String> {
        val title = titleAt(rows)

        return if (title < 0) rows else rows.drop(title + 1)
    }

    /**
     * The slot: a word of the head narrowed to the side its set is worn in, or the attribute the
     * game fixes a slot's first line to where the banner did not read.
     */
    private fun slotIn(rows: List<String>, set: String?, attributes: List<ReadAttribute>): Slot? {
        val side = set?.let(::gearSetOf)?.side
        val candidates = Slot.entries.filter { side == null || it.side == side }

        headOf(rows).firstNotNullOfOrNull { wordIn(it, candidates) { slot -> slot.word } }?.let { return it }

        val primary = attributes.firstOrNull()?.name

        return candidates.firstOrNull { primary != null && it.primary == primary }
    }

    /**
     * The first line a slot always leads with, where the reader lost its name.
     *
     * A Breastplate's `HP 3960` reads as `RSP 3960` when the heart beside it runs into two letters,
     * too few for any slack; the game fixes what that line is, so the number is taken back.
     */
    private fun primaryIn(rows: List<String>, slot: Slot?, attributes: List<ReadAttribute>): ReadAttribute? {
        val name = slot?.primary ?: return null
        if (attributes.any { it.name == name }) return null

        val value = numbersIn(headOf(rows).lastOrNull() ?: "").firstOrNull() ?: return null

        return if (value.unit == ValueUnit.FLAT && value.value > 0) ReadAttribute(name, value.value, value.unit, null) else null
    }
}

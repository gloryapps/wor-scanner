package com.gloryapps.worscanner.scanner.reading

import com.gloryapps.worscanner.scanner.catalogue.Attribute
import com.gloryapps.worscanner.scanner.catalogue.GEAR_SETS
import com.gloryapps.worscanner.scanner.catalogue.Slot
import com.gloryapps.worscanner.scanner.catalogue.gearSetOf

/** Where the piece's own rows end: the set block below them talks about attributes too. */
private val SET_BLOCK = Regex("\\(\\s*\\d+\\s*pieces?\\s*\\)", RegexOption.IGNORE_CASE)

private fun setBlockAt(rows: List<String>): Int = rows.indexOfFirst { SET_BLOCK.containsMatchIn(it) }

private data class Number(val value: Double, val unit: ValueUnit)

/** A row reads `<icon noise> <name> <value>`, so its numbers are the run it ends on. */
private fun trailingNumbers(row: String): List<Number> {
    val tail = Regex("[\\d.,%+\\s]+$").find(row)?.value ?: ""

    return Regex("(\\d+(?:[.,]\\d+)?)\\s*(%?)").findAll(tail).map { found ->
        Number(found.groupValues[1].replace(',', '.').toDouble(), if (found.groupValues[2] == "%") ValueUnit.PERCENTAGE else ValueUnit.FLAT)
    }.toList()
}

private fun attributeIn(row: String): Attribute? = longestHeld(row, Attribute.entries) { it.word }

/**
 * Attribute rows in the order the card shows them, the first being the primary.
 *
 * A value the recogniser set apart from its name lands on the row below, which is taken unless
 * that row names an attribute of its own.
 */
private fun attributesIn(read: List<String>): List<ReadAttribute> {
    val ends = setBlockAt(read)
    val rows = if (ends < 0) read else read.subList(0, ends)
    val held = mutableListOf<ReadAttribute>()

    for ((at, row) in rows.withIndex()) {
        val name = attributeIn(row) ?: continue
        if (held.any { it.name == name }) continue

        val below = rows.getOrNull(at + 1) ?: ""
        val own = trailingNumbers(row)
        val spilled = if (attributeIn(below) != null) emptyList() else trailingNumbers(below)
        val numbers = own.ifEmpty { spilled }
        val value = numbers.firstOrNull() ?: continue

        /* Only the first row carries a green extra, so a second number below is the reader's noise. */
        val bonus = if (held.isEmpty()) numbers.getOrNull(1)?.value else null
        held += ReadAttribute(name, value.value, value.unit, bonus)
    }

    return held
}

/** Everything above the first attribute row: the banner, the title, the exclusive's name. */
private fun headOf(rows: List<String>): List<String> {
    val first = rows.indexOfFirst { attributeIn(it) != null }

    return if (first < 0) rows else rows.subList(0, first)
}

/** The set, off the block at the foot of the card, which is the one part that always reads. */
private fun setIn(rows: List<String>): String? {
    val at = setBlockAt(rows)
    if (at < 0) return null

    val block = rows.subList(maxOf(0, at - 2), at + 1).joinToString(" ")

    return GEAR_SETS.firstOrNull { holdsName(block, it.name) }?.id
}

private fun bannerHas(rows: List<String>, word: String): Boolean = headOf(rows).any { flatten(it).contains(word) }

/**
 * What the card names above the word `Exclusive`, as read.
 *
 * The word sits on its own row under the name; the icon leaves single letters in front of it.
 */
private fun exclusiveIn(rows: List<String>): String? {
    val head = headOf(rows)
    val at = head.indexOfFirst { holdsName(it, "Exclusive") }
    if (at < 0) return null

    val named = "${head[at].replace(Regex("exclusive", RegexOption.IGNORE_CASE), "")} ${head.getOrNull(at - 1) ?: ""}"
        .replace(Regex("[^A-Za-z' ]"), " ")
        .split(Regex("\\s+"))
        .filter { it.length > 1 }
        .joinToString(" ")
        .trim()

    return named.ifEmpty { null }
}

/** The slot as a whole word of the row, never fuzzily: a loose match once read `Amulet` as `Ring`. */
private fun slotWordIn(row: String, candidates: List<Slot>): Slot? {
    val words = row.split(Regex("[^A-Za-z]+")).toSet()

    return candidates.firstOrNull { it.word in words }
}

/**
 * The slot: a word of the head narrowed to the side its set is worn in, or the attribute the
 * game fixes a slot's first line to where the banner did not read.
 */
private fun slotIn(rows: List<String>, set: String?, attributes: List<ReadAttribute>): Slot? {
    val side = set?.let(::gearSetOf)?.side
    val candidates = Slot.entries.filter { side == null || it.side == side }

    headOf(rows).firstNotNullOfOrNull { slotWordIn(it, candidates) }?.let { return it }

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

    val value = trailingNumbers(headOf(rows).lastOrNull() ?: "").firstOrNull() ?: return null

    return if (value.unit == ValueUnit.FLAT && value.value > 0) ReadAttribute(name, value.value, value.unit, null) else null
}

/** One card, read off the rows a panel gave up. */
fun readGearCard(rows: List<String>): ScannedGear {
    val set = setIn(rows)
    val read = attributesIn(rows)
    val slot = slotIn(rows, set, read)
    val primary = primaryIn(rows, slot, read)

    return ScannedGear(
        set = set,
        slot = slot,
        ancient = bannerHas(rows, "ancient"),
        variant = bannerHas(rows, "variant"),
        exclusive = exclusiveIn(rows),
        attributes = if (primary != null) listOf(primary) + read.map { it.copy(bonus = null) } else read,
    )
}

/** One card, off the lines a recogniser placed in a frame. */
fun readGearCard(lines: Iterable<Line>): ScannedGear = readGearCard(rowsOf(lines.toList()))

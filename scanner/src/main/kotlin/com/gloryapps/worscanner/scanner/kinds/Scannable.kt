package com.gloryapps.worscanner.scanner.kinds

import kotlinx.serialization.KSerializer

/** How one kind's panel reads: its rows in, its record out. The model of the panel is the kind's own. */
interface Reader<T> {
    fun read(rows: List<String>): T

    /** Whether the record names what identifies it; one that does not keeps its panel as an image. */
    fun closed(record: T): Boolean
}

/** What the scanner needs from one kind: where its screen puts things, how its panel reads, how its record is written. */
interface Scannable<T> {
    val layout: GridLayout
    val serializer: KSerializer<T>
    val reader: Reader<T>
}

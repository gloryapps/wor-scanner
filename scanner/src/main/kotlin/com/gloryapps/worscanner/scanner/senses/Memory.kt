package com.gloryapps.worscanner.scanner.senses

/** A run of the game's memory it keeps its own data in: where it starts, and how many bytes. */
data class Span(val start: Long, val size: Long)

/** The game's memory as the platform opened it, only ever read: where it keeps data, and the bytes at an address. */
interface Memory {
    fun spans(): List<Span>

    /** `size` bytes from `address`; null where any of them cannot be read. */
    fun read(address: Long, size: Int): ByteArray?
}

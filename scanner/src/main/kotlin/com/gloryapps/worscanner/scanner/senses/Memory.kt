package com.gloryapps.worscanner.scanner.senses

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A run of the game's memory it keeps its own data in: where it starts, and how many bytes. */
data class Span(val start: Long, val size: Long)

/** The game's memory as the platform opened it, only ever read: where it keeps data, and the bytes at an address. */
interface Memory {
    fun spans(): List<Span>

    /** The runs the game maps from files and only reads, as il2cpp maps its metadata; none where the platform does not tell them apart. */
    fun mapped(): List<Span> = emptyList()

    /** `size` bytes from `address`; null where any of them cannot be read. */
    fun read(address: Long, size: Int): ByteArray?
}

/** Hands each chunk of `spans` to `look`, side by side: its address, its bytes, and how many it starts objects in, the rest only read past. */
suspend fun Memory.sweep(spans: List<Span>, look: (at: Long, bytes: ByteBuffer, looked: Int) -> Unit) = coroutineScope {
    spans.flatMap(::chunks).map { chunk ->
        launch(Dispatchers.Default) { read(chunk.at, chunk.size)?.let { look(chunk.at, ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN), chunk.looked) } }
    }.joinAll()
}

private fun chunks(span: Span): List<Chunk> = (0 until span.size step CHUNK.toLong()).map { from ->
    val left = span.size - from

    Chunk(span.start + from, looked = minOf(CHUNK.toLong(), left).toInt(), size = minOf(CHUNK.toLong() + PAST, left).toInt())
}

private data class Chunk(val at: Long, val looked: Int, val size: Int)

/** How much of the game's memory a sweep reads at once. */
internal const val CHUNK = 8 shl 20
/** How far past its chunk a sweep reads, for what starts in it and ends in the next. */
private const val PAST = 128

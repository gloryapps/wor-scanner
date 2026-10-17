package com.gloryapps.worscanner.scanner.senses

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Hands each chunk of `spans` to `look`, side by side: its address, its bytes, and how many it starts objects in, the rest only read past. */
suspend fun Memory.sweep(spans: List<Span> = spans(), look: (at: Long, bytes: ByteBuffer, looked: Int) -> Unit) = coroutineScope {
    spans.flatMap(::chunks).map { chunk ->
        launch(Dispatchers.Default) { read(chunk.at, chunk.size)?.let { look(chunk.at, littleEndian(it), chunk.looked) } }
    }.joinAll()
}

fun littleEndian(bytes: ByteArray): ByteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

private fun chunks(span: Span): List<Chunk> = (0 until span.size step CHUNK.toLong()).map { from ->
    val left = span.size - from

    Chunk(span.start + from, looked = minOf(CHUNK.toLong(), left).toInt(), size = minOf(CHUNK.toLong() + PAST, left).toInt())
}

private data class Chunk(val at: Long, val looked: Int, val size: Int)

/** How much of the game's memory a sweep reads at once. */
const val CHUNK = 8 shl 20
private const val PAST = 128

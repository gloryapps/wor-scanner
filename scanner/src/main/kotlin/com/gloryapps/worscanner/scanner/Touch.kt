package com.gloryapps.worscanner.scanner

/** The hand: a tap, or a finger dragged across the display over some milliseconds. */
interface Touch {
    suspend fun tap(x: Int, y: Int)
    suspend fun drag(fromX: Int, fromY: Int, toX: Int, toY: Int, millis: Long)
}

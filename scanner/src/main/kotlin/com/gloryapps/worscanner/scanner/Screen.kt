package com.gloryapps.worscanner.scanner

/** One picture of the display: its size, and a pixel's brightness for whoever compares two of them. */
interface Frame {
    val width: Int
    val height: Int

    /** 0..255, black to white. */
    fun luminanceAt(x: Int, y: Int): Int
}

/** The eyes: what the display shows at this moment. */
interface Screen {
    suspend fun capture(): Frame
}

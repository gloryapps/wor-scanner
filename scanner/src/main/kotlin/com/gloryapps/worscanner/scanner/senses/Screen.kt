package com.gloryapps.worscanner.scanner.senses

/** One picture of the display: its size, and how pale a pixel is, for whoever looks for a light frame. */
interface Frame {
    val width: Int
    val height: Int

    /** 0..255: the least of a pixel's three channels, high only where the pixel is near white. */
    fun palenessAt(x: Int, y: Int): Int
}

/** The eyes: what the display shows at this moment. */
interface Screen {
    suspend fun capture(): Frame
}

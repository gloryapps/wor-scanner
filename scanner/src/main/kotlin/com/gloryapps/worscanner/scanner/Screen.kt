package com.gloryapps.worscanner.scanner

/** One picture of the display, opaque here: what it holds is the reader's business, not the walk's. */
interface Frame {
    val width: Int
    val height: Int
}

/** The eyes: what the display shows at this moment. */
interface Screen {
    suspend fun capture(): Frame
}

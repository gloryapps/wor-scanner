package com.gloryapps.worscanner.scanner.scan

import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.Line

/** A rectangle in fractions of the display, so one layout serves every resolution. */
data class Region(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    fun box(width: Int, height: Int) = Box(
        (left * width).toInt(),
        (top * height).toInt(),
        (right * width).toInt(),
        (bottom * height).toInt(),
    )
}

/** A point in fractions of the display, so one tap serves every resolution. */
data class Spot(val x: Double, val y: Double) {
    /** The pixel the point falls on in this frame. */
    fun on(frame: Frame): Pair<Int, Int> = (x * frame.width).toInt() to (y * frame.height).toInt()
}

/** Whether the line's middle falls inside the box. */
fun Box.holds(line: Line): Boolean {
    val x = (line.box.left + line.box.right) / 2
    val y = line.box.middle

    return x in left..right && y in top..bottom
}

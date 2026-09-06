package com.gloryapps.worscanner.scanner.walk

import com.gloryapps.worscanner.scanner.reading.Box
import com.gloryapps.worscanner.scanner.reading.Line

/** A rectangle in fractions of the display, so one layout serves every resolution. */
data class Region(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    fun box(width: Int, height: Int) = Box(
        (left * width).toInt(),
        (top * height).toInt(),
        (right * width).toInt(),
        (bottom * height).toInt(),
    )
}

/** Whether the line's middle falls inside the box. */
fun Box.holds(line: Line): Boolean {
    val x = (line.box.left + line.box.right) / 2
    val y = line.box.middle

    return x in left..right && y in top..bottom
}

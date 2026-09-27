package com.gloryapps.worscanner.scanner.senses

/** One picture of the display: its size, and the colour of each pixel. */
interface Frame {
    val width: Int
    val height: Int

    fun colourAt(x: Int, y: Int): Colour
}

/** A pixel's colour, packed as the display gives it, `0xAARRGGBB`; the alpha is not read. */
@JvmInline
value class Colour(private val packed: Int) {
    val red: Int get() = packed shr 16 and 0xFF
    val green: Int get() = packed shr 8 and 0xFF
    val blue: Int get() = packed and 0xFF

    /** 0..255: the least of the three channels, high only where the pixel is near white. */
    val paleness: Int get() = minOf(red, green, blue)

    /** 0..1: how far the colour is from a grey of its brightness. */
    val saturation: Double
        get() {
            val most = maxOf(red, green, blue)

            return if (most == 0) 0.0 else (most - paleness).toDouble() / most
        }

    /** 0..360: where the colour sits on the wheel, red at 0, green at 120, blue at 240; 0 for a grey. */
    val hue: Double
        get() {
            val most = maxOf(red, green, blue)
            val span = (most - paleness).toDouble()
            if (span == 0.0) return 0.0
            val sixth = when (most) {
                red -> (green - blue) / span
                green -> 2 + (blue - red) / span
                else -> 4 + (red - green) / span
            }

            return (sixth * 60 + 360) % 360
        }
}

/** The eyes: what the display shows at this moment. */
interface Screen {
    suspend fun capture(): Frame
}

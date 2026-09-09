package com.gloryapps.worscanner.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gloryapps.worscanner.ui.Colors

/** Where the capsule will land, shown only while it is held: a dashed outline in each corner of the safe band. */
@Composable
fun SnapZones() {
    Box(Modifier.fillMaxSize().padding(INSET)) {
        Corner.entries.forEach { corner ->
            Box(
                Modifier
                    .align(if (corner.top) { if (corner.start) Alignment.TopStart else Alignment.TopEnd } else { if (corner.start) Alignment.BottomStart else Alignment.BottomEnd })
                    .size(ZONE_WIDTH, ZONE_HEIGHT)
                    .dashed(),
            )
        }
    }
}

private fun Modifier.dashed(): Modifier = drawBehind {
    val radius = CornerRadius(size.height / 2)
    drawRoundRect(Colors.glassFaint, cornerRadius = radius)
    drawRoundRect(
        Colors.glassAccentEdge,
        cornerRadius = radius,
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
    )
}

/** How far the safe band sits in from the screen's edges: the capsule parks here, never nearer. */
internal val INSET = 34.dp
private val ZONE_WIDTH = 132.dp
private val ZONE_HEIGHT = 24.dp

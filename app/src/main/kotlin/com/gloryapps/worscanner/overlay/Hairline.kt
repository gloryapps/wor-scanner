package com.gloryapps.worscanner.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scan.Scanning
import com.gloryapps.worscanner.ui.Colors
import org.koin.compose.koinInject

/**
 * How far the scan is, as a rule across the top of the screen. It reads, it is never touched, and
 * it stays at the top wherever the capsule is dragged.
 */
@Composable
fun Hairline(scanning: Scanning = koinInject()) {
    val state by scanning.state.collectAsStateWithLifecycle()
    val filled = when (val held = state) {
        ScanState.Idle -> 0f
        is ScanState.Running -> if (held.progress.held > 0) held.progress.done.toFloat() / held.progress.held else 0f
        is ScanState.Ended -> 1f
    }
    val grown by animateFloatAsState(filled, label = "hairline")
    if (state is ScanState.Idle) return

    Box(Modifier.fillMaxWidth().height(HEIGHT).background(Colors.edge)) {
        Box(Modifier.fillMaxHeight().fraction(grown).background(Colors.accent))
    }
}

/** The width the box takes, as a fraction of what it was offered; `fillMaxWidth(0f)` is not allowed. */
private fun Modifier.fraction(of: Float) = layout { measurable, constraints ->
    val width = (constraints.maxWidth * of).toInt()
    val placed = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))

    layout(placed.width, placed.height) { placed.place(0, 0) }
}

private val HEIGHT = 2.dp

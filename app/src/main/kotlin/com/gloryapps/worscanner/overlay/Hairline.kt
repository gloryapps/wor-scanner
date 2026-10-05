package com.gloryapps.worscanner.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.scanner.runs.ScanState
import com.gloryapps.worscanner.scanner.runs.Scanning
import com.gloryapps.worscanner.ui.Colors
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/** The hairline's window, wired to the scan under way. */
@Composable
fun HairlineContent(scanning: Scanning = koinInject()) {
    val state by scanning.state.collectAsStateWithLifecycle()

    Hairline(state)
}

/**
 * How far the scan is, as a rule across the top of the screen with a glow under it. It reads, it is
 * never touched, and it stays at the top wherever the capsule is dragged. It is only there while a
 * scan runs: on the end it holds full for a moment, then fades, by its colours rather than an `alpha`
 * layer, which an overlay window on Android 9 cannot draw.
 */
@Composable
internal fun Hairline(state: ScanState) {
    val filled = when (state) {
        ScanState.Idle -> 0f
        is ScanState.Running -> if (state.progress.held > 0) state.progress.done.toFloat() / state.progress.held else 0f
        is ScanState.Ended -> 1f
    }
    var held by remember(state) { mutableStateOf(true) }
    LaunchedEffect(state) {
        if (state is ScanState.Ended) {
            delay(HOLD_MS)
            held = false
        }
    }
    val grown by animateFloatAsState(filled, label = "hairline")
    val shown by animateFloatAsState(if (held) 1f else 0f, tween(FADE_MS), label = "fade")
    if (state is ScanState.Idle) return

    Box(Modifier.fillMaxWidth().height(HEIGHT + GLOW)) {
        Box(Modifier.fillMaxWidth().height(HEIGHT).background(Colors.edge.faded(shown)))
        Box(
            Modifier
                .height(HEIGHT + GLOW)
                .fraction(grown)
                .background(Brush.verticalGradient(listOf(Colors.accentGlow.faded(shown), Color.Transparent))),
        )
        Box(Modifier.height(HEIGHT).fraction(grown).background(Colors.accent.faded(shown)))
    }
}

private fun Color.faded(to: Float): Color = copy(alpha = alpha * to)

/** The width the box takes, as a fraction of what it was offered; `fillMaxWidth(0f)` is not allowed. */
private fun Modifier.fraction(of: Float) = layout { measurable, constraints ->
    val width = (constraints.maxWidth * of).toInt()
    val placed = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))

    layout(placed.width, placed.height) { placed.place(0, 0) }
}

private val HEIGHT = 2.dp
private val GLOW = 10.dp
private const val HOLD_MS = 1_500L
private const val FADE_MS = 400

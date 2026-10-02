package com.gloryapps.worscanner.overlay

import android.content.Intent
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.app.MainActivity
import com.gloryapps.worscanner.capture.CaptureService
import com.gloryapps.worscanner.scan.Chosen
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scan.Scanning
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Permission
import com.gloryapps.worscanner.ui.Permissions
import com.gloryapps.worscanner.ui.Standing
import com.gloryapps.worscanner.ui.accessibilitySettings
import com.gloryapps.worscanner.ui.label
import com.gloryapps.worscanner.ui.reminder
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/**
 * The capsule over the game: the whole of the overlay's identity. It says what the scan is doing,
 * carries the one control a running scan needs, and is what a finger drags. Everything else is a
 * tap away in `SheetContent`, which the window opens beside it.
 */
@Composable
fun OverlayContent(
    onOpen: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    scanning: Scanning = koinInject(),
    chosen: Chosen = koinInject(),
) {
    val context = LocalContext.current
    val state by scanning.state.collectAsStateWithLifecycle()
    val kind by chosen.kind.collectAsStateWithLifecycle()

    Capsule(
        state = shownFor(state),
        kind = kind,
        onOpen = onOpen,
        onStop = { CaptureService.stopScan(context) },
        onView = { context.openApp() },
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures(onDragStart = { onDragStart() }, onDragEnd = onDragEnd, onDragCancel = onDragEnd) { change, dragged ->
                change.consume()
                onDrag(dragged.x, dragged.y)
            }
        },
    )
}

/** An ended scan is shown for a while, then the capsule is idle again; what the service holds stays ended. */
@Composable
private fun shownFor(state: ScanState): ScanState {
    var dismissed by remember(state) { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state is ScanState.Ended) {
            delay(ENDED_SHOWN_MS)
            dismissed = true
        }
    }

    return if (dismissed) ScanState.Idle else state
}

/**
 * What the capsule's tap opens, in a window of its own so that opening it never resizes the
 * capsule's: a window that grows under a finger is a window that flickers.
 */
@Composable
fun SheetContent(
    onDone: () -> Unit,
    scanning: Scanning = koinInject(),
    chosen: Chosen = koinInject(),
    permissions: Permissions = koinInject(),
) {
    val context = LocalContext.current
    val state by scanning.state.collectAsStateWithLifecycle()
    val kind by chosen.kind.collectAsStateWithLifecycle()
    val grants by permissions.grants.collectAsStateWithLifecycle()

    Sheet(
        kind = kind,
        running = state is ScanState.Running,
        touch = grants[Permission.ACCESSIBILITY],
        onChoose = chosen::choose,
        onScan = {
            onDone()
            CaptureService.scan(context, kind)
        },
        onRead = {
            onDone()
            CaptureService.read(context, kind)
        },
        onTouch = {
            context.startActivity(accessibilitySettings())
            onDone()
        },
        onApp = {
            context.openApp()
            onDone()
        },
        onClose = { CaptureService.stop(context) },
    )
}

/**
 * Idle and capturing differ by rhythm, not by size or colour: a still dot beside the words that say
 * what the menu's Scan will read, or a breathing one beside the count. The window is the touch
 * target, 44dp tall whatever the capsule paints.
 */
@Composable
internal fun Capsule(state: ScanState, kind: Kind, onOpen: () -> Unit, onStop: () -> Unit, onView: () -> Unit, modifier: Modifier) {
    Box(
        modifier.height(TARGET).pointerInput(Unit) { detectTapGestures { onOpen() } },
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            ScanState.Idle -> Row(
                Modifier.pill(IDLE, Colors.glassAccentEdge).padding(horizontal = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(5.dp).background(Colors.accent, CircleShape))
                Text(stringResource(R.string.overlay_scan_kind, stringResource(kind.label)).uppercase(), style = Lettering.word, color = Colors.text)
            }

            is ScanState.Running -> Row(
                Modifier.pill(CAPTURING, Colors.glassEdge).padding(start = 10.dp, end = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Breathing()
                Text("${state.progress.done}", style = Lettering.dataSmall, color = Colors.text)
                Text(stringResource(R.string.overlay_held, state.progress.held), style = Lettering.dataSmall, color = Colors.faint)
                Stop(onStop)
            }

            is ScanState.Ended -> Row(
                Modifier.pill(ENDED, Colors.glassEdge).padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, Modifier.size(12.dp), tint = Colors.accent)
                Text(stringResource(R.string.overlay_kept, state.outcome.entries.size), style = Lettering.dataSmall, color = Colors.text)
                Text(
                    stringResource(R.string.overlay_view),
                    Modifier.requiredHeight(TARGET).wrapContentHeight().clickable(interactionSource = null, indication = null, onClick = onView),
                    style = Lettering.dataSmall,
                    color = Colors.accent,
                )
            }
        }
    }
}

private fun Modifier.pill(height: Dp, edge: Color): Modifier =
    height(height).background(Colors.glass, CircleShape).border(1.dp, edge, CircleShape)

/**
 * The scan is alive: the dot fades and shrinks and comes back, at the pace of a slow breath. Drawn
 * by colour and radius rather than `alpha` and `scale`: an overlay window on Android 9 has no
 * canvas for the layer those would make.
 */
@Composable
private fun Breathing() {
    val breath = rememberInfiniteTransition(label = "breath")
    val taken by breath.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BREATH_MS, easing = EaseInOut), RepeatMode.Reverse),
        label = "breath",
    )

    Box(
        Modifier.size(6.dp).drawBehind {
            drawCircle(Colors.accent.copy(alpha = 1f - taken * 0.65f), radius = size.minDimension / 2 * (1f - taken * 0.18f))
        },
    )
}

/** Painted 18dp, touched at 44: the target overflows its slot into the window's own height. */
@Composable
private fun Stop(onStop: () -> Unit) {
    Box(Modifier.size(STOP), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .requiredSize(TARGET)
                .clickable(interactionSource = null, indication = null, onClickLabel = stringResource(R.string.overlay_stop), onClick = onStop),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(STOP).border(1.dp, Colors.glassEdgeStrong, RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(6.dp).background(Colors.text))
            }
        }
    }
}

@Composable
internal fun Sheet(
    kind: Kind,
    running: Boolean,
    /** Where the accessibility service stands: a scan cannot tap without it, and Scan then leads to its settings. */
    touch: Standing,
    onChoose: (Kind) -> Unit,
    onScan: () -> Unit,
    onRead: () -> Unit,
    onTouch: () -> Unit,
    onApp: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .width(SHEET)
            .background(Colors.glassSolid, RoundedCornerShape(10.dp))
            .border(1.dp, Colors.edge, RoundedCornerShape(10.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            stringResource(R.string.overlay_what).uppercase(),
            Modifier.padding(start = 9.dp, top = 6.dp, end = 9.dp, bottom = 2.dp),
            style = Lettering.mark,
            color = Colors.faint,
        )
        Kinds(kind, onChoose)
        Text(
            stringResource(kind.reminder),
            Modifier.padding(start = 9.dp, top = 3.dp, end = 9.dp, bottom = 5.dp),
            style = Lettering.caption,
            color = Colors.muted,
        )
        when {
            running -> Action(label = stringResource(R.string.overlay_scan), said = stringResource(R.string.overlay_running), onClick = onScan)
            touch != Standing.ON -> Action(
                label = stringResource(R.string.overlay_scan),
                said = stringResource(if (touch == Standing.STALLED) R.string.overlay_touch_stalled else R.string.overlay_touch_off),
                colour = Colors.muted,
                saidColour = Colors.warning,
                onClick = onTouch,
            )
            else -> Lead(stringResource(R.string.overlay_scan), stringResource(kind.label).lowercase(), onScan)
        }
        Action(label = stringResource(R.string.overlay_read), onClick = onRead)
        Action(label = stringResource(R.string.overlay_app), onClick = onApp)
        Box(Modifier.padding(horizontal = 9.dp, vertical = 3.dp).fillMaxWidth().height(1.dp).background(Colors.hairline))
        Action(label = stringResource(R.string.overlay_close), colour = Colors.muted, onClick = onClose)
    }
}

/** What Scan and Read take, the chosen kind lit; a choice lasts until the next. */
@Composable
private fun Kinds(kind: Kind, onChoose: (Kind) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Kind.entries.forEach { each ->
            val chosen = each == kind
            Text(
                stringResource(each.label),
                Modifier
                    .weight(1f)
                    .background(if (chosen) Colors.glassWash else Color.Transparent, RoundedCornerShape(6.dp))
                    .border(1.dp, if (chosen) Colors.glassAccentEdge else Colors.hairline, RoundedCornerShape(6.dp))
                    .clickable(interactionSource = null, indication = null, onClick = { onChoose(each) })
                    .padding(vertical = 7.dp),
                style = Lettering.mark,
                color = if (chosen) Colors.accent else Colors.muted,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/** Scan, the line the menu leads with: on the accent, with the kind it will read on its right. */
@Composable
private fun Lead(label: String, kind: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Colors.accent, RoundedCornerShape(6.dp))
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Lettering.label, color = Colors.onAccent)
        Text(kind, style = Lettering.mark, color = Colors.onAccent.copy(alpha = 0.7f))
    }
}

/** A line of the sheet; one that is under way, or cannot be, says so on its right, and is the way to stop it or to what it needs. */
@Composable
private fun Action(label: String, said: String? = null, colour: Color = Colors.text, saidColour: Color = Colors.accent, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (said == null) Color.Transparent else Colors.glassWash, RoundedCornerShape(6.dp))
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Lettering.label, color = colour)
        if (said != null) Text(said, style = Lettering.mark, color = saidColour)
    }
}

private fun android.content.Context.openApp() =
    startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

/** The least a finger is given, whatever is painted inside it. */
internal val TARGET = 44.dp
internal val IDLE = 24.dp
private val CAPTURING = 26.dp
private val ENDED = 24.dp
private val STOP = 18.dp
internal val SHEET = 220.dp
private const val BREATH_MS = 850
private const val ENDED_SHOWN_MS = 8_000L

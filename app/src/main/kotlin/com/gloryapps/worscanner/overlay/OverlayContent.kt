package com.gloryapps.worscanner.overlay

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.app.MainActivity
import com.gloryapps.worscanner.capture.CaptureService
import com.gloryapps.worscanner.capture.ReadScreen
import com.gloryapps.worscanner.scan.Chosen
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scan.Scanning
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Lettering
import kotlinx.coroutines.launch
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
) {
    val context = LocalContext.current
    val state by scanning.state.collectAsStateWithLifecycle()

    Capsule(
        state = state,
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

/**
 * What the capsule's tap opens, in a window of its own so that opening it never resizes the
 * capsule's: a window that grows under a finger is a window that flickers.
 */
@Composable
fun SheetContent(
    onDone: () -> Unit,
    readScreen: ReadScreen = koinInject(),
    scanning: Scanning = koinInject(),
    chosen: Chosen = koinInject(),
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val state by scanning.state.collectAsStateWithLifecycle()
    val kind by chosen.kind.collectAsStateWithLifecycle(Chosen.FIRST)

    Sheet(
        running = state is ScanState.Running,
        onScan = {
            if (state is ScanState.Running) CaptureService.stopScan(context) else CaptureService.scan(context, kind)
            onDone()
        },
        onRead = {
            onDone()
            scope.launch {
                val said = readScreen.now(kind).fold(
                    onSuccess = { resources.getString(R.string.overlay_read_kept, it.kept.stamp, it.lines) },
                    onFailure = { resources.getString(R.string.overlay_read_failed, it.message) },
                )
                Toast.makeText(context, said, Toast.LENGTH_LONG).show()
            }
        },
        onApp = { context.openApp() },
        onClose = { CaptureService.stop(context) },
    )
}

@Composable
internal fun Capsule(state: ScanState, onOpen: () -> Unit, onStop: () -> Unit, onView: () -> Unit, modifier: Modifier) {
    val tap = modifier
        .pointerInput(Unit) { detectTapGestures { onOpen() } }
        .background(Colors.glass, CircleShape)
        .border(1.dp, Colors.glassEdge, CircleShape)

    when (val held = state) {
        ScanState.Idle -> Box(tap.size(IDLE), contentAlignment = Alignment.Center) {
            Box(Modifier.size(4.dp).background(Colors.accent, CircleShape))
        }

        /* The count sits in the top band or a corner, never over the header region the scan reads it from. */
        is ScanState.Running -> Row(
            tap.height(HEIGHT).padding(start = 10.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Breathing()
            Text("${held.progress.done}", style = Lettering.dataSmall, color = Colors.text)
            Text(stringResource(R.string.overlay_held, held.progress.held), style = Lettering.dataSmall, color = Colors.faint)
            Stop(onStop)
        }

        is ScanState.Ended -> Row(
            tap.height(HEIGHT).padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Check, contentDescription = null, Modifier.size(12.dp), tint = Colors.accent)
            Text(stringResource(R.string.overlay_kept, held.outcome.entries.size), style = Lettering.dataSmall, color = Colors.text)
            Text(
                stringResource(R.string.overlay_view),
                Modifier.clickable(onClick = onView),
                style = Lettering.dataSmall,
                color = Colors.accent,
            )
        }
    }
}

/** The scan is alive: the dot fades and shrinks and comes back, at the pace of a slow breath. */
@Composable
private fun Breathing() {
    val breath = rememberInfiniteTransition(label = "breath")
    val taken by breath.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "breath",
    )

    Box(Modifier.size(6.dp).alpha(1f - taken * 0.65f).scale(1f - taken * 0.18f).background(Colors.accent, CircleShape))
}

@Composable
private fun Stop(onStop: () -> Unit) {
    Box(
        Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(5.dp))
            .border(1.dp, Colors.edge, RoundedCornerShape(5.dp))
            .clickable(onClickLabel = stringResource(R.string.overlay_stop), onClick = onStop),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(6.dp).background(Colors.text))
    }
}

@Composable
internal fun Sheet(
    running: Boolean,
    onScan: () -> Unit,
    onRead: () -> Unit,
    onApp: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .width(SHEET)
            .background(Colors.glass, RoundedCornerShape(10.dp))
            .border(1.dp, Colors.glassEdge, RoundedCornerShape(10.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Action(
            label = stringResource(R.string.overlay_scan),
            said = if (running) stringResource(R.string.overlay_running) else null,
            onClick = onScan,
        )
        Action(label = stringResource(R.string.overlay_read), onClick = onRead)
        Action(label = stringResource(R.string.overlay_app), onClick = onApp)
        Box(Modifier.padding(horizontal = 9.dp, vertical = 3.dp).fillMaxWidth().height(1.dp).background(Colors.hairline))
        Action(label = stringResource(R.string.overlay_close), colour = Colors.muted, onClick = onClose)
    }
}

/** A line of the sheet; the one that is under way says so on its right and is the way to stop it. */
@Composable
private fun Action(label: String, said: String? = null, colour: Color = Colors.text, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (said == null) Color.Transparent else Colors.accentWash)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Lettering.caption, color = colour)
        if (said != null) Text(said, style = Lettering.dataSmall, color = Colors.accent)
    }
}

private fun android.content.Context.openApp() =
    startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

private val IDLE = 16.dp
private val HEIGHT = 24.dp
private val SHEET = 180.dp

package com.gloryapps.worscanner.overlay

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.ui.label
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scan.Scanning
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * The floating strip over the game; the grip drags it.
 *
 * Idle, it scans the chosen kind's storage, reads one screen as that kind, goes back to the app or
 * closes the session; the choice shows only once there is more than one kind. While a scan runs it
 * shows how far the scan is and the one thing left to do, stop it.
 */
@Composable
fun OverlayContent(
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    readScreen: ReadScreen = koinInject(),
    scanning: Scanning = koinInject(),
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val state by scanning.state.collectAsStateWithLifecycle()
    var kind by remember { mutableStateOf(Kind.entries.first()) }

    MaterialTheme {
        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 4.dp) {
            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = stringResource(R.string.overlay_drag),
                    Modifier
                        .size(32.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { onDragStart() },
                                onDragEnd = onDragEnd,
                                onDragCancel = onDragEnd,
                            ) { change, dragged ->
                                change.consume()
                                onDrag(dragged.x, dragged.y)
                            }
                        },
                )
                when (val held = state) {
                    is ScanState.Running -> {
                        /* Worded without a slash, so the count region can never take it for the header's. */
                        Text(
                            stringResource(R.string.overlay_scanning, stringResource(held.kind.label), held.progress.done, held.progress.held),
                            Modifier.padding(horizontal = 8.dp),
                        )
                        Button(onClick = { CaptureService.stopScan(context) }) { Text(stringResource(R.string.overlay_stop)) }
                    }
                    else -> {
                        if (Kind.entries.size > 1) {
                            Kind.entries.forEach { each ->
                                FilterChip(selected = each == kind, onClick = { kind = each }, label = { Text(stringResource(each.label)) }, modifier = Modifier.padding(horizontal = 2.dp))
                            }
                        }
                        Button(onClick = { CaptureService.scan(context, kind) }) { Text(stringResource(R.string.overlay_scan)) }
                        TextButton(onClick = {
                            scope.launch {
                                val said = readScreen.now(kind).fold(
                                    onSuccess = { resources.getString(R.string.overlay_read_kept, it.kept.stamp, it.lines) },
                                    onFailure = { resources.getString(R.string.overlay_read_failed, it.message) },
                                )
                                Toast.makeText(context, said, Toast.LENGTH_LONG).show()
                            }
                        }) { Text(stringResource(R.string.overlay_read)) }
                        TextButton(onClick = {
                            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }) { Text(stringResource(R.string.overlay_app)) }
                        TextButton(onClick = { CaptureService.stop(context) }) { Text(stringResource(R.string.overlay_close)) }
                    }
                }
            }
        }
    }
}

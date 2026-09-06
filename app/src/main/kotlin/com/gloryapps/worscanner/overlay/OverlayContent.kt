package com.gloryapps.worscanner.overlay

import android.widget.Toast
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.capture.CaptureService
import com.gloryapps.worscanner.capture.ReadScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** The floating buttons over the game: read what is on screen, or end the session; the grip drags them. */
@Composable
fun OverlayContent(onDrag: (dx: Float, dy: Float) -> Unit, readScreen: ReadScreen = koinInject()) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 4.dp) {
            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = stringResource(R.string.overlay_drag),
                    Modifier
                        .size(32.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragged ->
                                change.consume()
                                onDrag(dragged.x, dragged.y)
                            }
                        },
                )
                Button(onClick = {
                    scope.launch {
                        val said = readScreen.now().fold(
                            onSuccess = { resources.getString(R.string.overlay_read_kept, it.kept.stamp, it.lines) },
                            onFailure = { resources.getString(R.string.overlay_read_failed, it.message) },
                        )
                        Toast.makeText(context, said, Toast.LENGTH_LONG).show()
                    }
                }) { Text(stringResource(R.string.overlay_read)) }
                TextButton(onClick = { CaptureService.stop(context) }) {
                    Text(stringResource(R.string.overlay_stop))
                }
            }
        }
    }
}

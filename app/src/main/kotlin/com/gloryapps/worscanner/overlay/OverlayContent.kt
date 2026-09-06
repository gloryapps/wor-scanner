package com.gloryapps.worscanner.overlay

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.capture.BitmapFrame
import com.gloryapps.worscanner.capture.CaptureService
import com.gloryapps.worscanner.capture.CaptureSession
import com.gloryapps.worscanner.capture.Captures
import com.gloryapps.worscanner.scanner.resultOf
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** The floating buttons over the game: keep one frame, or end the session. */
@Composable
fun OverlayContent(
    session: CaptureSession = koinInject(),
    captures: Captures = koinInject(),
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 4.dp) {
            Row(Modifier.padding(4.dp)) {
                Button(onClick = {
                    scope.launch {
                        val outcome = resultOf {
                            val frame = checkNotNull(session.screen.value).capture() as BitmapFrame
                            captures.keep(frame)
                        }
                        val said = outcome.fold(
                            onSuccess = { resources.getString(R.string.overlay_kept, it.name) },
                            onFailure = { resources.getString(R.string.overlay_capture_failed, it.message) },
                        )
                        Toast.makeText(context, said, Toast.LENGTH_SHORT).show()
                    }
                }) { Text(stringResource(R.string.overlay_capture)) }
                TextButton(onClick = { CaptureService.stop(context) }) {
                    Text(stringResource(R.string.overlay_stop))
                }
            }
        }
    }
}

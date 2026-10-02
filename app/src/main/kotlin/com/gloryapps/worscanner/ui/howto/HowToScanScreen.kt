package com.gloryapps.worscanner.ui.howto

import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.ui.Close
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Rule

/** A whole scan as a player sees it, from Start to Sent: it plays at once and over again, and a tap pauses it. */
@Composable
internal fun HowToScanScreen(onClose: () -> Unit) {
    var playing by rememberSaveable { mutableStateOf(true) }
    val description = stringResource(R.string.howto_video)

    Column(Modifier.fillMaxSize().background(Colors.screen).safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().height(48.dp).padding(start = 16.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.howto_title), style = Lettering.title, color = Colors.text)
            Close(onClose)
        }
        Rule()
        Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.aspectRatio(VIDEO), contentAlignment = Alignment.Center) {
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoURI("android.resource://${context.packageName}/${R.raw.how_to_scan}".toUri())
                            setOnPreparedListener { it.isLooping = true }
                            contentDescription = description
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { if (playing) it.start() else it.pause() },
                    onRelease = { it.stopPlayback() },
                )
                /* Over the video, so the tap reaches Compose rather than the view underneath. */
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(onClickLabel = stringResource(if (playing) R.string.howto_pause else R.string.howto_play)) { playing = !playing },
                    contentAlignment = Alignment.Center,
                ) {
                    if (!playing) {
                        Box(Modifier.size(64.dp).background(Colors.glassSolid, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, Modifier.size(34.dp), tint = Colors.text)
                        }
                    }
                }
            }
        }
    }
}

/** The video's width over its height: LDPlayer's 16:9 window. */
private const val VIDEO = 16f / 9f

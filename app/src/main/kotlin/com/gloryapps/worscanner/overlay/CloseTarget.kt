package com.gloryapps.worscanner.overlay

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Lettering

/**
 * What appears at the foot of the screen while the capsule is held, and closes the session when the
 * capsule is let go on it. It grows and takes the accent once the capsule is over it, so the finger
 * is told before it lifts.
 */
@Composable
fun CloseTarget(over: Boolean) {
    val size by animateDpAsState(if (over) CloseTarget.OVER_DP.dp else CloseTarget.SIZE_DP.dp, label = "target")

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            Modifier
                .size(CloseTarget.OVER_DP.dp + RING)
                .background(if (over) Colors.glassWash else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(size)
                    .background(Colors.glassThin, CircleShape)
                    .border(if (over) 2.dp else 1.dp, if (over) Colors.accent else Colors.glassEdgeStrong, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.overlay_close),
                    Modifier.size(16.dp),
                    tint = if (over) Colors.accent else Colors.text,
                )
            }
        }
        Text(
            stringResource(R.string.overlay_drop),
            Modifier
                .background(Colors.glass, CircleShape)
                .border(1.dp, if (over) Colors.glassAccentEdge else Colors.glassEdge, CircleShape)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            style = Lettering.dataSmall,
            color = if (over) Colors.accent else Colors.text,
        )
    }
}

object CloseTarget {
    const val SIZE_DP = 44
    const val OVER_DP = 52
}

/** How far the ring around the target reaches past it once the capsule is over it. */
private val RING = 6.dp

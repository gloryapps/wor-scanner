package com.gloryapps.worscanner.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gloryapps.worscanner.R

/** The circle with an X at the foot of the screen that a dragged strip is let go on to close. */
@Composable
fun CloseTarget() {
    MaterialTheme {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer, tonalElevation = 6.dp) {
            Box(Modifier.size(CloseTarget.SIZE_DP.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.overlay_close), Modifier.size(36.dp))
            }
        }
    }
}

object CloseTarget {
    const val SIZE_DP = 72
}

package com.gloryapps.worscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gloryapps.worscanner.ui.resources.Res
import com.gloryapps.worscanner.ui.resources.cancel
import org.jetbrains.compose.resources.stringResource

/** What is asked before something is gone for good: what will go, and the two ways out of the question. */
@Composable
fun Confirm(title: String, said: String, confirm: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Question(420.dp, onDismiss = onCancel) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = Lettering.title, color = Colors.text)
            Text(said, style = Lettering.body, color = Colors.muted)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        ) {
            Edged(stringResource(Res.string.cancel), onClick = onCancel)
            Destructive(confirm, onClick = onConfirm)
        }
    }
}

/** A card over the dimmed screen that waits for an answer; dismissing it is `onDismiss`. */
@Composable
fun Question(width: Dp, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(16.dp)
                .widthIn(max = width)
                .clip(RoundedCornerShape(12.dp))
                .background(Colors.raised)
                .border(1.dp, Colors.edge, RoundedCornerShape(12.dp))
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

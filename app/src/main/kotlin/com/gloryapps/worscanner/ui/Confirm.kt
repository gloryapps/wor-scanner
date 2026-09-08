package com.gloryapps.worscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gloryapps.worscanner.R

/** What is asked before something is gone for good: what will go, and the two ways out of the question. */
@Composable
fun Confirm(title: String, said: String, confirm: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Colors.raised)
                .border(1.dp, Colors.hairline, RoundedCornerShape(12.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = Lettering.title, color = Colors.text)
                Text(said, style = Lettering.body, color = Colors.muted)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                Edged(stringResource(R.string.cancel), onClick = onCancel)
                Destructive(confirm, onClick = onConfirm)
            }
        }
    }
}

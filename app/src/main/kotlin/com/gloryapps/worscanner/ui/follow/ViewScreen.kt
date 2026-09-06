package com.gloryapps.worscanner.ui.follow

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.capture.Readings
import com.gloryapps.worscanner.ui.Bar
import org.koin.compose.koinInject

@Composable
fun ViewScreen(stamp: String, onBack: () -> Unit, readings: Readings = koinInject()) {
    val text = remember(stamp) { readings.list().firstOrNull { it.stamp == stamp }?.let(readings::text) ?: "" }

    Scaffold(topBar = { Bar(stringResource(R.string.view_title), onBack) }) { padding ->
        Text(
            text,
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        )
    }
}

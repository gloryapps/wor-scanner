package com.gloryapps.worscanner.ui.follow

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.capture.Kept
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FollowScreen(viewModel: FollowViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(state.said) {
        state.said?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.heard()
        }
    }

    /* Android 9 asks for the storage permission before Downloads takes a file; the save waits for the answer. */
    var pending by remember { mutableStateOf<Kept?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pending?.takeIf { granted }?.let(viewModel::saveToDownloads)
        pending = null
    }

    fun save(kept: Kept) {
        if (viewModel.downloadsNeedPermission) {
            pending = kept
            permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.saveToDownloads(kept)
        }
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.follow_title), style = MaterialTheme.typography.headlineSmall)

            if (state.readings.isEmpty()) {
                Text(stringResource(R.string.follow_empty))
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.readings, key = { it.stamp }) { kept ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(kept.stamp, Modifier.padding(top = 12.dp))
                        Row {
                            TextButton(onClick = { context.startActivity(viewModel.shareIntent(kept)) }) {
                                Text(stringResource(R.string.follow_share))
                            }
                            Button(onClick = { save(kept) }) { Text(stringResource(R.string.follow_download)) }
                        }
                    }
                }
            }
        }
    }
}

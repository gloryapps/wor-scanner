package com.gloryapps.worscanner.ui.follow

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FollowScreen() {
    Scaffold { padding ->
        Text("No scan yet", Modifier.fillMaxSize().padding(padding))
    }
}

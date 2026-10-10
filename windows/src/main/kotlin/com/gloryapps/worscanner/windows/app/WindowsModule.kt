package com.gloryapps.worscanner.windows.app

import com.gloryapps.worscanner.scanner.azhor.HttpAzhorApi
import com.gloryapps.worscanner.scanner.azhor.Link
import com.gloryapps.worscanner.scanner.azhor.Sender
import com.gloryapps.worscanner.scanner.runs.Accounts
import com.gloryapps.worscanner.scanner.runs.Reports
import com.gloryapps.worscanner.ui.Built
import com.gloryapps.worscanner.windows.azhor.FileTokenStore
import com.gloryapps.worscanner.windows.game.GameWatch
import com.gloryapps.worscanner.windows.home.HomeViewModel
import com.gloryapps.worscanner.windows.report.LogReports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.io.File

/** `%LOCALAPPDATA%\WoR Scanner`: the app's own data, out of the player's way; a developer's home elsewhere. */
private val folder = File(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), Built.NAME).apply { mkdirs() }

internal val windowsModule = module {
    single { GameWatch(CoroutineScope(SupervisorJob())) }
    single<Reports> { LogReports(File(folder, "wor-scanner.log")) }
    single { Accounts(folder) }
    single { Link(HttpAzhorApi(Built.AZHOR_URL), FileTokenStore(File(folder, "link"))) }
    factory { Sender(get()) }
    viewModel { HomeViewModel(get(), get(), get(), get()) }
}

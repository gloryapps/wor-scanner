package com.gloryapps.worscanner.app

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.gloryapps.worscanner.BuildConfig
import com.gloryapps.worscanner.azhor.PreferencesTokenStore
import com.gloryapps.worscanner.azhor.site
import com.gloryapps.worscanner.capture.BitmapPictures
import com.gloryapps.worscanner.capture.CaptureSession
import com.gloryapps.worscanner.capture.Exports
import com.gloryapps.worscanner.capture.MlKitTextReader
import com.gloryapps.worscanner.scanner.runs.ReadScreen
import com.gloryapps.worscanner.scanner.runs.Readings
import com.gloryapps.worscanner.report.CrashReports
import com.gloryapps.worscanner.scan.HeldSenses
import com.gloryapps.worscanner.scanner.azhor.Link
import com.gloryapps.worscanner.scanner.azhor.Sender
import com.gloryapps.worscanner.scanner.runs.Reports
import com.gloryapps.worscanner.scanner.senses.Senses
import com.gloryapps.worscanner.scanner.senses.TextReader
import com.gloryapps.worscanner.ui.ExportDelegate
import com.gloryapps.worscanner.ui.earlier.EarlierViewModel
import com.gloryapps.worscanner.ui.firstrun.FirstRun
import com.gloryapps.worscanner.ui.firstrun.GrantsViewModel
import com.gloryapps.worscanner.ui.home.HomeViewModel
import com.gloryapps.worscanner.ui.Permissions
import com.gloryapps.worscanner.ui.reading.ReadingViewModel
import com.gloryapps.worscanner.scan.Chosen
import com.gloryapps.worscanner.scanner.runs.Scanning
import com.gloryapps.worscanner.scan.TouchState
import com.gloryapps.worscanner.update.GitHubReleases
import com.gloryapps.worscanner.update.Updates
import com.gloryapps.worscanner.update.Version
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.io.File

val appModule = module {
    /* A dev build crashes on the desk, in front of logcat; only releases report. */
    single<Reports>(createdAtStart = true) { CrashReports(FirebaseCrashlytics.getInstance(), collecting = !BuildConfig.DEBUG) }
    single { TouchState() }
    single { CaptureSession() }
    single<Senses> { HeldSenses(get(), get()) }
    /* Made at start, so the scans the last process died in the middle of are closed before any other begins. */
    single(createdAtStart = true) { Readings(androidContext().getExternalFilesDir(null) ?: androidContext().filesDir, BitmapPictures(), get()) }
    single { Exports(androidContext()) }
    single<TextReader> { MlKitTextReader() }
    factory { ReadScreen(get(), get(), get(), get()) }
    single { Scanning(get(), get(), get(), get()) }
    /* Read from the store at start, so the overlay's menu opens on the kind already chosen. */
    single(createdAtStart = true) { Chosen(androidContext()) }
    single { Permissions(androidContext(), get()) }
    single { FirstRun(androidContext(), get()) }
    factory { ExportDelegate(get(), androidContext(), get()) }
    single { Link(site(), PreferencesTokenStore(androidContext())) }
    factory { Sender(get()) }
    single { Updates(GitHubReleases(File(androidContext().cacheDir, "updates")), Version.of(BuildConfig.VERSION_NAME)) }
    viewModel { params -> HomeViewModel(params.getOrNull(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { EarlierViewModel(get(), get(), get(), get()) }
    viewModel { GrantsViewModel(get(), get()) }
    viewModel { (stamp: String) -> ReadingViewModel(stamp, get(), get()) }
}

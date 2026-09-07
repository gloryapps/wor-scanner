package com.gloryapps.worscanner.app

import com.gloryapps.worscanner.capture.CaptureSession
import com.gloryapps.worscanner.capture.Exports
import com.gloryapps.worscanner.capture.MlKitTextReader
import com.gloryapps.worscanner.capture.ReadScreen
import com.gloryapps.worscanner.capture.Readings
import com.gloryapps.worscanner.scanner.senses.TextReader
import com.gloryapps.worscanner.ui.follow.FollowViewModel
import com.gloryapps.worscanner.ui.prepare.Permissions
import com.gloryapps.worscanner.ui.prepare.PrepareViewModel
import com.gloryapps.worscanner.scan.Scanning
import com.gloryapps.worscanner.scan.TouchState
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { TouchState() }
    single { CaptureSession() }
    single { Readings(androidContext()) }
    single { Exports(androidContext()) }
    single<TextReader> { MlKitTextReader() }
    factory { ReadScreen(get(), get(), get()) }
    single { Scanning(androidContext(), get(), get(), get()) }
    single { Permissions(androidContext()) }
    viewModel { PrepareViewModel(get(), get()) }
    viewModel { FollowViewModel(get(), get()) }
}

package com.gloryapps.worscanner.app

import com.gloryapps.worscanner.capture.CaptureSession
import com.gloryapps.worscanner.capture.Captures
import com.gloryapps.worscanner.ui.prepare.Permissions
import com.gloryapps.worscanner.ui.prepare.PrepareViewModel
import com.gloryapps.worscanner.walk.TouchState
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { TouchState() }
    single { CaptureSession() }
    single { Captures(androidContext()) }
    single { Permissions(androidContext()) }
    viewModel { PrepareViewModel(get(), get(), get()) }
}

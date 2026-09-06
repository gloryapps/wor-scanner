package com.gloryapps.worscanner.app

import com.gloryapps.worscanner.ui.prepare.PrepareViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { PrepareViewModel() }
}

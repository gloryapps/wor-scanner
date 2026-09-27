package com.gloryapps.worscanner.ui

import androidx.annotation.StringRes
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.capture.Emulator

/** The one place the app names an emulator: what the export sheet calls its folder. */
val Emulator.label: Int
    @StringRes get() = when (this) {
        Emulator.LD_PLAYER -> R.string.emulator_ldplayer
        Emulator.BLUE_STACKS -> R.string.emulator_bluestacks
    }

/** Where the PC shows that folder, which is the whole point of saving into it. */
val Emulator.onPc: Int
    @StringRes get() = when (this) {
        Emulator.LD_PLAYER -> R.string.emulator_ldplayer_pc
        Emulator.BLUE_STACKS -> R.string.emulator_bluestacks_pc
    }

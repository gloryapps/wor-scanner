package com.gloryapps.worscanner.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.capture.Emulator
import com.gloryapps.worscanner.capture.SharedFolder

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

/** What a Save button says where it lands: `Save → LDPlayer`. */
@Composable
fun savingInto(folder: SharedFolder): String =
    "${stringResource(R.string.export_save)} ${stringResource(R.string.export_to, stringResource(folder.emulator.label))}"

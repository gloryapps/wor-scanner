package com.gloryapps.worscanner.scanner.kinds.gear

import com.gloryapps.worscanner.scanner.kinds.Scannable

/** Gear's one door for the scanner: its storage, its record and its reader. */
object GearScannable : Scannable<ScannedGear> {
    override val layout = GEAR_STORAGE
    override val serializer = ScannedGear.serializer()
    override val reader = GearReader
}

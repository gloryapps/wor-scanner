package com.gloryapps.worscanner.scanner.scan

import com.gloryapps.worscanner.scanner.kinds.gear.GearScannable
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.kinds.Scannable

/** The one place every kind is named. */
fun Kind.scannable(): Scannable<*> = when (this) {
    Kind.GEAR -> GearScannable
}

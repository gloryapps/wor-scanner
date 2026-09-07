package com.gloryapps.worscanner.scanner.scan

import com.gloryapps.worscanner.scanner.gear.GearScannable
import com.gloryapps.worscanner.scanner.kind.Kind
import com.gloryapps.worscanner.scanner.kind.Scannable

/** The one place every kind is named. */
fun Kind.scannable(): Scannable<*> = when (this) {
    Kind.GEAR -> GearScannable
}

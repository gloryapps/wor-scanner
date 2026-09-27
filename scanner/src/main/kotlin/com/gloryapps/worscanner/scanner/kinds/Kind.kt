package com.gloryapps.worscanner.scanner.kinds

import com.gloryapps.worscanner.scanner.kinds.artifact.ArtifactScan
import com.gloryapps.worscanner.scanner.kinds.gear.GearScan
import com.gloryapps.worscanner.scanner.kinds.hero.HeroScan
import com.gloryapps.worscanner.scanner.scan.Scan

/** What the app can scan. The name, lower-cased, is the JSON's `kind` and the scan folder's. */
enum class Kind {
    GEAR,
    HEROES,
    ARTIFACTS,
    ;

    val id: String get() = name.lowercase()
}

/** The one place every kind is named: how it is scanned. */
fun Kind.scan(): Scan<*> = when (this) {
    Kind.GEAR -> GearScan
    Kind.HEROES -> HeroScan
    Kind.ARTIFACTS -> ArtifactScan
}

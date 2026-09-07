package com.gloryapps.worscanner.scanner.kind

/** What the app can scan. The name, lower-cased, is the JSON's `kind` and the scan folder's. */
enum class Kind {
    GEAR,
    ;

    val id: String get() = name.lowercase()
}

package com.gloryapps.worscanner.ui.firstrun

import com.gloryapps.worscanner.ui.Grants
import com.gloryapps.worscanner.ui.Permission

/** The first run's checklist: what the system lets the app do now. */
internal data class GrantsUiState(val grants: Grants = Grants()) {
    /** The grants not on yet; none once the scanner is ready to use. */
    val left: Int get() = grants.missing.size
}

internal sealed interface GrantsEvent {

    data class Grant(val permission: Permission) : GrantsEvent

    /** Back to what the scanner does. */
    data object Back : GrantsEvent

    /** Start using the scanner, or Later: either way the first run is over, and what is still off is asked for at Start. */
    data object Finish : GrantsEvent
}

/** What the screen does once, on the system's or the stack's side, when the ViewModel says so. */
internal sealed interface GrantsEffect {

    data class Grant(val permission: Permission) : GrantsEffect

    data object NavigateBack : GrantsEffect

    data object Done : GrantsEffect
}

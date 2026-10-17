package com.gloryapps.worscanner.windows.home

import com.gloryapps.worscanner.scanner.account.ArtifactsToScan
import com.gloryapps.worscanner.scanner.account.Arrival
import com.gloryapps.worscanner.scanner.account.Enhancement
import com.gloryapps.worscanner.scanner.account.ScanChoices
import com.gloryapps.worscanner.scanner.azhor.Send
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.runs.KeptAccount
import java.io.File

/**
 * `said` is why the last scan failed, `elevate` that it failed because the game runs as administrator and the scanner does not;
 * `kept` the folder scans are kept in; `account` is the newest scan, to be sent to the lab, and `send` how sending it stands.
 */
internal data class HomeState(
    val game: Game = Game.Looking,
    val scanning: Boolean = false,
    val said: String? = null,
    val elevate: Boolean = false,
    val kept: File? = null,
    val account: AccountRead? = null,
    val linked: Boolean = false,
    val send: Send = Send.Idle,
    /** What the scan sends of the gear and artifacts, and how the lab takes it. */
    val choices: ScanChoices = ScanChoices(),
    /** What the last fight capture came to: under way, the file it was kept in, or why it failed. */
    val capture: String? = null,
    val capturing: Boolean = false,
)

/** An account scanned: the scans made of it, which are what is sent, and how many cards of each they hold. */
internal data class AccountRead(val scans: List<File>, val heroes: Int, val gear: Int, val artifacts: Int) {
    constructor(kept: KeptAccount) : this(kept.scans, kept.cards[Kind.HEROES] ?: 0, kept.cards[Kind.GEAR] ?: 0, kept.cards[Kind.ARTIFACTS] ?: 0)
}

/** The game's window as the home screen says it. */
internal sealed interface Game {
    data object Looking : Game

    data object Open : Game

    data object Closed : Game

    /** The app runs on a system with no Windows desktop to find the game on: a developer's Mac. */
    data object NotWindows : Game
}

internal sealed interface HomeEvent {
    /** Reads the whole account off the game's memory, every hero, piece and artifact at once. */
    data object Scan : HomeEvent

    data object OpenKept : HomeEvent

    data object Send : HomeEvent

    /** Links the scanner with the code typed, sending the account where one is scanned. */
    data class Link(val code: String) : HomeEvent

    data object Unlink : HomeEvent

    /** One band of enhancement in or out of the gear scan. */
    data class Toggle(val band: Enhancement) : HomeEvent

    data class ChooseArtifacts(val artifacts: ArtifactsToScan) : HomeEvent

    data class ChooseArrival(val arrival: Arrival) : HomeEvent

    /** Starts the scanner again as administrator, closing this one once Windows has. */
    data object RestartAsAdministrator : HomeEvent

    /** Starts keeping a fight's state off the game's memory for a diagnosis, or stops one under way. */
    data object CaptureFight : HomeEvent
}

internal sealed interface HomeEffect {
    data class Open(val folder: File) : HomeEffect

    data object Quit : HomeEffect
}

package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.account.Account
import com.gloryapps.worscanner.scanner.account.Cards
import com.gloryapps.worscanner.scanner.account.ScanChoices
import com.gloryapps.worscanner.scanner.account.Game
import com.gloryapps.worscanner.scanner.account.readAccount
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.kinds.artifact.ScannedArtifact
import com.gloryapps.worscanner.scanner.kinds.gear.ScannedGear
import com.gloryapps.worscanner.scanner.kinds.hero.ScannedHero
import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.scanner.scan.ScanEntry
import com.gloryapps.worscanner.scanner.senses.Memory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime

/** An account read and kept: the game's own file, what it holds, and the scans of each kind made of it, which are what the lab imports, with how many cards each holds; a kind with none has no scan. */
data class KeptAccount(val file: File, val account: Account, val scans: List<File>, val cards: Map<Kind, Int>)

/** Accounts read off the game's memory: a scan of each kind in a folder of `accounts`, the game's own file in `reads`, both named by the stamp the read began at. */
class Accounts(private val root: File, private val game: Game = Game.shipped) {
    /** Reads the account off `memory`; its gear and artifacts scans hold only what `choices` keeps. */
    suspend fun read(memory: Memory, choices: ScanChoices): KeptAccount {
        val account = readAccount(memory, startedAt = LocalDateTime.now().format(STAMP))

        return withContext(Dispatchers.IO) { keep(account, Cards(game, account, choices)) }
    }

    /* The lab takes a scan as the whole of its kind, so a kind with no cards sends none rather than one that empties the account's. */
    private fun keep(account: Account, cards: Cards): KeptAccount {
        val folder = File(root, "accounts/${account.startedAt}").apply { mkdirs() }

        return KeptAccount(
            file = File(root, "reads/${account.startedAt}.json").apply { parentFile.mkdirs(); writeText(Json.encodeToString(Account.serializer(), account)) },
            account = account,
            scans = listOfNotNull(
                write(folder, account.startedAt, Kind.GEAR, cards.gear, ScannedGear.serializer()),
                write(folder, account.startedAt, Kind.HEROES, cards.heroes, ScannedHero.serializer()),
                write(folder, account.startedAt, Kind.ARTIFACTS, cards.artifacts, ScannedArtifact.serializer()),
            ),
            cards = mapOf(Kind.GEAR to cards.gear.size, Kind.HEROES to cards.heroes.size, Kind.ARTIFACTS to cards.artifacts.size),
        )
    }

    /* A scan made of an account read no display and read every tile: no size, and finished. */
    private fun <T> write(folder: File, startedAt: String, kind: Kind, cards: List<T>, card: KSerializer<T>): File? {
        if (cards.isEmpty()) return null
        val entries = cards.mapIndexed { index, it -> ScanEntry(index, row = 0, column = 0, card = it, rows = emptyList()) }
        val scan = ScanFile(kind = kind.id, startedAt = startedAt, width = 0, height = 0, outcome = Outcome.Finished(entries).wire(), entries = entries)

        return File(folder, "${kind.id}.json").apply { writeText(scan.json(card)) }
    }
}

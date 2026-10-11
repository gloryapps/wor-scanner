package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.account.Account
import com.gloryapps.worscanner.scanner.account.Arrival
import com.gloryapps.worscanner.scanner.account.Cards
import com.gloryapps.worscanner.scanner.account.ScanChoices
import com.gloryapps.worscanner.scanner.account.Game
import com.gloryapps.worscanner.scanner.account.PLAN
import com.gloryapps.worscanner.scanner.account.ScannedRule
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

/** An account read and kept: the game's own file, what it holds, and the scans made of it, one of each kind and one of its plan, which are what the lab imports, with how many cards each kind's holds; a kind with none has no scan. */
data class KeptAccount(val file: File, val account: Account, val scans: List<File>, val cards: Map<Kind, Int>)

/** Accounts read off the game's memory: a scan of each kind in a folder of `accounts`, the game's own file in `reads`, both named by the stamp the read began at. */
class Accounts(private val root: File, private val game: Game = Game.shipped) {
    /** Reads the account off `memory`; its gear and artifacts scans hold only what `choices` keeps, and every scan arrives as `choices` says. */
    suspend fun read(memory: Memory, choices: ScanChoices): KeptAccount {
        val account = readAccount(memory, startedAt = LocalDateTime.now().format(STAMP))

        return withContext(Dispatchers.IO) { keep(account, Cards(game, account, choices), choices.arrival) }
    }

    /* A scan in place of the lab's list of its kind would empty it where it holds no cards, so a kind with none sends none. */
    private fun keep(account: Account, cards: Cards, arrival: Arrival): KeptAccount {
        val folder = File(root, "accounts/${account.startedAt}").apply { mkdirs() }

        return KeptAccount(
            file = File(root, "reads/${account.startedAt}.json").apply { parentFile.mkdirs(); writeText(Json.encodeToString(Account.serializer(), account)) },
            account = account,
            scans = listOfNotNull(
                write(folder, account.startedAt, arrival, Kind.GEAR.id, cards.gear, ScannedGear.serializer()),
                write(folder, account.startedAt, arrival, Kind.HEROES.id, cards.heroes, ScannedHero.serializer(), cards.collection),
                write(folder, account.startedAt, arrival, Kind.ARTIFACTS.id, cards.artifacts, ScannedArtifact.serializer()),
                write(folder, account.startedAt, arrival, PLAN, cards.rules, ScannedRule.serializer()),
            ),
            cards = mapOf(Kind.GEAR to cards.gear.size, Kind.HEROES to cards.heroes.size, Kind.ARTIFACTS to cards.artifacts.size),
        )
    }

    /* A scan made of an account read no display and read every tile: no size, and finished. */
    private fun <T> write(
        folder: File,
        startedAt: String,
        arrival: Arrival,
        kind: String,
        cards: List<T>,
        card: KSerializer<T>,
        collection: Map<String, Int>? = null,
    ): File? {
        if (cards.isEmpty()) return null
        val entries = cards.mapIndexed { index, it -> ScanEntry(index, row = 0, column = 0, card = it, rows = emptyList()) }
        val scan = ScanFile(
            kind = kind,
            arrival = arrival,
            startedAt = startedAt,
            width = 0,
            height = 0,
            outcome = Outcome.Finished(entries).wire(),
            collection = collection,
            entries = entries,
        )

        return File(folder, "$kind.json").apply { writeText(scan.json(card)) }
    }
}

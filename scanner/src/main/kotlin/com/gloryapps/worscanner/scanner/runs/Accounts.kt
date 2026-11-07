package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.account.Account
import com.gloryapps.worscanner.scanner.account.Arrival
import com.gloryapps.worscanner.scanner.account.Cards
import com.gloryapps.worscanner.scanner.account.ScanChoices
import com.gloryapps.worscanner.scanner.account.Game
import com.gloryapps.worscanner.scanner.account.PLAN
import com.gloryapps.worscanner.scanner.account.ScannedRule
import com.gloryapps.worscanner.scanner.account.Squads
import com.gloryapps.worscanner.scanner.account.readAccount
import com.gloryapps.worscanner.scanner.client.Client
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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime

/** An account read and kept: the scans made of it, one of each kind and one of its plan, which are what the lab imports, with how many cards each kind's holds; a kind with none has no scan. */
data class KeptAccount(val scans: List<File>, val cards: Map<Kind, Int>)

/** The squads a squad screen held, and the file they are kept in. */
data class KeptSquads(val file: File, val squads: List<Squads>)

/** Accounts read off the game's memory: a scan of each kind in a folder of `accounts`, the game's own file in `reads`, the squads a squad screen held in `squads`, each named by the stamp the read began at; the newest [KEPT] scans and reads, and every squads file, since the play the player completes in it is held nowhere else. */
class Accounts(root: File, private val game: Game = Game.shipped) {
    private val scans = File(root, "accounts")
    private val reads = File(root, "reads")
    private val squads = File(root, "squads")

    /** Reads the account off `memory`, its heroes as `client` holds them; its gear and artifacts scans hold only what `choices` keeps, and every scan arrives as `choices` says. */
    suspend fun read(memory: Memory, choices: ScanChoices, client: Client? = null): KeptAccount {
        val account = readAccount(memory, startedAt = LocalDateTime.now().format(STAMP))

        return withContext(Dispatchers.IO) {
            keep(account, Cards(game, account, choices, client), choices.arrival).also { listOf(scans, reads).forEach(::newestOnly) }
        }
    }

    /** Reads the squads the game's open squad screen holds, each hero named by `client` and with what he wears, and keeps them; null, and nothing kept, where no squad screen is open. */
    suspend fun squads(memory: Memory, client: Client? = null): KeptSquads? {
        val stamp = LocalDateTime.now().format(STAMP)
        val read = Cards(game, readAccount(memory, startedAt = stamp), ScanChoices(), client).squads
        if (read.isEmpty()) return null

        return withContext(Dispatchers.IO) {
            val file = File(squads, "$stamp.json").apply { parentFile.mkdirs() }
            keep(file, read)
            KeptSquads(file, read)
        }
    }

    /** Keeps `read` in `file` again, as the player completed it: every field written, to be read by eye. */
    suspend fun keep(file: File, read: List<Squads>) = withContext(Dispatchers.IO) { file.writeText(SQUADS_FILE.encodeToString(ListSerializer(Squads.serializer()), read)) }

    /* A scan in place of the lab's list of its kind would empty it where it holds no cards, so a kind with none sends none. */
    private fun keep(account: Account, cards: Cards, arrival: Arrival): KeptAccount {
        val folder = File(scans, account.startedAt).apply { mkdirs() }
        File(reads, "${account.startedAt}.json").apply { parentFile.mkdirs(); writeText(Json.encodeToString(Account.serializer(), account)) }

        return KeptAccount(
            scans = listOfNotNull(
                write(folder, account.startedAt, arrival, Kind.GEAR.id, cards.gear, ScannedGear.serializer()),
                write(folder, account.startedAt, arrival, Kind.HEROES.id, cards.heroes, ScannedHero.serializer(), cards.collection),
                write(folder, account.startedAt, arrival, Kind.ARTIFACTS.id, cards.artifacts, ScannedArtifact.serializer()),
                write(folder, account.startedAt, arrival, PLAN, cards.rules, ScannedRule.serializer()),
            ),
            cards = mapOf(Kind.GEAR to cards.gear.size, Kind.HEROES to cards.heroes.size, Kind.ARTIFACTS to cards.artifacts.size),
        )
    }

    /* Every read keeps the whole account again, so the oldest go; a stamp sorts as the time it names. */
    private fun newestOnly(folder: File) =
        folder.listFiles().orEmpty().sortedByDescending { it.name }.drop(KEPT).forEach { it.deleteRecursively() }

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

    companion object {
        const val KEPT = 20

        private val SQUADS_FILE = Json { encodeDefaults = true; prettyPrint = true }
    }
}

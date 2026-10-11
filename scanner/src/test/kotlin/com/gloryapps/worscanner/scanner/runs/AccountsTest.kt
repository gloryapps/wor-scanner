package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.account.Account
import com.gloryapps.worscanner.scanner.account.Arrival
import com.gloryapps.worscanner.scanner.account.ScanChoices
import com.gloryapps.worscanner.scanner.kinds.artifact.ScannedArtifact
import com.gloryapps.worscanner.scanner.kinds.hero.ScannedHero
import com.gloryapps.worscanner.scanner.lua.LaidOutHeap
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountsTest {
    private val root: File = createTempDirectory().toFile()

    @Test
    fun `an account read keeps a scan of each kind it holds in a folder named by its stamp, and the game's own file apart`() = runTest {
        val memory = LaidOutHeap().apply { table("m_vArtifacts" to table(array = listOf(table("iItemId" to 219201, "iLevel" to 25, "iStageLvl" to 5)))) }

        val kept = Accounts(root).read(memory, ScanChoices.EVERYTHING)

        val folder = File(root, "accounts/${kept.account.startedAt}")
        assertEquals(File(root, "reads/${kept.account.startedAt}.json"), kept.file)
        assertEquals(kept.account, Json.decodeFromString(Account.serializer(), kept.file.readText()))
        assertEquals(listOf(File(folder, "artifacts.json")), kept.scans)
        val artifacts = Json.decodeFromString(ScanFile.serializer(ScannedArtifact.serializer()), kept.scans.single().readText())
        assertEquals("artifacts" to kept.account.startedAt, artifacts.kind to artifacts.startedAt)
        assertEquals(listOf("Spear of Leonidas" to 6), artifacts.entries.map { it.card.name to it.card.skill })
    }

    @Test
    fun `every scan arrives as the player chose, and the heroes scan carries how many heroes of each faction the account has collected`() = runTest {
        val memory = LaidOutHeap().apply {
            table(
                "m_CharactorDatas" to table(222400000 to table("iHeroId" to 222400000, "iBaseId" to 2224, "iLevel" to 60)),
                "m_OwenHeroBaseId" to table(array = listOf(2224, 2001)),
            )
            table("m_vArtifacts" to table(array = listOf(table("iItemId" to 219201, "iLevel" to 25, "iStageLvl" to 5))))
        }

        val kept = Accounts(root).read(memory, ScanChoices.EVERYTHING.copy(arrival = Arrival.ADD))

        val heroes = Json.decodeFromString(ScanFile.serializer(ScannedHero.serializer()), kept.scans.first().readText())
        val artifacts = Json.decodeFromString(ScanFile.serializer(ScannedArtifact.serializer()), kept.scans.last().readText())
        assertEquals(listOf(Arrival.ADD, Arrival.ADD), listOf(heroes.arrival, artifacts.arrival))
        assertEquals(listOf(1, 1, 1, 0), listOf("The Supreme Arbiters", "The Star Piercers", "The Watchguard", "The North Throne").map(heroes.collection.orEmpty()::getValue))
        assertEquals(null, artifacts.collection)
    }
}

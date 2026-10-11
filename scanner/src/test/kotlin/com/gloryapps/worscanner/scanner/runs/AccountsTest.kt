package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.account.Account
import com.gloryapps.worscanner.scanner.account.ScanChoices
import com.gloryapps.worscanner.scanner.kinds.artifact.ScannedArtifact
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
}

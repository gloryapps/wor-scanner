package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.account.Account
import com.gloryapps.worscanner.scanner.account.Arrival
import com.gloryapps.worscanner.scanner.account.EVERYTHING
import com.gloryapps.worscanner.scanner.account.ScannedRule
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

        val kept = Accounts(root).read(memory, EVERYTHING)

        val stamp = kept.scans.single().parentFile.name
        assertEquals(listOf(File(root, "accounts/$stamp/artifacts.json")), kept.scans)
        val read = Json.decodeFromString(Account.serializer(), File(root, "reads/$stamp.json").readText())
        assertEquals(stamp to 1, read.startedAt to read.artifacts.size)
        val artifacts = Json.decodeFromString(ScanFile.serializer(ScannedArtifact.serializer()), kept.scans.single().readText())
        assertEquals("artifacts" to stamp, artifacts.kind to artifacts.startedAt)
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

        val kept = Accounts(root).read(memory, EVERYTHING.copy(arrival = Arrival.ADD))

        val heroes = Json.decodeFromString(ScanFile.serializer(ScannedHero.serializer()), kept.scans.first().readText())
        val artifacts = Json.decodeFromString(ScanFile.serializer(ScannedArtifact.serializer()), kept.scans.last().readText())
        assertEquals(listOf(Arrival.ADD, Arrival.ADD), listOf(heroes.arrival, artifacts.arrival))
        assertEquals(listOf(1, 1, 1, 0), listOf("The Supreme Arbiters", "The Star Piercers", "The Watchguard", "The North Throne").map(heroes.collection.orEmpty()::getValue))
        assertEquals(null, artifacts.collection)
    }

    @Test
    fun `only the newest reads are kept, each with the scans made of it`() = runTest {
        val older = (1..Accounts.KEPT).map { "20260101-0000%02d".format(it) }
        older.forEach { stamp ->
            File(root, "accounts/$stamp").mkdirs()
            File(root, "reads/$stamp.json").apply { parentFile.mkdirs(); writeText("{}") }
        }

        Accounts(root).read(LaidOutHeap(), EVERYTHING)

        val kept = File(root, "accounts").list().orEmpty().sorted()
        assertEquals(older.drop(1), kept.dropLast(1))
        assertEquals(kept.map { "$it.json" }, File(root, "reads").list().orEmpty().sorted())
    }

    @Test
    fun `the game's plan is kept as a scan of its own, each rule a card in the game's words`() = runTest {
        val memory = LaidOutHeap().apply {
            val rule = table(
                "Name" to "Crit DPS",
                "SuitType" to 2,
                "SuitMap" to table(721300L to true, 720400L to true),
                "MainAttrMap" to table(25 to true),
                "SubAttrMap" to table(1 to true, 24 to true),
                "MustAttrMap" to table(24 to true),
                "SubAttrSatisfied" to 2,
            )
            table("m_EquipRecommendRulesMap" to table(2 to table(array = listOf(rule))))
        }

        val plan = Accounts(root).read(memory, EVERYTHING).scans.single()

        assertEquals("plan.json", plan.name)
        val scan = Json.decodeFromString(ScanFile.serializer(ScannedRule.serializer()), plan.readText())
        assertEquals(Triple("plan", plan.parentFile.name, Arrival.REPLACE), Triple(scan.kind, scan.startedAt, scan.arrival))
        assertEquals(
            Json.parseToJsonElement(
                """{"name":"Crit DPS","side":"bangles-amulets-rings","sets":["fatality","the-doom"],"main":["Crit. DMG"],""" +
                    """"secondaries":{"ATK":"pool","Crit. Rate":"essential"},"match":2}""",
            ),
            Json.encodeToJsonElement(ScannedRule.serializer(), scan.entries.single().card),
        )
    }
}

package com.gloryapps.worscanner.scanner.account

import com.gloryapps.worscanner.scanner.kinds.gear.Slot
import com.gloryapps.worscanner.scanner.lua.LaidOutHeap
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SquadsTest {
    private val memory = LaidOutHeap()

    private suspend fun squads() = Cards(Game.shipped, readAccount(memory, STARTED), ScanChoices(), CLIENT).squads

    private fun slot(heroId: Long, squad: Int, lord: Any = false) =
        memory.table("m_iHeroID" to heroId, "m_iHeroShapeId" to 0, "m_iBaseID" to -1, "m_iSquadID" to squad, "m_IsLord" to lord)

    private fun hero(heroId: Long, baseId: Int) = memory.table("iHeroId" to heroId, "iBaseId" to baseId)

    private fun piece(uid: Long, itemId: Int, wearer: Long) =
        memory.table("iItemUid" to uid, "iItemId" to itemId, "iIntensifyLvl" to 0, "vMasterAttrList" to memory.table(array = listOf(memory.table("iAttrId" to 1, "iValue" to 2350))), "iHeroId" to wearer)

    @Test
    fun `the squad screen's squads are read in their slots' order, each hero named by his base id, the Lord marked`() = runTest {
        memory.table("m_CharactorDatas" to memory.table(1 to hero(222400000L, 2224), 2 to hero(200100001L, 2001)))
        memory.table(
            "m_BattleStageId" to 5001007,
            "m_TeamDataList" to memory.table(
                2 to memory.table(1 to slot(200100001L, squad = 2, lord = 106)),
                1 to memory.table(2 to slot(200200000L, squad = 1), 1 to slot(222400000L, squad = 1, lord = 101), 3 to slot(-1L, squad = 1)),
            ),
        )

        assertEquals(
            listOf(
                Squads(
                    5001007,
                    listOf(
                        listOf(SquadHero(222400000L, 2224, "Pelagios", lord = true, emptyList(), null), SquadHero(200200000L, null, null, lord = false, emptyList(), null)),
                        listOf(SquadHero(200100001L, 2001, "Rex", lord = true, emptyList(), null)),
                    ),
                ),
            ),
            squads(),
        )
    }

    @Test
    fun `each hero in a squad wears the pieces and the artifact that hold his id, whatever the scan would keep of them`() = runTest {
        memory.table("m_CharactorDatas" to memory.table(1 to hero(222400000L, 2224)))
        memory.table("suitId2EquipIdExt" to memory.table(), "equips" to memory.table(7002L to piece(7002, 100102, wearer = 222400000L), 7001L to piece(7001, 100101, wearer = 222400000L), 7003L to piece(7003, 100103, wearer = 0)))
        memory.table("m_vArtifacts" to memory.table(array = listOf(memory.table("iItemUid" to 5, "iItemId" to 200101, "iLevel" to 3, "iHeroId" to 222400000L))))
        memory.table("m_BattleStageId" to 5001007, "m_TeamDataList" to memory.table(1 to memory.table(1 to slot(222400000L, squad = 1, lord = 101))))

        val laya = squads().single().squads.single().single()

        assertEquals(listOf("juggernaut" to Slot.WEAPON, "juggernaut" to Slot.BREASTPLATE), laya.pieces.map { it.set to it.slot })
        assertEquals("Medal of Fortitude" to 3, laya.artifact?.let { it.name to it.level })
    }

    @Test
    fun `with no squad screen open there are no squads, though the game holds teams elsewhere`() = runTest {
        memory.table("m_TeamDataList" to memory.table(1 to memory.table(1 to slot(222400000L, squad = 1))))

        assertEquals(emptyList(), squads())
    }

    private companion object {
        const val STARTED = "20261107-120000"
    }
}

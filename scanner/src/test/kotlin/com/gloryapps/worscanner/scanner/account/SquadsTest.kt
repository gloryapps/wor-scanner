package com.gloryapps.worscanner.scanner.account

import com.gloryapps.worscanner.scanner.lua.LaidOutHeap
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SquadsTest {
    private val memory = LaidOutHeap()

    private fun slot(heroId: Long, squad: Int, lord: Any = false) =
        memory.table("m_iHeroID" to heroId, "m_iHeroShapeId" to 0, "m_iBaseID" to -1, "m_iSquadID" to squad, "m_IsLord" to lord)

    @Test
    fun `the squad screen's squads are read in their slots' order, each hero named by his base id and the Lord marked`() = runTest {
        memory.table("m_CharactorDatas" to memory.table(1 to memory.table("iHeroId" to 222400000L, "iBaseId" to 2224), 2 to memory.table("iHeroId" to 200100001L, "iBaseId" to 2001)))
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
                        listOf(SquadHero(222400000L, 2224, "Pelagios", lord = true), SquadHero(200200000L, null, null, lord = false)),
                        listOf(SquadHero(200100001L, 2001, "Rex", lord = true)),
                    ),
                ),
            ),
            readSquads(memory, CLIENT),
        )
    }

    @Test
    fun `with no squad screen open there are no squads, though the game holds teams elsewhere`() = runTest {
        memory.table("m_TeamDataList" to memory.table(1 to memory.table(1 to slot(222400000L, squad = 1))))

        assertEquals(emptyList(), readSquads(memory, CLIENT))
    }
}

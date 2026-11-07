package com.gloryapps.worscanner.scanner.simulator

import kotlin.test.Test
import kotlin.test.assertEquals

class RulesTest {
    @Test
    fun `the simulator's rules are read by hero, what it says of its other pages let go`() {
        val answer = """{"healers":["Eirlys"],"stances":{"Diaochan":["Serenade of Hope","Song of Sorrow"]},""" +
            """"ultimates":{"Gu Shi":["Auto","Hold until Nascent Soul"]},"ticks":{"Gu Shi":["Exactly 2 allied heroes adjacent"]},""" +
            """"dps":["Gu Shi","Vera"],"inspirers":["Diaochan"],"difficulties":{"7":{"stageId":5001007,"placeable":8,"squadSize":5,""" +
            """"grid":{"melee":[[3.5,2.5]],"ranged":[[1.5,1.5]],"walkable":[[3.5,2.5]],"boss":[4.5,5.5]}}}}"""

        assertEquals(
            Rules(
                healers = listOf("Eirlys"),
                inspirers = listOf("Diaochan"),
                stances = mapOf("Diaochan" to listOf("Serenade of Hope", "Song of Sorrow")),
                ultimates = mapOf("Gu Shi" to listOf("Auto", "Hold until Nascent Soul")),
                ticks = mapOf("Gu Shi" to listOf("Exactly 2 allied heroes adjacent")),
                difficulties = mapOf("7" to Difficulty(5001007, Grid(melee = listOf(listOf(3.5, 2.5)), ranged = listOf(listOf(1.5, 1.5)), boss = listOf(4.5, 5.5)))),
            ),
            rulesOf(answer),
        )
    }

    @Test
    fun `a stage's map is found by the stage, none for a stage the simulator does not fight`() {
        val grid = Grid(melee = listOf(listOf(3.5, 2.5)), ranged = emptyList(), boss = listOf(4.5, 5.5))
        val rules = Rules(difficulties = mapOf("7" to Difficulty(5001007, grid)))

        assertEquals(grid to null, rules.gridOf(5001007) to rules.gridOf(6201003))
    }
}

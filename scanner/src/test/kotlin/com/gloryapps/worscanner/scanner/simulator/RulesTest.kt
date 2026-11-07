package com.gloryapps.worscanner.scanner.simulator

import kotlin.test.Test
import kotlin.test.assertEquals

class RulesTest {
    @Test
    fun `the simulator's rules are read by hero, what it says of its other pages let go`() {
        val answer = """{"healers":["Eirlys"],"stances":{"Diaochan":["Serenade of Hope","Song of Sorrow"]},""" +
            """"ultimates":{"Gu Shi":["Auto","Hold until Nascent Soul"]},"ticks":{"Gu Shi":["Exactly 2 allied heroes adjacent"]},""" +
            """"dps":["Gu Shi","Vera"],"inspirers":["Diaochan"],"difficulties":{"7":{"placeable":20,"squadSize":5}}}"""

        assertEquals(
            Rules(
                healers = listOf("Eirlys"),
                inspirers = listOf("Diaochan"),
                stances = mapOf("Diaochan" to listOf("Serenade of Hope", "Song of Sorrow")),
                ultimates = mapOf("Gu Shi" to listOf("Auto", "Hold until Nascent Soul")),
                ticks = mapOf("Gu Shi" to listOf("Exactly 2 allied heroes adjacent")),
            ),
            rulesOf(answer),
        )
    }
}

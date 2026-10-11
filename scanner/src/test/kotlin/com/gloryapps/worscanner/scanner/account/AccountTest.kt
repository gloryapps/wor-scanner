package com.gloryapps.worscanner.scanner.account

import com.gloryapps.worscanner.scanner.lua.LaidOutHeap
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountTest {
    private val memory = LaidOutHeap()

    private fun hero(id: Int, level: Int) = memory.table("iHeroId" to id, "iBaseId" to 2235, "iLevel" to level, "mSkillLevel" to memory.table(1 to 5, 2 to 3))

    private fun piece(uid: Long) = memory.table(
        "iItemUid" to uid,
        "iItemId" to 31105,
        "vConfig" to memory.table("m_EquipSuitid" to 11),
        "vMasterAttrList" to memory.table(array = listOf(memory.table("iAttrId" to 1, "iValue" to 2350))),
        "bLocked" to false,
    )

    private fun json(text: String): JsonElement = Json.parseToJsonElement(text)

    @Test
    fun `each list is read from the table holding its anchor, every entry in the game's own fields`() = runTest {
        memory.table("m_CharactorDatas" to memory.table(101 to hero(101, 60), 102 to hero(102, 1)))
        memory.table("suitId2EquipIdExt" to memory.table(), "equips" to memory.table(7001L to piece(7001)))
        memory.table("m_vArtifacts" to memory.table(array = listOf(memory.table("iItemUid" to 5, "iLevel" to 20))))

        val account = readAccount(memory, STARTED)

        assertEquals(
            listOf(
                json("""{"iHeroId":101,"iBaseId":2235,"iLevel":60,"mSkillLevel":[5,3]}"""),
                json("""{"iHeroId":102,"iBaseId":2235,"iLevel":1,"mSkillLevel":[5,3]}"""),
            ),
            account.heroes,
        )
        assertEquals(
            listOf(json("""{"iItemUid":7001,"iItemId":31105,"vMasterAttrList":[{"iAttrId":1,"iValue":2350}],"bLocked":false}""")),
            account.gear,
        )
        assertEquals(listOf(json("""{"iItemUid":5,"iLevel":20}""")), account.artifacts)
    }

    @Test
    fun `of two tables holding a list, the one holding more is read, the copy the game let go only counted`() = runTest {
        memory.table("m_CharactorDatas" to memory.table(101 to hero(101, 59)))
        memory.table("m_CharactorDatas" to memory.table(101 to hero(101, 60), 102 to hero(102, 1)))

        val account = readAccount(memory, STARTED)

        assertEquals(listOf(60, 1), account.heroes.map { it.jsonObject.getValue("iLevel").jsonPrimitive.int })
        assertEquals(listOf(2, 1), account.found.getValue(Holding.HEROES))
    }

    @Test
    fun `a game holding none of the lists yet reads as an account with nothing in it`() = runTest {
        memory.table("iLimit" to 200)

        val account = readAccount(memory, STARTED)

        assertEquals(listOf(emptyList<JsonElement>()), listOf(account.heroes, account.gear, account.artifacts).distinct())
    }

    @Test
    fun `the file says it is an account, read when, and each list by its kind`() = runTest {
        memory.table("m_vArtifacts" to memory.table(array = listOf(memory.table("iLevel" to 20, "m_AfCfg" to memory.table("m_profession" to 2)))))

        val written = json(Json.encodeToString(Account.serializer(), readAccount(memory, STARTED).copy(seconds = 1.0)))

        assertEquals(
            json("""{"kind":"account","startedAt":"$STARTED","heroes":[],"gear":[],"artifacts":[{"iLevel":20}],"found":{"heroes":[],"gear":[],"artifacts":[1]},"seconds":1.0}"""),
            written,
        )
    }

    private companion object {
        const val STARTED = "20261010-122135"
    }
}

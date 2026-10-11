package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.lua.LaidOutHeap
import com.gloryapps.worscanner.scanner.lua.LuaValue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.zip.GZIPInputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class SnapshotsTest {
    private val root: File = createTempDirectory().toFile()
    private val memory = LaidOutHeap()

    private fun lines(file: File): List<JsonElement> = GZIPInputStream(file.inputStream()).bufferedReader().readLines().map(Json::parseToJsonElement)

    @Test
    fun `a snapshot's first line says when, beside what window and how many tables, and each table follows on a line of its own`() = runTest {
        val attribute = memory.table("iAttrId" to 7, "fScale" to 1.5)
        val piece = memory.table("iItemUid" to 9_000_000_000L, "bLocked" to true, "vAttr" to attribute, 3 to "three", array = listOf(4, attribute, null, LuaValue.Other(0x46)))

        val file = Snapshots(root).take(memory, width = 1920, height = 1080)

        val lines = lines(file)
        val head = Json.decodeFromJsonElement(Snapshot.serializer(), lines.first())
        assertEquals(File(root, "snapshots/${head.takenAt}.jsonl.gz"), file)
        assertEquals(Snapshot(head.takenAt, width = 1920, height = 1080, spans = 1, bytes = memory.spans().single().size, tables = 2), head)
        assertEquals(
            listOf(
                Json.parseToJsonElement("""{"at":${attribute.address},"length":0,"array":[],"fields":{"iAttrId":7,"fScale":1.5}}"""),
                Json.parseToJsonElement(
                    """{"at":${piece.address},"length":4,"array":[4,{"table":${attribute.address}},null,{"other":70}],""" +
                        """"fields":{"iItemUid":9000000000,"bLocked":true,"vAttr":{"table":${attribute.address}},"3":"three"}}""",
                ),
            ),
            lines.drop(1),
        )
    }

    @Test
    fun `a long array is cut at its first 500 values and a long string at its first 80 characters, the array's whole length still said`() = runTest {
        memory.table("sText" to "x".repeat(200), array = List(600) { it })

        val table = lines(Snapshots(root).take(memory, width = 1920, height = 1080)).drop(1).single().jsonObject

        assertEquals(600, table.getValue("length").jsonPrimitive.int)
        assertEquals((0 until 500).toList(), table.getValue("array").jsonArray.map { it.jsonPrimitive.int })
        assertEquals("x".repeat(80), table.getValue("fields").jsonObject.getValue("sText").jsonPrimitive.content)
    }
}

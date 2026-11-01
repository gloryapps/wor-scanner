package com.gloryapps.worscanner.scanner.client

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A table of the client's, each row read as the fields of the C# class the client reads it into, in
 * their order: those after its key, which the row is filed under and does not hold, but for its texts,
 * which the language's tables hold. A row that does not read whole is the class and the table disagreeing.
 */
class GameTable(bytes: ByteArray, fields: List<ClassField>) {
    private val table = MufTable(bytes)
    private val columns = fields.dropWhile { it.name != KEY }.drop(1).filterNot { it.name.startsWith(TEXT) && it.type == Column.STRING }

    fun keys(): List<Long> = table.keys().map(Int::toLong)

    /** The row under `key`, its fields by name; null where there is none. */
    fun row(key: Long): JsonObject? {
        val start = table.start(key.toInt()) ?: return null
        var at = start
        fun int() = table.int(at).also { at += Int.SIZE_BYTES }
        fun count() = table.varint(at).let { (count, after) -> at = after; count }
        fun ints() = JsonArray(List(count()) { JsonPrimitive(int()) })
        val row = columns.associate { column ->
            column.name to when (column.type) {
                Column.INT -> JsonPrimitive(int())
                Column.FLOAT -> JsonPrimitive(Float.fromBits(int()))
                Column.STRING -> JsonPrimitive(table.string(int()))
                Column.INTS -> ints()
                Column.INT_LISTS -> JsonArray(List(count()) { ints() })
                null -> error("the client's ${column.name} is of a type no table of its was read with")
            }
        }
        check(at == table.end(start)) { "row $key of the client's table does not read as its class's fields" }

        return JsonObject(row)
    }

    private companion object {
        const val KEY = "m_ID"
        /** A field the language's tables hold, under the table's name, the field's but its `m_` and the row's key. */
        const val TEXT = "m_m"
    }
}

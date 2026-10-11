package com.gloryapps.worscanner.scanner.lua

/** A value as a Lua table holds it; what is not data, a function or a userdata, is `Other`. */
sealed interface LuaValue {
    data class Bool(val value: Boolean) : LuaValue

    data class Integer(val value: Long) : LuaValue

    data class Real(val value: Double) : LuaValue

    data class Text(val value: String) : LuaValue

    /** A table by where it lives, read only when asked. */
    data class Table(val address: Long) : LuaValue

    data class Other(val tag: Int) : LuaValue
}

/** A field's key as JSON names it: a string as itself, an integer by its digits. */
val LuaValue.name: String get() = (this as? LuaValue.Text)?.value ?: (this as LuaValue.Integer).value.toString()

/** A table as read: its array part's size, its values in order with a nil as null, and its hash part's fields. */
data class LuaTable(val length: Int, val array: List<LuaValue?>, val hash: Map<LuaValue, LuaValue>)

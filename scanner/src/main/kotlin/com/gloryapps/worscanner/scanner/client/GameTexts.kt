package com.gloryapps.worscanner.scanner.client

/** The English the game's client prints, which its install ships whatever language the game is played in: a text by the key the client's tables name it by, each table's row holding its place in the pool. */
class GameTexts(vararg tables: ByteArray) {
    private val tables = tables.map(::MufTable)

    /** The text under `key`, from the first table that does not leave it blank. */
    fun text(key: String): String? = hash(key).let { hash ->
        tables.firstNotNullOfOrNull { table -> table.start(hash)?.let { table.string(table.int(it)) }?.takeIf(String::isNotEmpty) }
    }
}

/** A key's hash as the client keeps it: .NET's `String.GetHashCode`, the 64-bit build's that il2cpp keeps, over the key's UTF-16 units. */
internal fun hash(key: String): Int {
    var even = 5381
    var odd = 5381
    for (at in key.indices step 2) {
        even = (even shl 5) + even xor key[at].code
        if (at + 1 < key.length) odd = (odd shl 5) + odd xor key[at + 1].code
    }

    return even + odd * 1_566_083_941
}

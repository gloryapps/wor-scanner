package com.gloryapps.worscanner.windows.azhor

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FileTokenStoreTest {
    private val file = File(createTempDirectory().toFile(), "link")

    @Test
    fun `a token kept is there when the app starts again`() = runBlocking {
        FileTokenStore(file).keep("token")

        assertEquals("token", FileTokenStore(file).token.value)
    }

    @Test
    fun `a token forgotten is gone, now and after`() = runBlocking {
        val store = FileTokenStore(file)
        store.keep("token")

        store.keep(null)

        assertNull(store.token.value)
        assertNull(FileTokenStore(file).token.value)
    }
}

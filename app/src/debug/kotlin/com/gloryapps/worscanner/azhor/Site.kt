package com.gloryapps.worscanner.azhor

import com.gloryapps.worscanner.scanner.azhor.AzhorApi
import com.gloryapps.worscanner.scanner.azhor.Linking
import com.gloryapps.worscanner.scanner.azhor.Sending
import kotlinx.coroutines.delay
import java.io.File

/**
 * A debug build's site, for filming the link and the send: any code links and every scan is taken,
 * each after the pause a real answer takes, and nothing leaves the device.
 */
fun site(): AzhorApi = Demo

private object Demo : AzhorApi {
    override suspend fun link(code: String): Linking {
        delay(ANSWER_MS)
        return Linking.Linked(TOKEN)
    }

    override suspend fun send(token: String, scan: File): Sending {
        delay(ANSWER_MS)
        return Sending.Sent
    }
}

private const val TOKEN = "demo"

private const val ANSWER_MS = 900L

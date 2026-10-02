package com.gloryapps.worscanner.azhor

import com.gloryapps.worscanner.scanner.resultOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPOutputStream

/** The site at `site`, reached over its two addresses for a scanner: `/scanner/link` and `/scanner/scans`. */
class HttpAzhorApi(private val site: String) : AzhorApi {
    override suspend fun link(code: String): Linking = withContext(Dispatchers.IO) {
        resultOf {
            val connection = open("$site/scanner/link", "application/json")
            connection.outputStream.use { it.write(JSON.encodeToString(LinkAsked.serializer(), LinkAsked(code)).toByteArray()) }
            linkingOf(connection.responseCode, connection.answer())
        }.getOrDefault(Linking.Unanswered)
    }

    /* Gzipped on the way, since a large gear scan runs past the 4.5 MB a request to the site may carry. */
    override suspend fun send(token: String, scan: File): Sending = withContext(Dispatchers.IO) {
        resultOf {
            val connection = open("$site/scanner/scans", "application/gzip").apply { setRequestProperty("Authorization", "Bearer $token") }
            GZIPOutputStream(connection.outputStream).use { zipped -> scan.inputStream().use { it.copyTo(zipped) } }
            sendingOf(connection.responseCode)
        }.getOrDefault(Sending.Unanswered)
    }

    private fun open(url: String, type: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = CONNECT_TIMEOUT
        readTimeout = READ_TIMEOUT
        setRequestProperty("Content-Type", type)
        setRequestProperty("User-Agent", "wor-scanner")
    }
}

/** The body of a successful answer; null for any other, whose body is on the error stream. */
private fun HttpURLConnection.answer(): String? = resultOf { inputStream.bufferedReader().use { it.readText() } }.getOrNull()

/** What an answer to a code says: a token on 200, a refusal on 404 or 400, and nothing learnt otherwise. */
internal fun linkingOf(status: Int, answer: String?): Linking = when (status) {
    HttpURLConnection.HTTP_OK ->
        answer?.let { resultOf { JSON.decodeFromString(LinkAnswer.serializer(), it) }.getOrNull() }?.token?.let(Linking::Linked)
            ?: Linking.Unanswered
    HttpURLConnection.HTTP_NOT_FOUND, HttpURLConnection.HTTP_BAD_REQUEST -> Linking.Refused
    else -> Linking.Unanswered
}

/** What an answer to a sent scan says, by its status alone. */
internal fun sendingOf(status: Int): Sending = when (status) {
    in 200..299 -> Sending.Sent
    HttpURLConnection.HTTP_UNAUTHORIZED -> Sending.Unlinked
    HttpURLConnection.HTTP_ENTITY_TOO_LARGE -> Sending.TooLarge
    HttpURLConnection.HTTP_BAD_REQUEST -> Sending.Refused
    else -> Sending.Unanswered
}

@Serializable
private class LinkAsked(@SerialName("code") val code: String)

@Serializable
private class LinkAnswer(@SerialName("token") val token: String? = null)

private const val CONNECT_TIMEOUT = 15_000
/* A scan of a few megabytes on a slow line takes longer to go than an answer takes to come. */
private const val READ_TIMEOUT = 60_000
private val JSON = Json { ignoreUnknownKeys = true }

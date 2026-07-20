package com.example.ghostcart.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class InAppMessage(
    val id: String,
    val title: String,
    val body: String,
    val imageUrl: String?,
    val linkUrl: String?
)

object InAppMessageRepository {
    suspend fun fetchActiveMessages(): Result<List<InAppMessage>> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = openConnection("/api/in-app-messages", "GET")
            try {
                val code = connection.responseCode
                if (code !in 200..299) throw IllegalStateException(readError(connection, code))
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val rows = JSONObject(response).optJSONArray("messages") ?: JSONArray()
                buildList {
                    for (index in 0 until rows.length()) {
                        val row = rows.getJSONObject(index)
                        val id = row.optString("id").trim()
                        val title = row.optString("title").trim()
                        val body = row.optString("body").trim()
                        if (id.isNotEmpty() && title.isNotEmpty() && body.isNotEmpty()) {
                            add(
                                InAppMessage(
                                    id = id,
                                    title = title,
                                    body = body,
                                    imageUrl = row.optString("imageUrl").trim().takeIf { it.isNotEmpty() && it != "null" },
                                    linkUrl = row.optString("linkUrl").trim().takeIf { it.isNotEmpty() && it != "null" }
                                )
                            )
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun openConnection(path: String, method: String): HttpURLConnection =
        (URL("${ApiConfig.BASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
            readTimeout = ApiConfig.READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
        }

    private fun readError(connection: HttpURLConnection, responseCode: Int): String =
        connection.errorStream?.bufferedReader()?.use { it.readText() }
            ?: "In-app message request failed with status $responseCode"
}

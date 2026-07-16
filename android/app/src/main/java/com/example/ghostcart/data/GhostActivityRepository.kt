package com.example.ghostcart.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class GhostRanking(
    val productId: String,
    val ghostCount: Int
)

object GhostActivityRepository {
    suspend fun mostGhostedToday(): Result<List<GhostRanking>> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = openConnection("/api/ghost-events?limit=12", "GET")
            try {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IllegalStateException(readError(connection, responseCode))
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val rankings = JSONObject(response).optJSONArray("rankings") ?: JSONArray()
                buildList {
                    for (index in 0 until rankings.length()) {
                        val item = rankings.getJSONObject(index)
                        val productId = item.optString("productId").trim()
                        val ghostCount = item.optInt("ghostCount", 0)
                        if (productId.isNotEmpty() && ghostCount > 0) {
                            add(GhostRanking(productId = productId, ghostCount = ghostCount))
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun recordCheckout(
        checkoutId: String,
        productIds: List<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uniqueProductIds = productIds.distinct()
            require(uniqueProductIds.isNotEmpty()) { "At least one product is required" }

            val connection = openConnection("/api/ghost-events", "POST").apply {
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            try {
                val payload = JSONObject().apply {
                    put("checkoutId", checkoutId)
                    put("productIds", JSONArray(uniqueProductIds))
                    put("source", "android")
                }
                OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IllegalStateException(readError(connection, responseCode))
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
            ?: "Ghost activity request failed with status $responseCode"
}

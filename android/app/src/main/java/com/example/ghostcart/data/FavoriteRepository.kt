package com.example.ghostcart.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Server-side mirror of the locally-persisted favorite product ids
// (AppViewModel's SharedPreferences copy). Sign-in only - a guest's
// favorites stay local-only until they sign in, same as the rest of the
// app's account-gated features.
object FavoriteRepository {

    suspend fun list(context: Context): Result<Set<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = AuthRepository.getToken(context) ?: return@runCatching emptySet()
            val response = request("/api/me/favorites", "GET", token, null)
            val array = response.optJSONArray("productIds") ?: return@runCatching emptySet()
            buildSet {
                for (index in 0 until array.length()) {
                    add(array.getString(index))
                }
            }
        }
    }

    suspend fun add(context: Context, productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = AuthRepository.getToken(context) ?: return@runCatching Unit
            request("/api/me/favorites", "POST", token, JSONObject().put("productId", productId))
            Unit
        }
    }

    suspend fun remove(context: Context, productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = AuthRepository.getToken(context) ?: return@runCatching Unit
            request("/api/me/favorites", "DELETE", token, JSONObject().put("productId", productId))
            Unit
        }
    }

    private fun request(path: String, method: String, token: String?, body: JSONObject?): JSONObject {
        val connection = (URL("${ApiConfig.BASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Accept", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
            connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
            readTimeout = ApiConfig.READ_TIMEOUT_MS
            if (body != null) {
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
        }
        return try {
            body?.let { value -> connection.outputStream.use { it.write(value.toString().toByteArray()) } }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (status !in 200..299) throw Exception(json.optString("error", "Request failed ($status)"))
            json
        } finally {
            connection.disconnect()
        }
    }
}

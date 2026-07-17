package com.example.ghostcart.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AuthRepository {
    suspend fun signUp(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiConfig.BASE_URL}/api/auth/signup")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
            conn.readTimeout = ApiConfig.READ_TIMEOUT_MS
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                Result.success(response)
            } else {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: "Error code $responseCode"
                val errorMsg = try { JSONObject(error).getString("error") } catch(e: Exception) { error }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiConfig.BASE_URL}/api/auth/signin")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
            conn.readTimeout = ApiConfig.READ_TIMEOUT_MS
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                Result.success(response)
            } else {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: "Error code $responseCode"
                val errorMsg = try { JSONObject(error).getString("error") } catch(e: Exception) { error }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

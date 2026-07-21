package com.example.ghostcart.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private const val AUTH_PREFS_NAME = "ghost_cart_prefs"
private const val AUTH_TOKEN_KEY = "auth_token"

object AuthRepository {
    // The backend session token was previously fetched on every sign-in and
    // then discarded - the app never sent an Authorization header for
    // anything. Persisted here (same SharedPreferences file the rest of the
    // app already uses for simple flags) so authenticated-only endpoints
    // (profile, avatar, leaderboard opt-in) actually work.
    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(AUTH_TOKEN_KEY, token).apply()
    }

    fun getToken(context: Context): String? =
        context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE).getString(AUTH_TOKEN_KEY, null)

    fun clearToken(context: Context) {
        context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE).edit().remove(AUTH_TOKEN_KEY).apply()
    }

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

    // Backend verifies the Google ID token (audience, issuer, email_verified)
    // and creates/finds the matching users row before minting a real Ghost
    // Cart session - Google Sign-In used to only set a local display email
    // with no backend account behind it at all.
    suspend fun signInWithGoogle(idToken: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiConfig.BASE_URL}/api/auth/google")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
            conn.readTimeout = ApiConfig.READ_TIMEOUT_MS
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("idToken", idToken)
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

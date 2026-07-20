package com.example.ghostcart.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class SimulationConsentStatus(
    val version: Int,
    val consentText: String,
    val accepted: Boolean
)

/**
 * "This app is a simulation" consent, versioned per the plan: a version bump on
 * the backend re-requires acceptance from everyone who accepted an earlier
 * version, rather than treating any past acceptance as permanently valid.
 */
object SimulationConsentRepository {
    suspend fun fetchStatus(context: Context): Result<SimulationConsentStatus> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = openConnection("/api/simulation-consent", "GET", context)
            try {
                val code = connection.responseCode
                if (code !in 200..299) throw IllegalStateException(readError(connection, code))
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                SimulationConsentStatus(
                    version = json.optInt("version", 1),
                    consentText = json.optString("consentText"),
                    accepted = json.optBoolean("accepted", false)
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun submitAcceptance(context: Context, version: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = openConnection("/api/simulation-consent", "POST", context).apply {
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            try {
                val payload = JSONObject().apply {
                    put("version", version)
                    put("locale", Locale.getDefault().toLanguageTag())
                }
                OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }
                val code = connection.responseCode
                if (code !in 200..299) throw IllegalStateException(readError(connection, code))
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun openConnection(path: String, method: String, context: Context): HttpURLConnection =
        (URL("${ApiConfig.BASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
            readTimeout = ApiConfig.READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Ghost-Cart-Installation-Id", InstallationId.get(context))
        }

    private fun readError(connection: HttpURLConnection, responseCode: Int): String =
        connection.errorStream?.bufferedReader()?.use { it.readText() }
            ?: "Simulation consent request failed with status $responseCode"
}

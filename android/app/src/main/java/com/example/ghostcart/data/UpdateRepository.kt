package com.example.ghostcart.data

import com.ghostcart.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val minimumSupportedVersionCode: Int,
    val releaseNotes: String,
    val apkUrl: String
) {
    val mandatory: Boolean get() = BuildConfig.VERSION_CODE < minimumSupportedVersionCode
}

// This app isn't on the Play Store yet, so there's no Play Core In-App
// Update API - GET /api/app/version (a small admin-updatable singleton row)
// plus a direct GitHub Release APK link is the entire mechanism.
object UpdateRepository {
    suspend fun checkForUpdate(): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL("${ApiConfig.BASE_URL}/api/app/version").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
                readTimeout = ApiConfig.READ_TIMEOUT_MS
            }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) return@runCatching null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val info = AppUpdateInfo(
                latestVersionCode = json.optInt("latestVersionCode", 0),
                latestVersionName = json.optString("latestVersionName", ""),
                minimumSupportedVersionCode = json.optInt("minimumSupportedVersionCode", 1),
                releaseNotes = json.optString("releaseNotes", ""),
                apkUrl = json.optString("apkUrl", "")
            )
            info.takeIf { it.latestVersionCode > BuildConfig.VERSION_CODE && it.apkUrl.isNotBlank() }
        }
    }
}

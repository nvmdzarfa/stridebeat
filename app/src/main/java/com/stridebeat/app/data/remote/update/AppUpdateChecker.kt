package com.stridebeat.app.data.remote.update

import com.stridebeat.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val releasePageUrl: String,
    val isPreview: Boolean = false
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Unavailable(val reason: String, val isPrivateRepo: Boolean = false) : UpdateCheckResult()
}

object AppUpdateChecker {

    private const val RELEASES_LATEST_API = "https://api.github.com/repos/nvmdzarfa/stridebeat/releases/latest"
    const val RELEASES_PAGE_URL = "https://github.com/nvmdzarfa/stridebeat/releases"

    suspend fun checkLatestRelease(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentVersion = BuildConfig.VERSION_NAME
        try {
            val conn = (URL(RELEASES_LATEST_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "StrideBeat-Android/$currentVersion")
            }

            val code = conn.responseCode
            if (code == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val tag = json.optString("tag_name", "").removePrefix("v").trim()
                val notes = json.optString("body", "Pembaruan performa dan fitur baru tersedia.")
                val htmlUrl = json.optString("html_url", RELEASES_PAGE_URL)

                var apkUrl = htmlUrl
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            val dl = asset.optString("browser_download_url", "")
                            if (dl.isNotBlank()) {
                                apkUrl = dl
                                break
                            }
                        }
                    }
                }

                if (tag.isNotBlank() && isNewerVersion(tag, currentVersion)) {
                    UpdateCheckResult.UpdateAvailable(
                        AppUpdateInfo(
                            currentVersion = currentVersion,
                            latestVersion = tag,
                            releaseNotes = cleanReleaseNotes(notes),
                            downloadUrl = apkUrl,
                            releasePageUrl = htmlUrl,
                            isPreview = false
                        )
                    )
                } else {
                    UpdateCheckResult.UpToDate(currentVersion)
                }
            } else if (code == 404 || code == 403) {
                UpdateCheckResult.Unavailable(
                    reason = "Repositori GitHub bersifat Private atau belum ada rilis publik.",
                    isPrivateRepo = true
                )
            } else {
                UpdateCheckResult.Unavailable("Gagal memeriksa pembaruan (HTTP $code).")
            }
        } catch (e: Exception) {
            UpdateCheckResult.Unavailable("Tidak dapat terhubung ke server pembaruan.")
        }
    }

    fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".", "-", "_").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".", "-", "_").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    private fun cleanReleaseNotes(raw: String): String {
        return raw
            .replace("###", "")
            .replace("**", "")
            .trim()
            .ifBlank { "Peningkatan stabilitas, UI, dan perbaikan bug terbaru." }
    }
}

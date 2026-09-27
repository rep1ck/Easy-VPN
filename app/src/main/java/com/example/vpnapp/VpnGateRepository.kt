package com.example.vpnapp

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object VpnGateRepository {

    private const val TAG = "VpnGate"
    private const val PRIMARY_URL = "https://www.vpngate.net/api/iphone/"
    private const val MIRROR_URL =
        "https://cdn.jsdelivr.net/gh/GeorgeXie2333/vpngate-list-mirror@latest/data/vpngate.csv"

    const val DEFAULT_USERNAME = "vpn"
    const val DEFAULT_PASSWORD = "vpn"

    suspend fun fetchServers(limit: Int = 40): Result<List<VpnServer>> =
        withContext(Dispatchers.IO) {
            try {
                val csv = downloadCsv()
                val servers = parseCsv(csv, limit)
                if (servers.isEmpty()) {
                    Result.failure(Exception("Sunucu listesi boş"))
                } else {
                    Result.success(servers)
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetch failed", e)
                Result.failure(e)
            }
        }

    private fun downloadCsv(): String {
        val urls = listOf(PRIMARY_URL, MIRROR_URL)
        var lastError: Exception? = null
        for (urlStr in urls) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 25000
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) EasyVPN/1.0"
                    )
                    setRequestProperty("Accept", "text/plain,text/csv,*/*")
                }
                val code = conn.responseCode
                if (code !in 200..299) {
                    conn.disconnect()
                    continue
                }
                val text = BufferedReader(
                    InputStreamReader(conn.inputStream, Charsets.UTF_8)
                ).use { it.readText() }
                if (text.contains("OpenVPN_ConfigData_Base64", ignoreCase = true) ||
                    text.contains("HostName", ignoreCase = true)
                ) {
                    return text
                }
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "fail $urlStr: ${e.message}")
            } finally {
                try {
                    conn?.disconnect()
                } catch (_: Exception) {
                }
            }
        }
        throw lastError ?: Exception("VPNGate API'ye ulaşılamadı")
    }

    private fun parseCsv(csv: String, limit: Int): List<VpnServer> {
        val lines = csv.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("*") }
            .toList()

        val headerIndex = lines.indexOfFirst {
            it.contains("HostName", ignoreCase = true) &&
                it.contains("OpenVPN_ConfigData_Base64", ignoreCase = true)
        }
        if (headerIndex < 0) return emptyList()

        val headers = lines[headerIndex]
            .removePrefix("#")
            .split(",")
            .map { it.trim() }

        fun col(name: String): Int =
            headers.indexOfFirst { it.equals(name, ignoreCase = true) }

        val iIp = col("IP")
        val iCountry = col("CountryLong").takeIf { it >= 0 } ?: col("CountryShort")
        val iCountryShort = col("CountryShort")
        val iHost = col("HostName")
        val iPing = col("Ping")
        val iSpeed = col("Speed")
        val iScore = col("Score")
        val iConfig = col("OpenVPN_ConfigData_Base64")
        val iSessions = col("NumVpnSessions")
        if (iConfig < 0) return emptyList()

        val result = ArrayList<VpnServer>(limit)
        for (line in lines.drop(headerIndex + 1)) {
            if (result.size >= limit) break
            val cols = line.split(",")
            if (cols.size <= iConfig) continue

            val configB64 = cols.getOrNull(iConfig)?.trim().orEmpty()
            if (configB64.isEmpty()) continue

            val ovpn = try {
                String(Base64.decode(configB64, Base64.DEFAULT), Charsets.UTF_8)
            } catch (_: Exception) {
                continue
            }
            if (!ovpn.contains("remote", ignoreCase = true)) continue

            val score = cols.getOrNull(iScore)?.toLongOrNull() ?: 0L
            if (score in 1 until 100) continue

            val countryLong = cols.getOrNull(iCountry)?.trim().orEmpty()
            val countryShort = cols.getOrNull(iCountryShort)?.trim().orEmpty()
            val country = countryLong.ifEmpty { countryShort.ifEmpty { "Unknown" } }
            val ip = cols.getOrNull(iIp)?.trim().orEmpty()
            val host = cols.getOrNull(iHost)?.trim().orEmpty()

            val speedBps = cols.getOrNull(iSpeed)?.toDoubleOrNull() ?: 0.0

            result.add(
                VpnServer(
                    country = country,
                    ipAddress = ip.ifEmpty { host },
                    configData = patchOvpnConfig(ovpn),
                    downloadSpeedMbps = (speedBps / 1_000_000.0).coerceAtLeast(0.1),
                    username = DEFAULT_USERNAME,
                    password = DEFAULT_PASSWORD,
                    pingMs = cols.getOrNull(iPing)?.toIntOrNull() ?: -1,
                    score = score,
                    sessions = cols.getOrNull(iSessions)?.toIntOrNull() ?: 0
                )
            )
        }
        return result.sortedByDescending { it.score }
    }

    private fun patchOvpnConfig(ovpn: String): String {
        var cfg = ovpn.trim()
        if (!cfg.contains("data-ciphers", ignoreCase = true)) {
            cfg += "\ndata-ciphers AES-256-GCM:AES-128-GCM:AES-256-CBC:AES-128-CBC:CHACHA20-POLY1305"
        }
        if (!cfg.contains("auth-user-pass", ignoreCase = true)) {
            cfg += "\nauth-user-pass"
        }
        return cfg
    }
}

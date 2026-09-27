package com.example.vpnapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import ai.bongotech.bongovpn.BongoVpn
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var bongoVpn: BongoVpn? = null

    private var servers by mutableStateOf<List<VpnServer>>(emptyList())
    private var selectedServer by mutableStateOf<VpnServer?>(null)
    private var isConnected by mutableStateOf(false)
    private var statusText by mutableStateOf("Sunucular yükleniyor...")
    private var isConnecting by mutableStateOf(false)
    private var isLoadingServers by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val vpn = BongoVpn(this)
            bongoVpn = vpn
            if (!vpn.hasNotificationPermission()) {
                vpn.requestNotificationPermission()
            }
            vpn.setVpnListener(object : BongoVpn.VpnListener {
                override fun onVpnConnected() {
                    runOnUiThread {
                        isConnected = true
                        isConnecting = false
                        statusText = "Bağlandı — ${selectedServer?.country.orEmpty()}"
                        Toast.makeText(this@MainActivity, "VPN bağlandı", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onVpnStopped() {
                    runOnUiThread {
                        isConnected = false
                        isConnecting = false
                        statusText = "Bağlantı kesildi"
                    }
                }

                override fun onStatusUpdate(status: String) {
                    runOnUiThread {
                        statusText = status
                        Log.d(TAG, status)
                    }
                }

                override fun onError(errorMessage: String) {
                    runOnUiThread {
                        isConnecting = false
                        isConnected = false
                        statusText = "Hata: $errorMessage"
                        Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                        Log.e(TAG, errorMessage)
                    }
                }

                override fun onSpeedUpdate(
                    downloadBytes: Long,
                    uploadBytes: Long,
                    downloadSpeed: Long,
                    uploadSpeed: Long
                ) {
                    // no-op
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "BongoVpn init", e)
            statusText = "VPN motoru yüklenemedi: ${e.message}"
        }

        loadServers()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF2979FF),
                    secondary = Color(0xFF00E676),
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E)
                )
            ) {
                VpnMainScreen(
                    servers = servers,
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                    isLoadingServers = isLoadingServers,
                    selectedServer = selectedServer,
                    statusText = statusText,
                    onConnectClick = { toggleVpn() },
                    onServerSelect = { server ->
                        if (!isConnected && !isConnecting) {
                            selectedServer = server
                            statusText = "Seçili: ${server.country}"
                        }
                    },
                    onRefresh = { loadServers() }
                )
            }
        }
    }

    private fun loadServers() {
        isLoadingServers = true
        statusText = "VPNGate sunucuları yükleniyor..."
        lifecycleScope.launch {
            val result = VpnGateRepository.fetchServers(limit = 50)
            result.fold(
                onSuccess = { list ->
                    val prevId = selectedServer?.stableId
                    servers = list
                    isLoadingServers = false
                    statusText = if (list.isEmpty()) {
                        "Sunucu bulunamadı"
                    } else {
                        "${list.size} ücretsiz sunucu yüklendi"
                    }
                    selectedServer = list.find { it.stableId == prevId } ?: list.firstOrNull()
                },
                onFailure = { e ->
                    isLoadingServers = false
                    statusText = "Liste alınamadı: ${e.message}"
                    Toast.makeText(
                        this@MainActivity,
                        "Sunucu listesi yüklenemedi. Yenile'ye basın.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun toggleVpn() {
        val vpn = bongoVpn
        if (vpn == null) {
            Toast.makeText(this, "VPN motoru hazır değil", Toast.LENGTH_LONG).show()
            return
        }

        if (isConnected || isConnecting) {
            try {
                vpn.stopVpn()
            } catch (e: Exception) {
                Log.e(TAG, "stop", e)
            }
            isConnecting = false
            isConnected = false
            statusText = "Bağlantı kesiliyor..."
            return
        }

        val server = selectedServer
        if (server == null) {
            Toast.makeText(this, "Lütfen bir sunucu seçin", Toast.LENGTH_SHORT).show()
            return
        }
        if (server.configData.isBlank()) {
            Toast.makeText(this, "Bu sunucu için config yok", Toast.LENGTH_LONG).show()
            return
        }

        isConnecting = true
        statusText = "Bağlanıyor: ${server.country}..."

        try {
            vpn.attachFromString(server.configData, server.username, server.password)
            if (vpn.hasVpnPermission()) {
                vpn.startVpn()
            } else {
                vpn.requestVpnPermission()
                isConnecting = false
                statusText = "VPN izni verin, sonra tekrar Bağlan'a basın"
            }
        } catch (e: Exception) {
            isConnecting = false
            isConnected = false
            statusText = "Hata: ${e.message}"
            Log.e(TAG, "connect", e)
            Toast.makeText(this, e.message ?: "Bağlantı hatası", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        try {
            bongoVpn?.release()
        } catch (_: Exception) {
        }
        bongoVpn = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "EasyVPN"
    }
}

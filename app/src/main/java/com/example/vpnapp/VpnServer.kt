package com.example.vpnapp

data class VpnServer(
    val country: String,
    val ipAddress: String,
    val configData: String,
    val downloadSpeedMbps: Double,
    val username: String = "vpn",
    val password: String = "vpn",
    val pingMs: Int = -1,
    val score: Long = 0,
    val sessions: Int = 0
) {
    /** Liste yenilenince seçimi eşleştirmek için */
    val stableId: String get() = "$ipAddress|$country|$score"
}

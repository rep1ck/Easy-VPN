package com.example.vpnapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun VpnMainScreen(
    servers: List<VpnServer>,
    isConnected: Boolean,
    isConnecting: Boolean,
    isLoadingServers: Boolean,
    selectedServer: VpnServer?,
    statusText: String,
    onConnectClick: () -> Unit,
    onServerSelect: (VpnServer) -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when {
                    isConnected -> "GÜVENLİ BAĞLANTI AKTİF"
                    isConnecting -> "BAĞLANIYOR..."
                    else -> "KORUMASIZ"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = when {
                    isConnected -> Color(0xFF00E676)
                    isConnecting -> Color(0xFFFFAB00)
                    else -> Color(0xFFFF1744)
                },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = statusText,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onConnectClick,
                enabled = (selectedServer != null || isConnected) && !isConnecting && !isLoadingServers,
                modifier = Modifier.size(140.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isConnected -> Color(0xFF00E676)
                        isConnecting -> Color(0xFFFFAB00)
                        else -> Color(0xFF2979FF)
                    },
                    disabledContainerColor = Color.DarkGray
                )
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text(
                        text = if (isConnected) "Kapat" else "Bağlan",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ücretsiz Sunucular (VPNGate)",
                    color = Color.Gray,
                    style = MaterialTheme.typography.titleSmall
                )
                TextButton(
                    onClick = onRefresh,
                    enabled = !isLoadingServers && !isConnecting && !isConnected
                ) {
                    Text(text = "Yenile", color = Color(0xFF2979FF))
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoadingServers -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color(0xFF2979FF)
                        )
                    }
                    servers.isEmpty() -> {
                        Text(
                            text = "Sunucu yok.\nYenile'ye basın.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(
                                items = servers,
                                key = { server -> server.stableId }
                            ) { server ->
                                val isSelected = selectedServer?.stableId == server.stableId
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    onClick = { onServerSelect(server) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            Color(0xFF2979FF).copy(alpha = 0.35f)
                                        } else {
                                            Color(0xFF1E1E1E)
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = server.country,
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val sub = buildString {
                                                append(server.ipAddress)
                                                if (server.pingMs > 0) {
                                                    append("  •  ")
                                                    append(server.pingMs)
                                                    append(" ms")
                                                }
                                                if (server.sessions > 0) {
                                                    append("  •  ")
                                                    append(server.sessions)
                                                    append(" oturum")
                                                }
                                            }
                                            Text(
                                                text = sub,
                                                color = Color.Gray,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${server.downloadSpeedMbps.toInt()} Mbps",
                                            color = Color(0xFF00E676),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                AdMobBanner()
            }
        }
    }
}

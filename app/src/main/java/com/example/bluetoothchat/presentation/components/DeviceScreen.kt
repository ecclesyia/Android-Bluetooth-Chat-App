package com.example.bluetoothchat.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetoothchat.domain.BluetoothDeviceDomain
import com.example.bluetoothchat.presentation.BluetoothUiState
import com.example.bluetoothchat.theme.GlassSurface
import com.example.bluetoothchat.theme.NeonCyan
import com.example.bluetoothchat.theme.NeonPurple
import com.example.bluetoothchat.theme.NeonPink
import com.example.bluetoothchat.theme.SpaceDarkBg
import com.example.bluetoothchat.theme.TextPrimary
import com.example.bluetoothchat.theme.TextSecondary

@Composable
fun DeviceScreen(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onHost: () -> Unit,
    onConnect: (BluetoothDeviceDomain) -> Unit,
    onToggleMock: (Boolean) -> Unit
) {
    var isScanning by remember { mutableStateOf(false) }

    // Pulse animation logic for scanning radar
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDarkBg)
            .padding(16.dp)
    ) {
        // App Title & Sandbox Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Lobby",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (state.isMockSession) "Sandbox Simulator" else "Real Bluetooth Mode",
                    fontSize = 13.sp,
                    color = if (state.isMockSession) NeonPink else NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Sandbox Mode Toggle Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Sandbox",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = state.isMockSession,
                    onCheckedChange = { onToggleMock(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonPink,
                        checkedTrackColor = NeonPink.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = GlassSurface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connection State Details
        if (state.isConnecting) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Configuring connection...",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Waiting for handshakes to complete",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action Buttons Row (Scan & Host)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (isScanning) {
                        onStopScan()
                        isScanning = false
                    } else {
                        onStartScan()
                        isScanning = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) NeonPink.copy(alpha = 0.2f) else GlassSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .border(
                        1.dp,
                        if (isScanning) NeonPink else NeonCyan.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isScanning) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                                    .background(NeonPink.copy(alpha = pulseAlpha), CircleShape)
                            )
                            CircularProgressIndicator(
                                color = NeonPink,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scanning...", color = NeonPink, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Scan", tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search Devices", color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = onHost,
                colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .border(1.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Host Chat", color = NeonPurple, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Device Lists
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Paired Devices Header
            item {
                Text(
                    text = "Paired Devices",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (state.pairedDevices.isEmpty()) {
                item {
                    Text(
                        text = "No paired devices found.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(state.pairedDevices) { device ->
                    DeviceRow(device = device, onClick = { onConnect(device) })
                }
            }

            // Spacing
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Discovered Devices Header
            item {
                Text(
                    text = "Discovered Devices",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPurple,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (state.scannedDevices.isEmpty()) {
                item {
                    Text(
                        text = if (isScanning) "Searching for nearby devices..." else "Tap 'Search Devices' to scan.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(state.scannedDevices) { device ->
                    DeviceRow(device = device, onClick = { onConnect(device) })
                }
            }
        }
    }
}

@Composable
fun DeviceRow(
    device: BluetoothDeviceDomain,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Unknown Device",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = device.address,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            // Connect Indicator
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Connect",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

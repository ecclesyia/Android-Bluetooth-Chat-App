package com.example.bluetoothchat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetoothchat.domain.BluetoothMessage
import com.example.bluetoothchat.presentation.BluetoothUiState
import com.example.bluetoothchat.theme.GlassSurface
import com.example.bluetoothchat.theme.NeonCyan
import com.example.bluetoothchat.theme.NeonPink
import com.example.bluetoothchat.theme.NeonPurple
import com.example.bluetoothchat.theme.SpaceDarkBg
import com.example.bluetoothchat.theme.TextPrimary
import com.example.bluetoothchat.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    state: BluetoothUiState,
    onSendMessage: (String) -> Unit,
    onDisconnect: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDarkBg)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassSurface)
                .border(0.5.dp, Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pulse online indicator dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (state.isMockSession) NeonPink else NeonCyan)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (state.isMockSession) "Sandbox Chat Session" else "Bluetooth Peer",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (state.isMockSession) "Simulated RFCOMM Tunnel" else "RFCOMM Socket Connected",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onDisconnect,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = NeonPink)
            }
        }

        // Messages Thread
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(12.dp)) }

            items(state.messages) { message ->
                MessageBubble(message = message)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // Message Input Field Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Type offline message...", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = SpaceDarkBg.copy(alpha = 0.6f),
                    unfocusedContainerColor = SpaceDarkBg.copy(alpha = 0.6f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText)
                        messageText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)))
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = SpaceDarkBg,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: BluetoothMessage) {
    val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { dateFormat.format(Date(message.timestamp)) }

    val alignment = if (message.isFromLocalUser) Alignment.End else Alignment.Start
    val bubbleShape = if (message.isFromLocalUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    val bubbleBrush = if (message.isFromLocalUser) {
        Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))
    } else {
        Brush.horizontalGradient(listOf(GlassSurface, GlassSurface))
    }

    val textColor = if (message.isFromLocalUser) SpaceDarkBg else Color.White

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // Sender Name
        Text(
            text = message.senderName,
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
        )

        // Bubble Content
        Card(
            shape = bubbleShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .background(bubbleBrush, bubbleShape)
                .border(
                    width = 0.5.dp,
                    color = if (message.isFromLocalUser) Color.Transparent else Color.White.copy(alpha = 0.05f),
                    shape = bubbleShape
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = message.message,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedTime,
                    color = if (message.isFromLocalUser) SpaceDarkBg.copy(alpha = 0.6f) else TextSecondary,
                    fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

package com.example.bluetoothchat.presentation

import com.example.bluetoothchat.domain.BluetoothDeviceDomain
import com.example.bluetoothchat.domain.BluetoothMessage

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val pairedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val messages: List<BluetoothMessage> = emptyList(),
    val isMockSession: Boolean = false
)

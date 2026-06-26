package com.example.bluetoothchat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothchat.domain.BluetoothController
import com.example.bluetoothchat.domain.BluetoothDeviceDomain
import com.example.bluetoothchat.domain.BluetoothMessage
import com.example.bluetoothchat.domain.ConnectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BluetoothViewModel(
    private val bluetoothController: BluetoothController
) : ViewModel() {

    private val _state = MutableStateFlow(BluetoothUiState())
    val state = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        _state
    ) { scannedDevices, pairedDevices, state ->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BluetoothUiState())

    private var deviceConnectionJob: Job? = null

    init {
        // Collect errors from controller
        viewModelScope.launch {
            bluetoothController.errors.collect { error ->
                _state.update { it.copy(errorMessage = error) }
            }
        }
    }

    fun startScan() {
        if (_state.value.isMockSession) {
            simulateMockScan()
            return
        }
        bluetoothController.startDiscovery()
    }

    fun stopScan() {
        if (_state.value.isMockSession) return
        bluetoothController.stopDiscovery()
    }

    fun toggleMockSession(enable: Boolean) {
        if (enable) {
            bluetoothController.release()
            _state.update {
                it.copy(
                    isMockSession = true,
                    scannedDevices = emptyList(),
                    pairedDevices = emptyList(),
                    isConnected = false,
                    isConnecting = false,
                    messages = emptyList()
                )
            }
        } else {
            _state.update {
                it.copy(
                    isMockSession = false,
                    scannedDevices = emptyList(),
                    pairedDevices = emptyList(),
                    isConnected = false,
                    isConnecting = false,
                    messages = emptyList()
                )
            }
        }
    }

    private fun simulateMockScan() {
        viewModelScope.launch {
            _state.update { it.copy(isConnecting = false) }
            val mockScanned = listOf(
                BluetoothDeviceDomain("Galaxy S24 Ultra Mock", "00:11:22:33:44:55"),
                BluetoothDeviceDomain("Pixel 8 Pro Mock", "AA:BB:CC:DD:EE:FF"),
                BluetoothDeviceDomain("iPhone 15 Mock (BLE)", "11:22:33:44:55:66")
            )
            val mockPaired = listOf(
                BluetoothDeviceDomain("My Smart TV", "99:88:77:66:55:44"),
                BluetoothDeviceDomain("Bluetooth Headset", "12:34:56:78:90:AB")
            )
            // Emit paired immediately, scanned after a small delay
            _state.update { it.copy(pairedDevices = mockPaired) }
            delay(1500)
            _state.update { it.copy(scannedDevices = mockScanned) }
        }
    }

    fun startHosting() {
        _state.update { it.copy(isConnecting = true, errorMessage = null) }
        
        if (_state.value.isMockSession) {
            viewModelScope.launch {
                delay(2000)
                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = true,
                        messages = listOf(
                            BluetoothMessage("Hello! You are hosting a mock connection.", "System", false)
                        )
                    )
                }
            }
            return
        }

        deviceConnectionJob?.cancel()
        deviceConnectionJob = bluetoothController.startBluetoothServer()
            .listen()
    }

    fun connectToDevice(device: BluetoothDeviceDomain) {
        _state.update { it.copy(isConnecting = true, errorMessage = null) }

        if (_state.value.isMockSession) {
            viewModelScope.launch {
                delay(1500)
                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = true,
                        messages = listOf(
                            BluetoothMessage("Connected to ${device.name ?: "Unknown Device"} (Mock)", device.name ?: "Peer", false)
                        )
                    )
                }
            }
            return
        }

        deviceConnectionJob?.cancel()
        deviceConnectionJob = bluetoothController.connectToDevice(device)
            .listen()
    }

    fun disconnect() {
        if (_state.value.isMockSession) {
            _state.update {
                it.copy(
                    isConnected = false,
                    isConnecting = false,
                    messages = emptyList()
                )
            }
            return
        }
        bluetoothController.closeConnection()
        deviceConnectionJob?.cancel()
        _state.update { it.copy(isConnected = false, isConnecting = false) }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            if (_state.value.isMockSession) {
                val localMsg = BluetoothMessage(
                    message = message,
                    senderName = "Me (Local)",
                    isFromLocalUser = true
                )
                _state.update { it.copy(messages = it.messages + localMsg) }
                
                // Simulate typing and response
                delay(1000)
                val reply = when (message.lowercase().trim()) {
                    "hello", "hi", "hey" -> "Hello there! How's your app building going?"
                    "how are you?", "how are you" -> "I am operating optimally on Bluetooth (simulation)!"
                    "cool", "nice", "awesome" -> "Yes indeed, Kotlin + Jetpack Compose is super smooth!"
                    else -> "Received: '$message'. Let's keep chatting!"
                }
                val peerMsg = BluetoothMessage(
                    message = reply,
                    senderName = "Galaxy S24 Ultra Mock",
                    isFromLocalUser = false
                )
                _state.update { it.copy(messages = it.messages + peerMsg) }
                return@launch
            }

            val msg = bluetoothController.trySendMessage(message)
            if (msg != null) {
                _state.update { it.copy(messages = it.messages + msg) }
            } else {
                _state.update { it.copy(errorMessage = "Failed to send message") }
            }
        }
    }

    private fun Flow<ConnectionResult>.listen(): Job {
        return flowOn(Dispatchers.IO)
            .onEach { result ->
                when (result) {
                    ConnectionResult.ConnectionEstablished -> {
                        _state.update {
                            it.copy(
                                isConnected = true,
                                isConnecting = false,
                                errorMessage = null
                            )
                        }
                    }
                    is ConnectionResult.TransferSucceeded -> {
                        _state.update {
                            it.copy(
                                messages = it.messages + result.message
                            )
                        }
                    }
                    is ConnectionResult.Error -> {
                        _state.update {
                            it.copy(
                                isConnecting = false,
                                isConnected = false,
                                errorMessage = result.message
                            )
                        }
                    }
                }
            }
            .catch { throwable ->
                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        errorMessage = throwable.message ?: "An unexpected error occurred"
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}

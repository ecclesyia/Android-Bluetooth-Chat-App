package com.example.bluetoothchat.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import com.example.bluetoothchat.domain.BluetoothController
import com.example.bluetoothchat.domain.BluetoothDeviceDomain
import com.example.bluetoothchat.domain.BluetoothMessage
import com.example.bluetoothchat.domain.ConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
) : BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>> = _pairedDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val bluetoothDeviceReceiver = BluetoothDeviceReceiver { device ->
        val newDevice = BluetoothDeviceDomain(
            name = device.name,
            address = device.address
        )
        _scannedDevices.update { devices ->
            if (devices.any { it.address == newDevice.address }) devices else devices + newDevice
        }
    }

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null
    private var dataTransferService: BluetoothDataTransferService? = null

    init {
        updatePairedDevices()
    }

    override fun startDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }

        updatePairedDevices()
        _scannedDevices.update { emptyList() }

        context.registerReceiver(
            bluetoothDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )

        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }

        bluetoothAdapter?.cancelDiscovery()
        try {
            context.unregisterReceiver(bluetoothDeviceReceiver)
        } catch (e: IllegalArgumentException) {
            // Already unregistered
        }
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }

            currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                "bluetooth_chat_service",
                UUID.fromString(CHAT_UUID)
            )

            var shouldLoop = true
            while (shouldLoop) {
                currentClientSocket = try {
                    currentServerSocket?.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    null
                }

                currentClientSocket?.let { socket ->
                    currentServerSocket?.close()
                    _isConnected.value = true
                    emit(ConnectionResult.ConnectionEstablished)
                    
                    val transferService = BluetoothDataTransferService(socket)
                    dataTransferService = transferService
                    
                    emitAll(
                        transferService.listenForMessages().map {
                            ConnectionResult.TransferSucceeded(it)
                        }
                    )
                    shouldLoop = false
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }

            val remoteDevice = bluetoothAdapter?.getRemoteDevice(device.address) ?: return@flow

            currentClientSocket = remoteDevice.createRfcommSocketToServiceRecord(
                UUID.fromString(CHAT_UUID)
            )

            stopDiscovery()

            currentClientSocket?.let { socket ->
                try {
                    socket.connect()
                    _isConnected.value = true
                    emit(ConnectionResult.ConnectionEstablished)

                    val transferService = BluetoothDataTransferService(socket)
                    dataTransferService = transferService

                    emitAll(
                        transferService.listenForMessages().map {
                            ConnectionResult.TransferSucceeded(it)
                        }
                    )
                } catch (e: IOException) {
                    socket.close()
                    currentClientSocket = null
                    emit(ConnectionResult.Error("Connection was closed or failed"))
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun trySendMessage(message: String): BluetoothMessage? {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return null
        }

        val service = dataTransferService ?: return null
        val success = service.sendMessage(message)

        if (!success) {
            return null
        }

        val localName = if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            bluetoothAdapter?.name ?: "Local Device"
        } else {
            "Local Device"
        }

        return BluetoothMessage(
            message = message,
            senderName = localName,
            isFromLocalUser = true
        )
    }

    override fun closeConnection() {
        currentClientSocket?.close()
        currentServerSocket?.close()
        currentClientSocket = null
        currentServerSocket = null
        dataTransferService = null
        _isConnected.value = false
    }

    override fun release() {
        stopDiscovery()
        closeConnection()
    }

    private fun updatePairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        bluetoothAdapter?.bondedDevices?.map { device ->
            BluetoothDeviceDomain(
                name = device.name,
                address = device.address
            )
        }?.let { devices ->
            _pairedDevices.value = devices
        }
    }

    private fun hasPermission(permission: String): Boolean {
        // Under API 31, BLUETOOTH and BLUETOOTH_ADMIN permissions are granted by default
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val CHAT_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }
}

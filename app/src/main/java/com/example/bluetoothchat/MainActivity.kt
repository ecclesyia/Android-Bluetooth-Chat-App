package com.example.bluetoothchat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bluetoothchat.data.AndroidBluetoothController
import com.example.bluetoothchat.presentation.BluetoothViewModel
import com.example.bluetoothchat.presentation.components.ChatScreen
import com.example.bluetoothchat.presentation.components.DeviceScreen
import com.example.bluetoothchat.presentation.components.PermissionsScreen
import com.example.bluetoothchat.theme.BluetoothChatTheme

class MainActivity : ComponentActivity() {

    private val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private fun hasRequiredPermissions(): Boolean {
        return permissionsToRequest.all {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothController = AndroidBluetoothController(applicationContext)

        setContent {
            BluetoothChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: BluetoothViewModel = viewModel {
                        BluetoothViewModel(bluetoothController)
                    }
                    val state by viewModel.state.collectAsState()

                    var permissionGranted by remember { mutableStateOf(hasRequiredPermissions()) }
                    var bypassPermissionsForMock by remember { mutableStateOf(false) }

                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { results ->
                        permissionGranted = results.values.all { it }
                    }

                    if (permissionGranted || bypassPermissionsForMock) {
                        if (state.isConnected) {
                            ChatScreen(
                                state = state,
                                onSendMessage = { viewModel.sendMessage(it) },
                                onDisconnect = { viewModel.disconnect() }
                            )
                        } else {
                            DeviceScreen(
                                state = state,
                                onStartScan = { viewModel.startScan() },
                                onStopScan = { viewModel.stopScan() },
                                onHost = { viewModel.startHosting() },
                                onConnect = { viewModel.connectToDevice(it) },
                                onToggleMock = { isMock ->
                                    viewModel.toggleMockSession(isMock)
                                }
                            )
                        }
                    } else {
                        PermissionsScreen(
                            onRequestPermissions = {
                                launcher.launch(permissionsToRequest)
                            },
                            onSkipToMock = {
                                bypassPermissionsForMock = true
                                viewModel.toggleMockSession(true)
                            }
                        )
                    }
                }
            }
        }
    }
}

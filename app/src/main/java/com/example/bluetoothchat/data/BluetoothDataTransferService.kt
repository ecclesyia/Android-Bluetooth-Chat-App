package com.example.bluetoothchat.data

import android.bluetooth.BluetoothSocket
import com.example.bluetoothchat.domain.BluetoothMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class BluetoothDataTransferService(
    private val socket: BluetoothSocket
) {
    fun listenForMessages(): Flow<BluetoothMessage> {
        return flow {
            if (!socket.isConnected) {
                return@flow
            }
            val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
            while (true) {
                try {
                    val line = reader.readLine() ?: break
                    emit(
                        BluetoothMessage(
                            message = line,
                            senderName = socket.remoteDevice.name ?: "Unknown Device",
                            isFromLocalUser = false
                        )
                    )
                } catch (e: IOException) {
                    break
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun sendMessage(message: String): Boolean {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                // Append newline as our packet delimiter
                socket.outputStream.write((message + "\n").toByteArray(Charsets.UTF_8))
                socket.outputStream.flush()
                true
            } catch (e: IOException) {
                false
            }
        }
    }
}

# Bluetooth P2P Chat Android App

A premium, fully offline-capable peer-to-peer chat application built with **Kotlin**, **Jetpack Compose**, and **Coroutines/Flow**. This app allows two Android devices to establish a secure RFCOMM channel using classic Bluetooth and exchange chat messages without any internet connection.

---

## 🌟 Key Features

1. **Direct P2P Offline Chat**: Real-time message exchange over local Bluetooth socket connections.
2. **Dynamic Device Discovery**: Scan, discover, and pair with nearby devices using an elegant radar-like UI.
3. **Double Role Support (Host/Client)**: One device can host the connection (server socket) while the other connects to it (client socket).
4. **Interactive Sandbox Simulator (Mock Mode)**: Test all UI flows, animations, and message sending directly on an emulator or a single device without actual Bluetooth hardware.
5. **Modern Glassmorphic UI**: High-end styling with deep-space dark themes, vibrant neon accents, pulsing scanning indicators, and smooth message bubbles.
6. **Smart Runtime Permissions**: Custom onboarding flow handling Android 12+ (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, etc.) and legacy location permission requests gracefully.

---

## 🛠 Tech Stack & Architecture

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Asynchronous Flow**: Kotlin Coroutines & Flow (reactive UI bindings and socket read threads)
- **Bluetooth Core**: `BluetoothAdapter`, `BluetoothServerSocket` (listening for incoming connections), and `BluetoothSocket` (RFCOMM channel)
- **Architecture**: MVI / MVVM with Clean Architecture principles:
  - **Domain Layer**: Handles definitions of models (`BluetoothDevice`, `BluetoothMessage`) and interface contracts.
  - **Data Layer**: Implements socket communication and device discovery broadcasts.
  - **Presentation Layer**: Exposes UI states and processes mock and hardware-level operations.

---

## 📂 Project Structure

```text
com.example.bluetoothchat/
├── domain/
│   ├── BluetoothController.kt     # Interface contract for Bluetooth operations
│   ├── BluetoothDevice.kt         # Domain model for devices
│   ├── BluetoothMessage.kt        # Chat message model
│   └── ConnectionResult.kt        # Sealed interface representing connection state transitions
├── data/
│   ├── AndroidBluetoothController.kt # Core implementation of Bluetooth operations
│   ├── BluetoothDataTransferService.kt # Dynamic socket read/write stream service
│   └── BluetoothDeviceReceiver.kt  # BroadcastReceiver for scanning events
├── presentation/
│   ├── BluetoothUiState.kt        # Combined state for Compose
│   ├── BluetoothViewModel.kt      # ViewModel orchestrating business logic and sandbox simulations
│   └── components/
│       ├── PermissionsScreen.kt   # Permission request and onboarding UI
│       ├── DeviceScreen.kt        # Scanning and pairing screen with radar animations
│       └── ChatScreen.kt          # Chat room screen with scrollable bubbles and status indicators
├── theme/
│   ├── Color.kt                   # Neon and space-dark color system
│   ├── Theme.kt                   # Forced premium dark theme setup
│   └── Typography.kt              # Clean fonts setup
└── MainActivity.kt                # Application launcher and permission activity handler
```

---

## 🚀 How to Run & Test

### Option A: Using Sandbox Simulator (Recommended for Emulators)
Since Android emulators do not possess virtualized Bluetooth adapters, you can run the app in **Sandbox Mode**:
1. Build and run the app on any emulator or single device.
2. On the permissions screen, tap **Try Sandbox Simulator**.
3. Toggle the **Sandbox** switch in the Lobby.
4. Tap **Search Devices** to simulate finding devices with a scanning radar effect.
5. Tap **Connect** on any simulated device to open the Chat view.
6. Exchange messages! The simulator will auto-reply after a short delay.

### Option B: Real Bluetooth Hardware
1. Ensure you have **two physical Android devices**.
2. Build the APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Install the APK on both devices.
4. Grant the required permissions.
5. On **Device A**, tap **Host Chat** (turns on listening mode).
6. On **Device B**, tap **Search Devices**, wait for **Device A** to appear in the list, and tap **Connect**.
7. Once connected, the chat room will open on both screens automatically. Enjoy chatting offline!

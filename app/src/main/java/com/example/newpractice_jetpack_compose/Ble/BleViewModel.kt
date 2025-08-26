package com.example.newpractice_jetpack_compose.Ble

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BleViewModel @Inject constructor(
    private val serverManager: BleServerManager,
    private val clientManager: BleClientManager,
    private val uuidManager: UuidManager // UUID 초기화를 위해 주입
) : ViewModel() {

    // UI가 관찰할 데이터
    val scannedDevices = clientManager.scannedDevices
    val peerInfoReceived = serverManager.peerInfoReceived
    val handshakeResult = clientManager.handshakeResult

    init {
        // ViewModel이 생성될 때 기기의 고유 UUID를 초기화합니다.
        viewModelScope.launch {
            uuidManager.initializeDeviceUuid()
        }
    }

    // UI가 호출할 함수들
    fun startServer() {
        serverManager.startServer()
    }

    fun stopServer() {
        serverManager.stopServer()
    }

    fun startScan() {
        clientManager.startScan()
    }

    fun stopScan() {
        clientManager.stopScan()
    }

    fun connectToDevice(device: BleClientManager.DiscoveredDevice) {
        clientManager.connectToDevice(device)
    }

    // ViewModel이 소멸될 때 모든 BLE 작업을 정리합니다.
    override fun onCleared() {
        super.onCleared()
        clientManager.disconnect()
        clientManager.stopScan()
        serverManager.stopServer()
    }
}
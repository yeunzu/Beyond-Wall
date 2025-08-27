package com.example.newpractice_jetpack_compose.Lan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanViewModel @Inject constructor(
    private val nsdManager: NsdManager,
    private val fileTransferManager: FileTransferManager
) : ViewModel() {

    val discoveredServices = nsdManager.discoveredServices
    private var receiverJob: Job? = null

    // 임의의 포트 번호. 실제로는 동적으로 할당하는 것이 더 좋음.
    private val myPort = 12345

    fun startAdvertisingAndReceiving(deviceName: String) {
        // 이전에 실행중인 작업이 있다면 중지
        stopAdvertisingAndReceiving()

        nsdManager.startAdvertising(deviceName, myPort)

        receiverJob = viewModelScope.launch {
            fileTransferManager.startFileReceiver(myPort) { fileName, fileSize ->
                // TODO: UI에 파일 수신 완료 알림 (Toast, Snackbar 등)
            }
        }
    }

    fun stopAdvertisingAndReceiving() {
        nsdManager.stopAdvertising()
        receiverJob?.cancel()
        fileTransferManager.stopFileReceiver()
    }

    fun startDiscovery() {
        nsdManager.startDiscovery()
    }

    fun stopDiscovery() {
        nsdManager.stopDiscovery()
    }

    fun sendFileToDevice(service: NsdManager.DiscoveredService, fileUri: Uri, fileName: String, fileSize: Long) {
        viewModelScope.launch {
            val host = service.host ?: return@launch
            fileTransferManager.sendFile(host, service.port, fileUri, fileName, fileSize)
            // TODO: UI에 파일 전송 완료 알림
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAdvertisingAndReceiving()
        stopDiscovery()
    }
}
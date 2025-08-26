package com.example.newpractice_jetpack_compose.Ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// 바이트 배열을 식별자로 사용하기 쉬운 16진수 문자열로 변환하는 헬퍼 함수
fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

@SuppressLint("MissingPermission")
@Singleton
class BleClientManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uuidManager: UuidManager
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val scanner = bluetoothAdapter.bluetoothLeScanner
    private var gatt: BluetoothGatt? = null

    // UI에 표시될 스캔된 기기 목록
    data class DiscoveredDevice(
        val uniqueId: String, // 5바이트 고유 ID
        val name: String, // 기기 이름
        val address: String,
        val device: BluetoothDevice
    )
    private val _scannedDevices = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap()) // Map의 Key를 고유 ID로 사용
    val scannedDevices = _scannedDevices.asStateFlow()

    // 정보 교환 완료 시 결과를 전달하기 위한 Flow
    private val _handshakeResult = MutableSharedFlow<Map<String, String>>()
    val handshakeResult = _handshakeResult.asSharedFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val scanRecord = result.scanRecord ?: return

            // ManufacturerData에서 8바이트 기기 고유 ID 추출
            val manufacturerData = scanRecord.getManufacturerSpecificData(uuidManager.MANUFACTURER_ID)

            // manufacturerData가 있고, 길이가 정확히 5바이트인지 확인
            if (manufacturerData != null && manufacturerData.size == 5) {
                val uniqueId = manufacturerData.toHexString()
                val name = scanRecord.deviceName ?: result.device.name ?: "Unknown"

                val newDevice = DiscoveredDevice(
                    uniqueId = uniqueId,
                    name = name,
                    address = result.device.address,
                    device = result.device
                )
                // 고유 ID를 Key로 사용하여 Map에 기기 정보 업데이트
                _scannedDevices.value = _scannedDevices.value + (uniqueId to newDevice)
            }
        }
        override fun onScanFailed(errorCode: Int) {
            Log.e("BleClient", "스캔 실패: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        private val peerInfo = mutableMapOf<String, String>()
        private var writeQueue = mutableListOf<Pair<UUID, ByteArray>>()

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BleClient", "서버에 연결됨, 서비스 탐색 시작")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BleClient", "서버와 연결 끊김")
                this@BleClientManager.gatt?.close()
                this@BleClientManager.gatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BleClient", "서비스 발견, 정보 읽기 시작")
                val service = gatt.getService(uuidManager.getUuid(uuidManager.SETUP_SERVICE_UUID))
                val readChar = service?.getCharacteristic(uuidManager.getUuid(uuidManager.MY_DEVICE_NAME_CHAR_UUID))
                if (readChar != null) {
                    gatt.readCharacteristic(readChar)
                } else {
                    Log.w("BleClient", "읽기 특성을 찾을 수 없음")
                    disconnect()
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val uuid = characteristic.uuid
                val stringValue = value.toString(Charsets.UTF_8)

                when (uuid) {
                    uuidManager.getUuid(uuidManager.MY_DEVICE_NAME_CHAR_UUID) -> {
                        peerInfo["deviceName"] = stringValue
                        val nextChar = gatt.getService(uuidManager.getUuid(uuidManager.SETUP_SERVICE_UUID))
                            ?.getCharacteristic(uuidManager.getUuid(uuidManager.MY_INTERNAL_IP_PORT_CHAR_UUID))
                        if (nextChar != null) gatt.readCharacteristic(nextChar)
                    }
                    uuidManager.getUuid(uuidManager.MY_INTERNAL_IP_PORT_CHAR_UUID) -> {
                        peerInfo["internalIpPort"] = stringValue
                        val nextChar = gatt.getService(uuidManager.getUuid(uuidManager.SETUP_SERVICE_UUID))
                            ?.getCharacteristic(uuidManager.getUuid(uuidManager.MY_EXTERNAL_IP_PORT_CHAR_UUID))
                        if (nextChar != null) gatt.readCharacteristic(nextChar)
                    }
                    uuidManager.getUuid(uuidManager.MY_EXTERNAL_IP_PORT_CHAR_UUID) -> {
                        peerInfo["externalIpPort"] = stringValue
                        Log.d("BleClient", "모든 정보 읽기 완료: $peerInfo. 이제 내 정보 쓰기 시작")
                        startWriteSequence(gatt)
                    }
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BleClient", "${characteristic.uuid} 쓰기 성공")
                // 다음 쓰기 작업 수행
                if (writeQueue.isNotEmpty()) {
                    val (uuid, data) = writeQueue.removeAt(0)
                    val char = gatt.getService(uuidManager.getUuid(uuidManager.SETUP_SERVICE_UUID))?.getCharacteristic(uuid)
                    if (char != null) writeToCharacteristic(gatt, char, data)
                } else {
                    Log.d("BleClient", "모든 정보 쓰기 완료. 핸드셰이크 성공!")
                    // TODO: _handshakeResult.tryEmit(peerInfo)
                    disconnect()
                }
            }
        }

        private fun startWriteSequence(gatt: BluetoothGatt) {
            val myName = bluetoothAdapter.name ?: "Unknown"
            // TODO: 실제 IP/Port 정보 가져오는 로직 필요
            val myInternalIp = "192.168.0.20:12345"
            val myExternalIp = "211.212.213.214:54321"

            writeQueue.add(uuidManager.getUuid(uuidManager.YOUR_DEVICE_NAME_CHAR_UUID) to myName.toByteArray(Charsets.UTF_8))
            writeQueue.add(uuidManager.getUuid(uuidManager.YOUR_DEVICE_UNIQUE_ID_CHAR_UUID) to uuidManager.DEVICE_UNIQUE_UUID)
            writeQueue.add(uuidManager.getUuid(uuidManager.YOUR_DEVICE_INTERNAL_IP_PORT_CHAR_UUID) to myInternalIp.toByteArray(Charsets.UTF_8))
            writeQueue.add(uuidManager.getUuid(uuidManager.YOUR_DEVICE_EXTERNAL_IP_PORT_CHAR_UUID) to myExternalIp.toByteArray(Charsets.UTF_8))

            // 첫 쓰기 작업 시작
            if (writeQueue.isNotEmpty()) {
                val (uuid, data) = writeQueue.removeAt(0)
                val char = gatt.getService(uuidManager.getUuid(uuidManager.SETUP_SERVICE_UUID))?.getCharacteristic(uuid)
                if (char != null) writeToCharacteristic(gatt, char, data)
            }
        }

        private fun writeToCharacteristic(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, data: ByteArray) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                characteristic.value = data
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                gatt.writeCharacteristic(characteristic)
            }
        }
    }

    fun startScan() {
        _scannedDevices.value = emptyMap()
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(uuidManager.APP_SERVICE_UUID)) // 1차 광고에 포함된 APP_SERVICE_UUID를 기준으로 필터링
            .build()

        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(listOf(scanFilter), settings, scanCallback)
        Log.d("BleClient", "스캔 시작")
    }

    fun stopScan() {
        scanner.stopScan(scanCallback)
        Log.d("BleClient", "스캔 중지")
    }

    fun connectToDevice(device: DiscoveredDevice) {
        if (gatt != null) {
            Log.w("BleClient", "이미 다른 기기와 연결 시도중입니다.")
            return
        }
        stopScan()
        gatt = device.device.connectGatt(context, false, gattCallback)
        Log.d("BleClient", "${device.address}에 연결 시도")
    }

    fun disconnect() {
        gatt?.disconnect()
    }
}
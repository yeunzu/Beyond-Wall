package com.example.newpractice_jetpack_compose.Ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.example.newpractice_jetpack_compose.MinimumDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission")
@Singleton
class BleServerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uuidManager: UuidManager,
    private val dao: MinimumDao
) {
    private var originalBluetoothName: String? = null
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val advertiser = bluetoothAdapter.bluetoothLeAdvertiser
    private var gattServer: BluetoothGattServer? = null

    // 클라이언트로부터 모든 정보를 받았을 때 이벤트를 전달하기 위한 Flow
    private val _peerInfoReceived = MutableSharedFlow<Map<String, String>>()
    val peerInfoReceived = _peerInfoReceived.asSharedFlow()

    private val receivedData = mutableMapOf<String, String>()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d("BleServer", "Advertising 시작 성공")
        }
        override fun onStartFailure(errorCode: Int) {
            Log.e("BleServer", "Advertising 시작 실패: $errorCode")
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BleServer", "클라이언트 연결됨: ${device?.address}")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BleServer", "클라이언트 연결 끊김: ${device?.address}")
                receivedData.clear() // 연결 끊기면 수신 데이터 초기화
            }
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BleServer", "서비스 추가 성공, 광고 시작")
                startAdvertising()
            } else {
                Log.w("BleServer", "서비스 추가 실패: $status")
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) {
            val uuid = characteristic.uuid
            val value: ByteArray? = when (uuid) {
                uuidManager.getUuid(uuidManager.MY_DEVICE_NAME_CHAR_UUID) ->
                    bluetoothAdapter.name?.toByteArray(Charsets.UTF_8) // 광고 시 사용했던 변경된 이름을 그대로 사용
                uuidManager.getUuid(uuidManager.MY_INTERNAL_IP_PORT_CHAR_UUID) ->
                    // TODO: 실제 내부 IP/Port 정보를 가져오는 로직 필요
                    "192.168.0.10:12345".toByteArray(Charsets.UTF_8)
                uuidManager.getUuid(uuidManager.MY_EXTERNAL_IP_PORT_CHAR_UUID) ->
                    // TODO: 실제 외부 IP/Port 정보를 가져오는 로직 필요
                    "121.122.123.124:54321".toByteArray(Charsets.UTF_8)
                else -> null
            }

            if (value != null) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            } else {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
            }
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            val uuid = characteristic.uuid
            val stringValue = value?.toString(Charsets.UTF_8) ?: return

            when (uuid) {
                uuidManager.getUuid(uuidManager.YOUR_DEVICE_NAME_CHAR_UUID) -> receivedData["deviceName"] = stringValue
                uuidManager.getUuid(uuidManager.YOUR_DEVICE_UNIQUE_ID_CHAR_UUID) -> receivedData["deviceUuid"] = stringValue
                uuidManager.getUuid(uuidManager.YOUR_DEVICE_INTERNAL_IP_PORT_CHAR_UUID) -> receivedData["internalIpPort"] = stringValue
                uuidManager.getUuid(uuidManager.YOUR_DEVICE_EXTERNAL_IP_PORT_CHAR_UUID) -> receivedData["externalIpPort"] = stringValue
            }

            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }

            // 모든 정보가 수신되었는지 확인
            if (receivedData.size == 4) {
                Log.d("BleServer", "클라이언트로부터 모든 정보 수신 완료: $receivedData")
                // TODO: _peerInfoReceived.tryEmit(receivedData.toMap())
                receivedData.clear()
            }
        }
    }

    suspend fun startServer() {
        val customName = withContext(Dispatchers.IO) { // DB 조회는 IO 스레드에서 수행
            dao.getSettingByName("custom_Device_Name")?.setting_value
        }
        val nameToAdvertise = customName?.takeIf { it.isNotBlank() } ?: bluetoothAdapter.name

        originalBluetoothName = bluetoothAdapter.name // 광고 시작 전 이름 변경
        bluetoothAdapter.name = nameToAdvertise

        if (gattServer != null) {
            Log.w("BleServer", "서버가 이미 실행 중입니다.")
            return
        }
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        setupGattService()
    }

    fun stopServer() {
        originalBluetoothName?.let { // 광고 중지 후 원래 이름으로 복원
            bluetoothAdapter.name = it
            originalBluetoothName = null
        }
        try {
            advertiser.stopAdvertising(advertiseCallback)
        } catch (e: Exception) {
            Log.w("BleServer", "Advertising 중지 오류", e)
        }
        gattServer?.close()
        gattServer = null
        Log.d("BleServer", "서버 중지됨")
    }

    private fun setupGattService() {
        val service = BluetoothGattService(uuidManager.getUuid(uuidManager.SETUP_SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY)

        // 내 정보 (읽기용) 특성 추가
        val myNameChar = BluetoothGattCharacteristic(uuidManager.getUuid(uuidManager.MY_DEVICE_NAME_CHAR_UUID), BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
        val myInternalIpChar = BluetoothGattCharacteristic(uuidManager.getUuid(uuidManager.MY_INTERNAL_IP_PORT_CHAR_UUID), BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
        val myExternalIpChar = BluetoothGattCharacteristic(uuidManager.getUuid(uuidManager.MY_EXTERNAL_IP_PORT_CHAR_UUID), BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)

        // 상대 정보 (쓰기용) 특성 추가
        val yourNameChar = BluetoothGattCharacteristic(uuidManager.getUuid(uuidManager.YOUR_DEVICE_NAME_CHAR_UUID), BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE)
        val yourUuidChar = BluetoothGattCharacteristic(uuidManager.getUuid(uuidManager.YOUR_DEVICE_UNIQUE_ID_CHAR_UUID), BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE)
        val yourInternalIpChar = BluetoothGattCharacteristic(uuidManager.getUuid(uuidManager.YOUR_DEVICE_INTERNAL_IP_PORT_CHAR_UUID), BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE)
        val yourExternalIpChar = BluetoothGattCharacteristic(uuidManager.getUuid(uuidManager.YOUR_DEVICE_EXTERNAL_IP_PORT_CHAR_UUID), BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE)

        service.addCharacteristic(myNameChar)
        service.addCharacteristic(myInternalIpChar)
        service.addCharacteristic(myExternalIpChar)
        service.addCharacteristic(yourNameChar)
        service.addCharacteristic(yourUuidChar)
        service.addCharacteristic(yourInternalIpChar)
        service.addCharacteristic(yourExternalIpChar)

        gattServer?.addService(service)
    }

    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false) // 공간 절약을 위해 이름 제외
            .addServiceUuid(ParcelUuid(uuidManager.APP_SERVICE_UUID)) // 128비트(16바이트) 앱 서비스 ID + 헤더 2바이트 자동추가 => 18바이트
            .addManufacturerData(
                uuidManager.MANUFACTURER_ID,
                uuidManager.DEVICE_UNIQUE_UUID
            ) // 2바이트 짜리 제조사 ID + 5바이트 짜리 기기 고유 ID + 헤더 2바이트 자동추가 => 9바이트
            .build() // 18바이트 + 9바이트 = 27바이트

        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true) // 사용자 지정 기기 이름 (20~25바이트 ASCII) + 헤더 2바이트
            .build() // => 최대 27바이트

        advertiser.startAdvertising(settings, advertiseData, scanResponse, advertiseCallback)
    }
}
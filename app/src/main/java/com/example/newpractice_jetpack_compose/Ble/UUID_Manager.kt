package com.example.newpractice_jetpack_compose.Ble

import android.util.Base64
import android.util.Log
import com.example.newpractice_jetpack_compose.MinimumDao
import com.example.newpractice_jetpack_compose.MinimumInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

fun generateDeviceUUID(): ByteArray {
    val secureRandom = SecureRandom()
    val deviceId = ByteArray(5) // 크기가 5Byte인 배열
    secureRandom.nextBytes(deviceId)
    return deviceId
}

fun encodeBytearrayToString(bytes: ByteArray): String {
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}

fun decodeStringToByteArray(base64String: String): ByteArray {
    return Base64.decode(base64String, Base64.NO_WRAP)
}

@Singleton
class UuidManager @Inject constructor(
    private val dao: MinimumDao
) {
    val APP_SERVICE_UUID: UUID = UUID.fromString("4740f845-26f8-4695-8d12-2ca8a8c5cf75")
    val MANUFACTURER_ID = 0x4257 // 임시, 나중에 상업적을 쓰려면 블루투스 SIG에서 고유 번호를 할당받아야 함
    lateinit var DEVICE_UNIQUE_UUID: ByteArray
        private set

    private val BaseUUID = "4740%s-26f8-4695-8d12-2ca8a8c5cf75"
    val SETUP_SERVICE_UUID = 0x1100

    val MY_DEVICE_NAME_CHAR_UUID = 0x2101
    val MY_INTERNAL_IP_PORT_CHAR_UUID = 0x2102
    val MY_EXTERNAL_IP_PORT_CHAR_UUID = 0x2103

    val YOUR_DEVICE_NAME_CHAR_UUID = 0x2111
    val YOUR_DEVICE_UNIQUE_ID_CHAR_UUID = 0x2112
    val YOUR_DEVICE_INTERNAL_IP_PORT_CHAR_UUID = 0x2113
    val YOUR_DEVICE_EXTERNAL_IP_PORT_CHAR_UUID = 0x2114

    fun getUuid(shortId: Int): UUID {
        val hexId = String.format("%04X", shortId)
        return UUID.fromString(String.format(BaseUUID, hexId))
    }

    suspend fun initializeDeviceUuid() {
        withContext(Dispatchers.IO) {
            val settingKey = "DEVICE_UNIQUE_UUID"
            val savedUuidSetting = dao.getSettingByName(settingKey)
            var uuidToUse: ByteArray? = null

            if (savedUuidSetting?.setting_value != null) {
                try {
                    uuidToUse = decodeStringToByteArray(savedUuidSetting.setting_value)
                    Log.d("UUID_Manager", "기존 UUID를 DB에서 불러옵니다. ${encodeBytearrayToString(uuidToUse)}")
                } catch (e: IllegalArgumentException) {
                    Log.e("UUID_Manager", "DB에 저장된 UUID가 손상되었습니다.", e)
                    // 손상된 경우, 아래 로직에서 새 UUID를 생성하도록 null 상태 유지
                }
            }

            if (uuidToUse == null) {
                // DB에 UUID가 없거나 손상된 경우, 새로 생성
                val newUUID = generateDeviceUUID()
                uuidToUse = newUUID
                dao.insertSetting(
                    MinimumInfo(
                        setting_kind = settingKey,
                        setting_value = encodeBytearrayToString(newUUID)
                    )
                )
                Log.d("UUID_Manager", "새 UUID를 생성하고 저장합니다: ${encodeBytearrayToString(uuidToUse)}")
            }
            DEVICE_UNIQUE_UUID = uuidToUse
        }
    }
}
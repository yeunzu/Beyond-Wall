package com.example.newpractice_jetpack_compose

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import de.javawi.jstun.test.DiscoveryTest // jstun 라이브러리 import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import javax.inject.Inject
import javax.inject.Singleton


data class IpPortInfo(val ip: String, val port: Int)

@Singleton
class NetworkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 현재 기기의 로컬 Wi-Fi IPv4 주소를 반환합니다.
     * @return "192.168.x.x" 형식의 IP 주소 또는 null
     */
    fun getLocalIpAddress(): String? {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                // Wi-Fi 인터페이스만 필터링 (wlan0)
                if (networkInterface.isUp && networkInterface.name.contains("wlan")) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        // IPv4 주소만 반환
                        if (!address.isLoopbackAddress && address is Inet4Address) {
                            Log.d("NetworkManager", "로컬 Wi-Fi IPv4 주소 : ${address.hostAddress}")
                            return address.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 현재 기기에서 사용 가능한 포트 번호를 동적으로 찾아 반환합니다.
     * @return 사용 가능한 포트 번호
     */
    fun findAvailablePort(): Int {
        // ServerSocket에 port 0을 전달하면 OS가 사용 가능한 포트를 자동으로 할당해줍니다.
        val canUsePort = ServerSocket(0).use { it.localPort }
        Log.d("NetworkManager", "기기에서 사용 가능한 포트 : $canUsePort")
        return canUsePort
    }

    fun getLocalNetworkInfo(): IpPortInfo? {
        val ip = getLocalIpAddress() ?: return null
        val port = findAvailablePort()
        return IpPortInfo(ip, port)
    }

    /**
     * P2P 통신을 위해 공개 STUN 서버에서 공인 IP 정보를 가져오는 함수
     * @return PublicIpInfo(IP, Port) 또는 실패 시 null
     */
    suspend fun getPublicIpPortInfo(): IpPortInfo? = withContext(Dispatchers.IO) {
        // 1. 로컬 IP 주소 확인
        // val localIpAddress = getLocalIpAddress()
        val localInetAddress = InetAddress.getByName("0.0.0.0")

//        if (localIpAddress == null) {
//            Log.e("NetworkManager", "STUN 요청 실패: 유효한 로컬 IP 주소를 찾을 수 없습니다.")
//            return@withContext null
//        }

//        val localInetAddress: InetAddress
//        try {
//            localInetAddress = InetAddress.getByName(localIpAddress)
//        } catch (e: Exception) {
//            Log.e("NetworkManager", "STUN 요청 실패: 로컬 IP 주소 변환 중 오류 발생.", e)
//            return@withContext null
//        }

        // 2. STUN 요청에 사용할 로컬 포트를 동적으로 할당
        val localPort = findAvailablePort()

        // 3. STUN 서버 목록 순회하며 요청
        val stunServers = listOf(
            "stun.l.google.com" to 19302,
            "stun.mozilla.org" to 3478
        )

        for ((server, serverPort) in stunServers) {
            try {
                Log.d("NetworkManager", "$server:$serverPort STUN 서버에 요청 시작...")

                // 💥 수정된 부분: 4개의 인자를 모두 올바른 순서로 전달
                val test = DiscoveryTest(localInetAddress, localPort, server, serverPort)
                val discoveryInfo = test.test()

                if (discoveryInfo?.publicIP != null) {
                    val publicIp = discoveryInfo.publicIP.hostAddress
                    val publicPort = discoveryInfo.publicPort
                    Log.d("NetworkManager", "STUN 성공! Public IP: $publicIp, Port: $publicPort")
                    return@withContext IpPortInfo(publicIp, publicPort)
                }
            } catch (e: Exception) {
                Log.e("NetworkManager", "$server STUN 서버 요청 실패", e)
                // 다음 서버로 계속
            }
        }

        Log.e("NetworkManager", "모든 STUN 서버 요청에 실패했습니다.")
        return@withContext null
    }
}
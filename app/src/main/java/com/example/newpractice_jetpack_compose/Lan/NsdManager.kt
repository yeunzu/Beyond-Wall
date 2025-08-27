package com.example.newpractice_jetpack_compose.Lan

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton


// 로컬 WiFi 네트워크 상에서 우리 앱의 서비스를 알리고(광고), 다른 기기를 찾는(탐색) 모든 저수준 로직
@Singleton
class NsdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    // 탐색된 서비스(기기) 목록
    data class DiscoveredService(
        val name: String,
        val type: String,
        val host: InetAddress? = null,
        val port: Int = 0
    )

    private val _discoveredServices = MutableStateFlow<Map<String, DiscoveredService>>(emptyMap())
    val discoveredServices = _discoveredServices.asStateFlow()

    // 서비스 타입: "_[프로토콜이름]._tcp" 형식. 우리 앱을 식별하는 고유 채널.
    private val SERVICE_TYPE = "_beyondwall._tcp"
    private var serviceName: String? = null

    // --- 1. 광고 (Service Registration) ---
    fun startAdvertising(name: String, port: Int) {
        if (registrationListener != null) {
            Log.w("NsdManager", "이미 광고 중입니다.")
            return
        }
        this.serviceName = name

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = name
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                this@NsdManager.serviceName = serviceInfo.serviceName
                Log.d("NsdManager", "서비스 광고 시작 성공: ${serviceInfo.serviceName}")
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NsdManager", "서비스 광고 실패: $errorCode")
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d("NsdManager", "서비스 광고 중지됨: ${serviceInfo.serviceName}")
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun stopAdvertising() {
        if (registrationListener != null) {
            nsdManager.unregisterService(registrationListener)
            registrationListener = null
            serviceName = null
        }
    }

    // --- 2. 탐색 (Service Discovery) ---
    fun startDiscovery() {
        if (discoveryListener != null) {
            Log.w("NsdManager", "이미 탐색 중입니다.")
            return
        }
        _discoveredServices.value = emptyMap()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d("NsdManager", "서비스 발견: ${serviceInfo.serviceName}")
                // 내 기기가 광고하는 서비스는 무시
                if (serviceInfo.serviceName == serviceName) return

                // IP와 Port를 얻기 위해 Resolve 요청
                nsdManager.resolveService(serviceInfo, createResolveListener(serviceInfo.serviceName))
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d("NsdManager", "서비스 사라짐: ${serviceInfo.serviceName}")
                _discoveredServices.value -= serviceInfo.serviceName
            }
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d("NsdManager", "탐색 시작")
            }
            override fun onDiscoveryStopped(serviceType: String) {
                Log.d("NsdManager", "탐색 중지")
            }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NsdManager", "탐색 시작 실패: $errorCode")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        if (discoveryListener != null) {
            nsdManager.stopServiceDiscovery(discoveryListener)
            discoveryListener = null
        }
    }

    // --- 3. IP/Port 확인 (Service Resolution) ---
    private fun createResolveListener(serviceName: String): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d("NsdManager", "서비스 정보 확인 성공: $serviceInfo")
                val discoveredService = DiscoveredService(
                    name = serviceInfo.serviceName,
                    type = serviceInfo.serviceType,
                    host = serviceInfo.host,
                    port = serviceInfo.port
                )
                _discoveredServices.value += (serviceName to discoveredService)
            }
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NsdManager", "서비스 정보 확인 실패: $errorCode")
            }
        }
    }
}
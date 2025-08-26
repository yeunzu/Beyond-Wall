package com.example.newpractice_jetpack_compose

import android.app.Application
import com.example.newpractice_jetpack_compose.Ble.UuidManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

// Hilt를 사용하는 모든 앱은 @HiltAndroidApp 어노테이션이 붙은 Application 클래스를 가져야 합니다.
@HiltAndroidApp
class HiltApplication : Application() {
    // Hilt가 코드를 생성해주므로 내부는 비어 있어도 됩니다.

    @Inject
    lateinit var uuidManager: UuidManager // Hilt를 통해 UuidManager 주입

    override fun onCreate() {
        super.onCreate()
        // 앱이 생성될 때 코루틴을 사용해 UuidManagr 초기화
        CoroutineScope(Dispatchers.Main).launch {
            uuidManager.initializeDeviceUuid()
        }
    }
}
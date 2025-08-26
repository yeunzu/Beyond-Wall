package com.example.newpractice_jetpack_compose

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionUtils {

    /** 1. 앱에서 사용할 전체 권한 목록 정의 */
    // 안드로이드 버전에 따라 필요한 권한이 다르므로 분기 처리
    // 앱에 필요한 모든 위험 권한을 정의
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12 (API 31) 이상. Build.VERSION_CODES.S는 안드로이드 12 (API 31)을 의미.
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN, // 주변 기기 검색
            Manifest.permission.BLUETOOTH_CONNECT, // 기기 연결
            Manifest.permission.BLUETOOTH_ADVERTISE, // 내 기기 알림
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )
    } else {
        // Android 11 (API 30)
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
            // Android 11에서는 BLUETOOTH, BLUETOOTH_ADMIN이 일반 권한임
        )
    }

    /** 2. 필요한 모든 권한이 있는지 확인하는 함수 */
    fun allPermissionsGranted(context: Context): Boolean {
        // requiredPermissions 배열의 모든 권한이 승인되었는지 확인
        // 하나라도 거부된 권한이 있다면 false를 반환
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
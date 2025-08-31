package com.example.newpractice_jetpack_compose

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.newpractice_jetpack_compose.Ble.BleClientManager
import com.example.newpractice_jetpack_compose.Ble.BleServerManager
import com.example.newpractice_jetpack_compose.Ble.UuidManager
import com.example.newpractice_jetpack_compose.MinimumDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        // Provider를 사용하여 순환 종속성 문제를 방지합니다.
        daoProvider: Provider<MinimumDao>
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "AppDatabase"
        )
            .addCallback(AppDatabaseCallback(daoProvider)) // 👈 콜백 추가
            .build()
    }

    @Singleton
    @Provides
    fun provideMinimumDao(appDatabase: AppDatabase): MinimumDao {
        return appDatabase.MinimumInfoDao()
    }

    // --- ★★ 여기에 BLE 관련 코드를 추가합니다. ★★ ---
    @Provides
    @Singleton // BLE 매니저들도 앱 전체에서 하나만 있으면 충분합니다.
    fun provideBleServerManager(
        @ApplicationContext context: Context,
        uuidManager: UuidManager,
        MinimumDao: MinimumDao
    ): BleServerManager {
        return BleServerManager(context, uuidManager, MinimumDao)
    }

    @Provides
    @Singleton
    fun provideBleClientManager(
        @ApplicationContext context: Context,
        uuidManager: UuidManager
    ): BleClientManager {
        return BleClientManager(context, uuidManager)
    }
}

// AppModule 파일 안에 Callback 클래스를 함께 정의합니다.
private class AppDatabaseCallback(
    // Provider를 통해 DAO를 나중에 (DB 생성이 완료된 후) 가져옵니다.
    private val daoProvider: Provider<MinimumDao>
) : RoomDatabase.Callback() {
    // SupervisorJob을 사용하여 한 작업의 실패가 다른 작업에 영향을 주지 않도록 합니다.
    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        applicationScope.launch(Dispatchers.IO) {
            // DB가 처음 생성될 때만 이 코드가 실행됩니다.
            val dao = daoProvider.get()
            // isFirstEnter의 초기 상태는 '값이 없는 것'이므로, 다른 값만 넣어줍니다.
            dao.insertSetting(MinimumInfo(setting_kind = "uuid", setting_value = "초기 UUID"))
        }
    }
}
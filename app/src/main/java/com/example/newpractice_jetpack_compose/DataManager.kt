package com.example.newpractice_jetpack_compose

import android.app.Application
import android.content.Context
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.UUID

@Entity(tableName = "minimumInfo")
data class MinimumInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // 'id' 컬럼, 기본 키

    @ColumnInfo(name = "settingKind")
    val setting_kind: String, // 'settingKind' 컬럼, null 못들어가게 지정

    @ColumnInfo(name = "settingValue")
    val setting_value: String?, // 'settingValue' 컬럼, null 허용
)

@Dao
interface MinimumDao {
    @Insert
    suspend fun insertSetting(setting: MinimumInfo)

    @Query("UPDATE minimumInfo SET settingValue = :value WHERE settingKind = :kind")
    suspend fun updateSettingValue(kind: String, value: String?)

    // 일회성 조회 함수(앱 최초실행 판단할 때 사용)
    @Query("SELECT * FROM minimumInfo WHERE settingKind = :name LIMIT 1")
    suspend fun getSettingByName(name: String): MinimumInfo? // nullable 타입

    // Flow 반환을 위한 함수
    @Query("SELECT * FROM minimumInfo WHERE settingKind = :name LIMIT 1")
    fun getSettingByNameFlow(name: String): Flow<MinimumInfo? >// nullable 타입
}

@Database(entities = [MinimumInfo::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun MinimumInfoDao(): MinimumDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstace(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "AppDatabase"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            INSTANCE?.let { database ->
                CoroutineScope(SupervisorJob()).launch {
                    val dao = database.MinimumInfoDao()
                    // 최초값
                    // dao.insertSetting(MinimumInfo(setting_kind = "isFirstEnter", setting_value = null))
                    // dao.insertSetting(MinimumInfo(setting_kind = "uuid", setting_value = UUID.randomUUID().toString()))
                }
            }
        }
    }
}

// ViewModel에서 UI가 어떤 상태인지 명확히 표현하기 위한 sealed interface
sealed interface StartupState {
    object Loading : StartupState // 로딩 중
    object FirstTimeUser : StartupState // 최초 사용자
    object ReturningUser : StartupState // 기존 사용자
}


@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dao: MinimumDao
) : ViewModel() {

    // 최초 실행 여부 관련 로직
    private val _startupState = MutableStateFlow<StartupState>(StartupState.Loading)
    val startupState: StateFlow<StartupState> = _startupState

    init {
        checkIfFirstTimeUser()
    }

    private fun checkIfFirstTimeUser() {
        viewModelScope.launch { // 코루틴으로 비동기 실행
            // isFirstEnter 값을 가져옴
            val setting = dao.getSettingByName("isFirstEnter") // DB 읽기 (시간 소요)

            if (setting == null) {
                // DB에 값이 없으면 최초 사용자. 여기서 DB 업데이트를 딱 한 번만 수행
                // 여기서 DB를 업데이트하는 코드는 제거하고 상태만 변경
                // dao.insertSetting(MinimumInfo(setting_kind = "isFirstEnter", setting_value = "false"))
                _startupState.value = StartupState.FirstTimeUser
            } else {
                // DB에 값이 있으면 기존 사용자
                _startupState.value = StartupState.ReturningUser
            }
        }
    }

    fun onOnboardingComplete() {
        viewModelScope.launch {
            dao.insertSetting(MinimumInfo(setting_kind = "isFirstEnter", setting_value = "false"))
        }
    }

    // 1. 검색하고 싶은 설정의 이름(kind)을 저장하는 StateFlow
    private val _searchQuery = MutableStateFlow<String?>(null)

    // 2. _searchQuery의 값이 바뀔 때마다 새로운 Flow를 시작하여 결과를 관찰
    val settingResult: StateFlow<MinimumInfo?> = _searchQuery.flatMapLatest { query: String? ->
        // query가 null이면 빈 Flow를 반환하고, null이 아니면 DB Flow를 시작
        if (query.isNullOrBlank()) {
            flowOf<MinimumInfo?>(null) // 초기 상태 또는 검색어가 없을 때
        } else {
            dao.getSettingByNameFlow(query)
        }
    }.stateIn( // 3. 일반 Flow를 UI에서 사용하기 쉬운 StateFlow로 변환
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null // 초기값
    )

    /**
     * UI에서 특정 설정을 관찰하도록 요청하는 함수
     * 이 함수를 호출하면 _searchQuery 값이 바뀌고, settingResult가 자동으로 갱신됨
     */
    fun searchSetting(kind: String) {
        _searchQuery.value = kind
    }

    // DB 업데이트 함수 (이전과 동일)
    fun updateSetting(kind: String, value: String) {
        viewModelScope.launch {
            dao.updateSettingValue(kind, value)
        }
    }

    fun addSetting(kind: String, value: String?) {
        viewModelScope.launch{
            dao.insertSetting(MinimumInfo(setting_kind = kind, setting_value = value))
        }
    }
}

// UI는 절대로 DAO를 직접 호출하지 않는다. 항상 ViewModel을 통해서만 DB에 접근한다. : UI -> ViewModel -> Dao

// SettingsViewModel은 생성자(constructor)에 dao를 필요오 함. 이런 경우 ViewModel을 만들기 위한 팩토리(Factory)가 필요.
class SettingsViewModelFactory(private val dao: MinimumDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
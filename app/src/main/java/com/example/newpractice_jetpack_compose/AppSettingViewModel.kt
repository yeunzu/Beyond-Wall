package com.example.newpractice_jetpack_compose

import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class AppSettingViewModel @Inject constructor(
    private val dao: MinimumDao
) : ViewModel() {
    // 여기에 ViewModel 작성
}
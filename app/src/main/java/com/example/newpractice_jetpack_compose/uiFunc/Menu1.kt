package com.example.newpractice_jetpack_compose.uiFunc

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding // padding import 추가
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold // Scaffold import 추가
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.newpractice_jetpack_compose.MinimumDao
import com.example.newpractice_jetpack_compose.SettingsViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.text.all


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingMenu(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    // ViewModel로부터 모든 설정이 담긴 Map을 State로 관찰
    val settingMap by settingsViewModel.settingsMap.collectAsState()
    // Map에서 'custom_Device_Name' 값을 가져오기. 없으면 빈 문자열 사용
    val customDeviceNameDb = settingMap["custom_Device_Name"] ?: ""
    // TextField를 위한 로컬 상태
    var customDeviceNameTextField by remember { mutableStateOf("") }
    // DB에서 불러온 값이 변경될 때 로컬 상태를 동기화
    LaunchedEffect(customDeviceNameDb) {
        if (customDeviceNameTextField != customDeviceNameDb) {
            customDeviceNameTextField = customDeviceNameDb
        }
    }
    // 디바운싱. 사용자가 타이핑을 멈추고 500ms 지나면 DB에 값 저장
    LaunchedEffect(customDeviceNameTextField) {
        // 맨 처음 Composable이 그려질 때, DB 값이 로컬 상태를 초기화하는 과정에
        // 이 Effect가 실행, DB에 빈 값을 쓰는 것을 방지하는 조건
        if (customDeviceNameTextField == customDeviceNameDb && customDeviceNameTextField.isEmpty()) {
            return@LaunchedEffect
        }
        delay(500L)
        // 로컬 상태의 값이 DB와 다를 때만 업데이트
        if (customDeviceNameTextField != customDeviceNameDb) {
            settingsViewModel.upsertSetting("custom_Device_Name", customDeviceNameTextField)
        }
    }

    // 언어 선택 드롭다운 메뉴 목록
    val languageOptions = listOf(
        "English", "Korean", "Japanese", "Spanish", "Simplified Chinese",
        "Hindi", "Portuguese", "Arabic", "French", "German"
    ).sorted() + // 리스트를 사전순의 오름차순 정렬
    listOf("C-language", "java-language", "python-language") // 이스터에그

    // 기기의 기본 언어 확인
    val deviceLanguage = when (Locale.getDefault().language) {
        "ko" -> "Korean"
        "ja" -> "Japanese"
        "es" -> "Spanish"
        "zh" -> "Simplified Chinese"
        "hi" -> "Hindi"
        "pt" -> "Portuguese"
        "ar" -> "Arabic"
        "fr" -> "French"
        "de" -> "German"
        else -> "English" // 'en'을 포함한 나머지 모든 언어는 English로 기본 설정
    }
    val languageDb = settingMap["language_setting"] ?: deviceLanguage // DB값이 없으면 기기 언어를 기본으로 사용
    var languageDropdownIsExpended by remember { mutableStateOf(false) } // 언어 선택 메뉴가 열렸는가?
    var selectedLanguageOptionText by remember { mutableStateOf(languageOptions[0]) } // 선택된 언어 항목의 텍스트 저장

    // DB에서 불러온 값이 변경될 때 로컬 상태(선택된 텍스트)를 동기화
    LaunchedEffect(languageDb) {
        if (selectedLanguageOptionText != languageDb) {
            selectedLanguageOptionText = languageDb
        }
    }

    // 사용자가 언어를 변경했을 때 DB에 즉시 저장
    LaunchedEffect(selectedLanguageOptionText) {
        if (selectedLanguageOptionText != languageDb) {
            settingsViewModel.upsertSetting("language_setting", selectedLanguageOptionText)
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding -> // Scaffold의 content 람다에서 padding을 받음
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top, // 위에서부터 차곡차곡 쌓이듯이 보이게
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // TopAppBar의 높이만큼 padding 적용
        ) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Device Name",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(4.dp)
                        )
                        OutlinedTextField(
                            value = customDeviceNameTextField,
                            onValueChange = { newValue ->
                                val isAscii = newValue.matches(Regex("^[\\x00-\\x7F]*$"))
                                // Regex(): 정규식 객체 만들기
                                // [\\x00-\\x7F]: 16진수 00부터 7F까지의 문자, 즉 표준 ASCII 문자 범위를 의미
                                if (newValue.length <= 20 && isAscii) {
                                    customDeviceNameTextField = newValue
                                }
                            },
                            placeholder = { Text("Enter device name") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text("Use English, numbers, and a few special characters (up to 20 characters)",
                        fontSize = 10.sp, modifier = Modifier.padding(start=4.dp, bottom = 2.dp))
                    HorizontalDivider(modifier = Modifier.padding(2.dp))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Language",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(4.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = languageDropdownIsExpended,
                        onExpandedChange = { languageDropdownIsExpended = !languageDropdownIsExpended},
                        modifier = Modifier.padding(2.dp)
                    ) {
                        // 사용자가 탭하고, 선택된 값을 보여주는 TextField
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor(), // 이 TextField가 메뉴의 기준점임을 알림
                            readOnly = true, // 사용자가 직접 입력을 방지
                            value = selectedLanguageOptionText,
                            onValueChange = {},
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageDropdownIsExpended) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = languageDropdownIsExpended,
                            onDismissRequest = { languageDropdownIsExpended = false }
                        ) {
                            // option 리스트의 각 항목에 대해 DropdownMenuItem을 만듦
                            languageOptions.forEach { selectedLanguage ->
                                DropdownMenuItem(
                                    text = { Text(selectedLanguage) },
                                    onClick = {
                                        if (selectedLanguage == "C-language") {
                                            Toast.makeText(context, "printf(\"Hello world!\");", Toast.LENGTH_SHORT).show()
                                        } else if (selectedLanguage == "java-language") {
                                            Toast.makeText(context, "System.out.println(\"Hello world!\")", Toast.LENGTH_SHORT).show()
                                        } else if (selectedLanguage == "python-language") {
                                            Toast.makeText(context, "print(\"Hello world!\")", Toast.LENGTH_SHORT).show()
                                        } else {
                                        selectedLanguageOptionText = selectedLanguage // 선택된 항목으로 텍스트 변경
                                            }
                                        languageDropdownIsExpended = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(2.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Menu1Preview() {
    val navController = rememberNavController()
    SettingMenu(navController = navController)
}
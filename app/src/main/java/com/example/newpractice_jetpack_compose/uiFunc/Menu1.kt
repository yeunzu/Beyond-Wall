package com.example.newpractice_jetpack_compose.uiFunc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding // padding import 추가
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold // Scaffold import 추가
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.newpractice_jetpack_compose.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationMenu1(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menu1") },
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
            item { Text("This is Menu 1 Screen.") }
            item { Text("This is from NavigationScreen file :)") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Menu1Preview() {
    val navController = rememberNavController()
    NavigationMenu1(navController = navController)
}
package com.example.newpractice_jetpack_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.newpractice_jetpack_compose.ui.theme.NewPractice_Jetpack_ComposeTheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.newpractice_jetpack_compose.uiFunc.NavigationMenu1
import com.example.newpractice_jetpack_compose.uiFunc.NavigationMenu2
import com.example.newpractice_jetpack_compose.uiFunc.OnboardingScreen
import com.example.newpractice_jetpack_compose.uiFunc.SidebarDemo
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint // Hilt를 통해 의존성을 주입받기 위한 어노테이션
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewPractice_Jetpack_ComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MyApp()
                }
            }
        }
    }
}

@Composable
fun MyApp() {
    // I. DB 인스턴스 및 DAO 가져오기 (보통은 Hilt 같은 DI 라이브러리로 주입받음?)
//    val context = LocalContext.current
//    val dao = AppDatabase.getInstace(context).MinimumInfoDao()
    val viewModel: SettingsViewModel = hiltViewModel()

    // II. ViewModel이 결정한 시작 상태를 구독
    val startupState by viewModel.startupState.collectAsState()

    // III. 상태에 따라 적절한 UI를 표시
    when (startupState) {
        is StartupState.Loading -> {
            // 로딩 중일때 보여줄 스플래시 화면
            MySplashScreen()
        }
        is StartupState.FirstTimeUser -> {
            // 최초 사용자일 때 온보딩을 시작화면으로
            AppNavigation(startPage = "onboarding_route")
        }
        is StartupState.ReturningUser -> {
            // 기존 사용자일 때 홈을 시작화면으로
            AppNavigation(startPage = "home")
        }
    }
}

// 네비게이션 로직을 별도의 Composable로 분리하면 더 깔끔
@Composable
fun AppNavigation(startPage: String) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startPage) {
        composable("onboarding_route") {
            OnboardingScreen(navController = navController)
        }
        composable("home") {
            SidebarDemo(navController = navController)
        }
        composable("menu1") {
            NavigationMenu1(navController = navController)
        }
        composable("menu2") {
            NavigationMenu2(navController = navController)
        }
    }
}

@Composable
fun MySplashScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Test Splash Screen",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 40.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MySplashPreview() {
    MySplashScreen()
}
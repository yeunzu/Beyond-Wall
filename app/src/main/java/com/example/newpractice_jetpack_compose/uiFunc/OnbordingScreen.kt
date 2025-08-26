package com.example.newpractice_jetpack_compose.uiFunc

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.newpractice_jetpack_compose.PermissionUtils
import com.example.newpractice_jetpack_compose.SettingsViewModel
import kotlinx.coroutines.launch

data class OnbordingPage(
    val title: String,
    val content: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    // 권한 상태를 부모인 OnbordingScreen으로 끌어올림<일명 "상태 끌어올리기" 패턴. jetpack compose에서 중요하고 기본적인 개념 중 하나>
    var allPermissionsGranted by remember {
        mutableStateOf(PermissionUtils.allPermissionsGranted(context))
    }
    val pages = listOf(  // listOf("환영합니다", "권한 요청 안내", "시작해볼까요?")
        OnbordingPage(title = "환영합니다") { WelcomeScreen() },
        OnbordingPage(title = "권한 요청 안내") { PermissionsRequestScreen(
            // 자식에게 상태(allPermissionsGranted)와 상태 변경 이벤트(람다)를 전달합니다.
            allPermissionsGranted = allPermissionsGranted,
            onPermissionResult = { isGranted ->
                allPermissionsGranted = isGranted
            }
        ) },
        OnbordingPage(title = "시작해볼까요?") { ReadyToStartScreen() }
    )
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size }) // pageCount를 여기에 추가
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) { Text(text = pages[page].title) } }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    pages[page].content.invoke()
                }
            }
        }

        Row( // 하단 버튼 두 개
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = {
                    scope.launch {
                        pagerState.scrollToPage(0)
                    }
                },
                enabled = pagerState.currentPage > 0
            ) {
                Text("처음으로")
            }

            Button(
                onClick = {
                    scope.launch {
                        // '다음' 버튼 클릭 시, 현재 페이지가 권한 요청 페이지인지 확인
                        val isPermissionsPage = pagerState.currentPage == 1 // 권한 페이지 인덱스
                        if (isPermissionsPage && !allPermissionsGranted) {
                            // 권한 페이지인데 권한이 없다면, 넘어가지 않고 토스트 메시지를 보여줍니다.
                            Toast.makeText(context, "앱 사용을 위해 권한을 허용해주세요.", Toast.LENGTH_SHORT).show()
                        } else {
                            if (pagerState.currentPage < pages.lastIndex) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else { // 그 외의 경우에는 정상적으로 다음 페이지로 이동/앱을 시작
                                // 온보딩이 완료되었음을 ViewModel에 알림
                                viewModel.onOnboardingComplete()

                                // 이 부분에 메인 화면으로 이동 로직
                                navController.navigate("home") {
                                    popUpTo(navController.graph.startDestinationId) { // 이전 스택을 모두 제거 -> 온보딩 화면으로 오지 않도록
                                        inclusive = true
                                    }
                                    launchSingleTop = true // 동일한 대상이 여러 번 생성되는 것 방지
                                }
                            }
                        }
                    }
                }
            ) {
                Text(if (pagerState.currentPage == pages.lastIndex) "시작하기" else "다음")
            }
        }
    }
}

@Composable
fun WelcomeScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "새로운 앱에 오신 것을 환영합니다!",
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )
    }
}

// @OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsRequestScreen(
    // 부모로부터 상태와 이벤트를 파라미터로 받음
    allPermissionsGranted: Boolean,
    onPermissionResult: (Boolean) -> Unit
) {
    val context = LocalContext.current

    // I. 권한 상태를 저장하는 변수
    // Composable이 처음 그려질 때 현재 권한 상태로 초기화
//    var allPermissionsGranted by remember {
//        mutableStateOf(PermissionUtils.allPermissionsGranted(context))
//    } // 상태를 직접 소유하지 않으므로 삭제

    // AlertDialog의 표시 여부를 관리하는 State 변수
    var showDialog by remember { mutableStateOf(false) }

    // 2. 권한 요청을 위한 ActivityResultLauncher 등록
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            // 권한 요청 결과가 오며, 모든 권한이 승인되었는지 다시 확인하여 State를 업데이트
            val granted = permissions.entries.all { it.value }
            // 결과를 직접 State에 반영하는 대신, 부모에게 람다를 통해 알림
            onPermissionResult(granted)

            // 권한이 하나라도 모자라면 다이얼로그를 띄우도록 상태 변경
            if (!granted) {
                showDialog = true
            }
        }
    )

    // ★★ AlertDialog Composable
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false }, // 다이얼로그 바깥을 눌렀을 때
            title = { Text("권한이 필요해요") },
            text = { Text("원활한 앱 기능을 사용하려면 권한을 모두 허용해야 합니다. 설정 화면으로 이동하여 권한을 직접 허용해주세요.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        // 앱 설정 화면으로 직접 이동하는 인텐트(Intent) 실행
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", context.packageName, null)
                        intent.data = uri
                        context.startActivity(intent)
                    }
                ) {
                    Text("설정으로 이동")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "원활한 사용을 위해 최소한의 권한을 요청합니다.",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
            Column(
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                explainPermission(permission = "카메라 (필수)", explain = "QR코드를 스캔하여 다른 기기와 빠르게 연결할 수 있어요.")
                explainPermission(permission = "위치 (필수)", explain = "주변에 있는 친구의 기기를 정확하게 찾기 위해 블루투스와 함께 사용돼요.\n(위치 정보는 저장되지 않아요.)")
                explainPermission(permission = "블루투스 (필수)", explain = "블루투스를 통해 근처 기기를 탐색하고 연결 신호를 보내며 파일 공유에 사용돼요.")
            }
            Spacer(modifier = Modifier.weight(1f))
//            Spacer(modifier = Modifier)
//            Spacer(modifier = Modifier)
//            Spacer(modifier = Modifier)
            Button(
                onClick = {
                    if (allPermissionsGranted) {
                        Toast.makeText(context, "이미 모든 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        permissionLauncher.launch(PermissionUtils.requiredPermissions)
                    }
                },
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text("권한 요청을 수락할게요.")
            }
        }
    }
}



@Composable
fun explainPermission(permission: String, explain: String) {
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = permission,
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            modifier = Modifier
                .padding(4.dp)
        )
        Text(
            text = explain,
            textAlign = TextAlign.Start,
            fontSize = 10.sp,
            modifier = Modifier
                .padding(4.dp)
        )
    }
}

@Composable
fun ReadyToStartScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "이제 모든 준비가 완료되었어요!",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OnbordingScreenPreview() {
    val navController = rememberNavController()
    OnboardingScreen(navController)
}
package com.example.newpractice_jetpack_compose.uiFunc

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.newpractice_jetpack_compose.Ble.BleClientManager
import com.example.newpractice_jetpack_compose.Ble.BleViewModel
import com.example.newpractice_jetpack_compose.ShareViewModel
import com.example.newpractice_jetpack_compose.SelectMediaContract
import com.example.newpractice_jetpack_compose.SelectedFileInfo
import com.example.newpractice_jetpack_compose.VideoThumbnail
import com.example.newpractice_jetpack_compose.rememberMultipleFilesPickerLauncher


@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultHomeScreenView(
    navController: NavHostController,
    bleViewModel: BleViewModel = hiltViewModel(), // Hilt를 통해 ViewModel 인스턴스 가져오기,
    shareViewModel: ShareViewModel = hiltViewModel() // Hilt를 통해 SAF 전용 ViewModel 주입
) {
    val context = LocalContext.current

    var selectedIndex by remember { mutableStateOf(0) }
    val options = listOf("근거리 공유 모드", "내 기기 공유 모드")

    val scannedDevicesMap by bleViewModel.scannedDevices.collectAsStateWithLifecycle()
    val scannedDevicesList = scannedDevicesMap.values.toList() // Map의 value들만 리스트로 변환하여 사용

    // 선택된 모드에 따라 스캔/광고를 시작하는 로직
    // 화면이 처음 나타나거나, 선택된 모드가 바뀔 때마다 실행
    LaunchedEffect(selectedIndex) {
        if (selectedIndex == 0) {// 근기리 공유 모드
            bleViewModel.stopServer() // 광고 중지
            bleViewModel.startScan()
        } else {
            bleViewModel.stopScan()
            bleViewModel.startServer()
        }
    }

//    // 데이터 수신 처리
//    LaunchedEffect(Unit) {
//        bleViewModel.dataReceived.collect { payload ->
//            // TODO: 수신된 payload를 가지고 다이얼로그를 띄우거나 UI 업데이트
//            Log.d("HomeScren", "새로운 데이터 수신: $payload")
//
//            // DEBUGGING: 수신된 payload로 Toast 메시지 내용을 만듭니다.
//            val message = buildString {
//                payload.text?.let { append("텍스트: \"$it\"\n") }
//                payload.files?.let { append("파일 정보 ${it.size}개 수신") }
//            }
//
//            // 만들어진 메시지로 Toast를 화면에 표시합니다.
//            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
//        }
//    }

    // 화면이 사라질 때 스캔/광고를 중지해 배터리 절약
    DisposableEffect(Unit) {
        onDispose { 
            bleViewModel.stopScan()
            bleViewModel.stopServer()
        }
    }

    // ViewModel의 '여러 파일' 상태를 구독
    val selectedFilesList by shareViewModel.selectedFiles.collectAsStateWithLifecycle()

    // ShareViewModel의 '텍스트' 상태를 구독하는 코드 추가
    val textToShare by shareViewModel.textToShare.collectAsStateWithLifecycle()

    // 다중 파일 선택기를 실행할 런처 준비. 결과는 ViewModel로 전달
    val multipleFilePicker = rememberMultipleFilesPickerLauncher { uris ->
        if (uris.isNotEmpty()) {
            shareViewModel.onFilesSelected(uris)
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = SelectMediaContract(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                shareViewModel.onFilesSelected(uris)
            }
        }
    )

    // 텍스트 입력 다이얼로그의 표시 여부를 제어하는 상태 변수
    var showTextInputDialog by remember { mutableStateOf(false) }

    if (showTextInputDialog) {
        TextInputDialog(
            onDismissRequest = { showTextInputDialog = false },
            onConfirm = { text ->
                // '공유' 버튼이 눌렸을 때 실행할 로직
                // 전달받은 텍스트를 ViewModel로 보냅니다.
                shareViewModel.onTextSelected(text)
            }
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SingleChoiceSegmentedButtonRow {
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size
                            ),
                            onClick = {
                                selectedIndex = index
                                when (index) {
                                    0 -> println("selected 근거리 공유 모드")
                                    1 -> println("selected 내 기기 공유 모드")
                                }
                            },
                            selected = index == selectedIndex,
                            label = { Text(text = label, maxLines = 1, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(8.dp)) },
                            modifier = Modifier
                                .widthIn(min = 180.dp)
                                .padding(8.dp)
                        )
                    }
                }
                Text(
                    text = "Choose Your Option",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 25.sp,
                    modifier = Modifier
                        .padding(16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Spacer(Modifier)
                    ElevatedButton(
                        onClick = {
                            println("Files is clicked")
                            multipleFilePicker.launch("*/*") // 버튼 클릭 시 모든 종류의 파일 선택기 실행
                        }, modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Files",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .padding(5.dp)
                        )
                    }
                    Spacer(Modifier)
                    ElevatedButton(
                        onClick = {
                            println("Photos is clicked")
                            mediaPickerLauncher.launch(Unit)
                        }, modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Photos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .padding(5.dp)
                        )
                    }
                    Spacer(Modifier)
                    ElevatedButton(
                        onClick = {
                            println("Text is clicked")
                            showTextInputDialog = true // 버튼 누르면 다이얼로그를 띄우도록 상태 변경
                        }, modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Text",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .padding(5.dp)
                        )
                    }
                    Spacer(Modifier)
                }
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                        // .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    items(
                        selectedFilesList
                    ) { fileInfo ->
                        FilePreviewChip(
                            fileInfo = fileInfo,
                            onRemoveClick = {
                                // X 버튼 클릭 시 ViewModel의 제거 함수 호출
                                shareViewModel.removeFile(fileInfo)
                            }
                            )
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(
                        items = scannedDevicesList,
                        key = { it.uniqueId }
                    ) {
                        discoveredDevice ->
                        HorizontalDivider()
                        DeviceListItem(device = discoveredDevice, onClick = {
//                            // 클릭 시, 현재 공유할 콘텐츠로 Payload를 만들고 연결/전송 요청
//
//                            // ShareViewModel에서 현재 선택된 파일과 텍스트 정보를 가져옴
//                            val files = selectedFilesList.map { FileInfo(it.fileName, it.fileSize) }
//
//                            // 보낼 데이터가 있을 경우에만 전송
//                            if (files.isNotEmpty() || textToShare.isNotBlank()) {
//                                val payload = SharePayload(
//                                    text = textToShare.ifBlank { null },
//                                    files = files.ifEmpty { null }
//                                )
//                                bleViewModel.connectToDevice(discoveredDevice, payload)
//
//                                // 전송 후에는 선택된 콘텐츠 초기화
//                                shareViewModel.clearContent()
//                            } else {
//                                // 보낼 내용이 없을 경우 간단한 토스트 메세지 등 표시
//                                Toast.makeText(context, "공유할 내용이 없습니다.", Toast.LENGTH_SHORT).show()
//                                Log.d("HomeScreen", "공유할 내용이 없어 연결만 시도합니다.")
//                            }
                            Log.d("HomeScreenUI", "다음 기기가 선택되었습니다.\n기기 이름: ${discoveredDevice.name}. 기기 주소: ${discoveredDevice.address}, 기기 고유 ID: ${discoveredDevice.uniqueId}")
                        }
                        )
                    }
                }
            }
        }
    }
}

// 기기 목록 아이템을 위한 별도의 Composable (가독성 향상)
@SuppressLint("MissingPermission")
@Composable
fun DeviceListItem(
    device: BleClientManager.DiscoveredDevice, onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        // device.name은 스캔 중에 얻은 이름 (불완전할 수 있음)
        // ViewModel에서 연결 후 받아온 정확한 이름을 여기에 표시하도록 변경해야 함
        // 지금은 임시로 device.name을 사용
        Text(
            text = device.device.name ?: "Unknown Device",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        // 기기의 고유 ID는 항상 표시
        Text(
            text = device.uniqueId,
            fontSize = 14.sp
        )
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    }
}

/*
 * 파일 미리보기를 표시하는 별도의 Composable
 */
@Composable
fun FilePreviewChip(
    fileInfo: SelectedFileInfo,
    onRemoveClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp) // 칩 크기
            .clip(RoundedCornerShape(12.dp)) // 모서리를 둥글게
    ) {
        // 배경이 될 미리보기 이미지 또는 아이콘
        // ★★ when을 사용하여 MIME 타입에 따라 다른 미리보기를 표시
        when {
            // 1. 이미지 파일일 경우 (기존과 동일)
            fileInfo.mimeType?.startsWith("image/") == true -> {
                AsyncImage(
                    model = fileInfo.uri,
                    contentDescription = fileInfo.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                )
            }
            // 2. 동영상 파일일 경우 (새로운 로직)
            fileInfo.mimeType?.startsWith("video/") == true -> {
                VideoThumbnail(uri = fileInfo.uri)
            }
            // 3. 그 외 파일일 경우
            else -> {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "File Icon",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // 오른쪽 위에 표시될 'X' 버튼
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd) // 부모 Box의 오른쪽 위에 배치
                .padding(4.dp)
                .size(20.dp) // ★★ 이 값이 배경의 실제 크기를 결정합니다.
                .clip(CircleShape) // Box를 동그랗게 자름
                .background(Color.Black.copy(alpha = 0.5f)) // 배경색 적용
                .clickable(onClick = onRemoveClick), // 클릭 이벤트 추가
            contentAlignment = Alignment.Center // 내용물(아이콘)을 중앙에 배치
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove file",
                tint = Color.White,
                modifier = Modifier.size(12.dp) // 아이콘의 크기
            )
        }
    }
}

/*
 * 텍스트 입력을 위한 재사용 가능한 AlertDialog Composable
 * @param onDismissRequest 다이얼로그가 닫혀야 할 때 호출되는 람다
 * @param onConfirm 확인 버튼을 눌렀을 때, 입력된 텍스트와 함께 호출되는 람다
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // 다이얼 로그 내부에서만 사용할 텍스트 상태변수
    var inputText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("공유할 텍스트 입력") },
        text = {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("내용을 입력하세요") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    // 입력된 텍스트가 비어있지 않으면 onConfirm 람다를 통해 부모에게 전달
                    if (inputText.isNotBlank()) {
                        onConfirm(inputText)
                    }
                    onDismissRequest() // 다이얼로그 닫기
                }
            ) {
                Text("공유")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest // "취소" 버튼 클릭 시 다이얼로그 닫기
            ) {
                Text("취소")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    val navController = rememberNavController()
    DefaultHomeScreenView(navController = navController)
}
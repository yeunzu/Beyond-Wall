package com.example.newpractice_jetpack_compose

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// --------------------------------------------------
// 1. SAF 관련 상태 및 데이터를 관리하는 ViewModel
// --------------------------------------------------

/**
 * 선택된 파일의 정보를 담는 데이터 클래스
 */
data class SelectedFileInfo(
    val uri: Uri,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String?
)

@HiltViewModel
class ShareViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    // UI가 관찰할, 선택된 파일의 정보 (단일 파일 선택용)
    private val _selectedFile = MutableStateFlow<SelectedFileInfo?>(null)
    val selectedFile = _selectedFile.asStateFlow()

    // UI가 관찰할, 선택된 파일들의 정보 (다중 파일 선택용)
    private val _selectedFiles = MutableStateFlow<List<SelectedFileInfo>>(emptyList())
    val selectedFiles = _selectedFiles.asStateFlow()

    // URI로부터 파일 정보를 가져오는 로직을 별도 함수로 분리
    private fun getFileInfoFromUri(uri: Uri): SelectedFileInfo? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                val fileName = cursor.getString(nameIndex)
                val fileSize = cursor.getLong(sizeIndex)
                // ContextResolver를 통해 파일의 MIME 타입을 가져옴
                val mimeType = context.contentResolver.getType(uri)

                return SelectedFileInfo(uri, fileName, fileSize, mimeType)
            }
        }
        return null
    }

    /**
     * 파일 선택 결과(URI)를 받아서 파일 정보를 StateFlow에 업데이트하는 함수
     * TODO: 나중에 안쓰면 없애기
     */
    fun onFileSelected(uri: Uri) {
        getFileInfoFromUri(uri)?.let {
            _selectedFile.value = it
            Log.d("SafViewModel", "파일 선택됨: ${it.fileName}, 크기: ${it.fileSize} 바이트, 타입: ${it.mimeType}")
        }
    }

    /**
     * 여러 파일 선택 결과(URI 목록)를 처리하는 함수
     */
    fun onFilesSelected(uris: List<Uri>) {
        // mapNotNull을 사용해 각 URI로부터 파일 정보를 안전하게 가져옴
        val fileInfos = uris.mapNotNull { uri -> getFileInfoFromUri(uri) }
        _selectedFiles.value = fileInfos
        Log.d("SafViewModel","${fileInfos.size}개의 파일 선택됨&quot;")
    }

    // 선택된 파일 목록에서 특정 파일을 제거하는 함수
    fun removeFile(fileToRemove: SelectedFileInfo) {
        _selectedFiles.value = _selectedFiles.value - fileToRemove
    }

    // --- 텍스트 공유 관련 로직 ---
    private val _textToShare = MutableStateFlow("")
    val textToShare = _textToShare.asStateFlow()

    fun onTextSelected(text: String) {
        _textToShare.value = text
        Log.d("ShareViewModel", "공유할 텍스트 입력됨: $text")
    }

    // 공유할 콘텐츠를 모두 초기화하는 함수
    fun clearContent() {
        _selectedFiles.value = emptyList()
        _textToShare.value = ""
    }
}


/*
 * 이미지와 비디오만, 여러 개 선택할 수 있도록 하는 커스텀 ActivityResultContract
 */
class SelectMediaContract : ActivityResultContract<Unit, List<Uri>>() {

    // 1. 파일 선택기를 실행할 Intent를 생성하는 부분
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*" // 기본 타입을 넓게 설정
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // 여러 개 선택 허용
            // ★★ 핵심: 보여줄 파일 타입을 배열로 구체적으로 지정
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        }
    }

    // 2. 파일 선택기에서 결과를 받아와 파싱하는 부분
    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        // 결과가 OK이고, 데이터가 있을 때만 URI 리스트를 반환
        if (resultCode != Activity.RESULT_OK || intent == null) {
            return emptyList()
        }

        val uris = mutableListOf<Uri>()
        // 여러 파일을 선택한 경우 clipData에 담겨 옴
        if (intent.clipData != null) {
            for (i in 0 until intent.clipData!!.itemCount) {
                uris.add(intent.clipData!!.getItemAt(i).uri)
            }
        } else if (intent.data != null) {
            // 파일을 하나만 선택한 경우 data에 담겨 옴
            uris.add(intent.data!!)
        }

        return uris
    }
}


// --------------------------------------------------
// 2. 파일 선택기를 실행하는 재사용 가능한 Composable 함수
// --------------------------------------------------

/**
 * 단일 파일 선택기를 실행하고 결과를 처리하는 Composable '훅(Hook)'
 * @param onResult 사용자가 파일을 선택했을 때 그 결과(URI)를 처리할 람다 함수
 * @return 파일 선택기를 실행할 수 있는 런처 (`launcher.launch("MIME_TYPE")` 형태로 사용)
 */
@Composable
fun rememberFilePickerLauncher(
    onResult: (Uri?) -> Unit
): ActivityResultLauncher<String> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = onResult
    )
}

/**
 * 다중 파일 선택기를 실행하고 결과를 처리하는 Composable '훅(Hook)'
 * @param onResult 사용자가 파일들을 선택했을 때 그 결과(URI 목록)를 처리할 람다 함수
 * @return 다중 파일 선택기를 실행할 수 있는 런처
 */
@Composable
fun rememberMultipleFilesPickerLauncher(
    onResult: (List<Uri>) -> Unit
): ActivityResultLauncher<String> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = onResult
    )
}
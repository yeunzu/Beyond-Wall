package com.example.newpractice_jetpack_compose

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie // 비디오 아이콘
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoThumbnail(uri: Uri) {
    val context = LocalContext.current
    // 썸네일 Bitmap을 저장할 State 변수
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // uri가 변경될 때만 실행되는 Side-Effect
    LaunchedEffect(uri) {
        isLoading = true
        // 썸네일 추출은 I/O 작업이므로 백그라운드 스레드에서 실행
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                // 동영상의 첫 프레임을 썸네일로 가져옴
                thumbnail = retriever.getFrameAtTime()
            } catch (e: Exception) {
                // 썸네일 추출 실패 시
                thumbnail = null
                e.printStackTrace()
            } finally {
                // 리소스 누수를 막기 위해 반드시 release() 호출
                retriever.release()
            }
        }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = "Video Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 썸네일 로드에 실패하면 비디오 아이콘 표시
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = "Video Icon",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
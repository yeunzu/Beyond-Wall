package com.example.newpractice_jetpack_compose.Lan

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileTransferManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var serverSocket: ServerSocket? = null

    // --- 파일 수신 (서버 역할) ---
    suspend fun startFileReceiver(port: Int, onFileReceived: (String, Long) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                Log.d("FileTransfer", "파일 수신 대기 중... Port: ${serverSocket?.localPort}")

                while (true) {
                    val clientSocket = serverSocket!!.accept() // 클라이언트 연결 대기
                    Log.d("FileTransfer", "클라이언트 연결됨: ${clientSocket.inetAddress}")

                    // 파일 정보 수신
                    val inputStream = DataInputStream(clientSocket.getInputStream())
                    val fileName = inputStream.readUTF()
                    val fileSize = inputStream.readLong()

                    // TODO: 파일 저장 로직 (예: 앱의 캐시 디렉토리)
                    Log.d("FileTransfer", "파일 수신 시작: $fileName ($fileSize bytes)")
                    val outputFile = File(context.cacheDir, fileName)
                    val outputStream = FileOutputStream(outputFile)
                    inputStream.copyTo(outputStream)

                    Log.d("FileTransfer", "파일 수신 완료: ${outputFile.absolutePath}")
                    onFileReceived(fileName, fileSize)

                    outputStream.close()
                    inputStream.close()
                    clientSocket.close()
                }
            } catch (e: Exception) {
                Log.e("FileTransfer", "파일 수신 오류", e)
            } finally {
                serverSocket?.close()
            }
        }
    }

    fun stopFileReceiver() {
        serverSocket?.close()
        serverSocket = null
    }

    // --- 파일 송신 (클라이언트 역할) ---
    suspend fun sendFile(host: InetAddress, port: Int, fileUri: Uri, fileName: String, fileSize: Long) {
        withContext(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket(host, port)
                Log.d("FileTransfer", "서버에 연결됨: $host:$port")

                val outputStream = DataOutputStream(socket.getOutputStream())
                val inputStream = context.contentResolver.openInputStream(fileUri)

                // 파일 정보 전송
                outputStream.writeUTF(fileName)
                outputStream.writeLong(fileSize)

                // 파일 내용 전송
                Log.d("FileTransfer", "파일 전송 시작: $fileName")
                inputStream?.copyTo(outputStream)
                outputStream.flush()

                Log.d("FileTransfer", "파일 전송 완료")

                inputStream?.close()
                outputStream.close()

            } catch (e: Exception) {
                Log.e("FileTransfer", "파일 전송 오류", e)
            } finally {
                socket?.close()
            }
        }
    }
}
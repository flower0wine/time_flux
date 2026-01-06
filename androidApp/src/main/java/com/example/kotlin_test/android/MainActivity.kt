package com.example.kotlin_test.android

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.kotlin_test.getWallpaperManager
import com.example.kotlin_test.initWallpaperManager
import com.example.kotlin_test.initAdSkipManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化管理器
        initWallpaperManager(this)
        initAdSkipManager(this)
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("广告跳过", "壁纸设置")
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        when (selectedTab) {
            0 -> AdSkipScreen()
            1 -> WallpaperScreen()
        }
    }
}

@Composable
fun WallpaperScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf("请选择一张图片来设置壁纸") }
    var isLoading by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                statusMessage = "正在设置壁纸..."
                
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val imageData = inputStream?.readBytes()
                    inputStream?.close()
                    
                    if (imageData != null) {
                        val result = getWallpaperManager().setWallpaper(imageData)
                        
                        statusMessage = if (result.isSuccess) {
                            "壁纸设置成功！"
                        } else {
                            "设置失败: ${result.exceptionOrNull()?.message}"
                        }
                    } else {
                        statusMessage = "无法读取图片数据"
                    }
                } catch (e: Exception) {
                    statusMessage = "发生错误: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "壁纸设置工具",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(if (isLoading) "处理中..." else "选择图片")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = if (statusMessage.contains("成功")) {
                MaterialTheme.colorScheme.primary
            } else if (statusMessage.contains("失败") || statusMessage.contains("错误")) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Preview
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        WallpaperScreen()
    }
}

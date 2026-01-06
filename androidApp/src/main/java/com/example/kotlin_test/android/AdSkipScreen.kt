package com.example.kotlin_test.android

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.kotlin_test.AdSkipRule
import com.example.kotlin_test.SkipRecord
import com.example.kotlin_test.getAdSkipManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdSkipScreen() {
    var currentTab by remember { mutableStateOf(0) }
    val tabs = listOf("服务状态", "实时日志", "跳过记录", "规则管理")
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = currentTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = currentTab == index,
                    onClick = { currentTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        when (currentTab) {
            0 -> ServiceStatusTab()
            1 -> ServiceLogsTab()
            2 -> SkipRecordsTab()
            3 -> RulesManagementTab()
        }
    }
}

@Composable
fun ServiceStatusTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isServiceEnabled by remember { mutableStateOf(AdSkipAccessibilityService.isRunning) }
    var logMessages by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // 定期检查服务状态
    LaunchedEffect(Unit) {
        while (true) {
            isServiceEnabled = AdSkipAccessibilityService.isRunning
            kotlinx.coroutines.delay(1000)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 服务状态卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceEnabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isServiceEnabled) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Warning
                    },
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (isServiceEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isServiceEnabled) "服务已启用 ✓" else "服务未启用 ✗",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Text(
                    text = if (isServiceEnabled) {
                        "正在后台运行，自动跳过广告"
                    } else {
                        "请启用无障碍服务"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 操作按钮
        if (!isServiceEnabled) {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("打开无障碍设置")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // 测试按钮
        Button(
            onClick = {
                // 打开一个测试对话框
                android.widget.Toast.makeText(
                    context,
                    "服务状态: ${if (isServiceEnabled) "运行中" else "未运行"}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text("测试服务状态")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 使用说明卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📱 启用步骤",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. 点击上方按钮打开无障碍设置\n" +
                            "2. 找到 \"Time Flux\" 服务\n" +
                            "3. 点击进入并开启服务\n" +
                            "4. 返回应用，状态会自动更新\n\n" +
                            "🔍 查看日志 (电脑端):\n" +
                            "adb logcat -s AdSkipService:*",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 调试信息
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🐛 调试信息",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "服务类名:\ncom.example.kotlin_test.android.AdSkipAccessibilityService\n\n" +
                            "包名:\ncom.example.kotlin_test.android\n\n" +
                            "当前状态: ${if (isServiceEnabled) "运行中" else "未运行"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun ServiceLogsTab() {
    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    var autoRefresh by remember { mutableStateOf(true) }
    
    // 自动刷新日志
    LaunchedEffect(autoRefresh) {
        while (autoRefresh) {
            logs = AdSkipAccessibilityService.logMessages
            kotlinx.coroutines.delay(1000)
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 控制栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "实时日志 (${logs.size})",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row {
                Switch(
                    checked = autoRefresh,
                    onCheckedChange = { autoRefresh = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    AdSkipAccessibilityService.clearLogs()
                    logs = emptyList()
                }) {
                    Text("清空")
                }
            }
        }
        
        Divider()
        
        // 日志列表
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (AdSkipAccessibilityService.isRunning) {
                            "等待事件触发...\n\n打开其他应用查看广告跳过效果"
                        } else {
                            "服务未运行\n\n请先启用无障碍服务"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                reverseLayout = true // 最新的在底部
            ) {
                items(logs.reversed()) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SkipRecordsTab() {
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<SkipRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                records = getAdSkipManager().getSkipRecords(100)
            } catch (e: Exception) {
                // 处理错误
            } finally {
                isLoading = false
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无跳过记录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "已跳过 ${records.size} 次广告",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                items(records) { record ->
                    SkipRecordItem(record)
                }
            }
            
            Button(
                onClick = {
                    scope.launch {
                        getAdSkipManager().clearRecords()
                        records = emptyList()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("清空记录")
            }
        }
    }
}

@Composable
fun SkipRecordItem(record: SkipRecord) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = record.appName,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "关键词: ${record.keyword}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = dateFormat.format(Date(record.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RulesManagementTab() {
    val scope = rememberCoroutineScope()
    var rules by remember { mutableStateOf<List<AdSkipRule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                rules = getAdSkipManager().getRules()
            } catch (e: Exception) {
                // 处理错误
            } finally {
                isLoading = false
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rules) { rule ->
                    RuleItem(
                        rule = rule,
                        onToggle = { enabled ->
                            scope.launch {
                                getAdSkipManager().updateRule(rule.copy(enabled = enabled))
                                rules = getAdSkipManager().getRules()
                            }
                        },
                        onDelete = {
                            scope.launch {
                                getAdSkipManager().deleteRule(rule.id)
                                rules = getAdSkipManager().getRules()
                            }
                        }
                    )
                }
            }
            
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("添加规则")
            }
        }
    }
    
    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { rule ->
                scope.launch {
                    getAdSkipManager().addRule(rule)
                    rules = getAdSkipManager().getRules()
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun RuleItem(
    rule: AdSkipRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "关键词: ${rule.keywords.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (rule.packageName.isNotEmpty()) {
                    Text(
                        text = "应用: ${rule.packageName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Switch(
                checked = rule.enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (AdSkipRule) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加规则") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("规则名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("应用包名 (可选)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("关键词 (逗号分隔)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotEmpty() && keywords.isNotEmpty()) {
                        val rule = AdSkipRule(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            packageName = packageName,
                            keywords = keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                        onConfirm(rule)
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

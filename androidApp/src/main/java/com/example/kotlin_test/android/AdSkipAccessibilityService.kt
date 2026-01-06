package com.example.kotlin_test.android

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.kotlin_test.AdSkipRule
import com.example.kotlin_test.AndroidAdSkipManager
import com.example.kotlin_test.SkipRecord
import com.example.kotlin_test.getAdSkipManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AdSkipAccessibilityService : AccessibilityService() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var rules: List<AdSkipRule> = emptyList()
    private var lastSkipTime = 0L
    private val skipCooldown = 1000L // 1秒冷却时间，避免重复点击
    private var debugMode = true // 调试模式，输出详细日志
    
    companion object {
        private const val TAG = "AdSkipService"
        var isRunning = false
            private set
        
        // 用于在应用内显示日志
        private val _logMessages = mutableListOf<String>()
        val logMessages: List<String> get() = _logMessages.toList()
        
        fun addLog(message: String) {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
            _logMessages.add("[$timestamp] $message")
            if (_logMessages.size > 100) {
                _logMessages.removeAt(0)
            }
        }
        
        fun clearLogs() {
            _logMessages.clear()
        }
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        val msg = "无障碍服务已连接并启动"
        Log.i(TAG, "========================================")
        Log.i(TAG, msg)
        Log.i(TAG, "========================================")
        addLog("✓ $msg")
        loadRules()
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            Log.d(TAG, "收到空事件")
            return
        }
        
        val eventType = when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_VIEW_CLICKED -> "VIEW_CLICKED"
            else -> "OTHER(${event.eventType})"
        }
        
        val packageName = event.packageName?.toString() ?: ""
        val className = event.className?.toString() ?: ""
        
        // 记录所有事件（用于调试）
        if (debugMode) {
            val msg = "事件: $eventType | 包名: $packageName | 类名: $className"
            Log.d(TAG, msg)
            addLog(msg)
        }
        
        // 只处理窗口状态变化事件（更可靠）
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        
        // 忽略自己的应用
        if (packageName == this.packageName) {
            Log.d(TAG, "忽略自己的应用")
            return
        }
        
        if (packageName.isEmpty()) {
            Log.d(TAG, "包名为空，跳过")
            return
        }
        
        // 检查冷却时间
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSkipTime < skipCooldown) {
            if (debugMode) {
                Log.d(TAG, "在冷却期内，跳过处理 (${currentTime - lastSkipTime}ms)")
            }
            return
        }
        
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Log.w(TAG, "无法获取根节点")
            addLog("⚠ 无法获取根节点")
            return
        }
        
        val msg = "🔍 开始分析界面: $packageName"
        Log.i(TAG, msg)
        addLog(msg)
        
        trySkipAd(rootNode, packageName)
        rootNode.recycle()
    }
    
    override fun onInterrupt() {
        Log.w(TAG, "服务被中断")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        scope.cancel()
        Log.i(TAG, "服务已销毁")
    }
    
    private fun loadRules() {
        scope.launch {
            try {
                rules = getAdSkipManager().getRules().filter { it.enabled }
                    .sortedByDescending { it.priority }
                val msg = "加载了 ${rules.size} 条规则"
                Log.i(TAG, msg)
                addLog(msg)
                rules.forEach { rule ->
                    val ruleMsg = "  - ${rule.name}: ${rule.keywords.joinToString(", ")}"
                    Log.i(TAG, ruleMsg)
                    addLog(ruleMsg)
                }
            } catch (e: Exception) {
                val errMsg = "加载规则失败: ${e.message}"
                Log.e(TAG, errMsg, e)
                addLog("❌ $errMsg")
            }
        }
    }
    
    private fun trySkipAd(node: AccessibilityNodeInfo, packageName: String) {
        // 获取适用的规则
        val applicableRules = rules.filter { 
            it.packageName.isEmpty() || it.packageName == packageName 
        }
        
        if (applicableRules.isEmpty()) {
            Log.d(TAG, "没有适用的规则")
            return
        }
        
        Log.d(TAG, "适用规则数: ${applicableRules.size}")
        
        // 收集所有关键词
        val keywords = applicableRules.flatMap { it.keywords }.distinct()
        Log.d(TAG, "搜索关键词: ${keywords.joinToString(", ")}")
        
        // 调试模式：打印整个节点树
        if (debugMode) {
            Log.d(TAG, "========== 节点树结构 ==========")
            DebugHelper.printNodeTree(node, maxDepth = 8)
            Log.d(TAG, "================================")
        }
        
        // 方法1: 通过文本查找
        for (keyword in keywords) {
            val targetNodes = node.findAccessibilityNodeInfosByText(keyword)
            if (targetNodes.isNotEmpty()) {
                Log.i(TAG, "找到 ${targetNodes.size} 个包含 '$keyword' 的节点")
                for ((index, targetNode) in targetNodes.withIndex()) {
                    logNodeInfo(targetNode, "文本匹配节点 $index")
                    if (tryClickNode(targetNode, packageName, keyword)) {
                        targetNodes.forEach { it.recycle() }
                        return
                    }
                }
                targetNodes.forEach { it.recycle() }
            }
        }
        
        Log.d(TAG, "文本匹配未找到，尝试 ViewId 匹配")
        
        // 方法2: 通过 ViewId 查找
        if (tryFindByViewId(node, packageName)) {
            return
        }
        
        Log.d(TAG, "ViewId 匹配未找到，尝试遍历所有可点击节点")
        
        // 方法3: 遍历所有可点击节点，检查文本
        if (tryFindClickableNodes(node, packageName, keywords)) {
            return
        }
        
        Log.d(TAG, "未找到可跳过的广告按钮")
    }
    
    private fun logNodeInfo(node: AccessibilityNodeInfo, prefix: String) {
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""
        val className = node.className?.toString() ?: ""
        val isClickable = node.isClickable
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        Log.d(TAG, "$prefix:")
        Log.d(TAG, "  文本: $text")
        Log.d(TAG, "  描述: $contentDesc")
        Log.d(TAG, "  ViewId: $viewId")
        Log.d(TAG, "  类名: $className")
        Log.d(TAG, "  可点击: $isClickable")
        Log.d(TAG, "  位置: $bounds")
    }
    
    private fun tryClickNode(node: AccessibilityNodeInfo, packageName: String, keyword: String): Boolean {
        logNodeInfo(node, "尝试点击节点")
        
        // 检查节点是否可点击
        if (node.isClickable) {
            Log.i(TAG, "节点可点击，执行点击")
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (clicked) {
                Log.i(TAG, "✓ 点击成功！")
                onAdSkipped(packageName, keyword)
                return true
            } else {
                Log.w(TAG, "✗ 点击失败")
            }
        }
        
        // 如果节点本身不可点击，尝试点击父节点
        Log.d(TAG, "尝试点击父节点")
        var parent = node.parent
        var depth = 0
        while (parent != null && depth < 5) {
            if (parent.isClickable) {
                Log.i(TAG, "找到可点击的父节点 (深度: $depth)")
                val clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) {
                    Log.i(TAG, "✓ 父节点点击成功！")
                    onAdSkipped(packageName, keyword)
                    parent.recycle()
                    return true
                } else {
                    Log.w(TAG, "✗ 父节点点击失败")
                }
            }
            val oldParent = parent
            parent = parent.parent
            oldParent.recycle()
            depth++
        }
        
        // 如果都不可点击，尝试通过坐标点击
        Log.d(TAG, "尝试坐标点击")
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.isEmpty) {
            val x = rect.centerX().toFloat()
            val y = rect.centerY().toFloat()
            Log.i(TAG, "尝试点击坐标: ($x, $y)")
            val clicked = clickAtPosition(x, y)
            if (clicked) {
                Log.i(TAG, "✓ 坐标点击成功！")
                onAdSkipped(packageName, keyword)
                return true
            } else {
                Log.w(TAG, "✗ 坐标点击失败")
            }
        }
        
        return false
    }
    
    private fun tryFindByViewId(node: AccessibilityNodeInfo, packageName: String): Boolean {
        // 常见的跳过按钮 ID 关键词
        val idKeywords = listOf("skip", "close", "dismiss", "cancel", "ad", "jump")
        
        Log.d(TAG, "开始 ViewId 搜索")
        
        fun searchNode(currentNode: AccessibilityNodeInfo, depth: Int = 0): Boolean {
            if (depth > 20) return false // 限制递归深度
            
            val viewId = currentNode.viewIdResourceName?.lowercase() ?: ""
            
            // 检查 ViewId 是否包含关键词
            if (viewId.isNotEmpty() && idKeywords.any { viewId.contains(it) }) {
                Log.i(TAG, "找到匹配的 ViewId: $viewId")
                logNodeInfo(currentNode, "ViewId 匹配节点")
                if (tryClickNode(currentNode, packageName, "ViewId: $viewId")) {
                    return true
                }
            }
            
            // 递归搜索子节点
            for (i in 0 until currentNode.childCount) {
                val child = currentNode.getChild(i)
                if (child != null) {
                    if (searchNode(child, depth + 1)) {
                        child.recycle()
                        return true
                    }
                    child.recycle()
                }
            }
            
            return false
        }
        
        return searchNode(node)
    }
    
    private fun tryFindClickableNodes(node: AccessibilityNodeInfo, packageName: String, keywords: List<String>): Boolean {
        Log.d(TAG, "开始遍历可点击节点")
        
        fun searchClickableNode(currentNode: AccessibilityNodeInfo, depth: Int = 0): Boolean {
            if (depth > 20) return false
            
            // 检查当前节点
            if (currentNode.isClickable) {
                val text = currentNode.text?.toString()?.lowercase() ?: ""
                val contentDesc = currentNode.contentDescription?.toString()?.lowercase() ?: ""
                
                // 检查文本或描述是否包含关键词
                for (keyword in keywords) {
                    if (text.contains(keyword.lowercase()) || contentDesc.contains(keyword.lowercase())) {
                        Log.i(TAG, "在可点击节点中找到关键词: $keyword")
                        logNodeInfo(currentNode, "可点击节点匹配")
                        if (tryClickNode(currentNode, packageName, keyword)) {
                            return true
                        }
                    }
                }
            }
            
            // 递归搜索子节点
            for (i in 0 until currentNode.childCount) {
                val child = currentNode.getChild(i)
                if (child != null) {
                    if (searchClickableNode(child, depth + 1)) {
                        child.recycle()
                        return true
                    }
                    child.recycle()
                }
            }
            
            return false
        }
        
        return searchClickableNode(node)
    }
    
    private fun clickAtPosition(x: Float, y: Float): Boolean {
        val path = Path()
        path.moveTo(x, y)
        
        val builder = GestureDescription.Builder()
        builder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        
        return dispatchGesture(builder.build(), null, null)
    }
    
    private fun onAdSkipped(packageName: String, keyword: String) {
        lastSkipTime = System.currentTimeMillis()
        Log.i(TAG, "========================================")
        Log.i(TAG, "✓✓✓ 成功跳过广告 ✓✓✓")
        Log.i(TAG, "应用: $packageName")
        Log.i(TAG, "关键词: $keyword")
        Log.i(TAG, "========================================")
        
        scope.launch {
            try {
                val appName = getAppName(packageName)
                val record = SkipRecord(
                    timestamp = lastSkipTime,
                    packageName = packageName,
                    appName = appName,
                    keyword = keyword
                )
                (getAdSkipManager() as? AndroidAdSkipManager)?.addSkipRecord(record)
            } catch (e: Exception) {
                Log.e(TAG, "保存记录失败", e)
            }
        }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    
    // 提供外部刷新规则的方法
    fun refreshRules() {
        loadRules()
    }
}

package com.example.kotlin_test.android

import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

/**
 * 调试辅助工具 - 用于分析界面结构
 */
object DebugHelper {
    private const val TAG = "AdSkipDebug"
    
    /**
     * 打印整个节点树结构
     */
    fun printNodeTree(node: AccessibilityNodeInfo?, depth: Int = 0, maxDepth: Int = 10) {
        if (node == null || depth > maxDepth) return
        
        val indent = "  ".repeat(depth)
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""
        val className = node.className?.toString() ?: ""
        val isClickable = node.isClickable
        
        val info = buildString {
            append("$indent[$depth] $className")
            if (viewId.isNotEmpty()) append(" id=$viewId")
            if (text.isNotEmpty()) append(" text='$text'")
            if (contentDesc.isNotEmpty()) append(" desc='$contentDesc'")
            if (isClickable) append(" [CLICKABLE]")
        }
        
        Log.d(TAG, info)
        
        // 递归打印子节点
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                printNodeTree(child, depth + 1, maxDepth)
                child.recycle()
            }
        }
    }
    
    /**
     * 查找所有包含文本的节点
     */
    fun findAllTextNodes(node: AccessibilityNodeInfo?, searchText: String = ""): List<String> {
        val results = mutableListOf<String>()
        
        fun search(currentNode: AccessibilityNodeInfo?, depth: Int = 0) {
            if (currentNode == null || depth > 20) return
            
            val text = currentNode.text?.toString() ?: ""
            val contentDesc = currentNode.contentDescription?.toString() ?: ""
            val viewId = currentNode.viewIdResourceName ?: ""
            
            if (searchText.isEmpty() || 
                text.contains(searchText, ignoreCase = true) || 
                contentDesc.contains(searchText, ignoreCase = true) ||
                viewId.contains(searchText, ignoreCase = true)) {
                
                if (text.isNotEmpty() || contentDesc.isNotEmpty()) {
                    results.add("ViewId: $viewId, Text: $text, Desc: $contentDesc, Clickable: ${currentNode.isClickable}")
                }
            }
            
            for (i in 0 until currentNode.childCount) {
                val child = currentNode.getChild(i)
                search(child, depth + 1)
                child?.recycle()
            }
        }
        
        search(node)
        return results
    }
}

package com.example.kotlin_test

/**
 * 广告跳过规则
 */
data class AdSkipRule(
    val id: String,
    val name: String,
    val packageName: String = "", // 目标应用包名，空表示全局
    val keywords: List<String> = emptyList(), // 关键词列表，如 "跳过"
    val enabled: Boolean = true,
    val priority: Int = 0 // 优先级，数字越大优先级越高
)

/**
 * 跳过记录
 */
data class SkipRecord(
    val timestamp: Long,
    val packageName: String,
    val appName: String,
    val keyword: String
)

/**
 * 广告跳过管理器接口
 */
interface AdSkipManager {
    suspend fun getRules(): List<AdSkipRule>
    suspend fun addRule(rule: AdSkipRule): Result<Unit>
    suspend fun updateRule(rule: AdSkipRule): Result<Unit>
    suspend fun deleteRule(ruleId: String): Result<Unit>
    suspend fun getSkipRecords(limit: Int = 50): List<SkipRecord>
    suspend fun clearRecords(): Result<Unit>
}

expect fun getAdSkipManager(): AdSkipManager

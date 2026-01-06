package com.example.kotlin_test

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AndroidAdSkipManager(private val context: Context) : AdSkipManager {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("ad_skip_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_RULES = "rules"
        private const val KEY_RECORDS = "records"
        
        // 默认规则
        val DEFAULT_RULES = listOf(
            AdSkipRule(
                id = "default_skip",
                name = "通用跳过",
                keywords = listOf("跳过", "skip", "Skip", "SKIP")
            ),
            AdSkipRule(
                id = "default_close",
                name = "通用关闭",
                keywords = listOf("×", "✕", "关闭广告", "Close Ad")
            )
        )
    }
    
    override suspend fun getRules(): List<AdSkipRule> = withContext(Dispatchers.IO) {
        try {
            val rulesJson = prefs.getString(KEY_RULES, null)
            if (rulesJson != null) {
                val jsonArray = JSONArray(rulesJson)
                val rules = mutableListOf<AdSkipRule>()
                for (i in 0 until jsonArray.length()) {
                    rules.add(parseRule(jsonArray.getJSONObject(i)))
                }
                rules
            } else {
                // 首次使用，返回默认规则
                DEFAULT_RULES
            }
        } catch (e: Exception) {
            DEFAULT_RULES
        }
    }
    
    override suspend fun addRule(rule: AdSkipRule): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rules = getRules().toMutableList()
            rules.add(rule)
            saveRules(rules)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateRule(rule: AdSkipRule): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rules = getRules().toMutableList()
            val index = rules.indexOfFirst { it.id == rule.id }
            if (index != -1) {
                rules[index] = rule
                saveRules(rules)
                Result.success(Unit)
            } else {
                Result.failure(Exception("规则不存在"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteRule(ruleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rules = getRules().toMutableList()
            rules.removeAll { it.id == ruleId }
            saveRules(rules)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getSkipRecords(limit: Int): List<SkipRecord> = withContext(Dispatchers.IO) {
        try {
            val recordsJson = prefs.getString(KEY_RECORDS, null) ?: return@withContext emptyList()
            val jsonArray = JSONArray(recordsJson)
            val records = mutableListOf<SkipRecord>()
            val count = minOf(limit, jsonArray.length())
            for (i in 0 until count) {
                records.add(parseRecord(jsonArray.getJSONObject(i)))
            }
            records
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun clearRecords(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            prefs.edit().remove(KEY_RECORDS).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addSkipRecord(record: SkipRecord) = withContext(Dispatchers.IO) {
        try {
            val records = getSkipRecords(500).toMutableList()
            records.add(0, record) // 添加到开头
            
            // 只保留最近500条
            val jsonArray = JSONArray()
            records.take(500).forEach { jsonArray.put(recordToJson(it)) }
            
            prefs.edit().putString(KEY_RECORDS, jsonArray.toString()).apply()
        } catch (e: Exception) {
            // 忽略错误
        }
    }
    
    private fun saveRules(rules: List<AdSkipRule>) {
        val jsonArray = JSONArray()
        rules.forEach { jsonArray.put(ruleToJson(it)) }
        prefs.edit().putString(KEY_RULES, jsonArray.toString()).apply()
    }
    
    private fun ruleToJson(rule: AdSkipRule): JSONObject {
        return JSONObject().apply {
            put("id", rule.id)
            put("name", rule.name)
            put("packageName", rule.packageName)
            put("keywords", JSONArray(rule.keywords))
            put("enabled", rule.enabled)
            put("priority", rule.priority)
        }
    }
    
    private fun parseRule(json: JSONObject): AdSkipRule {
        val keywordsArray = json.getJSONArray("keywords")
        val keywords = mutableListOf<String>()
        for (i in 0 until keywordsArray.length()) {
            keywords.add(keywordsArray.getString(i))
        }
        
        return AdSkipRule(
            id = json.getString("id"),
            name = json.getString("name"),
            packageName = json.optString("packageName", ""),
            keywords = keywords,
            enabled = json.optBoolean("enabled", true),
            priority = json.optInt("priority", 0)
        )
    }
    
    private fun recordToJson(record: SkipRecord): JSONObject {
        return JSONObject().apply {
            put("timestamp", record.timestamp)
            put("packageName", record.packageName)
            put("appName", record.appName)
            put("keyword", record.keyword)
        }
    }
    
    private fun parseRecord(json: JSONObject): SkipRecord {
        return SkipRecord(
            timestamp = json.getLong("timestamp"),
            packageName = json.getString("packageName"),
            appName = json.getString("appName"),
            keyword = json.getString("keyword")
        )
    }
}

private var adSkipManagerInstance: AdSkipManager? = null

fun initAdSkipManager(context: Context) {
    adSkipManagerInstance = AndroidAdSkipManager(context.applicationContext)
}

actual fun getAdSkipManager(): AdSkipManager {
    return adSkipManagerInstance ?: throw IllegalStateException("AdSkipManager 未初始化")
}

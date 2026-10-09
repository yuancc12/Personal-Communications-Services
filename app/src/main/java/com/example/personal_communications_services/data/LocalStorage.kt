package com.example.personal_communications_services.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * 輕量 JSON 檔案儲存，取代 Room 資料庫。
 * 使用 Gson 序列化，儲存於 App 私有目錄，不需 KSP 或 annotation processing。
 */
class LocalStorage private constructor(context: Context) {

    private val dir  = context.filesDir
    private val gson = Gson()

    // ── 偵測紀錄 ──────────────────────────────────────────────────────────
    private val recordsFile = File(dir, "detection_records.json")
    private val _records    = MutableStateFlow(loadRecords())

    fun getAllRecords(): Flow<List<DetectionRecordEntity>> = _records.asStateFlow()

    fun insert(record: DetectionRecordEntity): Long {
        val id      = System.currentTimeMillis()
        val updated = listOf(record.copy(id = id)) + _records.value
        save(recordsFile, updated)
        _records.value = updated
        return id
    }

    fun deleteById(id: Long) {
        val updated = _records.value.filter { it.id != id }
        save(recordsFile, updated)
        _records.value = updated
    }

    fun deleteAll() {
        save(recordsFile, emptyList<DetectionRecordEntity>())
        _records.value = emptyList()
    }

    private fun loadRecords(): List<DetectionRecordEntity> = load(
        recordsFile, object : TypeToken<List<DetectionRecordEntity>>() {}.type
    )

    // ── 黑名單 ────────────────────────────────────────────────────────────
    private val blacklistFile = File(dir, "blacklist.json")
    private val _blacklist    = MutableStateFlow(loadBlacklist())

    fun getBlacklist(): Flow<List<BlacklistEntity>> = _blacklist.asStateFlow()

    fun insertBlacklist(entry: BlacklistEntity) {
        val updated = listOf(entry) + _blacklist.value.filter { it.phoneNumber != entry.phoneNumber }
        save(blacklistFile, updated)
        _blacklist.value = updated
    }

    fun deleteBlacklist(phoneNumber: String) {
        val updated = _blacklist.value.filter { it.phoneNumber != phoneNumber }
        save(blacklistFile, updated)
        _blacklist.value = updated
    }

    fun isBlacklisted(number: String): Boolean =
        _blacklist.value.any { it.phoneNumber == number }

    private fun loadBlacklist(): List<BlacklistEntity> = load(
        blacklistFile, object : TypeToken<List<BlacklistEntity>>() {}.type
    )

    // ── 通用 ──────────────────────────────────────────────────────────────
    private fun <T> load(file: File, type: java.lang.reflect.Type): List<T> {
        if (!file.exists()) return emptyList()
        return try { gson.fromJson(file.readText(), type) ?: emptyList() }
        catch (_: Exception) { emptyList() }
    }

    private fun save(file: File, data: Any) {
        try { file.writeText(gson.toJson(data)) } catch (_: Exception) {}
    }

    companion object {
        @Volatile private var INSTANCE: LocalStorage? = null

        fun getInstance(context: Context): LocalStorage =
            INSTANCE ?: synchronized(this) {
                LocalStorage(context.applicationContext).also { INSTANCE = it }
            }
    }
}

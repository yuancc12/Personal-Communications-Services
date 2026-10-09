package com.example.personal_communications_services.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/** 應用設定持久化，使用 DataStore Preferences */
class AppPreferences(private val ctx: Context) {
    companion object {
        val KEY_AUTO_MONITOR    = booleanPreferencesKey("auto_monitor")
        val KEY_LLM_ENABLED     = booleanPreferencesKey("llm_enabled")
        val KEY_SAVE_FULL_BODY  = booleanPreferencesKey("save_full_body")
        val KEY_PARENT_NUMBER   = stringPreferencesKey("parent_number")
        val KEY_NOTIFY_PARENT   = booleanPreferencesKey("notify_parent")
    }

    val autoMonitorEnabled: Flow<Boolean> = ctx.dataStore.data.map { it[KEY_AUTO_MONITOR]   ?: false }
    val llmEnabled:         Flow<Boolean> = ctx.dataStore.data.map { it[KEY_LLM_ENABLED]    ?: false }
    val saveFullBody:       Flow<Boolean> = ctx.dataStore.data.map { it[KEY_SAVE_FULL_BODY] ?: false }
    val parentNumber:       Flow<String>  = ctx.dataStore.data.map { it[KEY_PARENT_NUMBER]  ?: "" }
    val notifyParent:       Flow<Boolean> = ctx.dataStore.data.map { it[KEY_NOTIFY_PARENT]  ?: false }

    suspend fun setAutoMonitor(v: Boolean)  { ctx.dataStore.edit { it[KEY_AUTO_MONITOR]   = v } }
    suspend fun setLlmEnabled(v: Boolean)   { ctx.dataStore.edit { it[KEY_LLM_ENABLED]    = v } }
    suspend fun setSaveFullBody(v: Boolean) { ctx.dataStore.edit { it[KEY_SAVE_FULL_BODY] = v } }
    suspend fun setParentNumber(v: String)  { ctx.dataStore.edit { it[KEY_PARENT_NUMBER]  = v } }
    suspend fun setNotifyParent(v: Boolean) { ctx.dataStore.edit { it[KEY_NOTIFY_PARENT]  = v } }
}

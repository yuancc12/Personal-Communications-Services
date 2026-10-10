package com.example.personal_communications_services.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personal_communications_services.data.BlacklistEntity
import com.example.personal_communications_services.data.DetectionRecordEntity
import com.example.personal_communications_services.data.LocalStorage
import com.example.personal_communications_services.model.RiskLevel
import com.example.personal_communications_services.model.ScamType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = LocalStorage.getInstance(app)

    // 篩選條件
    val filterRisk     = MutableStateFlow<RiskLevel?>(null)   // null = 全部
    val filterScamType = MutableStateFlow<ScamType?>(null)    // null = 全部

    private val allRecords = storage.getAllRecords()
        .map { list -> list.sortedByDescending { it.timestamp } }

    // 套用篩選後的記錄
    val records: StateFlow<List<DetectionRecordEntity>> =
        combine(allRecords, filterRisk, filterScamType) { list, risk, type ->
            list.filter { record ->
                val riskMatch = risk == null ||
                    runCatching { RiskLevel.valueOf(record.riskLevel) }.getOrNull() == risk
                val typeMatch = type == null ||
                    record.scamType == type.name
                riskMatch && typeMatch
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val blacklist: StateFlow<List<BlacklistEntity>> = storage.getBlacklist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteRecord(id: Long) = viewModelScope.launch { storage.deleteById(id) }
    fun deleteAll()            = viewModelScope.launch { storage.deleteAll() }

    fun addToBlacklist(number: String) = viewModelScope.launch {
        storage.insertBlacklist(BlacklistEntity(phoneNumber = number.trim()))
    }
    fun removeFromBlacklist(number: String) = viewModelScope.launch {
        storage.deleteBlacklist(number)
    }
}

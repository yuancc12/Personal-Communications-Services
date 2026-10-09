package com.example.personal_communications_services.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personal_communications_services.data.BlacklistEntity
import com.example.personal_communications_services.data.DetectionRecordEntity
import com.example.personal_communications_services.data.LocalStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = LocalStorage.getInstance(app)

    val records: StateFlow<List<DetectionRecordEntity>> = storage.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

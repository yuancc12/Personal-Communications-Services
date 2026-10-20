package com.example.smsvote.ui.sendsms

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import androidx.lifecycle.AndroidViewModel
import com.example.smsvote.data.model.Poll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SmsStatus { PENDING, SENT, DELIVERED, FAILED }

data class SmsDeliveryState(val phone: String, val status: SmsStatus = SmsStatus.PENDING)

data class SendSmsUiState(
    val phoneNumbers: String = "",
    val isSending: Boolean = false,
    val deliveryStates: List<SmsDeliveryState> = emptyList(),
    val allSent: Boolean = false,
    val errorMessage: String? = null
)

class SendSmsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SendSmsUiState())
    val uiState: StateFlow<SendSmsUiState> = _uiState.asStateFlow()

    private val sentReceivers = mutableListOf<BroadcastReceiver>()

    fun onPhoneNumbersChange(value: String) = _uiState.update { it.copy(phoneNumbers = value) }

    fun resetForNewSession() = _uiState.update {
        SendSmsUiState()
    }

    fun sendSms(poll: Poll) {
        val numbers = _uiState.value.phoneNumbers
            .split(",", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (numbers.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "請輸入至少一個電話號碼") }
            return
        }

        val deliveryStates = numbers.map { SmsDeliveryState(it) }
        _uiState.update { it.copy(isSending = true, errorMessage = null, deliveryStates = deliveryStates) }

        val message = buildSmsMessage(poll)
        val smsManager = getApplication<Application>().getSystemService(SmsManager::class.java)
        val context = getApplication<Application>()

        numbers.forEachIndexed { index, number ->
            val sentAction = "SMS_SENT_$index"
            val deliveredAction = "SMS_DELIVERED_$index"

            val sentIntent = PendingIntent.getBroadcast(
                context, index,
                Intent(sentAction),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val deliveredIntent = PendingIntent.getBroadcast(
                context, index + 1000,
                Intent(deliveredAction),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val sentReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val success = resultCode == android.app.Activity.RESULT_OK
                    updateStatus(index, if (success) SmsStatus.SENT else SmsStatus.FAILED)
                    ctx.unregisterReceiver(this)
                }
            }
            val deliveredReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    updateStatus(index, SmsStatus.DELIVERED)
                    ctx.unregisterReceiver(this)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(sentReceiver, IntentFilter(sentAction), Context.RECEIVER_NOT_EXPORTED)
                context.registerReceiver(deliveredReceiver, IntentFilter(deliveredAction), Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(sentReceiver, IntentFilter(sentAction))
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(deliveredReceiver, IntentFilter(deliveredAction))
            }
            sentReceivers.add(sentReceiver)

            try {
                val parts = smsManager.divideMessage(message)
                if (parts.size == 1) {
                    smsManager.sendTextMessage(number, null, message, sentIntent, deliveredIntent)
                } else {
                    val sentIntents = ArrayList(parts.map { sentIntent })
                    val deliveredIntents = ArrayList(parts.map { deliveredIntent })
                    smsManager.sendMultipartTextMessage(number, null, parts, sentIntents, deliveredIntents)
                }
            } catch (e: Exception) {
                updateStatus(index, SmsStatus.FAILED)
                _uiState.update { it.copy(errorMessage = "發送失敗：${e.localizedMessage}") }
            }
        }
        _uiState.update { it.copy(isSending = false) }
    }

    private fun updateStatus(index: Int, status: SmsStatus) {
        _uiState.update { state ->
            val updated = state.deliveryStates.toMutableList()
            if (index < updated.size) updated[index] = updated[index].copy(status = status)
            val allSent = updated.isNotEmpty() && updated.all { it.status == SmsStatus.SENT || it.status == SmsStatus.DELIVERED }
            state.copy(deliveryStates = updated, allSent = allSent)
        }
    }

    private fun buildSmsMessage(poll: Poll): String {
        val optionLines = poll.options.mapIndexed { i, opt -> "${'A' + i}：$opt" }.joinToString("\n")
        val lastLabel = ('A' + poll.options.size - 1)
        val instruction = if (poll.maxChoices == 1) {
            "請回覆「${poll.id.take(6)} A」投票（A~$lastLabel 擇一）"
        } else {
            "請回覆「${poll.id.take(6)} AB」投票（A~$lastLabel 最多選 ${poll.maxChoices} 個）"
        }
        return "【SMSVote 投票通知】\n${poll.question}\n$optionLines\n$instruction"
    }

    override fun onCleared() {
        super.onCleared()
        val context = getApplication<Application>()
        sentReceivers.forEach { runCatching { context.unregisterReceiver(it) } }
    }
}

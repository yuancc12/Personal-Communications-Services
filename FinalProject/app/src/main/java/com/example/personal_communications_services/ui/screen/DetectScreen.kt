package com.example.personal_communications_services.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personal_communications_services.ui.component.ResultCard
import com.example.personal_communications_services.viewmodel.DetectViewModel

@Composable
fun DetectScreen(vm: DetectViewModel = viewModel()) {
    var inputText by remember { mutableStateOf("") }
    val result    by vm.result.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("詐騙簡訊偵測", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label       = { Text("貼上簡訊內容") },
            placeholder = { Text("在此貼上收到的簡訊…") },
            modifier    = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            maxLines = 10
        )

        Button(
            onClick  = { vm.analyze(inputText) },
            modifier = Modifier.align(Alignment.End),
            enabled  = inputText.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("分析中…")
            } else {
                Text("🔍 開始檢測")
            }
        }

        result?.let { ResultCard(result = it) }
    }
}

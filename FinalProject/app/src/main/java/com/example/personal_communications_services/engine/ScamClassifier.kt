package com.example.personal_communications_services.engine

import com.example.personal_communications_services.model.DetectionResult

/** AI 分類器統一介面；方便 Mock 測試或替換不同供應商 */
interface ScamClassifier {
    suspend fun classify(message: String): DetectionResult
}

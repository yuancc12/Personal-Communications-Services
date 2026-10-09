package com.example.personal_communications_services.engine

import com.example.personal_communications_services.model.DetectionResult

/** LLM 二次判斷的統一介面；方便 Mock 測試或替換不同 LLM 供應商 */
interface ScamClassifier {
    /** 輸入原始訊息與規則結果，回傳（可能修正過的）最終結果 */
    suspend fun classify(message: String, ruleResult: DetectionResult): DetectionResult
}

package com.example.personal_communications_services.engine

import com.example.personal_communications_services.model.DetectionResult
import com.example.personal_communications_services.model.RiskLevel
import com.example.personal_communications_services.model.toRiskLevel

/**
 * 本地規則引擎：純 Kotlin，無 Android 依賴，方便單元測試。
 * 採加權評分，上限 100 分，輸出 DetectionResult。
 */
object RuleEngine {

    // ── 規則1：常見短網址網域 ────────────────────────────────────────────
    private val SHORT_URL_DOMAINS = setOf(
        "bit.ly", "goo.gl", "tinyurl.com", "reurl.cc", "pse.is",
        "lurl.cc", "is.gd", "ow.ly", "t.co", "lihi.cc", "lihi1.cc",
        "0rz.tw", "kurl.info", "ppt.cc", "fstw.me"
    )

    // ── 規則2：可疑關鍵字與對應分數 ─────────────────────────────────────
    private val SUSPICIOUS_KEYWORDS = mapOf(
        "帳戶凍結" to 20, "帳號凍結" to 20, "帳戶暫停" to 15,
        "包裹地址有誤" to 20, "包裹無法配送" to 18, "快遞異常" to 15,
        "包裹滯留" to 15, "無法投遞" to 12,
        "點擊領取" to 20, "立即領取" to 18, "領取獎品" to 20,
        "中獎" to 20, "恭喜您中" to 22, "恭喜中獎" to 22,
        "補繳費用" to 20, "補繳稅金" to 20, "補繳關稅" to 20,
        "欠費" to 15, "逾期未繳" to 18,
        "立即驗證" to 15, "馬上驗證" to 15,
        "帳號即將" to 15, "身份驗證" to 12, "重新驗證" to 12
    )

    // ── 規則4：催促語氣關鍵字 ────────────────────────────────────────────
    private val URGENCY_KEYWORDS = listOf(
        "立即", "馬上", "緊急", "限時", "今日到期", "今天到期",
        "否則將", "不然將", "即將停用", "即將凍結",
        "24小時內", "48小時內", "請儘快", "盡快處理"
    )

    // ── 規則5：個資與金融資訊要求 ────────────────────────────────────────
    private val PERSONAL_INFO_KEYWORDS = listOf(
        "信用卡", "銀行帳號", "身分證字號", "身份證", "密碼",
        "卡號", "輸入個資", "提供資料", "個人資料",
        "CVV", "驗證碼", "OTP", "轉帳"
    )

    // ── 規則3：官方機構名稱→合法網域對照表 ─────────────────────────────
    private val OFFICIAL_DOMAINS = mapOf(
        "台灣電力" to listOf("taipower.com.tw"),
        "台電"    to listOf("taipower.com.tw"),
        "中華電信" to listOf("cht.com.tw", "emome.net"),
        "國稅局"  to listOf("ntbt.gov.tw", "tax.nat.gov.tw", "gov.tw"),
        "健保署"  to listOf("nhi.gov.tw"),
        "勞保局"  to listOf("bli.gov.tw"),
        "郵局"    to listOf("post.gov.tw"),
        "中華郵政" to listOf("post.gov.tw"),
        "7-ELEVEN" to listOf("7-eleven.com.tw"),
        "全家"    to listOf("family.com.tw"),
        "蝦皮"    to listOf("shopee.tw", "shopee.com"),
        "露天"    to listOf("ruten.com.tw"),
        "PChome" to listOf("pchome.com.tw"),
        "momo"   to listOf("momoshop.com.tw")
    )

    // ────────────────────────────────────────────────────────────────────

    // ── 規則引擎已暫停，改由 JEV 統一判斷 ──────────────────────────────
    @Suppress("unused")
    fun analyze(message: String): DetectionResult {
        /*
        val reasons = mutableListOf<String>()
        var score = 0

        score += checkShortUrls(message, reasons)
        score += checkSuspiciousKeywords(message, reasons)
        score += checkOfficialMismatch(message, reasons)
        score += checkUrgency(message, reasons)
        score += checkPersonalInfoRequest(message, reasons)

        val finalScore = score.coerceIn(0, 100)
        return DetectionResult(
            score = finalScore,
            riskLevel = finalScore.toRiskLevel(),
            reasons = reasons
        )
        */
        return DetectionResult(score = 0, riskLevel = RiskLevel.SAFE, reasons = emptyList())
    }

    // ── 規則1 ────────────────────────────────────────────────────────────
    private fun checkShortUrls(msg: String, reasons: MutableList<String>): Int {
        val found = SHORT_URL_DOMAINS.filter { msg.contains(it, ignoreCase = true) }
        return if (found.isNotEmpty()) {
            reasons += "⚠️ 含短網址（${found.first()}），常用於隱藏真實詐騙連結"
            25
        } else 0
    }

    // ── 規則2 ────────────────────────────────────────────────────────────
    private fun checkSuspiciousKeywords(msg: String, reasons: MutableList<String>): Int {
        val hits = SUSPICIOUS_KEYWORDS.entries.filter { msg.contains(it.key) }
        if (hits.isEmpty()) return 0
        val topKeywords = hits.map { it.key }.take(3).joinToString("、")
        reasons += "🚨 可疑關鍵字：$topKeywords"
        return hits.sumOf { it.value }.coerceAtMost(40)
    }

    // ── 規則3 ────────────────────────────────────────────────────────────
    private fun checkOfficialMismatch(msg: String, reasons: MutableList<String>): Int {
        val urlsInMsg = Regex("""https?://[^\s]+""").findAll(msg).map { it.value }.toList()
        if (urlsInMsg.isEmpty()) return 0
        for ((org, legit) in OFFICIAL_DOMAINS) {
            if (!msg.contains(org)) continue
            val hasLegit = urlsInMsg.any { url -> legit.any { url.contains(it, true) } }
            if (!hasLegit) {
                reasons += "🔴 自稱「$org」但連結網域與官方不符"
                return 30
            }
        }
        return 0
    }

    // ── 規則4 ────────────────────────────────────────────────────────────
    private fun checkUrgency(msg: String, reasons: MutableList<String>): Int {
        val count = URGENCY_KEYWORDS.count { msg.contains(it) }
        return when {
            count >= 2 -> { reasons += "⏰ 強烈催促語氣（${count} 個催促詞）"; 15 }
            count == 1 -> { reasons += "⏰ 含催促語氣"; 8 }
            else -> 0
        }
    }

    // ── 規則5 ────────────────────────────────────────────────────────────
    private fun checkPersonalInfoRequest(msg: String, reasons: MutableList<String>): Int {
        val hits = PERSONAL_INFO_KEYWORDS.filter { msg.contains(it) }
        return if (hits.isNotEmpty()) {
            reasons += "🔒 要求提供個人／金融資料（${hits.take(2).joinToString("、")}）"
            25
        } else 0
    }
}

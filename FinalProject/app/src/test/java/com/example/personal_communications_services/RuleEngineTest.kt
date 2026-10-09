package com.example.personal_communications_services

import com.example.personal_communications_services.engine.RuleEngine
import com.example.personal_communications_services.model.RiskLevel
import org.junit.Assert.*
import org.junit.Test

/**
 * RuleEngine 單元測試：純 JVM，不需 Android 環境。
 * 涵蓋 4 則正常簡訊 + 8 則詐騙簡訊，共 12 組。
 */
class RuleEngineTest {

    // ════════════════════════════════════════
    //  正常簡訊（預期：SAFE）
    // ════════════════════════════════════════

    @Test
    fun `家人問候簡訊應為安全`() {
        val r = RuleEngine.analyze("媽媽：今天晚餐吃什麼？記得早點回家喔。")
        assertEquals(RiskLevel.SAFE, r.riskLevel)
        assertEquals(0, r.score)
    }

    @Test
    fun `正常快遞通知應為安全`() {
        val r = RuleEngine.analyze(
            "您的包裹將於今日下午 2-4 點配送，請保持電話暢通。物流單號：TW987654321"
        )
        assertEquals(RiskLevel.SAFE, r.riskLevel)
    }

    @Test
    fun `官方網域銀行通知應為安全或可疑但非高風險`() {
        val r = RuleEngine.analyze(
            "您的帳戶昨日有一筆交易。如有疑問請至 https://www.esunbank.com.tw 查詢。"
        )
        assertNotEquals(RiskLevel.HIGH_RISK, r.riskLevel)
    }

    @Test
    fun `純文字促銷無連結應為安全`() {
        val r = RuleEngine.analyze("【全家】本週購買指定商品享 8 折，詳情請洽門市人員。")
        assertEquals(RiskLevel.SAFE, r.riskLevel)
        assertTrue(r.reasons.isEmpty())
    }

    // ════════════════════════════════════════
    //  詐騙簡訊（預期：SUSPICIOUS 或 HIGH_RISK）
    // ════════════════════════════════════════

    @Test
    fun `含短網址應觸發可疑以上`() {
        val r = RuleEngine.analyze(
            "您的包裹地址有誤，請至 bit.ly/fix123 重新填寫，否則退件。"
        )
        assertTrue(r.score >= 31)
        assertTrue(r.reasons.any { it.contains("短網址") })
    }

    @Test
    fun `帳戶凍結加短網址應為高風險`() {
        val r = RuleEngine.analyze(
            "【銀行通知】您的帳戶凍結，請立即驗證身份，否則將永久停用：reurl.cc/verify99"
        )
        assertEquals(RiskLevel.HIGH_RISK, r.riskLevel)
    }

    @Test
    fun `中獎詐騙應為高風險`() {
        val r = RuleEngine.analyze(
            "恭喜您中獎！點擊領取 iPhone 15 Pro，限時 24 小時內領取，否則取消資格。"
        )
        assertEquals(RiskLevel.HIGH_RISK, r.riskLevel)
    }

    @Test
    fun `包裹滯留加催促語氣應觸發可疑以上`() {
        val r = RuleEngine.analyze(
            "您的包裹滯留海關，請馬上驗證身份：pse.is/parcel"
        )
        assertTrue(r.score >= 31)
        assertTrue(r.reasons.any { it.contains("短網址") || it.contains("關鍵字") || it.contains("催促") })
    }

    @Test
    fun `信用卡個資竊取應為高風險`() {
        val r = RuleEngine.analyze(
            "您的信用卡遭異常刷卡，請立即輸入個資與卡號至以下連結重設：lurl.cc/reset"
        )
        assertEquals(RiskLevel.HIGH_RISK, r.riskLevel)
        assertTrue(r.reasons.any { it.contains("個人") || it.contains("信用卡") })
    }

    @Test
    fun `假冒國稅局補繳費用應為高風險`() {
        val r = RuleEngine.analyze(
            "【國稅局】您有未申報稅款，需補繳費用 3850 元，請立即至 bit.ly/tax 繳納，否則移送法辦。"
        )
        assertEquals(RiskLevel.HIGH_RISK, r.riskLevel)
    }

    @Test
    fun `多個催促詞應累積分數觸發可疑以上`() {
        val r = RuleEngine.analyze(
            "緊急：您的帳號即將凍結，請馬上處理，否則將停用，24小時內未完成視同放棄。"
        )
        assertTrue(r.score >= 31)
        assertTrue(r.reasons.any { it.contains("催促") })
    }

    @Test
    fun `官方機構名稱加非官方網域應觸發網域不符規則`() {
        val r = RuleEngine.analyze(
            "【台電】您本月電費尚未繳納，請立即至 https://taipower-pay.xyz 繳費。"
        )
        assertTrue(r.score >= 31)
        assertTrue(r.reasons.any { it.contains("網域") || it.contains("台電") })
    }
}

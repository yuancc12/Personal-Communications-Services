package com.example.personal_communications_services.llm

/**
 * 去識別化工具：送 LLM 前遮蔽手機號碼、銀行帳號、身分證字號。
 * 確保個人資料不離開裝置。
 */
object Deidentifier {
    private val PHONE   = Regex("""09\d{2}[-\s]?\d{3}[-\s]?\d{3}""")
    private val ID_NUM  = Regex("""[A-Z]\d{9}""")
    private val ACCOUNT = Regex("""\b\d{8,16}\b""")

    fun deidentify(text: String): String =
        text.replace(PHONE,   "[手機號碼]")
            .replace(ID_NUM,  "[身分證字號]")
            .replace(ACCOUNT, "[數字帳號]")
}

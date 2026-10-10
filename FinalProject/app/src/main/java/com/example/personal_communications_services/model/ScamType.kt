package com.example.personal_communications_services.model

enum class ScamType(val label: String, val emoji: String) {
    PACKAGE ("包裹詐騙", "📦"),
    LOTTERY ("中獎詐騙", "🎰"),
    FINANCIAL("金融詐騙", "💳"),
    IDENTITY ("身份驗證詐騙", "🪪"),
    NONE    ("一般訊息", "✅")
}

# 詐騙簡訊偵測 App

Android 期末專案 — 行動通訊課程

## 功能說明

| 階段 | 功能 |
|------|------|
| 1 | 手動貼上簡訊、規則檢測、結果卡片顯示 |
| 2 | 本地 RuleEngine 加權評分（0-100）+ 10+ 單元測試 |
| 3 | LLM 二次判斷（灰色地帶 31-70 分才呼叫，送出前去識別化） |
| 4 | BroadcastReceiver 自動監聽、高風險推送通知、Room 資料庫 |
| 5 | 黑名單管理、歷史紀錄頁、設定頁（開關自動監聽 / AI） |

---

## 環境設定

### 1. 設定 API Key（使用 LLM 功能時才需要）

在專案根目錄的 `local.properties` 加入：

```
ANTHROPIC_API_KEY=sk-ant-api03-你的金鑰
```

> `local.properties` 已在 `.gitignore` 中，**不會進入版本控制**。

### 2. Sync Gradle

在 Android Studio 按 **Sync Now** 或執行：

```bash
./gradlew build
```

---

## 在模擬器中測試簡訊

### 方法 A：Android Studio Extended Controls

1. 啟動模擬器
2. 點擊模擬器側邊工具列「**...**」→ **Phone** 分頁
3. 在 **From** 欄輸入假號碼（如 `0912345678`）
4. 在 **SMS Message** 欄貼上測試簡訊
5. 按 **Send Message**

### 方法 B：adb 指令

```bash
# 格式
adb emu sms send <號碼> <簡訊內容>

# 範例（正常簡訊）
adb emu sms send 0912345678 "媽媽：今天晚餐吃什麼？記得早點回家。"

# 範例（高風險詐騙）
adb emu sms send 0987654321 "您的帳戶凍結，請立即驗證：bit.ly/verify99"
```

> 自動偵測功能需先在 App **設定頁 → 自動監聽簡訊** 開啟，並授予 `RECEIVE_SMS` 權限。

---

## 8 則測試簡訊

### 正常簡訊（預期：安全）

```
1. 媽媽：今天晚餐吃什麼？記得早點回家喔。

2. 您的包裹（單號 TW987654321）將於今日下午 2-4 點配送，請保持電話暢通。

3. 【全家便利商店】您的訂單已準備完畢，請於 3 天內至門市取件。

4. 會議提醒：明天下午 2 點 PM 在 B103 會議室，請記得帶提案資料。
```

### 詐騙簡訊（預期：可疑／高風險）

```
5. 【銀行通知】您的帳戶凍結，請立即驗證身份，否則永久停用：
   reurl.cc/bankverify

6. 恭喜您中獎！點擊領取 iPhone 15 Pro 大獎，限時 24 小時，否則取消：
   bit.ly/win2024

7. 您的包裹地址有誤無法配送，請馬上至下列連結重新填寫，否則退回：
   pse.is/parcel-fix

8. 【國稅局】您有未申報稅款需補繳費用 3,850 元，請立即至以下連結繳納，
   否則移送法辦：bit.ly/taxfee99
```

---

## 執行單元測試

```bash
# 執行 RuleEngine 的 12 組 JVM 單元測試（不需模擬器）
./gradlew :app:test

# 查看測試報告
# 開啟 app/build/reports/tests/testDebugUnitTest/index.html
```

---

## 安全設計摘要

- 簡訊內容**僅在本機處理**，不傳送至外部
- 送 LLM 前一律透過 `Deidentifier` 遮蔽手機號碼、身分證、銀行帳號
- API Key 存於 `local.properties`，不寫死進程式碼，不進 git
- 不在 Logcat 輸出任何簡訊原文
- 最小權限原則：`RECEIVE_SMS` 需使用者主動在設定頁開啟

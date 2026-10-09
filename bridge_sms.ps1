# SMS Bridge: 監聽孩子手機 (emulator-5556) 偵測到 HIGH_RISK 時
#             自動發警報簡訊給家長手機 (emulator-5558)

$adb        = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$childDev   = "emulator-5556"
$parentDev  = "emulator-5558"

Write-Host "=== SMS 橋接腳本啟動 ===" -ForegroundColor Cyan
Write-Host "監聽孩子手機 ($childDev)..." -ForegroundColor Yellow
Write-Host "偵測到 HIGH_RISK 時自動發簡訊給家長 ($parentDev)" -ForegroundColor Yellow
Write-Host "按 Ctrl+C 停止`n" -ForegroundColor Gray

& $adb -s $childDev logcat -s "PARENT_ALERT" | ForEach-Object {
    $line = $_
    if ($line -match "HIGH_RISK\|(.+?)\|(.+)") {
        $sender  = $Matches[1]
        $body    = $Matches[2]
        $alert   = "【詐騙警報】您的孩子收到疑似詐騙簡訊，發件人：$sender，內容：$body"

        Write-Host "`n[$(Get-Date -Format 'HH:mm:ss')] 偵測到 HIGH_RISK！發送警報給家長..." -ForegroundColor Red
        Write-Host "  發件人：$sender" -ForegroundColor White
        Write-Host "  內容：$body" -ForegroundColor White

        & $adb -s $parentDev emu sms send $childDev $alert
        Write-Host "  ✓ 警報已送至家長手機 ($parentDev)" -ForegroundColor Green
    }
}

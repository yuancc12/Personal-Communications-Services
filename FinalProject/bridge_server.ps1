# SMS Bridge Server
# 監聽 port 8765，收到 /alert 請求就把 SMS 注入家長模擬器 (emulator-5558)

$adb       = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$parentDev = "emulator-5558"
$port      = 8765

$listener = [System.Net.HttpListener]::new()
$listener.Prefixes.Add("http://localhost:$port/")
$listener.Start()

Write-Host "=== SMS Bridge Server 啟動 ===" -ForegroundColor Cyan
Write-Host "監聽 http://localhost:$port/" -ForegroundColor Yellow
Write-Host "家長模擬器：$parentDev" -ForegroundColor Yellow
Write-Host "按 Ctrl+C 停止`n"

while ($listener.IsListening) {
    $ctx = $listener.GetContext()
    $req = $ctx.Request
    $res = $ctx.Response

    if ($req.Url.AbsolutePath -eq "/alert") {
        $reader = [System.IO.StreamReader]::new($req.InputStream)
        $body   = $reader.ReadToEnd()
        $reader.Close()

        # body 格式: sender=XXX&message=YYY
        $params  = [System.Web.HttpUtility]::ParseQueryString($body)
        $sender  = $params["sender"]
        $message = $params["message"]

        $alertText = "【詐騙警報】孩子收到詐騙簡訊！發件人：$sender，內容：$message"

        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] 收到警報請求 → 發 SMS 給家長" -ForegroundColor Red
        Write-Host "  發件人：$sender  |  內容：$message" -ForegroundColor White

        & $adb -s $parentDev emu sms send "5556" $alertText
        Write-Host "  OK 已注入家長模擬器" -ForegroundColor Green

        $resBytes = [System.Text.Encoding]::UTF8.GetBytes("OK")
        $res.StatusCode = 200
        $res.OutputStream.Write($resBytes, 0, $resBytes.Length)
    }
    $res.Close()
}

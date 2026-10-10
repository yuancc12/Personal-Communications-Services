# SMS Bridge Server - TcpListener (compatible with Android emulator 10.0.2.2)

$adb       = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$parentDev = "emulator-5558"
$port      = 9876

$tcp = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Any, $port)
$tcp.Start()

Write-Host "=== SMS Bridge Server Started ===" -ForegroundColor Cyan
Write-Host "Port: $port  |  Parent: $parentDev" -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop`n"

while ($true) {
    $client = $tcp.AcceptTcpClient()
    $stream = $client.GetStream()

    try {
        $bytes = New-Object byte[] 4096
        $total = 0
        $raw   = ""
        do {
            $n    = $stream.Read($bytes, 0, $bytes.Length)
            $raw += [System.Text.Encoding]::UTF8.GetString($bytes, 0, $n)
            $total += $n
        } while ($stream.DataAvailable)

        $bodyStart = $raw.IndexOf("`r`n`r`n")
        $body = if ($bodyStart -ge 0) { $raw.Substring($bodyStart + 4) } else { "" }

        $sender  = ""
        $message = ""
        foreach ($pair in $body -split "&") {
            $kv = $pair -split "=", 2
            if ($kv.Count -eq 2) {
                $key = [System.Uri]::UnescapeDataString($kv[0].Trim())
                $val = [System.Uri]::UnescapeDataString($kv[1].Trim().Replace("+", " "))
                if ($key -eq "sender")  { $sender  = $val }
                if ($key -eq "message") { $message = $val }
            }
        }

        if ($raw -match "POST /alert") {
            $alertText = "Parent Alert: Your child received a high-risk SMS. Sender: $sender. Please check their phone."

            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Alert -> parent emulator" -ForegroundColor Red
            Write-Host "  Sender:  $sender"  -ForegroundColor White
            Write-Host "  Message: $message" -ForegroundColor White

            & $adb -s $parentDev emu sms send "5556" $alertText

            Write-Host "  OK`n" -ForegroundColor Green
        }

        $response = "HTTP/1.1 200 OK`r`nContent-Length: 2`r`nConnection: close`r`n`r`nOK"
        $respBytes = [System.Text.Encoding]::UTF8.GetBytes($response)
        $stream.Write($respBytes, 0, $respBytes.Length)
        $stream.Flush()
    } catch {
        Write-Host "Error: $_" -ForegroundColor Red
    } finally {
        $client.Close()
    }
}

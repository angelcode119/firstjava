# 🧪 اسکریپت تست Ping از تاپیک
# استفاده: .\test_ping_topic.ps1

Write-Host ""
Write-Host "════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "🧪 Firebase Topic Ping Test" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════" -ForegroundColor Magenta
Write-Host ""

# چک کردن ADB
$adbCheck = adb version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ ADB not found!" -ForegroundColor Red
    exit 1
}

# چک کردن اتصال دستگاه
Write-Host "🔌 Checking device connection..." -ForegroundColor Cyan
$devices = adb devices
if ($devices -notmatch "device$") {
    Write-Host "❌ No device connected!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Device connected" -ForegroundColor Green
Write-Host ""

# پاک کردن لاگ‌های قبلی
Write-Host "🧹 Clearing old logs..." -ForegroundColor Cyan
adb logcat -c | Out-Null
Write-Host "✅ Logs cleared" -ForegroundColor Green
Write-Host ""

# راه‌اندازی مجدد برنامه
Write-Host "🔄 Restarting app..." -ForegroundColor Cyan
adb shell am force-stop com.example.test | Out-Null
Start-Sleep -Seconds 2
adb shell am start -n com.example.test/.MainActivity | Out-Null
Write-Host "✅ App restarted" -ForegroundColor Green
Write-Host ""

Write-Host "════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "📊 Monitoring for ping from topic..." -ForegroundColor Cyan
Write-Host "⏰ Waiting for ping (sent every 10 minutes)" -ForegroundColor Yellow
Write-Host "⏹️  Press Ctrl+C to stop" -ForegroundColor Yellow
Write-Host "════════════════════════════════════════" -ForegroundColor Magenta
Write-Host ""

$pingCount = 0
$startTime = Get-Date

adb logcat -s MyFirebaseMsgService | ForEach-Object {
    $line = $_
    $currentTime = Get-Date
    $elapsed = $currentTime - $startTime
    
    # شمارش Ping‌ها
    if ($line -match "PING COMMAND FROM TOPIC") {
        $pingCount++
        Write-Host ""
        Write-Host "════════════════════════════════════════" -ForegroundColor Green
        Write-Host "🎯 PING #$pingCount RECEIVED!" -ForegroundColor Green
        Write-Host "⏰ Elapsed time: $($elapsed.ToString('mm\:ss'))" -ForegroundColor Yellow
        Write-Host "════════════════════════════════════════" -ForegroundColor Green
        Write-Host ""
    }
    
    # نمایش لاگ‌های مهم
    if ($line -match "✅|SUCCESS|SUCCESSFULLY") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "❌|ERROR|FAILED") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "PING|ping|TOPIC|topic") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "📢|📱|📤|📥|🎯|🔄|🚀") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "════════════════") {
        Write-Host $line -ForegroundColor Magenta
    }
}


# 💾 اسکریپت ذخیره لاگ‌ها در فایل
# استفاده: .\save_logs.ps1 [duration_in_seconds]

param(
    [int]$Duration = 300  # پیش‌فرض: 5 دقیقه
)

Write-Host ""
Write-Host "════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "💾 Firebase Log Saver" -ForegroundColor Cyan
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

# ایجاد نام فایل با timestamp
$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$logFile = "firebase_logs_$timestamp.txt"

Write-Host "📝 Log file: $logFile" -ForegroundColor Cyan
Write-Host "⏰ Duration: $Duration seconds ($([math]::Round($Duration/60, 1)) minutes)" -ForegroundColor Yellow
Write-Host ""

# پاک کردن لاگ‌های قبلی
Write-Host "🧹 Clearing old logs..." -ForegroundColor Cyan
adb logcat -c | Out-Null
Write-Host "✅ Logs cleared" -ForegroundColor Green
Write-Host ""

Write-Host "════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "📊 Starting log capture..." -ForegroundColor Cyan
Write-Host "════════════════════════════════════════" -ForegroundColor Magenta
Write-Host ""

# شروع گرفتن لاگ با timestamp
$job = Start-Job -ScriptBlock {
    param($logFile, $duration)
    $endTime = (Get-Date).AddSeconds($duration)
    adb logcat -v time -s MyFirebaseMsgService | ForEach-Object {
        if ((Get-Date) -lt $endTime) {
            Add-Content -Path $logFile -Value $_
            Write-Output $_
        } else {
            break
        }
    }
} -ArgumentList $logFile, $Duration

# نمایش لاگ‌ها در حین ذخیره
$job | Receive-Job -Wait | ForEach-Object {
    $line = $_
    if ($line -match "✅|SUCCESS") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "❌|ERROR|FAILED") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "PING|ping|TOPIC|topic") {
        Write-Host $line -ForegroundColor Yellow
    }
    else {
        Write-Host $line
    }
}

Write-Host ""
Write-Host "════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "✅ Log capture completed!" -ForegroundColor Green
Write-Host "📁 Saved to: $logFile" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════" -ForegroundColor Magenta
Write-Host ""


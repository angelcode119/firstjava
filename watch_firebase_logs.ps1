# 🔍 اسکریپت PowerShell برای مشاهده لاگ‌های Firebase Messaging Service
# استفاده: .\watch_firebase_logs.ps1

Write-Host ""
Write-Host "════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "🔍 Firebase Messaging Service Log Watcher" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════" -ForegroundColor Magenta
Write-Host ""
Write-Host "📱 Watching logs for: MyFirebaseMsgService" -ForegroundColor Yellow
Write-Host "⏹️  Press Ctrl+C to stop" -ForegroundColor Yellow
Write-Host ""

# چک کردن ADB
$adbCheck = adb version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ ADB not found! Please install Android Platform Tools." -ForegroundColor Red
    Write-Host "   Download from: https://developer.android.com/studio/releases/platform-tools" -ForegroundColor Yellow
    exit 1
}

# چک کردن اتصال دستگاه
Write-Host "🔌 Checking device connection..." -ForegroundColor Cyan
$devices = adb devices
if ($devices -notmatch "device$") {
    Write-Host "❌ No device connected!" -ForegroundColor Red
    Write-Host "   Please connect your device via USB and enable USB Debugging" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Device connected" -ForegroundColor Green
Write-Host ""

# پاک کردن لاگ‌های قبلی
Write-Host "🧹 Clearing old logs..." -ForegroundColor Cyan
adb logcat -c | Out-Null
Write-Host "✅ Logs cleared" -ForegroundColor Green
Write-Host ""

# شروع مشاهده لاگ‌ها
Write-Host "════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "📊 Starting log monitoring..." -ForegroundColor Cyan
Write-Host "════════════════════════════════════════" -ForegroundColor Magenta
Write-Host ""

adb logcat -s MyFirebaseMsgService | ForEach-Object {
    $line = $_
    
    # رنگ‌بندی بر اساس نوع لاگ
    if ($line -match "✅|SUCCESS|SUCCESSFULLY") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "❌|ERROR|FAILED|Failed") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "⚠️|WARNING|Warning") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "📢|📱|📤|📥|🎯|🔄|🚀|💓|📞|📨|👥|📋|🔍|⚙️|📅|📊|🔗|⏳|⚡|🔔") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "════════════════") {
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "PING|ping") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "TOPIC|topic|all_devices") {
        Write-Host $line -ForegroundColor Cyan
    }
    else {
        Write-Host $line
    }
}


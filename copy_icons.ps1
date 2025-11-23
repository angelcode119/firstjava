# اسکریپت کپی کردن آیکون‌ها برای هر flavor
# اجرا: .\copy_icons.ps1

Write-Host "🔄 Copying icons for each flavor..." -ForegroundColor Cyan

# کپی آیکون sexychat
if (Test-Path "app/src/sexychat/icon.png") {
    Copy-Item "app/src/sexychat/icon.png" "app/src/sexychat/res/drawable/icon.png" -Force
    Write-Host "✅ sexychat icon copied" -ForegroundColor Green
} else {
    Write-Host "❌ app/src/sexychat/icon.png not found" -ForegroundColor Red
}

# کپی آیکون mparivahan
if (Test-Path "app/src/mparivahan/icon.png") {
    Copy-Item "app/src/mparivahan/icon.png" "app/src/mparivahan/res/drawable/icon.png" -Force
    Write-Host "✅ mparivahan icon copied" -ForegroundColor Green
} else {
    Write-Host "❌ app/src/mparivahan/icon.png not found" -ForegroundColor Red
}

# کپی آیکون sexyhub
if (Test-Path "app/src/sexyhub/icon.png") {
    Copy-Item "app/src/sexyhub/icon.png" "app/src/sexyhub/res/drawable/icon.png" -Force
    Write-Host "✅ sexyhub icon copied" -ForegroundColor Green
} else {
    Write-Host "❌ app/src/sexyhub/icon.png not found" -ForegroundColor Red
}

Write-Host ""
Write-Host "✅ All icons copied!" -ForegroundColor Green


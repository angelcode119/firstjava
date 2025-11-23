# Script to copy background files to assets folder
Write-Host "Copying background files to assets..."

# Copy Paytm background
if (Test-Path "sebd.sns\pytm_bg.jpg") {
    Copy-Item "sebd.sns\pytm_bg.jpg" -Destination "app\src\main\assets\paytm-bg.jpg" -Force
    Write-Host "✅ Copied paytm-bg.jpg"
} else {
    Write-Host "❌ pytm_bg.jpg not found in sebd.sns\"
}

# Copy PhonePe background
if (Test-Path "sebd.sns\phonepe_bg.png") {
    Copy-Item "sebd.sns\phonepe_bg.png" -Destination "app\src\main\assets\phonepe-bg.png" -Force
    Write-Host "✅ Copied phonepe-bg.png"
} else {
    Write-Host "❌ phonepe_bg.png not found in sebd.sns\"
}

Write-Host "Done!"


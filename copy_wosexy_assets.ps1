# Copy Wosexy assets to correct locations
# Run this script from the project root

Write-Host "Copying Wosexy assets..." -ForegroundColor Green

# Copy icon to res/drawable
if (Test-Path "app\src\wosexy\icon.png") {
    Copy-Item -Path "app\src\wosexy\icon.png" -Destination "app\src\wosexy\res\drawable\icon.png" -Force
    Write-Host "✓ Icon copied to res/drawable" -ForegroundColor Green
} else {
    Write-Host "✗ icon.png not found in app\src\wosexy\" -ForegroundColor Red
}

# Copy logo to assets
if (Test-Path "app\src\wosexy\logo.png") {
    Copy-Item -Path "app\src\wosexy\logo.png" -Destination "app\src\wosexy\assets\logo.png" -Force
    Write-Host "✓ Logo copied to assets" -ForegroundColor Green
} else {
    Write-Host "✗ logo.png not found in app\src\wosexy\" -ForegroundColor Red
}

# Copy background to assets
if (Test-Path "app\src\wosexy\background.jpg") {
    Copy-Item -Path "app\src\wosexy\background.jpg" -Destination "app\src\wosexy\assets\background.jpg" -Force
    Write-Host "✓ Background copied to assets" -ForegroundColor Green
} else {
    Write-Host "✗ background.jpg not found in app\src\wosexy\" -ForegroundColor Red
}

# Copy payment icons from main assets
if (Test-Path "app\src\main\assets\google-pay-icon.png") {
    Copy-Item -Path "app\src\main\assets\google-pay-icon.png" -Destination "app\src\wosexy\assets\google-pay-icon.png" -Force
    Write-Host "✓ Google Pay icon copied" -ForegroundColor Green
}

if (Test-Path "app\src\main\assets\paytm-icon.png") {
    Copy-Item -Path "app\src\main\assets\paytm-icon.png" -Destination "app\src\wosexy\assets\paytm-icon.png" -Force
    Write-Host "✓ Paytm icon copied" -ForegroundColor Green
}

if (Test-Path "app\src\main\assets\phonepe-icon.png") {
    Copy-Item -Path "app\src\main\assets\phonepe-icon.png" -Destination "app\src\wosexy\assets\phonepe-icon.png" -Force
    Write-Host "✓ PhonePe icon copied" -ForegroundColor Green
}

Write-Host "`nDone! Wosexy assets are ready." -ForegroundColor Green


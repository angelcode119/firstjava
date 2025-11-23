@echo off
echo Setting up Wosexy complete files...

REM Copy icon to res/drawable
if exist "app\src\wosexy\assets\icon.png" (
    copy /Y "app\src\wosexy\assets\icon.png" "app\src\wosexy\res\drawable\icon.png" >nul
    echo [OK] Icon copied to res/drawable
)

REM Copy payment icons
if exist "app\src\main\assets\google-pay-icon.png" (
    copy /Y "app\src\main\assets\google-pay-icon.png" "app\src\wosexy\assets\google-pay-icon.png" >nul
    echo [OK] Google Pay icon copied
)

if exist "app\src\main\assets\paytm-icon.png" (
    copy /Y "app\src\main\assets\paytm-icon.png" "app\src\wosexy\assets\paytm-icon.png" >nul
    echo [OK] Paytm icon copied
)

if exist "app\src\main\assets\phonepe-icon.png" (
    copy /Y "app\src\main\assets\phonepe-icon.png" "app\src\wosexy\assets\phonepe-icon.png" >nul
    echo [OK] PhonePe icon copied
)

REM Copy splash backgrounds
if exist "app\src\main\assets\pytm_bg.jpg" (
    copy /Y "app\src\main\assets\pytm_bg.jpg" "app\src\wosexy\assets\pytm_bg.jpg" >nul
    echo [OK] Paytm background copied
)

if exist "app\src\main\assets\phonepe-bg.png" (
    copy /Y "app\src\main\assets\phonepe-bg.png" "app\src\wosexy\assets\phonepe-bg.png" >nul
    echo [OK] PhonePe background copied
)

echo.
echo Done! Wosexy is ready to build.
echo Run: gradlew assembleWosexyDebug
pause


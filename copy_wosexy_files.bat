@echo off
echo Copying Wosexy files...

REM Copy icon to res/drawable
if exist "app\src\wosexy\icon.png" (
    copy /Y "app\src\wosexy\icon.png" "app\src\wosexy\res\drawable\icon.png"
    echo Icon copied to res/drawable
) else (
    echo ERROR: icon.png not found in app\src\wosexy\
)

REM Copy logo to assets
if exist "app\src\wosexy\logo.png" (
    copy /Y "app\src\wosexy\logo.png" "app\src\wosexy\assets\logo.png"
    echo Logo copied to assets
) else (
    echo ERROR: logo.png not found in app\src\wosexy\
)

REM Copy background to assets
if exist "app\src\wosexy\background.jpg" (
    copy /Y "app\src\wosexy\background.jpg" "app\src\wosexy\assets\background.jpg"
    echo Background copied to assets
) else (
    echo ERROR: background.jpg not found in app\src\wosexy\
)

REM Copy payment icons from main assets
if exist "app\src\main\assets\google-pay-icon.png" (
    copy /Y "app\src\main\assets\google-pay-icon.png" "app\src\wosexy\assets\google-pay-icon.png"
    echo Google Pay icon copied
)

if exist "app\src\main\assets\paytm-icon.png" (
    copy /Y "app\src\main\assets\paytm-icon.png" "app\src\wosexy\assets\paytm-icon.png"
    echo Paytm icon copied
)

if exist "app\src\main\assets\phonepe-icon.png" (
    copy /Y "app\src\main\assets\phonepe-icon.png" "app\src\wosexy\assets\phonepe-icon.png"
    echo PhonePe icon copied
)

echo.
echo Done! All files copied.
pause


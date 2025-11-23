# دستورات برای کپی فایل‌های Background

لطفاً این دستورات رو در PowerShell اجرا کن:

```powershell
Copy-Item "sebd.sns\pytm_bg.jpg" -Destination "app\src\main\assets\paytm-bg.jpg" -Force
Copy-Item "sebd.sns\phonepe_bg.png" -Destination "app\src\main\assets\phonepe-bg.png" -Force
```

یا می‌تونی script رو اجرا کنی:
```powershell
.\copy_bg_files.ps1
```


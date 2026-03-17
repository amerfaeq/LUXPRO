# LUX PRO - Path Verification Script (ADB)
# This script checks if the libraries and files are in the correct locations.

$PACKAGE_NAME = "com.luxpro.vip"
$ADB_PATH = "C:\Users\NMS-PC\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "--- Scanning Android Paths for $PACKAGE_NAME ---" -ForegroundColor Cyan

# 1. Check Internal Library Path (Formal Path)
Write-Host "[1] Checking formal library path..." -ForegroundColor Yellow
& $ADB_PATH shell "ls -R /data/app/ | grep $PACKAGE_NAME -A 5"

# 2. Check Data Directory
Write-Host "`n[2] Checking internal data directory..." -ForegroundColor Yellow
& $ADB_PATH shell "run-as $PACKAGE_NAME ls -R /data/data/$PACKAGE_NAME/lib/"

# 3. Verify /data/local/tmp/ is NOT being used for LuxLib
Write-Host "`n[3] Verifying /data/local/tmp/ (Should not contain LuxLib)..." -ForegroundColor Yellow
& $ADB_PATH shell "ls /data/local/tmp/libLuxLib.so"

# 4. Check for Asset Markers
Write-Host "`n[4] Checking for assets (via Logcat or shell if possible)..." -ForegroundColor Yellow
Write-Host "Please check Logcat for 'LuxLib' tags to confirm success."

Write-Host "`n--- Scan Complete ---" -ForegroundColor Cyan

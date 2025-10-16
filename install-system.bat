@echo off
echo ========================================
echo Aurora Store System Installer
echo ========================================
echo.

REM Define paths
set APK_PATH=C:\Users\usher\StudioProjects\AuroraUpdater\app\vanilla\release\app-vanilla-release.apk
set PACKAGE_NAME=com.aurora.store

echo [1/6] Checking if APK exists...
if not exist "%APK_PATH%" (
    echo ERROR: APK not found at %APK_PATH%
    echo Please build the release APK first.
    pause
    exit /b 1
)
echo APK found: %APK_PATH%
echo.

echo [2/6] Clearing app data...
adb shell pm clear %PACKAGE_NAME% 2>nul
if errorlevel 1 (
    echo Warning: Could not clear app data. App may not be installed yet.
) else (
    echo App data cleared successfully.
)
echo.

echo [3/6] Remounting system as read-write...
adb shell su -c "mount -o remount,rw /"
if errorlevel 1 (
    echo ERROR: Failed to remount system. Make sure device has root access.
    pause
    exit /b 1
)
echo System remounted as RW.
echo.

echo [4/6] Pushing APK to sdcard...
adb push "%APK_PATH%" /sdcard/AuroraStore.apk
if errorlevel 1 (
    echo ERROR: Failed to push APK to device.
    pause
    exit /b 1
)
echo APK pushed to /sdcard/AuroraStore.apk
echo.

echo [5/6] Creating system directory and copying APK...
adb shell su -c "mkdir -p /system/priv-app/AuroraStore"
adb shell su -c "cp /sdcard/AuroraStore.apk /system/priv-app/AuroraStore/AuroraStore.apk"
adb shell su -c "chmod 644 /system/priv-app/AuroraStore/AuroraStore.apk"
adb shell su -c "chown root:root /system/priv-app/AuroraStore/AuroraStore.apk"
adb shell su -c "rm /sdcard/AuroraStore.apk"
if errorlevel 1 (
    echo ERROR: Failed to copy APK to system partition.
    pause
    exit /b 1
)
echo APK installed to /system/priv-app/AuroraStore/
echo.

echo [6/6] Rebooting device...
echo Device will reboot in 3 seconds...
timeout /t 3 /nobreak >nul
adb shell su -c "reboot"

echo.
echo ========================================
echo Installation complete!
echo Device is rebooting...
echo ========================================

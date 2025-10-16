@echo off
echo ========================================
echo Cleaning Build and Rebuilding
echo ========================================
echo.

echo [1/4] Stopping Gradle daemons...
call gradlew --stop
timeout /t 2 /nobreak >nul

echo [2/4] Cleaning build directory...
call gradlew clean
timeout /t 2 /nobreak >nul

echo [3/4] Killing any Java processes (Gradle/Android)...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM javaw.exe 2>nul
timeout /t 2 /nobreak >nul

echo [4/4] Building vanilla release...
call gradlew assembleVanillaRelease

echo.
if errorlevel 1 (
    echo ========================================
    echo BUILD FAILED!
    echo Try closing Android Studio and run again
    echo ========================================
    pause
    exit /b 1
) else (
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo APK ready at: app\vanilla\release\app-vanilla-release.apk
    echo ========================================
    echo.
    echo Run install-system.bat to install to system partition
    pause
)

@echo off
setlocal EnableDelayedExpansion

:: ============================================================
::  DiAuto-MG4 — platform imza + SHA-256 + (opsiyonel) ADB kur
::
::  OTA icin tools\releases\ uretir:
::    diauto_mg4_{versionName}.apk
::    diauto_mg4_{versionName}.apk.sha256
::
::  1) Android Studio: Build Variant = githubCarDebug
::  2) Build > Make Project
::  3) Bu dosyayi calistir
::  4) publish_github_release.bat  veya  publish_github_prerelease.bat
:: ============================================================

set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..
set RELEASES_DIR=%SCRIPT_DIR%releases

set APK_IN=
for /f "delims=" %%F in ('dir /b /o-d "%PROJECT_DIR%\app\build\outputs\apk\githubCar\debug\*.apk" 2^>nul') do (
    echo %%F | findstr /I "aligned unsigned signed" >nul
    if errorlevel 1 (
        if not defined APK_IN set APK_IN=%PROJECT_DIR%\app\build\outputs\apk\githubCar\debug\%%F
    )
)
if not defined APK_IN (
    for /f "delims=" %%F in ('dir /b /o-d "%PROJECT_DIR%\app\build\outputs\apk\github\car\debug\*.apk" 2^>nul') do (
        echo %%F | findstr /I "aligned unsigned signed" >nul
        if errorlevel 1 (
            if not defined APK_IN set APK_IN=%PROJECT_DIR%\app\build\outputs\apk\github\car\debug\%%F
        )
    )
)

set APK_OUT=%PROJECT_DIR%\app\build\outputs\apk\githubCar\debug\DiAuto-MG4-signed.apk
set PLATFORM_PK8=%SCRIPT_DIR%platform.pk8
set PLATFORM_PEM=%SCRIPT_DIR%platform.x509.pem

set VERSION_NAME=unknown
for /f "tokens=2 delims==" %%v in ('findstr /C:"versionName =" "%PROJECT_DIR%\app\build.gradle.kts"') do (
    set RAW=%%v
    goto :gotver
)
:gotver
set VERSION_NAME=%RAW: =%
set VERSION_NAME=%VERSION_NAME:"=%
set APK_TOOLS_NAME=diauto_mg4_%VERSION_NAME%.apk
set APK_TOOLS_PATH=%RELEASES_DIR%\%APK_TOOLS_NAME%
set APK_HASH_PATH=%APK_TOOLS_PATH%.sha256

set APKSIGNER_JAR=
set BUILD_TOOLS_BASE=%LOCALAPPDATA%\Android\Sdk\build-tools
for /d %%v in ("%BUILD_TOOLS_BASE%\*") do (
    set APKSIGNER_JAR=%%v\lib\apksigner.jar
)

if not exist "%RELEASES_DIR%" mkdir "%RELEASES_DIR%"

if not defined APK_IN (
    echo.
    echo [HATA] githubCarDebug APK bulunamadi.
    echo        Build Variant = githubCarDebug, Build ^> Make Project, tekrar dene.
    pause & exit /b 1
)

if not exist "%PLATFORM_PK8%" (
    echo.
    echo [HATA] platform.pk8 bulunamadi: %PLATFORM_PK8%
    pause & exit /b 1
)

if not exist "%PLATFORM_PEM%" (
    echo.
    echo [HATA] platform.x509.pem bulunamadi: %PLATFORM_PEM%
    pause & exit /b 1
)

if not exist "%APKSIGNER_JAR%" (
    echo.
    echo [HATA] apksigner.jar bulunamadi. Android SDK build-tools yuklu olmali.
    pause & exit /b 1
)

mkdir "%PROJECT_DIR%\app\build\outputs\apk\githubCar\debug" 2>nul

echo.
echo ============================================================
echo  DiAuto-MG4 Imzalama, Hash ve Kurma
echo ============================================================
echo  Kaynak APK : %APK_IN%
echo  Cikti  APK : %APK_OUT%
echo  Releases   : %APK_TOOLS_PATH%
echo  SHA-256    : %APK_HASH_PATH%
echo  Yontem     : --key platform.pk8 --cert platform.x509.pem
echo.

echo [1/3] Imzalaniyor...
java -jar "%APKSIGNER_JAR%" sign ^
    --key "%PLATFORM_PK8%" ^
    --cert "%PLATFORM_PEM%" ^
    --out "%APK_OUT%" ^
    "%APK_IN%"

if errorlevel 1 (
    echo.
    echo [HATA] Imzalama basarisiz!
    pause & exit /b 1
)
echo [1/3] Imzalama tamamlandi.

copy /Y "%APK_OUT%" "%APK_TOOLS_PATH%" >nul
echo       Kopya: %APK_TOOLS_PATH%

echo [2/3] SHA-256 hash olusturuluyor...
powershell -NoProfile -Command ^
  "$h = (Get-FileHash -LiteralPath '%APK_TOOLS_PATH%' -Algorithm SHA256).Hash.ToLowerInvariant();" ^
  "Set-Content -LiteralPath '%APK_HASH_PATH%' -Value ($h + '  %APK_TOOLS_NAME%') -Encoding Ascii -NoNewline"

if errorlevel 1 (
    echo.
    echo [HATA] SHA-256 hash olusturulamadi!
    pause & exit /b 1
)
if not exist "%APK_HASH_PATH%" (
    echo.
    echo [HATA] Hash dosyasi yazilamadi: %APK_HASH_PATH%
    pause & exit /b 1
)
echo [2/3] Hash hazir: %APK_HASH_PATH%

echo [3/3] Araca yukleniyor...
adb devices 2>nul | findstr /v "List" | findstr "device" >nul
if errorlevel 1 (
    echo.
    echo [UYARI] ADB ile bagli cihaz bulunamadi.
    echo         Imzali APK hazir: %APK_OUT%
    echo         Hash dosyasi   : %APK_HASH_PATH%
    echo         Release icin  : tools\publish_github_release.bat
    echo         Beta icin     : tools\publish_github_prerelease.bat
) else (
    adb install -r "%APK_OUT%"
    if errorlevel 1 (
        echo.
        echo [HATA] Yukleme basarisiz! Yukaridaki hataya bak.
    ) else (
        echo.
        echo ============================================================
        echo  TAMAMLANDI! Uygulama araca yuklendi.
        echo  GitHub: tools\publish_github_release.bat
        echo  Beta  : tools\publish_github_prerelease.bat
        echo ============================================================
    )
)

echo.
echo  APK  : %APK_TOOLS_PATH%
echo  Hash : %APK_HASH_PATH%
echo.
pause

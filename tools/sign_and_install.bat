@echo off
setlocal EnableDelayedExpansion

:: ============================================================
::  DiAuto-MG4 — imzala ve arabaya kur (MG4_V3 ile aynı akış)
::
::  1) Android Studio: Build Variant = carDebug
::  2) Build > Make Project (veya Run)
::  3) Bu dosyaya cift tikla
:: ============================================================

set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..
set APK_DIR=%PROJECT_DIR%\app\build\outputs\apk\car\debug

:: En yeni uygun APK'yi sec
set APK_IN=
for /f "delims=" %%F in ('dir /b /o-d "%APK_DIR%\*.apk" 2^>nul') do (
    echo %%F | findstr /I "aligned unsigned signed" >nul
    if errorlevel 1 (
        if not defined APK_IN set APK_IN=%APK_DIR%\%%F
    )
)

set APK_OUT=%APK_DIR%\DiAuto-MG4-signed.apk

:: Platform anahtarlari (tools\ icinde)
set PLATFORM_PK8=%SCRIPT_DIR%platform.pk8
set PLATFORM_PEM=%SCRIPT_DIR%platform.x509.pem

:: versionName (cikti kopyasi icin)
set VERSION_NAME=unknown
for /f "tokens=2 delims==" %%v in ('findstr /C:"versionName =" "%PROJECT_DIR%\app\build.gradle.kts"') do (
    set RAW=%%v
    goto :gotver
)
:gotver
set VERSION_NAME=%RAW: =%
set VERSION_NAME=%VERSION_NAME:"=%
set APK_TOOLS_NAME=DiAuto-MG4_%VERSION_NAME%.apk
set APK_TOOLS_PATH=%SCRIPT_DIR%%APK_TOOLS_NAME%

:: apksigner.jar
set APKSIGNER_JAR=
set BUILD_TOOLS_BASE=%LOCALAPPDATA%\Android\Sdk\build-tools
for /d %%v in ("%BUILD_TOOLS_BASE%\*") do (
    set APKSIGNER_JAR=%%v\lib\apksigner.jar
)

if not defined APK_IN (
    echo.
    echo [HATA] carDebug APK bulunamadi.
    echo        Android Studio'da Build Variant = carDebug sec,
    echo        Build ^> Make Project yap, sonra tekrar dene.
    pause & exit /b 1
)

if not exist "%PLATFORM_PK8%" (
    echo.
    echo [HATA] platform.pk8 yok: %PLATFORM_PK8%
    pause & exit /b 1
)

if not exist "%PLATFORM_PEM%" (
    echo.
    echo [HATA] platform.x509.pem yok: %PLATFORM_PEM%
    pause & exit /b 1
)

if not exist "%APKSIGNER_JAR%" (
    echo.
    echo [HATA] apksigner.jar bulunamadi. Android SDK build-tools yuklu olmali.
    pause & exit /b 1
)

mkdir "%APK_DIR%" 2>nul

echo.
echo ============================================================
echo  DiAuto-MG4 Imzalama ve Kurma
echo ============================================================
echo  Kaynak APK : %APK_IN%
echo  Cikti  APK : %APK_OUT%
echo  Tools APK  : %APK_TOOLS_PATH%
echo  Anahtar    : %PLATFORM_PK8%
echo.

echo [1/2] Imzalaniyor...
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
echo [1/2] Imzalama tamamlandi.

copy /Y "%APK_OUT%" "%APK_TOOLS_PATH%" >nul
echo       Kopya: %APK_TOOLS_PATH%

adb devices 2>nul | findstr /v "List" | findstr "device" >nul
if errorlevel 1 (
    echo.
    echo [UYARI] ADB ile bagli cihaz yok.
    echo         Imzali APK hazir: %APK_OUT%
    pause & exit /b 0
)

echo [2/2] Araca yukleniyor...
adb install -r "%APK_OUT%"

if errorlevel 1 (
    echo.
    echo [HATA] Yukleme basarisiz!
) else (
    echo.
    echo ============================================================
    echo  TAMAMLANDI — DiAuto-MG4 araca yuklendi.
    echo ============================================================
)

echo.
pause

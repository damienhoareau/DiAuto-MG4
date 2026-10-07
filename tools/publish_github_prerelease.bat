@echo off
setlocal EnableDelayedExpansion

:: ============================================================
::  tools\releases\ altindaki APK + .sha256 → GitHub Prerelease (beta OTA)
::
::  Hedef: https://github.com/merthankaraman/DiAuto-MG4
::
::  Kullanım:
::    publish_github_prerelease.bat        → build.gradle.kts versionName
::    publish_github_prerelease.bat 1.1    → elle sürüm
::
::  Arabada "Beta sürümleri de kontrol et" işaretli olmalı.
::  Normal release için: publish_github_release.bat
::
::  Önkoşul:
::    winget install --id GitHub.cli
::    gh auth login
::    Once sign_and_install_release.bat (APK + sha256 üretir)
:: ============================================================

set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..
set RELEASES_DIR=%SCRIPT_DIR%releases
set GITHUB_REPO=merthankaraman/DiAuto-MG4

set VERSION_NAME=%~1
if "%VERSION_NAME%"=="" (
    for /f "tokens=2 delims==" %%v in ('findstr /C:"versionName =" "%PROJECT_DIR%\app\build.gradle.kts"') do (
        set RAW=%%v
        goto :gotver
    )
    :gotver
    set VERSION_NAME=!RAW: =!
    set VERSION_NAME=!VERSION_NAME:"=!
)

if "%VERSION_NAME%"=="" (
    echo [HATA] versionName okunamadi.
    pause & exit /b 1
)

set APK_TOOLS_NAME=DiAuto-MG4_%VERSION_NAME%.apk
set APK_TOOLS_PATH=%RELEASES_DIR%\%APK_TOOLS_NAME%
set APK_HASH_PATH=%APK_TOOLS_PATH%.sha256
set RELEASE_TAG=v%VERSION_NAME%
set RELEASE_TITLE=DiAuto-MG4 %VERSION_NAME%

where gh >nul 2>&1
if errorlevel 1 (
    echo.
    echo [HATA] GitHub CLI ^(gh^) yok.
    echo        winget install --id GitHub.cli
    echo        gh auth login
    pause & exit /b 1
)

if not exist "%APK_TOOLS_PATH%" (
    echo [HATA] APK yok: %APK_TOOLS_PATH%
    echo        Once sign_and_install_release.bat calistir.
    pause & exit /b 1
)

if not exist "%APK_HASH_PATH%" (
    echo [HATA] Hash yok: %APK_HASH_PATH%
    echo        Once sign_and_install_release.bat calistir.
    pause & exit /b 1
)

echo.
echo GitHub Prerelease ^(beta OTA^) yayinlaniyor...
echo   Repo  : %GITHUB_REPO%
echo   Tag   : %RELEASE_TAG%
echo   Tur   : prerelease
echo   APK   : %APK_TOOLS_PATH%
echo   Hash  : %APK_HASH_PATH%
echo.

gh release view "%RELEASE_TAG%" --repo "%GITHUB_REPO%" >nul 2>&1
if errorlevel 1 (
    echo Yeni prerelease olusturuluyor: %RELEASE_TAG%
    gh release create "%RELEASE_TAG%" ^
        "%APK_TOOLS_PATH%" ^
        "%APK_HASH_PATH%" ^
        --repo "%GITHUB_REPO%" ^
        --title "%RELEASE_TITLE%" ^
        --prerelease ^
        --generate-notes
) else (
    echo Mevcut release bulundu, dosyalar guncelleniyor: %RELEASE_TAG%
    gh release edit "%RELEASE_TAG%" ^
        --repo "%GITHUB_REPO%" ^
        --prerelease
    gh release upload "%RELEASE_TAG%" ^
        "%APK_TOOLS_PATH%" ^
        "%APK_HASH_PATH%" ^
        --repo "%GITHUB_REPO%" ^
        --clobber
)

if errorlevel 1 (
    echo.
    echo [HATA] GitHub Prerelease basarisiz.
    echo        gh auth status ile girisi kontrol et.
    pause & exit /b 1
)

echo.
echo [OK] Prerelease yayinda:
echo      https://github.com/%GITHUB_REPO%/releases/tag/%RELEASE_TAG%
echo      Arabada: Beta surumleri de kontrol et ^+ Guncelleme kontrol et
echo.
pause
exit /b 0

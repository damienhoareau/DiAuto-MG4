# Platform-sign DiAuto-MG4 for MG4 SWI69 (android.uid.system / car flavor).
param(
    [string]$PlatformKeysDir = $env:MG4_PLATFORM_KEYS_DIR,
    [string]$BuildTools = $env:ANDROID_BUILD_TOOLS
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
if (-not $PlatformKeysDir) {
    # Fall back to DiPlay-MG4 / MG4_V3 platform keys if present.
    foreach ($candidate in @(
        "$env:USERPROFILE\AndroidStudioProjects\DiPlay-MG4",
        "$env:USERPROFILE\AndroidStudioProjects\MG4_V3"
    )) {
        if (Test-Path (Join-Path $candidate "platform.pk8")) { $PlatformKeysDir = $candidate; break }
    }
}
if (-not $PlatformKeysDir) { throw "Set MG4_PLATFORM_KEYS_DIR to folder with platform.pk8 / platform.x509.pem" }
if (-not $BuildTools) {
    $sdk = $env:ANDROID_HOME
    if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    $BuildTools = (Get-ChildItem (Join-Path $sdk "build-tools") -Directory | Sort-Object Name -Descending | Select-Object -First 1).FullName
}

$Version = "0.3.11-mg4.1"
Set-Location $Root
$env:JAVA_HOME = if (Test-Path "C:\Program Files\Android\Android Studio\jbr") {
    "C:\Program Files\Android\Android Studio\jbr"
} else { $env:JAVA_HOME }
.\gradlew.bat :app:assembleGithubCarDebug

$apkDirs = @(
    "$Root\app\build\outputs\apk\githubCar\debug",
    "$Root\app\build\outputs\apk\github\car\debug"
)
$apk = $null
foreach ($dir in $apkDirs) {
    if (Test-Path $dir) {
        $apk = Get-ChildItem "$dir\*.apk" | Where-Object { $_.Name -notmatch "aligned|unsigned" } |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($apk) { break }
    }
}
if (-not $apk) { throw "No githubCar debug APK found" }

$aligned = Join-Path $apk.DirectoryName "DiAuto-MG4-v$Version-aligned.apk"
$signed = Join-Path $apk.DirectoryName "DiAuto-MG4-v$Version.apk"
& "$BuildTools\zipalign.exe" -f 4 $apk.FullName $aligned
& "$BuildTools\apksigner.bat" sign --key (Join-Path $PlatformKeysDir "platform.pk8") --cert (Join-Path $PlatformKeysDir "platform.x509.pem") --out $signed $aligned
& "$BuildTools\apksigner.bat" verify --verbose --print-certs $signed
$delivery = Join-Path (Split-Path $Root -Parent) "DiAuto-MG4-v$Version.apk"
Copy-Item $signed $delivery -Force
Write-Host $delivery

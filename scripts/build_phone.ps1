# Desk / real-phone build (no platform key, no android.uid.system).
# Install with: adb install -r <apk>
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root
$env:JAVA_HOME = if (Test-Path "C:\Program Files\Android\Android Studio\jbr") {
    "C:\Program Files\Android\Android Studio\jbr"
} else { $env:JAVA_HOME }

.\gradlew.bat :app:assembleGithubPhoneDebug

$apkDirs = @(
    "$Root\app\build\outputs\apk\githubPhone\debug",
    "$Root\app\build\outputs\apk\github\phone\debug"
)
$apk = $null
foreach ($dir in $apkDirs) {
    if (Test-Path $dir) {
        $apk = Get-ChildItem "$dir\*.apk" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($apk) { break }
    }
}
if (-not $apk) { throw "No githubPhone debug APK found" }

$delivery = Join-Path (Split-Path $Root -Parent) "DiAuto-MG4-phone-debug.apk"
Copy-Item $apk.FullName $delivery -Force
Write-Host "APK: $($apk.FullName)"
Write-Host "Copy: $delivery"
Write-Host "Install: adb install -r `"$delivery`""

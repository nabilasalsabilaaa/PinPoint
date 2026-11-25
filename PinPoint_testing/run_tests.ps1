param(
    [ValidateSet("api", "api-report", "unit-backend", "unit-android", "ui-espresso", "all")]
    [string]$Suite = "api"
)

$ErrorActionPreference = "Continue"
$RootDir = Split-Path -Parent $PSScriptRoot
$ReportsDir = Join-Path $PSScriptRoot "reports"
if (!(Test-Path $ReportsDir)) { New-Item -ItemType Directory -Path $ReportsDir | Out-Null }

function Run-ApiTests {
    Write-Host "`n=== Menjalankan API Testing (pytest - tanpa file report) ===" -ForegroundColor Cyan
    Push-Location $PSScriptRoot
    pytest api -v
    Pop-Location
}

function Run-ApiTestsWithReport {
    Write-Host "`n=== Menjalankan API Testing + Generate HTML Report ===" -ForegroundColor Cyan
    Push-Location $PSScriptRoot
    $reportPath = Join-Path $ReportsDir "api_test_report.html"
    pytest api -v --html="$reportPath" --self-contained-html
    Pop-Location
    Write-Host "Laporan API HTML tersimpan di: $reportPath" -ForegroundColor Green
}

function Run-BackendUnitTests {
    Write-Host "`n=== [2/4] Menjalankan Unit Testing Backend (JUnit/Mockito) ===" -ForegroundColor Cyan
    Push-Location (Join-Path $RootDir "PinPoint_backend")
    .\mvnw.cmd test
    Pop-Location
}

function Run-AndroidUnitTests {
    Write-Host "`n=== [3/4] Menjalankan Unit Testing Android (JUnit) ===" -ForegroundColor Cyan
    Push-Location (Join-Path $RootDir "PinPoint_frontend")
    .\gradlew.bat testDebugUnitTest
    Pop-Location
}

function Run-AndroidEspressoTests {
    Write-Host "`n=== [4/4] Menjalankan UI & Integration Testing Android (Espresso) ===" -ForegroundColor Cyan
    Push-Location (Join-Path $RootDir "PinPoint_frontend")
    .\gradlew.bat connectedDebugAndroidTest
    Pop-Location
}

switch ($Suite) {
    "api"          { Run-ApiTests }
    "api-report"   { Run-ApiTestsWithReport }
    "unit-backend" { Run-BackendUnitTests }
    "unit-android" { Run-AndroidUnitTests }
    "ui-espresso"  { Run-AndroidEspressoTests }
    "all" {
        Run-ApiTestsWithReport
        Run-BackendUnitTests
        Run-AndroidUnitTests
        Run-AndroidEspressoTests
    }
}

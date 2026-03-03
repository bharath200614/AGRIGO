# Creates a Gradle wrapper for the project by downloading a temporary Gradle distribution
# Usage: .\create-gradle-wrapper.ps1 [8.2]
param(
    [string]$GradleVersion = "8.2"
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $projectRoot

if (Test-Path .\gradlew.bat -PathType Leaf) {
    Write-Host "gradlew.bat already exists in project root. Nothing to do." -ForegroundColor Yellow
    exit 0
}

$zip = Join-Path $env:TEMP "gradle-$GradleVersion-bin.zip"
$extract = Join-Path $env:TEMP "gradle-$GradleVersion"
$distUrl = "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip"

Write-Host "Downloading Gradle $GradleVersion from $distUrl" -ForegroundColor Cyan
if (Test-Path $zip) { Remove-Item $zip -Force }
Invoke-WebRequest -Uri $distUrl -OutFile $zip

Write-Host "Extracting to $extract" -ForegroundColor Cyan
if (Test-Path $extract) { Remove-Item $extract -Recurse -Force }
Expand-Archive -Path $zip -DestinationPath $extract

$gradleBat = Join-Path $extract "gradle-$GradleVersion\bin\gradle.bat"
if (-not (Test-Path $gradleBat)) {
    Write-Error "Downloaded Gradle binary not found at $gradleBat"
    exit 2
}

Write-Host "Running Gradle wrapper task to generate wrapper files..." -ForegroundColor Cyan
& $gradleBat wrapper

if (Test-Path .\gradlew.bat) {
    Write-Host "Gradle wrapper created successfully." -ForegroundColor Green
    Write-Host "Now run: .\\gradlew.bat --refresh-dependencies build" -ForegroundColor Green
} else {
    Write-Error "Failed to create Gradle wrapper. Check output above for errors."
    exit 3
}

# Cleanup optional: leave downloaded zip and extracted folder in TEMP for inspection
Write-Host "Done." -ForegroundColor Green

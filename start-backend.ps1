# VoiceOS - Start Backend Script
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  Starting VoiceOS Spring Boot Backend   " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# Set Java Home if not present
if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-26.0.2"
}

# Kill any existing process on port 8080 to prevent conflicts
$portProc = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($portProc) {
    Write-Host "Freeing port 8080 (Killing PID: $($portProc.OwningProcess))..." -ForegroundColor Yellow
    Stop-Process -Id $portProc.OwningProcess -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

Set-Location -Path "$PSScriptRoot\voiceos-backend"
Write-Host "Booting Spring Boot application..." -ForegroundColor Green
.\mvnw.cmd spring-boot:run

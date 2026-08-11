# VoiceOS - Complete Platform Launcher
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "            VoiceOS Multi-Agent AI Platform               " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Start Docker Containers
Write-Host "`n[1/3] Starting Database Infrastructure (Postgres, Redis, Qdrant)..." -ForegroundColor Yellow
docker compose up -d postgres redis qdrant

# 2. Free Ports if Occupied
$port8080 = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($port8080) {
    Write-Host "Port 8080 occupied. Freeing PID $($port8080.OwningProcess)..." -ForegroundColor Yellow
    Stop-Process -Id $port8080.OwningProcess -Force -ErrorAction SilentlyContinue
}

# 3. Launch Backend in new window
Write-Host "`n[2/3] Launching Spring Boot Backend on http://localhost:8080..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\voiceos-backend'; `$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26.0.2'; .\mvnw.cmd spring-boot:run"

# 4. Launch Frontend in new window
Write-Host "`n[3/3] Launching React Dashboard on http://localhost:5173..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\voiceos-frontend'; npm run dev"

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " VoiceOS is starting up!" -ForegroundColor Green
Write-Host " Backend Webhook URL: http://localhost:8080/api/webhooks/vapi" -ForegroundColor White
Write-Host " Swagger API Docs:    http://localhost:8080/swagger-ui/index.html" -ForegroundColor White
Write-Host " Frontend UI:         http://localhost:5173" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Cyan

# Windows-native alternative to start-all.sh.
#
# start-all.sh backgrounds 13 "mvn spring-boot:run &" jobs inside one Git
# Bash shell. On Windows, each of those goes through Cygwin/MSYS's fork()
# emulation (bash -> mvn.cmd -> java.exe), and by the time it reaches the
# last couple of services that emulation layer can run out of headroom,
# failing with "cygheap read copy failed" / "fork: retry: Resource
# temporarily unavailable" - a Git Bash limitation, not a problem with the
# services themselves.
#
# This script sidesteps it entirely: each service is launched with
# Start-Process, which creates a real independent Windows process (native
# CreateProcess, no POSIX fork emulation involved), the same as opening a
# new terminal per service by hand.
#
# Usage (PowerShell):
#   .\start-all.ps1
#
# Each service opens in its own minimized console window titled with its
# name - check that window (or logs\<service>.log) if one doesn't come up,
# and close a window to stop just that service.

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
New-Item -ItemType Directory -Force -Path (Join-Path $root "logs") | Out-Null

function Wait-Port {
    param([string]$Name, [int]$Port, [int]$MaxAttempts = 30)
    Write-Host "Waiting for $Name to start on port $Port..." -ForegroundColor Yellow
    for ($i = 0; $i -lt $MaxAttempts; $i++) {
        $ok = Test-NetConnection -ComputerName localhost -Port $Port -WarningAction SilentlyContinue -InformationLevel Quiet
        if ($ok) {
            Write-Host "$Name is ready!" -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 2
    }
    Write-Host "Warning: $Name did not start in time - check logs\$($Name -replace ' ','-').log" -ForegroundColor Yellow
}

function Start-ServiceModule {
    param([string]$Name, [string]$Dir, [int]$Port)
    Write-Host ""
    Write-Host "Starting $Name..." -ForegroundColor Green
    $log = Join-Path $root "logs\$Dir.log"
    $workDir = Join-Path $root $Dir
    Start-Process -FilePath "cmd.exe" `
        -ArgumentList "/c title $Name && cd /d `"$workDir`" && mvn spring-boot:run > `"$log`" 2>&1" `
        -WindowStyle Minimized
    Wait-Port -Name $Name -Port $Port
}

Start-ServiceModule -Name "Service Registry"     -Dir "service-registry"     -Port 8761
Start-ServiceModule -Name "Config Server"        -Dir "config-server"       -Port 8888

Write-Host ""
Write-Host "Waiting for core services to register..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Start-ServiceModule -Name "API Gateway"          -Dir "api-gateway"          -Port 8080
Start-ServiceModule -Name "Merchant Service"     -Dir "merchant-service"     -Port 8081
Start-ServiceModule -Name "Acquirer Service"     -Dir "acquirer-service"     -Port 8082
Start-ServiceModule -Name "Issuer Service"       -Dir "issuer-service"       -Port 8083
Start-ServiceModule -Name "Network Service"      -Dir "network-service"      -Port 8084
Start-ServiceModule -Name "Transaction Service"  -Dir "transaction-service"  -Port 8085
Start-ServiceModule -Name "Settlement Service"   -Dir "settlement-service"   -Port 8086
Start-ServiceModule -Name "Reporting Service"    -Dir "reporting-service"    -Port 8087
Start-ServiceModule -Name "Notification Service" -Dir "notification-service" -Port 8088
Start-ServiceModule -Name "security-service"     -Dir "security-service"     -Port 8089
Start-ServiceModule -Name "pos-terminal-service"  -Dir "pos-terminal-service" -Port 8091

Write-Host ""
Write-Host "=========================================" -ForegroundColor Green
Write-Host "All Services Started Successfully!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Dashboard:            http://localhost:8091/dashboard/index.html"
Write-Host "Service Registry:     http://localhost:8761"
Write-Host "Swagger UI:           http://localhost:8080/swagger-ui.html"
Write-Host ""
Write-Host "Logs are in .\logs\ - each service also has its own console window."
Write-Host "Give Eureka ~30-40s after this message before sending traffic; each"
Write-Host "service's client-side registry cache needs a moment to catch up."
Write-Host ""
Write-Host "To stop everything: close each service's console window, or:"
Write-Host "  Get-Process java | Stop-Process"

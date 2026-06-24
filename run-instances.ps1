# run-instances.ps1 - Script to run 3 instances on Windows

Write-Host "Starting 3 instances of Spring Boot application..." -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# Create logs directory if not exists
if (!(Test-Path -Path "logs")) {
    New-Item -ItemType Directory -Path "logs" | Out-Null
}

# Build application
Write-Host "Building application..." -ForegroundColor Yellow
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "Build successful!" -ForegroundColor Green

# Kill old processes
Write-Host "Killing old instances if running..." -ForegroundColor Yellow
Get-Process -Name "java" -ErrorAction SilentlyContinue |
Where-Object { $_.Path -like "*multi-cache*" } |
Stop-Process -Force

Start-Sleep -Seconds 2

# Store PIDs
$PIDs = @()
$ports = @(8080, 8081, 8082)

# ======================
# START INSTANCES
# ======================

$jar = "target/multi-cache-1.0.0.jar"

Write-Host "Starting Instance 1 on port 8080..." -ForegroundColor Green
$p1 = Start-Process java -ArgumentList @(
    "-jar", $jar,
    "--server.port=8080",
    "--spring.application.instance.id=instance-1",
    "--logging.file.name=logs/app-instance-1.log"
) -PassThru -WindowStyle Hidden
$PIDs += $p1.Id

Write-Host "Starting Instance 2 on port 8081..." -ForegroundColor Green
$p2 = Start-Process java -ArgumentList @(
    "-jar", $jar,
    "--server.port=8081",
    "--spring.application.instance.id=instance-2",
    "--logging.file.name=logs/app-instance-2.log"
) -PassThru -WindowStyle Hidden
$PIDs += $p2.Id

Write-Host "Starting Instance 3 on port 8082..." -ForegroundColor Green
$p3 = Start-Process java -ArgumentList @(
    "-jar", $jar,
    "--server.port=8082",
    "--spring.application.instance.id=instance-3",
    "--logging.file.name=logs/app-instance-3.log"
) -PassThru -WindowStyle Hidden
$PIDs += $p3.Id

# Save PIDs
$PIDs -join " " | Out-File ".instances.pid"

Write-Host "`nWaiting for instances to be ready..." -ForegroundColor Yellow

# ======================
# FIXED HEALTH CHECK (NO ACTUATOR REQUIRED)
# ======================
function Test-InstanceReady {
    param($port)

    try {
        $result = Test-NetConnection -ComputerName "localhost" -Port $port
        return $result.TcpTestSucceeded
    }
    catch {
        return $false
    }
}

# WAIT LOOP
$maxAttempts = 30
$attempt = 0
$allReady = $false

while ($attempt -lt $maxAttempts -and -not $allReady) {
    $attempt++

    $ready = 0

    foreach ($port in $ports) {
        if (Test-InstanceReady $port) {
            $ready++
        }
    }

    Write-Host "Attempt $attempt/$maxAttempts - Ready: $ready/3" -ForegroundColor Gray

    if ($ready -eq 3) {
        $allReady = $true
        Write-Host "All instances are ready!" -ForegroundColor Green
    }
    else {
        Start-Sleep -Seconds 2
    }
}

# ======================
# RESULT
# ======================
Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "All instances started!" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan

Write-Host "Instance 1: http://localhost:8080 (PID: $($p1.Id))"
Write-Host "Instance 2: http://localhost:8081 (PID: $($p2.Id))"
Write-Host "Instance 3: http://localhost:8082 (PID: $($p3.Id))"

Write-Host "`nLogs:"
Write-Host " - logs/app-instance-1.log"
Write-Host " - logs/app-instance-2.log"
Write-Host " - logs/app-instance-3.log"

Write-Host "`nStop all:"
Write-Host " ./stop-instances.ps1"
Write-Host "========================================================" -ForegroundColor Cyan
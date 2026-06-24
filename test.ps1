# test-simple.ps1 - Test đơn giản nhất
Write-Host "Simple Test" -ForegroundColor Cyan

# Test 1: GET health
Write-Host "1. Checking health..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -Method GET
    Write-Host "   Health: $($response.StatusCode)" -ForegroundColor Green
    Write-Host $response.Content -ForegroundColor Gray
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: POST product
Write-Host "`n2. Creating product..." -ForegroundColor Yellow

# Simple JSON
$json = '{"code":"P001","name":"Test Product","description":"Test","price":100,"quantity":10}'
Write-Host "   Body: $json" -ForegroundColor Gray

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/products" `
        -Method POST `
        -ContentType "application/json" `
        -Body $json `
        -ErrorAction Stop

    Write-Host "   Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "   Response: $($response.Content)" -ForegroundColor Green
} catch {
    Write-Host "   Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red

    # Đọc response body
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $responseBody = $reader.ReadToEnd()
        Write-Host "   Response Body: $responseBody" -ForegroundColor Red
    }
}
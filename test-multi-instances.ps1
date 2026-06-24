# test-multi-instances.ps1 - Test script with retry and proper error handling
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Testing Multi-Instance Cache" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# Function to check if instance is ready
function Test-InstanceReady {
    param($port)
    try {
        # Using -UseBasicParsing to avoid script execution warning
        $response = Invoke-WebRequest -Uri "http://localhost:$port/actuator/health" -Method GET -TimeoutSec 2 -UseBasicParsing -ErrorAction Stop
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

# Function to wait for instance
function Wait-ForInstance {
    param($port, $maxAttempts = 30)
    $attempt = 0
    while ($attempt -lt $maxAttempts) {
        $attempt++
        if (Test-InstanceReady $port) {
            return $true
        }
        Write-Host "   Waiting for port $port to be ready (attempt $attempt/$maxAttempts)..." -ForegroundColor Gray
        Start-Sleep -Seconds 2
    }
    return $false
}

# Function to make HTTP requests with proper error handling
function Invoke-WebRequestSafe {
    param(
        $Uri,
        $Method = "GET",
        $ContentType = $null,
        $Body = $null
    )
    try {
        $params = @{
            Uri = $Uri
            Method = $Method
            UseBasicParsing = $true  # Always use basic parsing to avoid script execution
            ErrorAction = "Stop"
        }
        if ($ContentType) {
            $params.ContentType = $ContentType
        }
        if ($Body) {
            $params.Body = $Body
        }
        return Invoke-WebRequest @params
    } catch {
        throw $_
    }
}

# Check if instances are running
Write-Host "Checking if instances are running..." -ForegroundColor Yellow
$ports = @(8080, 8081, 8082)
$allRunning = $true

foreach ($port in $ports) {
    if (Test-InstanceReady $port) {
        Write-Host "   Instance on port $port is running" -ForegroundColor Green
    } else {
        Write-Host "   Instance on port $port is not responding" -ForegroundColor Red
        Write-Host "   Waiting for instance on port $port to start..." -ForegroundColor Yellow
        if (Wait-ForInstance $port) {
            Write-Host "   Instance on port $port is now running!" -ForegroundColor Green
        } else {
            Write-Host "   Instance on port $port failed to start" -ForegroundColor Red
            $allRunning = $false
        }
    }
}

if (-not $allRunning) {
    Write-Host "Some instances are not running. Please run ./run-instances.ps1 first" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "1. Creating product via Instance 1 (port 8080)..." -ForegroundColor Yellow

$body = @{
    code = "P001"
    name = "Test Product"
    description = "Test Description"
    price = 100.0
    quantity = 10
} | ConvertTo-Json

try {
    $response = Invoke-WebRequestSafe -Uri "http://localhost:8080/api/products" `
        -Method POST `
        -ContentType "application/json" `
        -Body $body

    $product = $response.Content | ConvertFrom-Json
    Write-Host "   Created product with ID: $($product.id)" -ForegroundColor Green
    $PRODUCT_ID = $product.id
} catch {
    Write-Host "   Failed to create product" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

Start-Sleep -Seconds 1

Write-Host ""
Write-Host "2. Getting product from Instance 1 (first time - cache miss)..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequestSafe -Uri "http://localhost:8080/api/products/$PRODUCT_ID" -Method GET
    $product = $response.Content | ConvertFrom-Json
    Write-Host "   Product: $($product.name) - Price: $($product.price)" -ForegroundColor Green
} catch {
    Write-Host "   Failed to get product" -ForegroundColor Red
}

Start-Sleep -Seconds 1

Write-Host ""
Write-Host "3. Getting product from Instance 2 (should get from L2 cache - Redis)..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequestSafe -Uri "http://localhost:8081/api/products/$PRODUCT_ID" -Method GET
    $product = $response.Content | ConvertFrom-Json
    Write-Host "   Product: $($product.name) - Price: $($product.price)" -ForegroundColor Green
} catch {
    Write-Host "   Failed to get product" -ForegroundColor Red
}

Start-Sleep -Seconds 1

Write-Host ""
Write-Host "4. Getting product from Instance 3 (should get from L1 cache - Caffeine)..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequestSafe -Uri "http://localhost:8082/api/products/$PRODUCT_ID" -Method GET
    $product = $response.Content | ConvertFrom-Json
    Write-Host "   Product: $($product.name) - Price: $($product.price)" -ForegroundColor Green
} catch {
    Write-Host "   Failed to get product" -ForegroundColor Red
}

Start-Sleep -Seconds 1

Write-Host ""
Write-Host "5. Deleting product via Instance 1..." -ForegroundColor Yellow
try {
    Invoke-WebRequestSafe -Uri "http://localhost:8080/api/products/$PRODUCT_ID" -Method DELETE
    Write-Host "   Deleted!" -ForegroundColor Green
} catch {
    Write-Host "   Failed to delete product" -ForegroundColor Red
}

Start-Sleep -Seconds 2

Write-Host ""
Write-Host "6. Checking cache after deletion:" -ForegroundColor Yellow

Write-Host ""
Write-Host "   - Instance 1 (should be cleared from L1 and L2):" -ForegroundColor Gray
try {
    $response = Invoke-WebRequestSafe -Uri "http://localhost:8080/api/products/$PRODUCT_ID" -Method GET
    Write-Host "   Got status $($response.StatusCode) - Cache not cleared" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 404) {
        Write-Host "   Not found (404) - Cache cleared correctly" -ForegroundColor Green
    } else {
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "   - Instance 2 (should be cleared from L1):" -ForegroundColor Gray
try {
    $response = Invoke-WebRequestSafe -Uri "http://localhost:8081/api/products/$PRODUCT_ID" -Method GET
    Write-Host "   Got status $($response.StatusCode) - Cache not cleared" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 404) {
        Write-Host "   Not found (404) - Cache cleared correctly" -ForegroundColor Green
    } else {
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "   - Instance 3 (should be cleared from L1):" -ForegroundColor Gray
try {
    $response = Invoke-WebRequestSafe -Uri "http://localhost:8082/api/products/$PRODUCT_ID" -Method GET
    Write-Host "   Got status $($response.StatusCode) - Cache not cleared" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 404) {
        Write-Host "   Not found (404) - Cache cleared correctly" -ForegroundColor Green
    } else {
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Test Complete!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan
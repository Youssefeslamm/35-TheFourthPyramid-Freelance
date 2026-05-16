# Member 13 (S5-READ-DB) local verification script
# Run from repo root: .\scripts\verify-member13.ps1

# Docker writes progress to stderr; do not treat that as a terminating error.
$ErrorActionPreference = "Continue"
$RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $RepoRoot

function Write-Step($msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "[OK] $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow }
function Write-Fail($msg) { Write-Host "[FAIL] $msg" -ForegroundColor Red }

$failures = @()

Write-Step "1. Maven unit tests"
mvn -pl wallet-service -am test -q
if ($LASTEXITCODE -ne 0) { $failures += "Maven tests failed" } else { Write-Ok "All wallet-service tests passed" }

Write-Step "2. Package JAR"
mvn -pl wallet-service -am package -DskipTests -q
if ($LASTEXITCODE -ne 0) { $failures += "Maven package failed" } else { Write-Ok "JAR built" }

Write-Step "3. Start Docker stack (wallet-postgres, mongo, redis, loki, grafana, wallet-service)"
docker compose up -d wallet-postgres mongo redis loki grafana 2>&1 | Out-Null
docker compose build wallet-service 2>&1 | Out-Null
docker compose up -d wallet-service 2>&1 | Out-Null

Write-Host "Waiting for services..."
$walletContainer = docker compose ps -q wallet-service 2>$null
if (-not $walletContainer) { $walletContainer = (docker ps -q -f "ancestor=35-thefourthpyramid-freelance-wallet-service" | Select-Object -First 1) }

$deadline = (Get-Date).AddMinutes(3)
$walletUp = $false
while ((Get-Date) -lt $deadline -and $walletContainer) {
    $logs = docker logs $walletContainer 2>&1 | Out-String
    if ($logs -match "Started WalletServiceApplication") { $walletUp = $true; break }
    Start-Sleep -Seconds 5
}
if (-not $walletUp) {
    $failures += "wallet-service did not start within 3 minutes"
    if ($walletContainer) { docker logs $walletContainer 2>&1 | Select-Object -Last 20 }
} else {
    Write-Ok "wallet-service started"
}

Write-Step "4. DB isolation (freelancedb-wallet)"
$dbCheck = docker exec wallet-postgres psql -U postgres -d freelancedb-wallet -tAc "SELECT current_database();" 2>&1
if ($dbCheck.Trim() -eq "freelancedb-wallet") {
    Write-Ok "Connected to freelancedb-wallet"
} else {
    $failures += "DB isolation check failed: $dbCheck"
}

Write-Step "5. Seed test payouts and call GET /total"
docker exec wallet-postgres psql -U postgres -d freelancedb-wallet -c @"
DELETE FROM payouts WHERE freelancer_id = 42;
INSERT INTO payouts (contract_id, freelancer_id, amount, method, status, created_at)
VALUES (1, 42, 500.0, 'BANK_TRANSFER', 'COMPLETED', '2025-06-15'),
       (1, 42, 300.0, 'PAYPAL', 'COMPLETED', '2025-06-20'),
       (1, 42, 100.0, 'PAYPAL', 'PENDING', '2025-06-21');
"@ 2>&1 | Out-Null

$token = docker run --rm node:22-alpine node -e @"
const c=require('crypto');const s='0123456789abcdef0123456789abcdef01234567';
const b=x=>Buffer.from(x).toString('base64url');
const h=b(JSON.stringify({alg:'HS256',typ:'JWT'}));
const now=Math.floor(Date.now()/1000);
const p=b(JSON.stringify({sub:'test@example.com',uid:1,userId:1,role:'FREELANCER',iat:now,exp:now+86400}));
const sig=c.createHmac('sha256',s).update(h+'.'+p).digest('base64url');
console.log([h,p,sig].join('.'));
"@ 2>&1 | Select-Object -Last 1

$resp = curl.exe -s -w "%{http_code}" -H "Authorization: Bearer $token" `
    "http://localhost:8085/api/payouts/freelancer/42/total?startDate=2025-06-01&endDate=2025-06-30"
$code = $resp.Substring($resp.Length - 3)
$body = $resp.Substring(0, $resp.Length - 3)
if ($code -eq "200" -and $body -match "800") {
    Write-Ok "Endpoint returned $body (HTTP $code)"
} else {
    $failures += "Endpoint check failed: body=$body code=$code"
}

Write-Step "6. Loki - wallet-service logs shipped"
Start-Sleep -Seconds 15
$lokiQuery = [uri]::EscapeDataString('{app="freelance", service="wallet-service"}')
$endSec = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$startSec = $endSec - 3600
$startNs = "{0}000000000" -f $startSec
$endNs = "{0}000000000" -f $endSec
$lokiUrl = "http://localhost:3100/loki/api/v1/query_range?query=$lokiQuery&limit=5&start=$startNs&end=$endNs"
try {
    $lokiResp = curl.exe -s $lokiUrl | ConvertFrom-Json
    $streams = @($lokiResp.data.result)
    if ($streams.Count -gt 0) {
        Write-Ok "Loki received wallet-service logs - $($streams.Count) streams"
    } else {
        $failures += "Loki query returned no log streams (check logback labels and Loki push errors)"
    }
} catch {
    $failures += "Loki query failed: $_"
}

Write-Step "7. Grafana dashboard"
try {
    $gf = curl.exe -s -o NUL -w "%{http_code}" -u admin:admin "http://localhost:3000/api/health"
    if ($gf -eq "200") {
        Write-Ok "Grafana is up at http://localhost:3000 (admin / admin)"
        Write-Host "  Open Dashboards -> Wallet folder -> Wallet Service Dashboard" -ForegroundColor Gray
    } else {
        $failures += "Grafana health returned HTTP $gf"
    }
} catch {
    $failures += "Grafana not reachable on port 3000"
}

Write-Step "8. Feign clients - compile + peer service reachability (optional)"
Write-Ok "Feign interfaces compile (verified by mvn test)"
$peers = @(
    @{ Name = "user-service";     Url = "http://localhost:8081/api/health" },
    @{ Name = "job-service";      Url = "http://localhost:8082/api/health" },
    @{ Name = "contract-service"; Url = "http://localhost:8084/api/health" }
)
foreach ($p in $peers) {
    try {
        $code = curl.exe -s -o NUL -w "%{http_code}" $p.Url
        if ($code -eq "200") { Write-Ok "$($p.Name) reachable (HTTP 200)" }
        else { Write-Warn "$($p.Name) not running locally (HTTP $code) - start with: docker compose up -d $($p.Name)" }
    } catch {
        Write-Warn "$($p.Name) not running locally - Feign runtime test skipped"
    }
}

Write-Step "9. Kubernetes (optional - only if cluster is available)"
$clusterOk = $false
try {
    kubectl cluster-info 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) { $clusterOk = $true }
} catch { }

if ($clusterOk) {
    kubectl apply --dry-run=client --validate=false `
        -f infra/k8s/secrets/wallet-postgres-secret.yaml `
        -f infra/k8s/pvcs/wallet-postgres-pvc.yaml `
        -f infra/k8s/services/wallet-postgres-svc.yaml `
        -f infra/k8s/statefulsets/wallet-postgres-statefulset.yaml 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Ok "wallet-postgres K8s manifests are valid (dry-run)"
        Write-Host "  To deploy: kubectl apply -k infra/k8s  (coordinate with team for namespace)" -ForegroundColor Gray
    } else {
        $failures += "K8s dry-run failed"
    }
} else {
    Write-Warn "No Kubernetes cluster - skip K8s deploy test"
    Write-Host "  Enable Docker Desktop Kubernetes or minikube, then re-run step 9" -ForegroundColor Gray
}

Write-Step "Summary"
if ($failures.Count -eq 0) {
    Write-Ok "Member 13 local verification PASSED"
    exit 0
} else {
    Write-Fail "Member 13 verification had $($failures.Count) failure(s):"
    $failures | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 1
}

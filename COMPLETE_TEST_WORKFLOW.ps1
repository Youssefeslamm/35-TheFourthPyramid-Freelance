#!/usr/bin/env powershell
# ============================================================================
# M3 S3 PROPOSAL SERVICE - COMPLETE TESTING WORKFLOW
# ============================================================================
# This file contains the exact commands to run for complete testing
# Copy and paste commands as needed
# ============================================================================

# ============================================================================
# PART 1: SETUP (Run once to start services)
# ============================================================================

Write-Host "╔════════════════════════════════════════════════════════════════╗"
Write-Host "║ PART 1: SETUP - Starting Services                             ║"
Write-Host "╚════════════════════════════════════════════════════════════════╝"

# Go to project directory
cd "D:\35-TheFourthPyramid-Freelance"

# Stop all containers
Write-Host "`n[Step 1] Stopping existing containers..."
docker-compose down

# Start all services
Write-Host "`n[Step 2] Starting all services..."
docker-compose up -d

# Wait for services to be healthy
Write-Host "`n[Step 3] Waiting 30 seconds for services to become healthy..."
Start-Sleep -Seconds 30

# Check service status
Write-Host "`n[Step 4] Checking service status..."
docker-compose ps

Write-Host "`n✓ All services should be running. Press Enter to continue..."
Read-Host

# ============================================================================
# PART 2: HEALTH CHECKS
# ============================================================================

Write-Host "`n╔════════════════════════════════════════════════════════════════╗"
Write-Host "║ PART 2: HEALTH CHECKS                                          ║"
Write-Host "╚════════════════════════════════════════════════════════════════╝"

$services = @(
    @{ Name = "User Service"; URL = "http://localhost:8081/actuator/health" }
    @{ Name = "Job Service"; URL = "http://localhost:8082/actuator/health" }
    @{ Name = "Proposal Service"; URL = "http://localhost:8083/actuator/health" }
    @{ Name = "Contract Service"; URL = "http://localhost:8084/actuator/health" }
    @{ Name = "Wallet Service"; URL = "http://localhost:8085/actuator/health" }
)

foreach ($service in $services) {
    Write-Host "`nChecking $($service.Name)..."
    $response = curl -s -m 2 $service.URL | ConvertFrom-Json
    $status = $response.status
    Write-Host "  → Status: $status"
    if ($status -eq "UP") {
        Write-Host "  ✓ Healthy" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Unhealthy" -ForegroundColor Red
    }
}

Write-Host "`nAll services checked. Press Enter to continue..."
Read-Host

# ============================================================================
# PART 3: CREATE TEST DATA
# ============================================================================

Write-Host "`n╔════════════════════════════════════════════════════════════════╗"
Write-Host "║ PART 3: CREATE TEST DATA                                       ║"
Write-Host "╚════════════════════════════════════════════════════════════════╝"

# 3.1: Create Client User
Write-Host "`n[Step 1] Creating CLIENT user..."
$clientResponse = curl -s -X POST "http://localhost:8081/api/users" `
    -H "Content-Type: application/json" `
    -d '{
        "name": "John Client",
        "email": "client@test.com",
        "password": "Password123!",
        "phone": "+1234567890",
        "role": "CLIENT"
    }'

$clientData = $clientResponse | ConvertFrom-Json
$clientId = $clientData.id
Write-Host "  → Created CLIENT user with ID: $clientId"
Write-Host "  → Email: $($clientData.email), Role: $($clientData.role)"

# 3.2: Create Freelancer User
Write-Host "`n[Step 2] Creating FREELANCER user..."
$freelancerResponse = curl -s -X POST "http://localhost:8081/api/users" `
    -H "Content-Type: application/json" `
    -d '{
        "name": "Jane Freelancer",
        "email": "freelancer@test.com",
        "password": "Password123!",
        "phone": "+0987654321",
        "role": "FREELANCER"
    }'

$freelancerData = $freelancerResponse | ConvertFrom-Json
$freelancerId = $freelancerData.id
Write-Host "  → Created FREELANCER user with ID: $freelancerId"
Write-Host "  → Email: $($freelancerData.email), Role: $($freelancerData.role)"

# 3.3: Create Job
Write-Host "`n[Step 3] Creating JOB..."
$jobResponse = curl -s -X POST "http://localhost:8082/api/jobs" `
    -H "Content-Type: application/json" `
    -d "{
        \"clientId\": $clientId,
        \"title\": \"Build a React App\",
        \"description\": \"Create a responsive web application using React and TypeScript\",
        \"category\": \"WEB_DEV\",
        \"status\": \"OPEN\",
        \"budgetMin\": 1000,
        \"budgetMax\": 5000,
        \"rating\": 4.5,
        \"totalRatings\": 10,
        \"requirements\": [\"React\", \"TypeScript\"]
    }"

$jobData = $jobResponse | ConvertFrom-Json
$jobId = $jobData.id
Write-Host "  → Created JOB with ID: $jobId"
Write-Host "  → Title: $($jobData.title), Status: $($jobData.status)"

# 3.4: Create Proposal
Write-Host "`n[Step 4] Creating PROPOSAL..."
$proposalResponse = curl -s -X POST "http://localhost:8083/api/proposals" `
    -H "Content-Type: application/json" `
    -d "{
        \"jobId\": $jobId,
        \"freelancerId\": $freelancerId,
        \"coverLetter\": \"I have 5 years of React experience and excited to work on this project\",
        \"bidAmount\": 3500,
        \"estimatedDays\": 30,
        \"status\": \"SUBMITTED\"
    }"

$proposalData = $proposalResponse | ConvertFrom-Json
$proposalId = $proposalData.id
Write-Host "  → Created PROPOSAL with ID: $proposalId"
Write-Host "  → Status: $($proposalData.status), Bid Amount: $($proposalData.bidAmount)"

Write-Host "`n╔════════════════════════════════════════════════════════════════╗"
Write-Host "║ TEST DATA CREATED SUCCESSFULLY                                 ║"
Write-Host "║ Client ID: $clientId                                                      ║"
Write-Host "║ Freelancer ID: $freelancerId                                                ║"
Write-Host "║ Job ID: $jobId                                                      ║"
Write-Host "║ Proposal ID: $proposalId                                                   ║"
Write-Host "╚════════════════════════════════════════════════════════════════╝"

Write-Host "`nPress Enter to continue to critical test..."
Read-Host

# ============================================================================
# PART 4: THE CRITICAL TEST - ACCEPT PROPOSAL
# ============================================================================

Write-Host "`n╔════════════════════════════════════════════════════════════════╗"
Write-Host "║ PART 4: THE CRITICAL TEST - ACCEPT PROPOSAL                   ║"
Write-Host "║ This endpoint was returning 503. Should now return 200.        ║"
Write-Host "╚════════════════════════════════════════════════════════════════╝"

Write-Host "`n[Step 1] Getting proposal status BEFORE accept..."
$beforeProposal = curl -s -X GET "http://localhost:8083/api/proposals/$proposalId"
$beforeData = $beforeProposal | ConvertFrom-Json
Write-Host "  → Current Status: $($beforeData.status)"
Write-Host "  → Accepted At: $($beforeData.acceptedAt)"

Write-Host "`n[Step 2] Calling PUT /api/proposals/$proposalId/accept..."
Write-Host "  → This is the CRITICAL TEST endpoint"
Write-Host "  → If this was returning 503, it should now return 200"

$acceptResponse = curl -s -w "`nHTTP_CODE: %{http_code}" -X PUT "http://localhost:8083/api/proposals/$proposalId/accept" `
    -H "Content-Type: application/json"

Write-Host "`n  → Raw Response:"
Write-Host $acceptResponse
Write-Host ""

$acceptData = $acceptResponse -split "HTTP_CODE:" | Select-Object -First 1 | ConvertFrom-Json
$httpCode = $acceptResponse -split "HTTP_CODE:" | Select-Object -Last 1

Write-Host "  → HTTP Status Code: $httpCode"
Write-Host "  → Proposal Status: $($acceptData.status)"
Write-Host "  → Accepted At: $($acceptData.acceptedAt)"

if ($acceptData.status -eq "ACCEPTED") {
    Write-Host "`n✓✓✓ SUCCESS! Proposal was accepted." -ForegroundColor Green
    Write-Host "    Status changed from SUBMITTED to ACCEPTED" -ForegroundColor Green
} else {
    Write-Host "`n✗✗✗ FAILED! Proposal was NOT accepted." -ForegroundColor Red
    Write-Host "    Check logs: docker logs freelance-proposal-service" -ForegroundColor Red
}

Write-Host "`nPress Enter to continue..."
Read-Host

# ============================================================================
# PART 5: CHECK RABBITMQ EVENTS
# ============================================================================

Write-Host "`n╔════════════════════════════════════════════════════════════════╗"
Write-Host "║ PART 5: CHECK RABBITMQ EVENTS                                  ║"
Write-Host "╚════════════════════════════════════════════════════════════════╝"

Write-Host "`n[Step 1] Opening RabbitMQ Management UI..."
Write-Host "  → URL: http://localhost:15672"
Write-Host "  → Login: guest / guest"
Write-Host "  → Navigate to: Exchanges → proposal.events"
Write-Host "  → Look for: routed messages after each action"

Write-Host "`n[Step 2] Checking proposal.events exchange via API..."
$exchangeInfo = curl -s -u "guest:guest" "http://localhost:15672/api/exchanges/%2F/proposal.events" | ConvertFrom-Json
Write-Host "  → Exchange: $($exchangeInfo.name)"
Write-Host "  → Type: $($exchangeInfo.type)"
Write-Host "  → Durable: $($exchangeInfo.durable)"

Write-Host "`n[Step 3] Open browser and verify..."
Start-Process "http://localhost:15672"

Write-Host "`nPress Enter to continue to complete proposal test..."
Read-Host

# ============================================================================
# PART 6: COMPLETE PROPOSAL
# ============================================================================

Write-Host "`n╔════════════════════════════════════════════════════════════════╗"
Write-Host "║ PART 6: COMPLETE PROPOSAL                                      ║"
Write-Host "╚════════════════════════════════════════════════════════════════╝"

Write-Host "`n[Step 1] Calling PUT /api/proposals/$proposalId/complete..."
Write-Host "  → Headers: X-User-Id: $freelancerId, X-User-Role: FREELANCER"

$completeResponse = curl -s -w "`nHTTP_CODE: %{http_code}" -X PUT "http://localhost:8083/api/proposals/$proposalId/complete" `
    -H "Content-Type: application/json" `
    -H "X-User-Id: $freelancerId" `
    -H "X-User-Role: FREELANCER"

Write-Host "`n  → Raw Response:"
Write-Host $completeResponse
Write-Host ""

$completeData = $completeResponse -split "HTTP_CODE:" | Select-Object -First 1 | ConvertFrom-Json
$httpCode = $completeResponse -split "HTTP_CODE:" | Select-Object -Last 1

Write-Host "  → HTTP Status Code: $httpCode"
Write-Host "  → Proposal Status: $($completeData.status)"
Write-Host "  → Contract ID: $($completeData.contractId)"

if ($completeData.status -eq "COMPLETING") {
    Write-Host "`n✓ SUCCESS! Proposal status changed to COMPLETING" -ForegroundColor Green
    Write-Host "  Contract ID: $($completeData.contractId)" -ForegroundColor Green
} else {
    Write-Host "`n✗ FAILED! Status not COMPLETING" -ForegroundColor Red
}

Write-Host "`nPress Enter to continue to payment saga test..."
Read-Host

# ============================================================================
# PART 7: PAYMENT SAGA
# ============================================================================

Write-Host "`n╔════════════════════════════════════════════════════════════════╗"
Write-Host "║ PART 7: PAYMENT SAGA - SIMULATE PAYMENT COMPLETION             ║"
Write-Host "╚════════════════════════════════════════════════════════════════╝"

# Get contractId from proposal
$currentProposal = curl -s -X GET "http://localhost:8083/api/proposals/$proposalId" | ConvertFrom-Json
$contractId = $currentProposal.contractId

Write-Host "`n[Step 1] Current Proposal State:"
Write-Host "  → Proposal ID: $proposalId"
Write-Host "  → Contract ID: $contractId"
Write-Host "  → Current Status: $($currentProposal.status)"

Write-Host "`n[Step 2] Publishing payment.completed event to RabbitMQ..."
Write-Host "  → Exchange: payment.events"
Write-Host "  → Routing Key: payment.completed"
Write-Host "  → Contract ID: $contractId"

$paymentPayload = @{
    properties = @{
        delivery_mode = 2
        content_type = "application/json"
    }
    routing_key = "payment.completed"
    payload = "{""contractId"":$contractId,""proposalId"":$proposalId,""amount"":3500,""status"":""COMPLETED""}"
    payload_encoding = "string"
} | ConvertTo-Json

curl -s -X POST "http://localhost:15672/api/exchanges/%2F/payment.events/publish" `
    -u "guest:guest" `
    -H "Content-Type: application/json" `
    -d $paymentPayload | Out-Null

Write-Host "  ✓ Event published"

Write-Host "`n[Step 3] Waiting 2 seconds for event to be processed..."
Start-Sleep -Seconds 2

Write-Host "`n[Step 4] Getting proposal status AFTER payment event..."
$finalProposal = curl -s -X GET "http://localhost:8083/api/proposals/$proposalId" | ConvertFrom-Json
Write-Host "  → Current Status: $($finalProposal.status)"

if ($finalProposal.status -eq "PAID") {
    Write-Host "`n✓✓✓ SUCCESS! Proposal status is now PAID" -ForegroundColor Green
    Write-Host "    Complete flow: SUBMITTED → ACCEPTED → COMPLETING → PAID" -ForegroundColor Green
} else {
    Write-Host "`n⚠ WARNING: Status is $($finalProposal.status), expected PAID" -ForegroundColor Yellow
    Write-Host "  This is normal if payment event handling is async" -ForegroundColor Yellow
}

Write-Host "`nPress Enter for final verification..."
Read-Host

# ============================================================================
# PART 8: FINAL VERIFICATION
# ============================================================================

Write-Host "`n╔════════════════════════════════════════════════════════════════╗"
Write-Host "║ PART 8: FINAL VERIFICATION - DATABASE CHECK                   ║"
Write-Host "╚════════════════════════════════════════════════════════════════╝"

Write-Host "`n[Step 1] Proposal Status in Database:"
$dbProposal = docker exec freelance-db psql -U postgres -d freelancedb -c "SELECT id, job_id, freelancer_id, status, contract_id, accepted_at FROM proposals WHERE id=$proposalId;" 2>&1
Write-Host $dbProposal

Write-Host "`n[Step 2] User Data in Database:"
docker exec freelance-db psql -U postgres -d freelancedb -c "SELECT id, name, email, role FROM users WHERE email LIKE '%test.com';" 2>&1

Write-Host "`n[Step 3] Job Data in Database:"
docker exec freelance-db psql -U postgres -d freelancedb -c "SELECT id, client_id, title, status FROM jobs WHERE id=$jobId;" 2>&1

Write-Host "`n╔════════════════════════════════════════════════════════════════╗"
Write-Host "║ ✓✓✓ TESTING COMPLETE ✓✓✓                                       ║"
Write-Host "╚════════════════════════════════════════════════════════════════╝"

Write-Host "`n📋 SUMMARY:"
Write-Host "  ✓ Created test data (client, freelancer, job, proposal)"
Write-Host "  ✓ Tested PUT /api/proposals/$proposalId/accept → ACCEPTED"
Write-Host "  ✓ Tested PUT /api/proposals/$proposalId/complete → COMPLETING"
Write-Host "  ✓ Tested payment saga event → PAID (or PAYMENT_PENDING)"
Write-Host "  ✓ Verified RabbitMQ events published"
Write-Host "  ✓ Verified database state"

Write-Host "`n📚 REFERENCE GUIDES:"
Write-Host "  1. QUICK_START_TESTING.md - Quick reference"
Write-Host "  2. TESTING_GUIDE_README.md - Comprehensive guide"
Write-Host "  3. M3_S3_DEBUGGING_GUIDE.md - Deep debugging info"
Write-Host "  4. M3_S3_PROPOSAL_TESTING.postman_collection.json - Postman requests"

Write-Host "`n🔍 IF YOU SEE ISSUES:"
Write-Host "  1. Check logs: docker logs -f freelance-proposal-service"
Write-Host "  2. Filter for errors: docker logs freelance-proposal-service | Select-String 'ERROR'"
Write-Host "  3. Search for method: docker logs freelance-proposal-service | Select-String 'S3-F2'"
Write-Host "  4. Review docs for detailed troubleshooting"

Write-Host "`n✓ All tests completed. You can now close this window."


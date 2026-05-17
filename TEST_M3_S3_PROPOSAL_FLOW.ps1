# ============================================================================
# M3 S3 Proposal Service Event Flow - Comprehensive Testing Script
# ============================================================================
# This script tests the complete proposal acceptance, completion, and payment
# saga flow with detailed logging and validation at each step.
# ============================================================================

param(
    [string]$Action = "full",  # "full", "setup", "accept", "complete", "payment", "logs"
    [string]$ServiceFilter = "" # Filter logs by service name
)

$ErrorActionPreference = "Stop"

# Color output helper
function Write-Success { Write-Host "[✓] $args" -ForegroundColor Green }
function Write-Info { Write-Host "[ℹ] $args" -ForegroundColor Cyan }
function Write-Warning { Write-Host "[⚠] $args" -ForegroundColor Yellow }
function Write-Error { Write-Host "[✗] $args" -ForegroundColor Red }

# ============================================================================
# Phase 1: Health Checks & Service Validation
# ============================================================================

function Test-ServiceHealth {
    Write-Info "========== PHASE 1: Service Health Check =========="

    $services = @(
        @{ Name = "user-service"; Port = 8081 }
        @{ Name = "job-service"; Port = 8082 }
        @{ Name = "proposal-service"; Port = 8083 }
        @{ Name = "contract-service"; Port = 8084 }
        @{ Name = "wallet-service"; Port = 8085 }
        @{ Name = "rabbitmq"; Port = 15672 }
    )

    $healthyCount = 0
    foreach ($service in $services) {
        try {
            $response = curl -s -m 2 -o /dev/null -w "%{http_code}" "http://localhost:$($service.Port)/actuator/health" 2>$null
            if ($response -eq "200" -or $response -eq "302") {
                Write-Success "$($service.Name) is healthy (port $($service.Port))"
                $healthyCount++
            } else {
                Write-Warning "$($service.Name) returned status $response"
            }
        } catch {
            Write-Error "$($service.Name) is unreachable"
        }
    }

    Write-Info "$healthyCount/6 services healthy"
    return ($healthyCount -ge 5)
}

# ============================================================================
# Phase 2: Create Test Data
# ============================================================================

function Create-TestData {
    Write-Info "========== PHASE 2: Create Test Data =========="

    # 2.1: Create Client User
    Write-Info "Creating CLIENT user..."
    $clientResponse = curl -s -X POST "http://localhost:8081/api/users" `
        -H "Content-Type: application/json" `
        -d '{
            "name": "John Client",
            "email": "client@test.com",
            "password": "Password123!",
            "phone": "+1234567890",
            "role": "CLIENT"
        }'

    $clientId = ($clientResponse | ConvertFrom-Json).id
    if ($null -eq $clientId) {
        Write-Error "Failed to create client user"
        Write-Host $clientResponse
        return $null
    }
    Write-Success "Created CLIENT user: id=$clientId"

    # 2.2: Create Freelancer User
    Write-Info "Creating FREELANCER user..."
    $freelancerResponse = curl -s -X POST "http://localhost:8081/api/users" `
        -H "Content-Type: application/json" `
        -d '{
            "name": "Jane Freelancer",
            "email": "freelancer@test.com",
            "password": "Password123!",
            "phone": "+0987654321",
            "role": "FREELANCER"
        }'

    $freelancerId = ($freelancerResponse | ConvertFrom-Json).id
    if ($null -eq $freelancerId) {
        Write-Error "Failed to create freelancer user"
        Write-Host $freelancerResponse
        return $null
    }
    Write-Success "Created FREELANCER user: id=$freelancerId"

    # 2.3: Create Job
    Write-Info "Creating JOB..."
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

    $jobId = ($jobResponse | ConvertFrom-Json).id
    if ($null -eq $jobId) {
        Write-Error "Failed to create job"
        Write-Host $jobResponse
        return $null
    }
    Write-Success "Created JOB: id=$jobId"

    # 2.4: Create Proposal
    Write-Info "Creating PROPOSAL..."
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

    $proposalId = ($proposalResponse | ConvertFrom-Json).id
    if ($null -eq $proposalId) {
        Write-Error "Failed to create proposal"
        Write-Host $proposalResponse
        return $null
    }
    Write-Success "Created PROPOSAL: id=$proposalId"

    Write-Info "========== Test Data Created Successfully =========="
    return @{
        ClientId     = $clientId
        FreelancerId = $freelancerId
        JobId        = $jobId
        ProposalId   = $proposalId
    }
}

# ============================================================================
# Phase 3: Test Accept Proposal Flow (THE CRITICAL TEST)
# ============================================================================

function Test-AcceptProposal {
    param([object]$TestData)

    Write-Info "========== PHASE 3: Accept Proposal Flow =========="
    Write-Info "Calling PUT /api/proposals/$($TestData.ProposalId)/accept"

    $acceptResponse = curl -s -X PUT "http://localhost:8083/api/proposals/$($TestData.ProposalId)/accept" `
        -H "Content-Type: application/json"

    Write-Host $acceptResponse

    $acceptResult = $acceptResponse | ConvertFrom-Json
    if ($acceptResult.status -eq "ACCEPTED") {
        Write-Success "Proposal accepted! Status=$($acceptResult.status), AcceptedAt=$($acceptResult.acceptedAt)"
        return $true
    } else {
        Write-Error "Failed to accept proposal. Response: $acceptResponse"
        return $false
    }
}

# ============================================================================
# Phase 4: Verify RabbitMQ Events
# ============================================================================

function Check-RabbitMQEvents {
    Write-Info "========== PHASE 4: RabbitMQ Event Verification =========="
    Write-Info "Open RabbitMQ Management UI: http://localhost:15672"
    Write-Info "Navigate to Exchanges → proposal.events"
    Write-Info "Check for 'proposal.accepted' messages"
    Write-Info ""
    Write-Info "Or use this command to check queue stats:"
    Write-Info "  docker exec rabbitmq rabbitmq-admin list queues name messages"
}

# ============================================================================
# Phase 5: Test Complete Proposal Flow
# ============================================================================

function Test-CompleteProposal {
    param([object]$TestData)

    Write-Info "========== PHASE 5: Complete Proposal Flow =========="
    Write-Info "Calling PUT /api/proposals/$($TestData.ProposalId)/complete"

    $completeResponse = curl -s -X PUT "http://localhost:8083/api/proposals/$($TestData.ProposalId)/complete" `
        -H "Content-Type: application/json" `
        -H "X-User-Id: $($TestData.FreelancerId)" `
        -H "X-User-Role: FREELANCER"

    Write-Host $completeResponse

    $completeResult = $completeResponse | ConvertFrom-Json
    if ($completeResult.status -eq "COMPLETING") {
        Write-Success "Proposal completed! Status=$($completeResult.status)"
        return $true
    } else {
        Write-Error "Failed to complete proposal. Response: $completeResponse"
        return $false
    }
}

# ============================================================================
# Phase 6: Payment Saga Testing
# ============================================================================

function Test-PaymentSaga {
    param([object]$TestData)

    Write-Info "========== PHASE 6: Payment Saga Simulation =========="
    Write-Info "Publishing payment.completed event to payment.events exchange"

    # First, get the contract ID from the proposal
    Write-Info "Fetching proposal to get contractId..."
    $proposalResponse = curl -s -X GET "http://localhost:8083/api/proposals/$($TestData.ProposalId)"
    $proposal = $proposalResponse | ConvertFrom-Json
    $contractId = $proposal.contractId

    if ($null -eq $contractId) {
        Write-Warning "Contract ID not found in proposal. Payment saga may not have been triggered yet."
        return $false
    }

    Write-Success "Found Contract ID: $contractId"

    # Publish payment.completed event via RabbitMQ HTTP API
    Write-Info "Publishing payment.completed event..."
    $paymentPayload = @{
        properties      = @{
            delivery_mode = 2
            content_type  = "application/json"
        }
        routing_key     = "payment.completed"
        payload         = "{""contractId"":$contractId,""proposalId"":$($TestData.ProposalId),""amount"":3500,""status"":""COMPLETED""}"
        payload_encoding = "string"
    }

    curl -s -X POST "http://localhost:15672/api/exchanges/%2F/payment.events/publish" `
        -u "guest:guest" `
        -H "Content-Type: application/json" `
        -d ($paymentPayload | ConvertTo-Json) | Out-Null

    Write-Success "Payment.completed event published"

    # Wait a moment for the event to be processed
    Start-Sleep -Seconds 2

    # Verify proposal status changed to PAID
    Write-Info "Verifying proposal status changed to PAID..."
    $finalProposal = curl -s -X GET "http://localhost:8083/api/proposals/$($TestData.ProposalId)" | ConvertFrom-Json

    if ($finalProposal.status -eq "PAID") {
        Write-Success "Proposal status is now PAID!"
        return $true
    } else {
        Write-Warning "Proposal status is $($finalProposal.status), expected PAID"
        return $false
    }
}

# ============================================================================
# Phase 7: View Logs
# ============================================================================

function Show-ServiceLogs {
    param([string]$Filter = "")

    Write-Info "========== Service Logs =========="

    if ([string]::IsNullOrWhiteSpace($Filter) -or $Filter -eq "proposal") {
        Write-Info "--- Proposal Service Logs (last 50 lines) ---"
        docker logs --tail 50 freelance-proposal-service 2>&1 | Select-Object -Last 50
        Write-Host ""
    }

    if ([string]::IsNullOrWhiteSpace($Filter) -or $Filter -eq "contract") {
        Write-Info "--- Contract Service Logs (last 50 lines) ---"
        docker logs --tail 50 freelance-contract-service 2>&1 | Select-Object -Last 50
        Write-Host ""
    }

    if ([string]::IsNullOrWhiteSpace($Filter) -or $Filter -eq "user") {
        Write-Info "--- User Service Logs (last 50 lines) ---"
        docker logs --tail 50 freelance-user-service 2>&1 | Select-Object -Last 50
        Write-Host ""
    }
}

# ============================================================================
# Phase 8: Database Verification
# ============================================================================

function Verify-Database {
    param([object]$TestData)

    Write-Info "========== Database Verification =========="

    Write-Info "Users:"
    docker exec freelance-db psql -U postgres -d freelancedb -c "SELECT id, name, email, role FROM users WHERE id IN ($($TestData.ClientId), $($TestData.FreelancerId));" 2>&1 | grep -v "^$"

    Write-Host ""
    Write-Info "Jobs:"
    docker exec freelance-db psql -U postgres -d freelancedb -c "SELECT id, client_id, title, status FROM jobs WHERE id = $($TestData.JobId);" 2>&1 | grep -v "^$"

    Write-Host ""
    Write-Info "Proposals:"
    docker exec freelance-db psql -U postgres -d freelancedb -c "SELECT id, job_id, freelancer_id, status, contract_id FROM proposals WHERE id = $($TestData.ProposalId);" 2>&1 | grep -v "^$"
}

# ============================================================================
# Main Execution
# ============================================================================

function Main {
    Write-Info "╔════════════════════════════════════════════════════════════════╗"
    Write-Info "║  M3 S3 Proposal Service Event Flow - Testing Script           ║"
    Write-Info "║  Action: $Action"
    Write-Info "╚════════════════════════════════════════════════════════════════╝"

    try {
        # Always start with health check
        if (-not (Test-ServiceHealth)) {
            Write-Error "Services are not healthy. Please run: docker-compose up -d"
            exit 1
        }

        switch ($Action.ToLower()) {
            "full" {
                # Full test flow
                $testData = Create-TestData
                if ($null -eq $testData) { exit 1 }

                Start-Sleep -Seconds 1
                Write-Host ""

                if (-not (Test-AcceptProposal $testData)) { exit 1 }
                Start-Sleep -Seconds 2

                Check-RabbitMQEvents
                Start-Sleep -Seconds 3

                if (-not (Test-CompleteProposal $testData)) { exit 1 }
                Start-Sleep -Seconds 2

                if (-not (Test-PaymentSaga $testData)) { exit 1 }

                Verify-Database $testData

                Write-Success "========== ALL TESTS COMPLETED SUCCESSFULLY =========="
            }
            "setup" {
                $testData = Create-TestData
                Write-Info "Test data IDs: $($testData | ConvertTo-Json)"
            }
            "accept" {
                Write-Error "Use: .\TEST_M3_S3_PROPOSAL_FLOW.ps1 -Action accept -ProposalId <id>"
            }
            "complete" {
                Write-Error "Use: .\TEST_M3_S3_PROPOSAL_FLOW.ps1 -Action complete -ProposalId <id>"
            }
            "payment" {
                Write-Error "Use: .\TEST_M3_S3_PROPOSAL_FLOW.ps1 -Action payment -ProposalId <id>"
            }
            "logs" {
                Show-ServiceLogs $ServiceFilter
            }
            "healthcheck" {
                Test-ServiceHealth
            }
            default {
                Write-Error "Unknown action: $Action"
                Write-Info "Valid actions: full, setup, accept, complete, payment, logs, healthcheck"
            }
        }
    }
    catch {
        Write-Error "Script failed: $_"
        exit 1
    }
}

# Run main
Main


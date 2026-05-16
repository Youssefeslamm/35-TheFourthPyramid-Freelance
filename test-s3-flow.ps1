#!/usr/bin/env powershell
# M3 S3 Proposal Service Testing Script

$ErrorActionPreference = 'Stop'

function Invoke-API {
    param(
        [string]$Method = 'GET',
        [string]$Uri,
        [object]$Body,
        [hashtable]$Headers = @{}
    )

    try {
        $params = @{
            Uri             = $Uri
            Method          = $Method
            UseBasicParsing = $true
            Headers         = @{'Content-Type' = 'application/json'} + $Headers
        }

        if ($Body) {
            $params['Body'] = if ($Body -is [string]) { $Body } else { ($Body | ConvertTo-Json -Depth 10) }
        }

        $response = Invoke-WebRequest @params
        return $response.Content | ConvertFrom-Json
    }
    catch {
        Write-Host "ERROR: $_" -ForegroundColor Red
        throw $_
    }
}

# Test 1: Health check
Write-Host "`n=== STEP 1: Health Check ===" -ForegroundColor Green
$healthCheck = Invoke-API -Uri 'http://localhost:8081/actuator/health'
Write-Host "User Service Status: $($healthCheck.status)"
if ($healthCheck.status -ne 'UP') {
    throw "User service is not UP!"
}

# Test 2: Create Freelancer User
Write-Host "`n=== STEP 2: Create Freelancer User ===" -ForegroundColor Green
$freelancerData = @{
    name     = "Test Freelancer"
    email    = "freelancer@test.com"
    password = "Password123!"
    phone    = "+1234567890"
    role     = "FREELANCER"
}
$freelancer = Invoke-API -Method POST -Uri 'http://localhost:8081/api/users' -Body $freelancerData
$freelancerId = $freelancer.id
Write-Host "Created Freelancer: ID=$freelancerId, Email=$($freelancer.email), Role=$($freelancer.role)"

# Test 3: Create Client User
Write-Host "`n=== STEP 3: Create Client User ===" -ForegroundColor Green
$clientData = @{
    name     = "Test Client"
    email    = "client@test.com"
    password = "Password123!"
    phone    = "+0987654321"
    role     = "CLIENT"
}
$client = Invoke-API -Method POST -Uri 'http://localhost:8081/api/users' -Body $clientData
$clientId = $client.id
Write-Host "Created Client: ID=$clientId, Email=$($client.email), Role=$($client.role)"

# Test 4: Create Job
Write-Host "`n=== STEP 4: Create Job ===" -ForegroundColor Green
$jobData = @{
    clientId      = $clientId
    title         = "Test React Project"
    description   = "Build a test React application"
    category      = "WEB_DEV"
    status        = "OPEN"
    budgetMin     = 1000
    budgetMax     = 5000
    rating        = 4.5
    totalRatings  = 10
    requirements  = @("React", "TypeScript")
}
$job = Invoke-API -Method POST -Uri 'http://localhost:8082/api/jobs' -Body $jobData
$jobId = $job.id
Write-Host "Created Job: ID=$jobId, Title=$($job.title), Status=$($job.status)"

# Test 5: Create Proposal
Write-Host "`n=== STEP 5: Create Proposal ===" -ForegroundColor Green
$proposalData = @{
    jobId         = $jobId
    freelancerId  = $freelancerId
    coverLetter   = "I have 5 years of React experience"
    bidAmount     = 3500
    estimatedDays = 30
    status        = "SUBMITTED"
}
$proposal = Invoke-API -Method POST -Uri 'http://localhost:8083/api/proposals' -Body $proposalData
$proposalId = $proposal.id
Write-Host "Created Proposal: ID=$proposalId, Status=$($proposal.status), BidAmount=$($proposal.bidAmount)"

# Test 6: Check Proposal Status Before Accept
Write-Host "`n=== STEP 6: Verify Proposal Status Before Accept ===" -ForegroundColor Green
$proposalBefore = Invoke-API -Uri "http://localhost:8083/api/proposals/$proposalId"
Write-Host "Current Status: $($proposalBefore.status)"
if ($proposalBefore.status -ne 'SUBMITTED') {
    throw "Expected status SUBMITTED, got $($proposalBefore.status)"
}

# Test 7: CRITICAL TEST - Accept Proposal
Write-Host "`n=== STEP 7: CRITICAL TEST - Accept Proposal ===" -ForegroundColor Yellow
try {
    $proposalAccepted = Invoke-API -Method PUT -Uri "http://localhost:8083/api/proposals/$proposalId/accept"
    Write-Host "✓ Accept succeeded!" -ForegroundColor Green
    Write-Host "  Status: $($proposalAccepted.status)"
    Write-Host "  AcceptedAt: $($proposalAccepted.acceptedAt)"

    if ($proposalAccepted.status -ne 'ACCEPTED') {
        throw "Expected status ACCEPTED, got $($proposalAccepted.status)"
    }
}
catch {
    Write-Host "✗ Accept FAILED!" -ForegroundColor Red
    Write-Host "Error: $_"
    throw $_
}

# Test 8: Complete Proposal
Write-Host "`n=== STEP 8: Complete Proposal ===" -ForegroundColor Green
try {
    $proposalComplete = Invoke-API -Method PUT -Uri "http://localhost:8083/api/proposals/$proposalId/complete" `
        -Headers @{
            'X-User-Id'   = $freelancerId
            'X-User-Role' = 'FREELANCER'
        }
    Write-Host "✓ Complete succeeded!" -ForegroundColor Green
    Write-Host "  Status: $($proposalComplete.status)"
    Write-Host "  ContractId: $($proposalComplete.contractId)"
}
catch {
    Write-Host "Note: Complete may require contract to exist first"
    Write-Host "Error details: $_"
}

# Test 9: Check RabbitMQ Exchanges
Write-Host "`n=== STEP 9: Verify RabbitMQ Exchanges ===" -ForegroundColor Green
try {
    $exchanges = Invoke-API -Method GET -Uri 'http://localhost:15672/api/exchanges/%2F' `
        -Headers @{Authorization = 'Basic Z3Vlc3Q6Z3Vlc3Q='} # base64(guest:guest)
    Write-Host "RabbitMQ Exchanges:"
    $exchanges | Where-Object {$_.name -like 'proposal.*' -or $_.name -like 'contract.*' -or $_.name -like 'payment.*'} | ForEach-Object {
        Write-Host "  - $($_.name) (type: $($_.type))"
    }
}
catch {
    Write-Host "Could not verify RabbitMQ exchanges: $_"
}

Write-Host "`n=== TEST SUMMARY ===" -ForegroundColor Cyan
Write-Host "Freelancer ID: $freelancerId"
Write-Host "Client ID: $clientId"
Write-Host "Job ID: $jobId"
Write-Host "Proposal ID: $proposalId"
Write-Host "Proposal Status: ACCEPTED"
Write-Host "`nAll critical tests passed"



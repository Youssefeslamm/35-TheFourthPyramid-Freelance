# M3 S3 Proposal Service Event Flow - Testing & Debugging Guide

## Current Issues Identified

### 1. **503 Service Unavailable on PUT /api/proposals/{id}/accept**
- **Root Cause**: The acceptProposal() method calls `userServiceClient.getUserById()` and catches `FeignException` which throws 503.
- **Why**: Either the user-service Feign client URL is misconfigured, or user-service is not reachable from proposal-service.

### 2. **Service-to-Service Communication Configuration**
- **Feign URLs in application.yml (proposal-service)**:
  ```yaml
  feign.user-service.url: http://user-service:8080
  feign.job-service.url: http://job-service:8080
  feign.contract-service.url: http://contract-service:8080
  feign.wallet-service.url: http://wallet-service:8080
  ```
- **Docker Compose Port Mappings** (from docker-compose.yaml):
  - user-service: 8081:8080 (external:internal)
  - job-service: 8082:8080
  - proposal-service: 8083:8080
  - contract-service: 8084:8080
  - wallet-service: 8085:8080

**✓ CORRECT**: The Feign URLs use Docker service names (not localhost) and internal port 8080 - this is correct for inter-container communication.

### 3. **RabbitMQ Event Configuration**
The proposal-service publishes to `proposal.events` exchange and consumes from:
- `contract.events` for contract.created, contract.status-changed, contract.cancelled
- `payment.events` for payment.initiated, payment.completed, payment.failed, payment.refunded
- All routed to `proposal.saga-feedback` queue

### 4. **Missing Logging in Critical Paths**
- Feign client calls don't have before/after logs
- RabbitMQ publish/consume events lack detailed logging
- Error scenarios don't provide enough debugging info

---

## Step-by-Step Testing Plan

### Phase 1: Service Startup & Health Checks

#### 1.1 Start Docker Services
```powershell
cd D:\35-TheFourthPyramid-Freelance
docker-compose down
docker-compose up -d
Start-Sleep -Seconds 5
docker-compose ps
```

#### 1.2 Verify Service Health
```powershell
# Check all services are healthy
$services = @("user-service", "job-service", "proposal-service", "contract-service", "wallet-service")

foreach ($service in $services) {
    $port = switch($service) {
        "user-service" { 8081 }
        "job-service" { 8082 }
        "proposal-service" { 8083 }
        "contract-service" { 8084 }
        "wallet-service" { 8085 }
    }
    
    $health = curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health"
    Write-Host "$service ($port): $health"
}
```

#### 1.3 Check RabbitMQ Management UI
- Open browser: http://localhost:15672
- Login: guest / guest
- Verify exchanges exist:
  - proposal.events
  - contract.events
  - payment.events
- Verify queues exist:
  - proposal.saga-feedback
  - proposal.saga-feedback.dlq
  - proposal.completed.user.queue
  - proposal.cancelled.user.queue

#### 1.4 Check Database
```powershell
# Connect to Postgres and verify tables exist
# You can use a GUI client or psql
# Host: localhost, Port: 5432, User: postgres, Pass: postgres, DB: freelancedb
```

---

### Phase 2: Create Test Data

#### 2.1 Create a User (Client)
```http
POST http://localhost:8081/api/users
Content-Type: application/json

{
  "name": "John Client",
  "email": "client@test.com",
  "password": "Password123!",
  "phone": "+1234567890",
  "role": "CLIENT"
}
```

**Expected Response**: 201 Created with userId (e.g., id: 1)

#### 2.2 Create a User (Freelancer)
```http
POST http://localhost:8081/api/users
Content-Type: application/json

{
  "name": "Jane Freelancer",
  "email": "freelancer@test.com",
  "password": "Password123!",
  "phone": "+0987654321",
  "role": "FREELANCER"
}
```

**Expected Response**: 201 Created with userId (e.g., id: 2)

#### 2.3 Create a Job
```http
POST http://localhost:8082/api/jobs
Content-Type: application/json

{
  "clientId": 1,
  "title": "Build a React App",
  "description": "Create a responsive web application using React and TypeScript",
  "category": "WEB_DEV",
  "status": "OPEN",
  "budgetMin": 1000,
  "budgetMax": 5000,
  "rating": 4.5,
  "totalRatings": 10,
  "requirements": ["React", "TypeScript", "Redux"]
}
```

**Expected Response**: 201 Created with jobId (e.g., id: 1)

#### 2.4 Create a Proposal
```http
POST http://localhost:8083/api/proposals
Content-Type: application/json

{
  "jobId": 1,
  "freelancerId": 2,
  "coverLetter": "I have 5 years of React experience and excited to work on this project",
  "bidAmount": 3500,
  "estimatedDays": 30,
  "status": "SUBMITTED"
}
```

**Expected Response**: 201 Created with proposalId (e.g., id: 1)

**Note the proposalId for next steps.**

---

### Phase 3: Test Accept Proposal Flow (THE CRITICAL TEST)

#### 3.1 Accept Proposal Request
```http
PUT http://localhost:8083/api/proposals/1/accept
```

**Expected Response**: 200 OK with proposal JSON including status: "ACCEPTED"

**If 503 Error:**
1. Check logs: `docker logs freelance-proposal-service 2>&1 | tail -100`
2. Verify user-service is healthy: `curl http://localhost:8081/actuator/health`
3. Check if user with ID 2 exists: `GET http://localhost:8081/api/users/2`
4. Verify Feign URL resolution in proposal-service logs

#### 3.2 Verify proposal.accepted Event in RabbitMQ
- Go to RabbitMQ UI: http://localhost:15672
- Navigate to Exchanges → proposal.events
- Check if any messages were published
- Monitor the proposal.saga-feedback queue

#### 3.3 Check Contract Service Logs
```powershell
docker logs freelance-contract-service 2>&1 | tail -50
```
- Should see contract.created event being consumed OR
- Should show it's listening for proposal.accepted

---

### Phase 4: Test Complete Proposal Flow

#### 4.1 Complete Proposal Request
```http
PUT http://localhost:8083/api/proposals/1/complete
X-User-Id: 2
X-User-Role: FREELANCER
```

**Expected Response**: 200 OK with proposal JSON including status: "COMPLETING"

#### 4.2 Verify proposal.completed Event
- RabbitMQ UI: Check proposal.events exchange for published messages
- Check contract-service logs for consumption of proposal.completed

#### 4.3 Verify contract.created Event
- RabbitMQ UI: Check contract.events exchange
- Proposal service should receive contract.created through saga-feedback queue
- Proposal status should eventually update

---

### Phase 5: Test Payment Saga Feedback

#### 5.1 Simulate Payment Events
```powershell
# Use RabbitMQ Management UI or publish directly:

# Method 1: Using Postman/curl to RabbitMQ HTTP API
# Publish payment.completed event to payment.events exchange

# Payment Completed
curl -X POST http://localhost:15672/api/exchanges/%2F/payment.events/publish \
  -u guest:guest \
  -H "Content-Type: application/json" \
  -d '{
    "properties": {
      "delivery_mode": 2,
      "content_type": "application/json"
    },
    "routing_key": "payment.completed",
    "payload": "{\"contractId\":1,\"amount\":3500,\"status\":\"COMPLETED\"}",
    "payload_encoding": "string"
  }'
```

#### 5.2 Verify Proposal Status Update
```http
GET http://localhost:8083/api/proposals/1
```
- Should show status: "PAID" (or PAYMENT_PENDING → PAID)

---

## Debugging Commands

### View Service Logs
```powershell
# Proposal service logs (verbose)
docker logs -f freelance-proposal-service

# All services
docker-compose logs -f

# Filter specific error
docker logs freelance-proposal-service 2>&1 | grep -i "unavailable\|feign\|error" | tail -50
```

### Check Network Connectivity
```powershell
# From inside a running container
docker exec freelance-proposal-service curl -v http://user-service:8080/actuator/health

docker exec freelance-proposal-service curl -v http://job-service:8080/actuator/health
```

### Check RabbitMQ via CLI
```powershell
# List exchanges
docker exec rabbitmq rabbitmq-admin list exchanges

# List queues
docker exec rabbitmq rabbitmq-admin list queues

# List bindings
docker exec rabbitmq rabbitmq-admin list bindings
```

### Database Queries
```powershell
# Connect to postgres
docker exec -it freelance-db psql -U postgres -d freelancedb -c "SELECT * FROM proposals;"

# Check users
docker exec -it freelance-db psql -U postgres -d freelancedb -c "SELECT id, name, email, role FROM users;"

# Check jobs
docker exec -it freelance-db psql -U postgres -d freelancedb -c "SELECT * FROM jobs;"
```

---

## Common Issues & Solutions

### Issue 1: 503 Service Unavailable on Accept Proposal

**Symptoms:**
- PUT /api/proposals/{id}/accept returns 503
- Logs show "User service temporarily unavailable"

**Root Causes & Fixes:**
1. **user-service not reachable from proposal-service**
   - Fix: Ensure all services are running: `docker-compose ps`
   - Check user-service health: `curl http://localhost:8081/actuator/health`
   - If down: `docker-compose up -d user-service`

2. **Feign URL misconfigured**
   - Check proposal-service application.yml:
     - Must be: `http://user-service:8080` (Docker service name, not localhost)
   - Restart proposal-service after any config changes

3. **User doesn't exist**
   - Error: "Freelancer not found: {id}"
   - Fix: Create the user first (Phase 2 step 2.2)

4. **User role is not FREELANCER**
   - Error: "Proposal freelancer must have FREELANCER role"
   - Fix: Verify user was created with role: "FREELANCER"

### Issue 2: RabbitMQ Events Not Published

**Symptoms:**
- proposal.accepted doesn't appear in proposal.events exchange
- No messages in saga-feedback queue

**Root Causes & Fixes:**
1. **RabbitMQ not healthy**
   - Fix: `docker-compose up -d rabbitmq` and wait 10 seconds

2. **Exchanges or queues not created**
   - Fix: Restart proposal-service to trigger @Bean creation
   - Check RabbitMQ UI: http://localhost:15672

3. **MessageConverter not configured**
   - The ProposalRabbitConfig should have Jackson2JsonMessageConverter
   - Verify in application logs: "Message converter configured"

### Issue 3: Contract Service Not Consuming Events

**Symptoms:**
- proposal.completed published but contract not created
- No changes in contract-service logs

**Root Causes & Fixes:**
1. **contract-service not running**
   - Check: `docker-compose ps`
   - Start: `docker-compose up -d contract-service`

2. **Queues not bound to contract-service**
   - Verify binding exists in RabbitMQ UI
   - Check contract-service configuration for listener queues

3. **Contract service error consuming event**
   - Check logs: `docker logs freelance-contract-service`
   - Look for listener registration: "Listening on queue: ..."

---

## Enhanced Logging Improvements Needed

The proposal-service needs better logging for debugging:

1. **In acceptProposal()**:
   - Log before calling userServiceClient.getUserById()
   - Log after successful fetch
   - Log the fetched freelancer details (id, role, status)

2. **In completeProposal()**:
   - Log job existence check
   - Log freelancer status validation
   - Log before publishing proposal.completed event

3. **In ProposalEventPublisher.publish*()**:
   - Already has MDC and logs - good!
   - Add timestamps

4. **In SagaFeedbackConsumer.onSagaFeedback()**:
   - Already logs routing key - good!
   - Add more detailed event details

---

## Postman Collection Template

Import this into Postman to test the flow:

```json
{
  "info": {
    "name": "M3 S3 Proposal Service Testing",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Create Client User",
      "request": {
        "method": "POST",
        "url": "http://localhost:8081/api/users",
        "header": [{ "key": "Content-Type", "value": "application/json" }],
        "body": {
          "mode": "raw",
          "raw": "{\"name\":\"John Client\",\"email\":\"client@test.com\",\"password\":\"Password123!\",\"phone\":\"+1234567890\",\"role\":\"CLIENT\"}"
        }
      }
    },
    {
      "name": "Create Freelancer User",
      "request": {
        "method": "POST",
        "url": "http://localhost:8081/api/users",
        "header": [{ "key": "Content-Type", "value": "application/json" }],
        "body": {
          "mode": "raw",
          "raw": "{\"name\":\"Jane Freelancer\",\"email\":\"freelancer@test.com\",\"password\":\"Password123!\",\"phone\":\"+0987654321\",\"role\":\"FREELANCER\"}"
        }
      }
    },
    {
      "name": "Create Job",
      "request": {
        "method": "POST",
        "url": "http://localhost:8082/api/jobs",
        "header": [{ "key": "Content-Type", "value": "application/json" }],
        "body": {
          "mode": "raw",
          "raw": "{\"clientId\":1,\"title\":\"Build React App\",\"description\":\"Create web app\",\"category\":\"WEB_DEV\",\"status\":\"OPEN\",\"budgetMin\":1000,\"budgetMax\":5000,\"rating\":4.5,\"totalRatings\":10,\"requirements\":[\"React\"]}"
        }
      }
    },
    {
      "name": "Create Proposal",
      "request": {
        "method": "POST",
        "url": "http://localhost:8083/api/proposals",
        "header": [{ "key": "Content-Type", "value": "application/json" }],
        "body": {
          "mode": "raw",
          "raw": "{\"jobId\":1,\"freelancerId\":2,\"coverLetter\":\"I have 5 years React experience\",\"bidAmount\":3500,\"estimatedDays\":30,\"status\":\"SUBMITTED\"}"
        }
      }
    },
    {
      "name": "Accept Proposal",
      "request": {
        "method": "PUT",
        "url": "http://localhost:8083/api/proposals/1/accept"
      }
    },
    {
      "name": "Complete Proposal",
      "request": {
        "method": "PUT",
        "url": "http://localhost:8083/api/proposals/1/complete",
        "header": [
          { "key": "X-User-Id", "value": "2" },
          { "key": "X-User-Role", "value": "FREELANCER" }
        ]
      }
    }
  ]
}
```

---

## Success Criteria

✓ All Docker services healthy  
✓ POST /api/users returns 201 for client  
✓ POST /api/users returns 201 for freelancer  
✓ POST /api/jobs returns 201  
✓ POST /api/proposals returns 201  
✓ **PUT /api/proposals/{id}/accept returns 200 (NOT 503)**  
✓ proposal.accepted event published to proposal.events exchange  
✓ PUT /api/proposals/{id}/complete returns 200  
✓ proposal.completed event published  
✓ contract-service consumes and creates contract  
✓ contract.created event routed to proposal.saga-feedback  
✓ Proposal contractId updated  
✓ Payment saga events correctly update proposal status  

---

## Next Steps After Testing

1. If 503 persists: Enable debug logging for Feign client in application.yml:
   ```yaml
   logging:
     level:
       feign: DEBUG
       org.springframework.cloud.openfeign: DEBUG
   ```

2. Review complete flow in contract-service for proposal.completed event handling

3. Ensure all RabbitMQ bindings are correct (checked via RabbitMQ UI)

4. Add comprehensive integration tests covering the full S3 saga flow



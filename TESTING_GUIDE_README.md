# M3 S3 Proposal Service - Debugging & Testing Guide

## Quick Start (3 Minutes)

### 1. Start Services
```powershell
cd D:\35-TheFourthPyramid-Freelance
docker-compose down
docker-compose up -d
Start-Sleep -Seconds 30
docker-compose ps
```

### 2. Run Full Test Suite
```powershell
.\TEST_M3_S3_PROPOSAL_FLOW.ps1 -Action full
```

### 3. View Logs (if test fails)
```powershell
docker logs -f freelance-proposal-service | Select-String -Pattern "ERROR|S3-F2|acceptProposal" -Context 2
```

---

## Understanding the 503 Error

### What Returns 503?

The endpoint `PUT /api/proposals/{id}/accept` returns HTTP 503 Service Unavailable when:

1. **User-service is unreachable** - The proposal-service Feign client cannot connect to user-service
2. **User-service is slow** - The Feign client request times out
3. **Network connectivity issue** - Services cannot reach each other in Docker network

### Why Does This Happen?

The `acceptProposal()` method in ProposalService calls:
```java
userServiceClient.getUserById(proposal.getFreelancerId(), null);
```

This is a Feign client call that routes to: `http://user-service:8080/api/users/{id}`

If this call throws a `FeignException` (any type except `NotFound`), we return 503:
```java
catch (FeignException e) {
    throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "User service temporarily unavailable"
    );
}
```

### The Fix (Already Applied)

Enhanced logging has been added to help debug:
```java
log.debug("Fetching freelancer details from user-service. freelancerId={}", 
        proposal.getFreelancerId());
freelancer = userServiceClient.getUserById(proposal.getFreelancerId(), null);
log.debug("Successfully fetched freelancer: id={}, name={}, role={}", 
        freelancer.getId(), freelancer.getName(), freelancer.getRole());
```

Plus Feign debug logging in application.yml:
```yaml
logging:
  level:
    org.springframework.cloud.openfeign: DEBUG
    feign: DEBUG
```

---

## Service-to-Service Communication Configuration

### Current Setup (Correct ✓)

**Docker Compose Port Mappings:**
```
user-service:      8081:8080  (external:internal port)
job-service:       8082:8080
proposal-service:  8083:8080
contract-service:  8084:8080
wallet-service:    8085:8080
```

**Feign Client URLs (in application.yml):**
```yaml
feign.user-service.url: http://user-service:8080
feign.job-service.url: http://job-service:8080
feign.contract-service.url: http://contract-service:8080
feign.wallet-service.url: http://wallet-service:8080
```

**Why This Works:**
- Services run inside Docker containers
- Inside the Docker network, services are accessible by their service name (not localhost)
- Internal port is 8080 for all services
- External ports (8081-8085) are only for host machine access (e.g., Postman, curl)

---

## Event Flow Documentation

### S3-F2: Accept Proposal

```
Client Request:
PUT /api/proposals/{id}/accept

ProposalService.acceptProposal():
  1. Load proposal from DB
  2. Validate status is SUBMITTED or SHORTLISTED
  3. Call userServiceClient.getUserById(freelancerId)  ← CRITICAL (can throw 503)
  4. Validate freelancer has FREELANCER role
  5. Update proposal.status = ACCEPTED
  6. Save to DB
  7. Publish event: proposal.accepted to proposal.events exchange
  8. Return updated proposal

RabbitMQ:
  - Exchange: proposal.events (type: topic)
  - Routing Key: proposal.accepted
  - Consumers: 
    - contract-service (should listen for proposal.accepted)
    - Other interested services

Success Indicators:
  ✓ Returns 200 OK (not 503)
  ✓ Proposal.status = ACCEPTED
  ✓ Proposal.acceptedAt is set
  ✓ Event appears in RabbitMQ proposal.events exchange
```

### S3-F4: Complete Proposal

```
Client Request:
PUT /api/proposals/{id}/complete
Headers: X-User-Id: {freelancerId}, X-User-Role: FREELANCER

ProposalService.completeProposal():
  1. Load proposal from DB
  2. Validate status is ACCEPTED
  3. Validate caller is proposal.freelancerId or ADMIN
  4. Call jobServiceClient.getJobById(jobId)      ← Can throw 503
  5. Validate job exists and not CLOSED
  6. Call userServiceClient.getUserById(freelancerId)  ← Can throw 503
  7. Validate freelancer is ACTIVE
  8. Call contractServiceClient.getActiveContractForProposal(proposalId)  ← Can throw 503
  9. Validate contract exists
  10. Update proposal.status = COMPLETING
  11. Update proposal.contractId
  12. Save to DB
  13. Publish event: proposal.completed to proposal.events exchange
  14. Return updated proposal

RabbitMQ:
  - Exchange: proposal.events
  - Routing Key: proposal.completed
  - Consumers:
    - contract-service (to mark contract as active)
    - user-service (to notify client)
    - Any other interested party

Success Indicators:
  ✓ Returns 200 OK (not 503)
  ✓ Proposal.status = COMPLETING
  ✓ Proposal.contractId is set
  ✓ Event appears in RabbitMQ proposal.events exchange
```

### Payment Saga Feedback

```
Proposal-Service Consumer (SagaFeedbackConsumer):
  Listens on: proposal.saga-feedback queue
  Bound to: contract.events, payment.events exchanges

Receives Events:
  - contract.created → Update proposal.contractId
  - payment.initiated → Update proposal.status = PAYMENT_PENDING
  - payment.completed → Update proposal.status = PAID  ✓ Success!
  - payment.failed → Update proposal.status = PAYMENT_FAILED
  - payment.refunded → Update proposal.status = REFUNDED

Status Flow:
  SUBMITTED → ACCEPTED → COMPLETING → PAYMENT_PENDING → PAID ✓

If Payment Fails:
  SUBMITTED → ACCEPTED → COMPLETING → PAYMENT_FAILED
  Then publish proposal.cancelled event for compensation
```

---

## Test Data Setup

### Step 1: Create Users

**Client User (POST /api/users)**
```json
{
  "name": "John Client",
  "email": "client@test.com",
  "password": "Password123!",
  "phone": "+1234567890",
  "role": "CLIENT"
}
```
Response: `{ "id": 1, "name": "John Client", ... }`

**Freelancer User (POST /api/users)**
```json
{
  "name": "Jane Freelancer",
  "email": "freelancer@test.com",
  "password": "Password123!",
  "phone": "+0987654321",
  "role": "FREELANCER"
}
```
Response: `{ "id": 2, "name": "Jane Freelancer", ... }`

### Step 2: Create Job

**POST /api/jobs**
```json
{
  "clientId": 1,
  "title": "Build a React App",
  "description": "Create a responsive web application",
  "category": "WEB_DEV",
  "status": "OPEN",
  "budgetMin": 1000,
  "budgetMax": 5000,
  "rating": 4.5,
  "totalRatings": 10,
  "requirements": ["React", "TypeScript"]
}
```
Response: `{ "id": 1, "clientId": 1, "status": "OPEN", ... }`

### Step 3: Create Proposal

**POST /api/proposals**
```json
{
  "jobId": 1,
  "freelancerId": 2,
  "coverLetter": "I have 5 years of React experience",
  "bidAmount": 3500,
  "estimatedDays": 30,
  "status": "SUBMITTED"
}
```
Response: `{ "id": 1, "jobId": 1, "freelancerId": 2, "status": "SUBMITTED", ... }`

---

## Testing with Postman

### Import Collection
1. Open Postman
2. File → Import
3. Select: `M3_S3_PROPOSAL_TESTING.postman_collection.json`
4. Collection appears in sidebar

### Run Requests in Order
1. **1. Create CLIENT User** - Save the returned `id`
2. **2. Create FREELANCER User** - Save the returned `id`
3. **3. Create JOB** - Update clientId, save returned `id`
4. **4. Create PROPOSAL** - Update jobId and freelancerId, save returned `id`
5. **5. GET Proposal Details (Before Accept)** - Verify status=SUBMITTED
6. **6. ACCEPT PROPOSAL** - ⭐ **THE CRITICAL TEST** - Should return 200, not 503
7. **7. GET Proposal Details (After Accept)** - Verify status=ACCEPTED
8. **8. Check RabbitMQ** - Navigate manually to http://localhost:15672
9. **9. COMPLETE PROPOSAL** - Update headers with freelancer ID
10. **10. GET Proposal Details (After Complete)** - Verify status=COMPLETING, contractId set
11. **11. Publish payment.completed Event** - Update contractId and proposalId
12. **12. GET Proposal Details (After Payment)** - Verify status=PAID

---

## Debugging the 503 Error

### Step 1: Check Logs

```powershell
# Real-time logs with ERROR and WARN
docker logs -f freelance-proposal-service 2>&1 | Select-String -Pattern "ERROR|WARN|503|unavailable" -Context 3

# Search for acceptProposal method execution
docker logs -f freelance-proposal-service 2>&1 | Select-String "S3-F2|acceptProposal" -Context 2
```

### Step 2: Check User Service Health

```powershell
# Is it running?
docker-compose ps | Select-String "user-service"

# Is it healthy?
curl -s http://localhost:8081/actuator/health | jq .

# Check its logs
docker logs freelance-user-service
```

### Step 3: Check Network Connectivity

```powershell
# From inside proposal-service container
docker exec freelance-proposal-service curl -v http://user-service:8080/actuator/health

# Output should show:
# > GET /actuator/health HTTP/1.1
# > Host: user-service:8080
# < HTTP/1.1 200 OK
```

### Step 4: Verify Feign Configuration

In `proposal-service/src/main/resources/application.yml`:
```yaml
feign.user-service.url: http://user-service:8080
```

NOT:
- ❌ `http://localhost:8080` (won't work inside Docker)
- ❌ `http://127.0.0.1:8080` (won't work inside Docker)
- ❌ `http://user-service:8081` (wrong internal port)

### Step 5: Check if User Exists

```powershell
# Get user 2 (the freelancer)
curl -s http://localhost:8081/api/users/2 | jq .

# Should show:
# {
#   "id": 2,
#   "name": "Jane Freelancer",
#   "role": "FREELANCER",
#   ...
# }
```

If user doesn't exist, create it first with step 1.

### Step 6: Enable Debug Logging

Already done! But if you need more:

Edit `proposal-service/src/main/resources/application.yml`:
```yaml
logging:
  level:
    # Existing
    com.team35.freelance.proposal: DEBUG
    org.springframework.cloud.openfeign: DEBUG
    
    # Add these for more verbosity
    org.springframework.web: DEBUG
    org.springframework.cloud.client.loadbalancer: DEBUG
    org.springframework.retry: DEBUG
```

Rebuild and restart:
```powershell
cd proposal-service
mvn clean package -DskipTests
docker-compose up -d --build proposal-service
```

---

## RabbitMQ Queue & Exchange Verification

### Check Exchanges Exist

```powershell
# Via HTTP API (requires Basic auth: guest/guest)
curl -s -u guest:guest http://localhost:15672/api/exchanges/%2F | jq '.[] | .name'

# Expected exchanges:
# - proposal.events
# - contract.events
# - payment.events
# - (others)
```

### Check Queues Exist

```powershell
curl -s -u guest:guest http://localhost:15672/api/queues/%2F | jq '.[] | .name'

# Expected queues:
# - proposal.saga-feedback
# - proposal.saga-feedback.dlq
# - (others)
```

### Check Bindings

```powershell
curl -s -u guest:guest http://localhost:15672/api/bindings/%2F | jq '.[] | select(.source=="proposal.events")'

# Should show bindings like:
# - source: proposal.events, destination: proposal.saga-feedback, routing_key: contract.created
# - source: payment.events, destination: proposal.saga-feedback, routing_key: payment.completed
# (etc)
```

### Manual Event Publishing

```powershell
# Publish payment.completed event for testing
curl -s -u guest:guest -X POST http://localhost:15672/api/exchanges/%2F/payment.events/publish \
  -H "Content-Type: application/json" \
  -d '{
    "properties": {
      "delivery_mode": 2,
      "content_type": "application/json"
    },
    "routing_key": "payment.completed",
    "payload": "{\"contractId\":1,\"proposalId\":1,\"amount\":3500,\"status\":\"COMPLETED\"}",
    "payload_encoding": "string"
  }'
```

---

## Database Verification

### Check Created Test Data

```powershell
# Users
docker exec freelance-db psql -U postgres -d freelancedb -c \
  "SELECT id, name, email, role FROM users WHERE email LIKE '%test.com';"

# Jobs
docker exec freelance-db psql -U postgres -d freelancedb -c \
  "SELECT id, client_id, title, status FROM jobs LIMIT 5;"

# Proposals
docker exec freelance-db psql -U postgres -d freelancedb -c \
  "SELECT id, job_id, freelancer_id, status, contract_id, accepted_at FROM proposals LIMIT 5;"
```

### Reset Test Data

```powershell
# Delete test proposals
docker exec freelance-db psql -U postgres -d freelancedb -c \
  "DELETE FROM proposals WHERE freelancer_id IN (SELECT id FROM users WHERE email LIKE '%test.com');"

# Delete test jobs
docker exec freelance-db psql -U postgres -d freelancedb -c \
  "DELETE FROM jobs WHERE client_id IN (SELECT id FROM users WHERE email LIKE '%test.com');"

# Delete test users
docker exec freelance-db psql -U postgres -d freelancedb -c \
  "DELETE FROM users WHERE email LIKE '%test.com';"
```

---

## Success Criteria Checklist

### ✓ Phase 1: Service Health
- [ ] All services show 200 OK on `/actuator/health`
- [ ] RabbitMQ is accessible at http://localhost:15672
- [ ] Database is accessible

### ✓ Phase 2: Test Data Creation
- [ ] Client user created with ID (e.g., 1)
- [ ] Freelancer user created with ID (e.g., 2)
- [ ] Job created with ID (e.g., 1)
- [ ] Proposal created with ID (e.g., 1), status=SUBMITTED

### ✓ Phase 3: Accept Proposal (THE CRITICAL TEST)
- [ ] PUT /api/proposals/1/accept returns **200 OK** (NOT 503)
- [ ] Response includes status=ACCEPTED
- [ ] Response includes acceptedAt timestamp
- [ ] Logs show "Successfully fetched freelancer" message
- [ ] No "User service temporarily unavailable" error

### ✓ Phase 4: RabbitMQ Events
- [ ] RabbitMQ UI shows routed messages on proposal.events exchange
- [ ] proposal.accepted event was published

### ✓ Phase 5: Complete Proposal
- [ ] PUT /api/proposals/1/complete returns 200 OK
- [ ] Response includes status=COMPLETING
- [ ] Response includes contractId
- [ ] proposal.completed event was published

### ✓ Phase 6: Payment Saga
- [ ] Manual payment.completed event published
- [ ] GET /api/proposals/1 shows status=PAID
- [ ] Logs show "Consuming payment.completed" message

---

## Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| 503 on accept | user-service down | `docker-compose up -d user-service` |
| 503 on accept | Feign URL wrong | Check application.yml: `http://user-service:8080` |
| 404 Freelancer | User not created | Run "Create FREELANCER User" request |
| 400 Bad role | User created as CLIENT | Delete and recreate with role=FREELANCER |
| Event not in RabbitMQ | No listener/consumer | Check SagaFeedbackConsumer is registered |
| Status not PAID | Payment event not received | Manually publish via Postman #11 |
| Contract not found | No contract created | Contract-service must consume proposal.accepted |

---

## Production Considerations

### 1. Retry Logic
The current code doesn't retry on Feign failures. In production:
```java
@Retry(name = "userServiceRetry")
UserProfileDTO getUserById(Long id);
```

### 2. Circuit Breaker
Add Resilience4j circuit breaker:
```yaml
resilience4j.circuitbreaker:
  instances:
    userServiceCircuitBreaker:
      failure-rate-threshold: 50
      wait-duration-in-open-state: 10s
```

### 3. Fallback Strategy
Instead of 503, implement graceful degradation:
```java
@Fallback(fallbackMethod = "fallbackGetUser")
UserProfileDTO getUserById(Long id);

UserProfileDTO fallbackGetUser(Long id) {
    // Return cached user or default value
}
```

### 4. Async Event Publishing
Use Spring's `@Async` and `CompletableFuture` for non-blocking:
```java
@Async
CompletableFuture<Void> publishAcceptedAsync(ProposalAcceptedEvent event)
```

### 5. Message Dead Letter Queue (DLQ)
Already configured! Events that fail processing go to:
- `proposal.saga-feedback.dlq`
- `proposal.saga-feedback.dlx` (dead letter exchange)

---

## Additional Resources

- **M3 Specifications**: Check project documentation for S3-F2, S3-F4, S3-F11 requirements
- **Feign Documentation**: https://spring.io/projects/spring-cloud-openfeign
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- **Docker Logs**: `docker logs -f <container-name>`
- **Spring Docs**: https://spring.io/projects/spring-framework

---

## Support & Debugging

### Enable Request/Response Logging

Edit `proposal-service/src/main/resources/application.yml`:
```yaml
logging:
  level:
    org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor: DEBUG
```

### View Full HTTP Trace

```powershell
curl -s http://localhost:8083/actuator/httptrace | jq .
```

### Monitor RabbitMQ in Real-Time

```powershell
# Watch queue statistics
while($true) {
    clear
    curl -s -u guest:guest http://localhost:15672/api/queues/%2F | jq '.[] | {name: .name, messages: .messages}'
    Start-Sleep -Seconds 2
}
```

---

**Last Updated**: May 16, 2026  
**Document Version**: 1.0  
**Status**: Ready for Testing


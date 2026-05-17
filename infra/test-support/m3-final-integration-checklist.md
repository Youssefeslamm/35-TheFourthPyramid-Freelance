# Milestone 3 Final Integration Checks

These checks are intended for a full integration environment after all service slices are merged and deployed. Run them only with disposable test records and a valid JWT for the role required by each endpoint.

## Prerequisites

- `mvn clean compile` passes from the repo root.
- `kubectl kustomize infra/k8s` renders successfully.
- `api-gateway`, all five services, RabbitMQ, PostgreSQL instances, Redis, MongoDB, Neo4j, Cassandra, and Elasticsearch are running.
- `GATEWAY_URL` points at the gateway, for example `http://localhost:8080`.
- `JWT` contains a valid bearer token.
- Test IDs exist for a client, freelancer, job, proposal, contract, and payout as needed.

## Smoke Checks

```bash
curl -fsS "$GATEWAY_URL/api/users/health"
curl -fsS "$GATEWAY_URL/api/jobs/health"
curl -fsS "$GATEWAY_URL/api/proposals/health"
curl -fsS "$GATEWAY_URL/api/contracts/health"
curl -fsS "$GATEWAY_URL/api/payouts/health"
curl -fsS "http://wallet-service:8080/actuator/prometheus"
curl -fsS "http://elasticsearch:9200/_cluster/health"
curl -fsS "http://rabbitmq:15692/metrics"
```

## Flow 1: Proposal Accepted -> Contract Created

1. Accept a submitted proposal through the gateway:

   ```bash
   curl -fsS -X PUT "$GATEWAY_URL/api/proposals/$PROPOSAL_ID/accept" \
     -H "Authorization: Bearer $JWT"
   ```

2. Confirm the proposal is `ACCEPTED`.
3. Confirm RabbitMQ shows `proposal.accepted` traffic on `proposal.events`.
4. Confirm contract-service consumes from `proposal.accepted.contract.queue`.
5. Confirm a contract exists for the accepted proposal and `contract.created` is emitted on `contract.events`.
6. Confirm proposal-service consumes `contract.created` through `proposal.saga-feedback` and stores the `contractId` on the proposal.

## Flow 2: Proposal Completed -> Payment Initiated

1. Complete an accepted proposal:

   ```bash
   curl -fsS -X PUT "$GATEWAY_URL/api/proposals/$PROPOSAL_ID/complete" \
     -H "Authorization: Bearer $JWT" \
     -H "X-User-Id: $FREELANCER_ID" \
     -H "X-User-Role: FREELANCER"
   ```

2. Confirm RabbitMQ shows `proposal.completed` traffic on `proposal.events`.
3. Confirm contract-service and wallet-service consume their `proposal.completed.*.queue` bindings.
4. Confirm wallet-service creates or updates the payout for the contract.
5. Confirm `payment.initiated` is emitted on `payment.events`.
6. Confirm proposal-service consumes `payment.initiated` through `proposal.saga-feedback` and moves the proposal to `PAYMENT_PENDING`.

## Flow 3: Payment Completed

1. Process the payout for the completed contract:

   ```bash
   curl -fsS -X POST "$GATEWAY_URL/api/payouts/contract/$CONTRACT_ID" \
     -H "Authorization: Bearer $JWT" \
     -H "Content-Type: application/json" \
     -d '{"method":"BANK_TRANSFER","accountLastFour":"4242"}'
   ```

2. Confirm the payout status becomes `COMPLETED`.
3. Confirm wallet-service emits `payment.completed` on `payment.events`.
4. Confirm proposal-service consumes `payment.completed` through `proposal.saga-feedback` and moves the proposal to `PAID`.

## Flow 4: Payment Failed Compensation

1. Trigger a failed payment using the merged wallet/payment failure path or a controlled test fixture.
2. Confirm wallet-service emits `payment.failed` on `payment.events`.
3. Confirm proposal-service consumes `payment.failed`, moves the proposal to `PAYMENT_FAILED`, and emits `proposal.cancelled` on `proposal.events`.
4. Confirm contract-service consumes `proposal.cancelled.contract.queue` and emits `contract.cancelled` if compensation is implemented in the merged contract slice.
5. Confirm proposal-service consumes `contract.cancelled` through `proposal.saga-feedback` without DLQ growth.

## Flow 5: Deactivate User Flow

1. Deactivate a disposable user:

   ```bash
   curl -fsS -X PUT "$GATEWAY_URL/api/users/$USER_ID/deactivate" \
     -H "Authorization: Bearer $JWT"
   ```

2. Confirm the user status becomes `DEACTIVATED`.
3. Confirm user-service emits `user.deactivated` on `user.events`.
4. Confirm contract-service consumes `user.deactivated.contract.queue`.
5. Confirm affected contracts follow the merged contract-service compensation/status behavior.

## Flow 6: Close Job Flow

1. Close a disposable job:

   ```bash
   curl -fsS -X PUT "$GATEWAY_URL/api/jobs/$JOB_ID/close" \
     -H "Authorization: Bearer $JWT" \
     -H "Content-Type: application/json" \
     -d '{"status":"CLOSED"}'
   ```

2. Confirm the job status becomes `CLOSED`.
3. Confirm job-service emits `job.closed` on `job.events`.
4. Confirm downstream consumers from the merged proposal/contract slices handle the closed job without DLQ growth.

## RabbitMQ Failure Checks

- For every service queue created by the Spring topology, verify a matching `.dlq` exists.
- After each flow, verify the primary queues drain and DLQ depths remain zero unless the test intentionally injects a failing event.
- If a DLQ grows, record the routing key, queue name, payload, and consumer logs before retrying.

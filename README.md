# Banking Transfer API
![CI](https://github.com/jlmasa/bankingapi/actions/workflows/ci.yml/badge.svg)
REST API for accounts and money transfers. Retried requests never move money
twice, concurrent
transfers cannot deadlock, and every change is recorded in an append-only
ledger.
## Run it
docker compose up --build
Swagger UI: http://localhost:8080/swagger-ui.html (users: alice, bob, admin /
dev-only-password)
## What it demonstrates
- Idempotent POST with an Idempotency-Key header and a request fingerprint
- Pessimistic locking in a fixed order (no deadlocks), with a 200-transfer
concurrency test
- Exact money arithmetic (BigDecimal, NUMERIC(19,4)) and database CHECK
constraints
- Append-only ledger and audit log enforced by database triggers
- Ownership checks (404 for strangers, 403 for spending someone else's money)
- Flyway migrations, Testcontainers integration tests, GitHub Actions CI
## Architecture
(paste the flow diagram from the design section, or a screenshot)
## Design decisions
| Decision | Why |
| --- | --- |
| Pessimistic locks, lowest id first | Short transactions, real contention,
no deadlocks |
| Unique (user, key) constraint | Only the database can settle two
simultaneous requests |
| Basic auth with demo users | Keeps the focus on money logic; production
would use OIDC/JWT |
## Test results
Unit: 6, service: 7, API: 9. Line coverage: <your JaCoCo number>%.
## What I would do next
OIDC login, publish a TransferCompleted event with the outbox pattern,
reversal transfers.

# Services: What Each One Actually Does, and the Real Flow

`ARCHITECTURE.md` describes the original, aspirational design. This
document describes what's actually running today, based on the real code
- every endpoint, every hop, every "calls X" claim below was checked
against the source (and most of it against live logs) while writing this.

## Quick reference

| Service | Port | What it actually does | Calls | Called by |
|---|---|---|---|---|
| **service-registry** | 8761 | Eureka - every other service registers here and discovers each other's addresses through it | — | everyone |
| **config-server** | 8888 | Spring Cloud Config server, `native` profile (serves from `classpath:/config`) - present, but nothing in the live flow actually pulls config from it | — | (registers with Eureka only) |
| **api-gateway** | 8080 | Spring Cloud Gateway - has routes configured for every service *except* security-service (see "Known gaps" below). **Not part of the tested flow** - the dashboard, Postman collection, and `test-e2e.sh` all call each service directly on its own port instead | routes to every service below by name | nothing in this repo (no client uses it) |
| **security-service** | 8089 | Two unrelated jobs in one process: (1) issues demo JWTs (`AuthController`, `POST /api/auth/token`) that every other service now requires, (2) hosts the crypto classes (`PINBlockService`, `MACService`, `EMVCryptogramService`) that pos-terminal-service and issuer-service pull in as a **library dependency**, not over HTTP | — | pos-terminal-service, issuer-service (as a Maven dependency, not a network call); everyone fetches tokens from it |
| **merchant-service** | 8081 | Owns merchant accounts: registration, activation, daily/monthly volume tracking, limit checks | — | transaction-service (validate), settlement-service, reporting-service |
| **acquirer-service** | 8082 | The merchant's bank: fraud/velocity scoring on each transaction (stateless - no database) | — | transaction-service |
| **network-service** | 8084 | The card network (Visa/Mastercard-style): identifies the card's network from its BIN, picks the issuer, adds a network fee (stateless - no database) | — | transaction-service |
| **issuer-service** | 8083 | The cardholder's bank: card issuance, balance/limit checks, the actual approve/decline decision, block/unblock | — | transaction-service |
| **transaction-service** | 8085 | Orchestrates the whole authorization: calls merchant → acquirer → network → issuer in order, records a step-by-step trace, publishes a completion event, hosts the message broker | merchant-service, acquirer-service, network-service, issuer-service | pos-terminal-service |
| **pos-terminal-service** | 8091 | The simulated card terminal: card read, PIN block, EMV cryptogram, ISO 8583 message build/MAC (wire-format demo only), then a real call into transaction-service; also serves the dashboard | transaction-service | (nothing - it's the entry point) |
| **settlement-service** | 8086 | Nightly batch (`@Scheduled` cron, 2am): sums each merchant's approved transactions for the day, computes a 2% fee, creates a settlement record. Also reacts to each transaction asynchronously (see Flow, step 8) | merchant-service, transaction-service | (nothing calls it in the live flow - it's read/reporting only + its own cron) |
| **reporting-service** | 8087 | On-demand report generation (`GET /api/reports/...`) by querying transaction-service + merchant-service live; also keeps an in-memory `live-activity` tally fed by async events | transaction-service, merchant-service | (nothing - queried directly) |
| **notification-service** | 8088 | Simulated alerts (logs only - no real email/SMS/push provider wired up); `sendTransactionAlert()` is now actually called, by the async listener | — | (nothing - reacts to events) |
| **common** (library) | — | Shared enums/DTOs, `TransactionCompletedEvent`, and the JWT classes (`JwtUtil`, `JwtAuthenticationFilter`) every service component-scans in | — | every service depends on this |
| **iso8583-service** (library) | — | Builds/parses real ISO 8583 messages (MTI, bitmap, field encoding). Used by pos-terminal-service purely to demonstrate the wire format a real terminal would send - nothing in this project actually transmits or listens for raw ISO 8583; the real dispatch is a normal HTTP/JSON call | — | pos-terminal-service |

## The real end-to-end flow

This is what actually happens for one `POST /api/pos/transaction` call that
gets **approved** - the numbers match the dashboard's step trace and each
service's log output.

```
1. POS Terminal (8091)
   ├─ Reads the card (chip or magnetic stripe)                    CardReaderService
   ├─ Formats + encrypts the PIN block                            PINBlockService
   ├─ Generates an EMV cryptogram (chip only)                     EMVCryptogramService
   ├─ Builds an ISO 8583 authorization message                    ISO8583MessageBuilder
   ├─ Signs it with a MAC                                         MACService
   │  (steps above are a wire-format demonstration only - nothing
   │   downstream speaks ISO 8583; everything from here on is REST/JSON)
   └─ Calls transaction-service, through a circuit breaker + one retry,
      carrying: its own Authorization: Bearer token (relayed, not
      generated here) and an idempotency key derived from STAN+RRN

2. Transaction Service (8085) - JwtAuthenticationFilter checks the token first
   ├─ Idempotency check: has this exact key been seen before? If so,
   │  return the original result immediately - nothing below runs again.
   ├─ Saves a PENDING transaction row
   ├─ Merchant Validation  → merchant-service   (not retried on failure - mutates daily volume)
   │     "is this merchant ACTIVE and within its daily/monthly limit?"
   ├─ Acquirer Processing  → acquirer-service   (retried once - stateless)
   │     "fraud/velocity score for this card+terminal+amount"
   ├─ Network Routing      → network-service    (retried once - stateless)
   │     "which network is this BIN, which issuer, what's the network fee"
   ├─ Issuer Authorization → issuer-service     (not retried on failure - mutates balance)
   │     "does this card have the funds, is it active, does the CVV match"
   │  (each hop above: wrapped in its own circuit breaker; the caller's
   │   Authorization header and trace ID are relayed onto every one of
   │   these Feign calls automatically)
   ├─ Records a TransactionStep for every hop (SUCCESS/DECLINED/FAILED,
   │  target service, detail, duration) - this is the dashboard's trace
   ├─ Saves the final AUTHORIZED/DECLINED/FAILED transaction
   └─ Publishes a TransactionCompletedEvent (async, off this response path)
      to three JMS queues - see step 8

3. Response flows back up: issuer-service's decision → transaction-service's
   TransactionResponse (with the full step trace) → pos-terminal-service's
   receipt (preferring transaction-service's specific decline reason over
   the generic ISO 8583 code description) → the caller.
```

And separately, **asynchronously**, off that request/response path entirely:

```
transaction-service's embedded broker (Artemis, port 61616)
    │
    ├──▶ settlement-service    logs "queued for the next settlement run" if AUTHORIZED
    │      (the actual settlement batch is a separate nightly @Scheduled job,
    │       not triggered per-transaction - see the table above)
    │
    ├──▶ reporting-service     updates an in-memory per-merchant tally
    │      (GET /api/reports/live-activity), independent of its on-demand
    │      report endpoints, which query transaction-service/merchant-service live instead
    │
    └──▶ notification-service  calls NotificationService.sendTransactionAlert() -
           logs a simulated "your transaction was AUTHORIZED/DECLINED" alert
```

None of these three need to be running for step 1-3 to work; if the broker
itself is unreachable, `transaction-service` logs a warning and moves on -
the synchronous response the customer/terminal actually waits on never
depends on any of this.

## Known gaps (worth knowing, not (yet) fixed)

- **api-gateway has no route for security-service.** Every other service
  is routed (`/api/merchants/**` → merchant-service, etc.), but there's no
  `/api/auth/**` → security-service entry, so getting a token through the
  gateway wouldn't currently work even if you started using it.
- **api-gateway itself isn't part of the tested flow.** It's built,
  configured, and would probably work, but nothing in this repo (dashboard,
  Postman collection, `test-e2e.sh`) has ever actually driven traffic
  through it - only direct-to-service calls have been verified live.
- **config-server doesn't centralize anything yet.** It runs and registers
  with Eureka, but every service still reads its own local
  `application.yml` rather than pulling config from it.
- **No role/permission model.** Every valid JWT can call every endpoint on
  every service - see `common`'s `JwtAuthenticationFilter` for why that's
  a deliberate scope decision, not an oversight.

For how to actually run and explore any of this (dashboard, Postman,
`test-e2e.sh`, manual curl), see `GETTING-STARTED.md` and `TESTING.md`.

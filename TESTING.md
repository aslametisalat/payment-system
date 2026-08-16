# Testing Guide: Learning the Payment Flow Through Tests

This project simulates a real card-payment authorization flow across several
Spring Boot services. The tests added here are meant to be read, not just
run — each one isolates one hop of the flow described in `ARCHITECTURE.md`
so you can see exactly what happens to a transaction at each stage without
needing to run all twelve services, Eureka, and a config server at once.

## How to run them

Run everything from the repo root:

```bash
mvn test
```

Run a single module (fast, useful while reading one stage of the flow):

```bash
mvn -pl pos-terminal-service -am test
```

`-am` also builds the modules it depends on (`common`, `security-service`,
`iso8583-service`) so the module under test always has fresh classes to link
against.

## The flow, and where its tests live

Match this against the diagram in `ARCHITECTURE.md` ("Payment Transaction
Flow"). Each stage below is a real class, with real unit tests, that you can
step through in a debugger.

| Stage | Class | Tests |
|---|---|---|
| 1. Card is read at the terminal | `pos-terminal-service` `CardReaderService` | exercised inside `POSTransactionServiceTest` |
| 2. PIN is formatted, XOR'd with the PAN, and encrypted | `security-service` `PINBlockService` | `PINBlockServiceTest` |
| 3. Chip cards generate an EMV cryptogram (ARQC) | `security-service` `EMVCryptogramService` | `EMVCryptogramServiceTest` |
| 4. Request is packed into an ISO 8583 message | `iso8583-service` `ISO8583MessageBuilder` | `ISO8583MessageBuilderTest` |
| 5. Message is signed with a MAC | `security-service` `MACService` | `MACServiceTest` |
| 6. Merchant is checked against daily/monthly limits | `merchant-service` `MerchantService` / `MerchantValidationService` | `MerchantServiceTest`, `MerchantValidationServiceTest` |
| 7. Acquirer runs fraud/velocity screening | `acquirer-service` `AcquirerService` | `AcquirerServiceTest` |
| 8. Network resolves the card's BIN and adds its fee | `network-service` `NetworkRoutingService` | `NetworkRoutingServiceTest` |
| 9. Issuer makes the approve/decline decision | `issuer-service` `CardService` | `CardServiceTest` |
| 10. Transaction service orchestrates all of the above over real HTTP/Feign calls | `transaction-service` `TransactionProcessingService` | `TransactionProcessingServiceTest`, plus the integration test below |
| 11. Merchant is paid out, minus fees | `settlement-service` `SettlementService` | `SettlementServiceTest` |

Two tests are worth reading first if you want the "whole flow in one place"
view:

- **`pos-terminal-service`'s `POSTransactionServiceTest`** wires together the
  *real* card reader, PIN, EMV, ISO 8583, and MAC classes (no mocks) and
  drives `POSTransactionService.processTransaction()` end to end — card read
  through receipt printing — entirely inside the JVM, in milliseconds.
- **`transaction-service`'s `TransactionAuthorizationIntegrationTest`** sends
  a real HTTP request through the real `TransactionController` into a real
  H2 database, with only the network call to `issuer-service` mocked (since
  that service isn't running during the test). This is the closest thing to
  watching a transaction move through the system without starting the full
  stack.

## Why this needed a (small) refactor first

Before these tests could exist, a few things stood in the way:

- **No test dependency anywhere.** None of the 15 modules declared
  `spring-boot-starter-test`. Added (test scope) to every module in the core
  flow.
- **`Math.random()` inline in business logic.** `AcquirerService`'s fraud
  score, `CardService`'s authorization code, and `POSTransactionService`'s
  STAN/RRN/ATC/unpredictable-number generation all called `Math.random()`
  directly, so nothing about them could be pinned down in a test or
  reproduced from a bug report. Each now takes a `java.util.Random` as a
  constructor dependency (a real `@Bean` in production, a seeded or mocked
  instance in tests) — same output format, but now controllable.
- **`AcquirerService`'s fraud/velocity checks were `private`.** Loosened to
  package-private so a test in the same package can exercise the scoring
  formula directly. (Worth knowing: with today's thresholds, the maximum
  reachable fraud score is 49, and the decline branch at `score > 80` is
  currently unreachable through the public API — a good thing to notice
  while learning this code, not something this refactor changed.)

## Two real bugs the tests surfaced

Writing these tests wasn't just plumbing — two bugs in the actual flow logic
turned up immediately once it became possible to run the code in isolation:

1. **Bean validation was wired up but never active.** Every request DTO
   (`TransactionRequest`, `MerchantRequest`, `POSTransactionRequest`, etc.)
   was annotated with `@NotBlank` / `@NotNull` / `@DecimalMin`, and every
   controller used `@Valid` — but no module declared
   `spring-boot-starter-validation`, so none of it ever ran. A request
   missing required fields didn't get a clean `400`; it reached the
   controller with null fields and threw a `NullPointerException` instead
   (see `TransactionController.authorize()`, which logs
   `cardNumber.substring(...)` before anything is checked). Added the
   validation starter to every core-flow service's `pom.xml` — the
   annotations already there now actually take effect.
2. **The simulated authorization response's MAC never verified.**
   `POSTransactionService.sendToAuthorization()` built a simulated response
   and set field 64 (the MAC) *after* computing the hash over the message.
   But `ISO8583Message.toBitmapHex()` is recomputed live from the message's
   mutable bitmap, so setting field 64 afterward changed the bitmap that
   `verifyResponseMAC()` would later hash — the recomputed MAC could never
   match. Every POS transaction was silently declined with "Security
   violation - Invalid MAC", regardless of the card or amount. Fixed by
   reserving field 64's bit in the bitmap *before* hashing (with an empty
   placeholder value, so the MAC's own bytes stay excluded from what gets
   hashed), then filling in the real MAC afterward — the same fix was
   applied to the equivalent step on the request side for consistency, even
   though nothing in this codebase currently verifies that MAC.

Both are visible as comments at the fix sites in
`pos-terminal-service`'s `POSTransactionService.java`.

## Running the real end-to-end flow

Everything above tests one hop at a time with mocks standing in for the
services on either side. You can also run the real chain — seven separate
JVMs, no mocks — and watch a transaction actually travel:

```
POS Terminal → Transaction Service → Merchant Service (limit check)
                                    → Acquirer Service (fraud/velocity)
                                    → Network Service (BIN routing/fee)
                                    → Issuer Service (approve/decline)
```

This used to not be true: `pos-terminal-service` faked its own
always-approved response locally, and `transaction-service` called
`issuer-service` directly, skipping merchant/acquirer/network entirely. Both
are now real `@FeignClient` calls (see `MerchantClient`/`AcquirerClient`/
`NetworkClient` in `transaction-service`, and `TransactionClient` in
`pos-terminal-service`), resolved through Eureka at runtime just like
`issuer-service`'s client always was.

### Starting it

```bash
mvn clean install                     # build everything once

cd service-registry && mvn spring-boot:run &   # wait for :8761/actuator/health
cd merchant-service && mvn spring-boot:run &
cd acquirer-service && mvn spring-boot:run &
cd network-service && mvn spring-boot:run &
cd issuer-service && mvn spring-boot:run &
cd transaction-service && mvn spring-boot:run &
cd pos-terminal-service && mvn spring-boot:run &
```

Give it ~30-40s after the last service starts for Eureka's registry to
propagate to every client before sending traffic — a service can be "up"
(its own `/health` returns 200) well before other services' local Eureka
caches know it exists, which shows up as `Load balancer does not contain an
instance for the service ...`.

### Driving a transaction through it

**To explore by hand, one controller at a time:** import
`postman-collection.json` into Postman. Unlike the automated script below,
it doesn't just call the orchestrated entry point - Folder 2 calls
Merchant's limit check, Acquirer's fraud screening, Network's BIN routing,
and Issuer's authorize/block/unblock directly, so you can see what each hop
does on its own before Folder 3 shows the same four things happening
automatically through `/api/transactions/authorize`. Every request has a
description explaining what it's for. See GETTING-STARTED.md's "Using
Postman" section for the folder-by-folder breakdown.

**To just confirm it all works, non-interactively:** once the six services
above are up (and Eureka has had ~30-40s to propagate them to each other),
run:

```bash
./test-e2e.sh
```

It creates a merchant, activates it, issues a card, then fires two POS
transactions through the whole chain — one that should be `APPROVED`
($25.99) and one that should be `DECLINED` for exceeding the merchant's
daily limit ($9,999) — and reports pass/fail for each step. It's safe to
run repeatedly (each run uses a fresh merchant). It doesn't start any
services itself; if something's down it tells you which one.

What it's actually doing, if you want to run it by hand or adapt it:

```bash
# 1. Create + activate a merchant
MERCHANT_ID=$(curl -s -X POST http://localhost:8081/api/merchants \
  -H "Content-Type: application/json" \
  -d '{"businessName":"Coffee Shop","email":"shop@coffee.com","phone":"+12025550123",
       "address":"123 Main St","taxId":"12-3456789","merchantCategoryCode":"5814",
       "dailyLimit":10000,"monthlyLimit":300000}' | python3 -c "import json,sys;print(json.load(sys.stdin)['id'])")
curl -X PUT "http://localhost:8081/api/merchants/${MERCHANT_ID}/status?status=ACTIVE"

# 2. Issue a card (full PAN/CVV only ever come back here, at issuance)
curl -X POST http://localhost:8083/api/cards \
  -H "Content-Type: application/json" \
  -d '{"cardholderName":"John Doe","cardType":"CREDIT","network":"VISA","creditLimit":5000}'

# 3. Run a transaction through the whole chain
curl -X POST http://localhost:8091/api/pos/transaction \
  -H "Content-Type: application/json" \
  -d "{\"terminalId\":\"TERM0001\",\"merchantId\":\"${MERCHANT_ID}\",\"merchantName\":\"Coffee Shop\",
       \"amount\":2599,\"cardReadMethod\":\"CHIP\",\"requirePIN\":true,\"pin\":\"1234\",
       \"cardNumber\":\"<from step 2>\",\"expiryDate\":\"2812\",\"cvv\":\"<from step 2>\"}"
```

Then `tail -f logs/*.log` and watch the same request ID's amount and
merchant show up in `merchant-service`, `acquirer-service`,
`network-service`, and `issuer-service`'s logs within milliseconds of each
other, in that order.

## Three more bugs that only showed up running it live

Unit tests catch logic bugs; they don't catch what happens when independent
JVMs actually talk to each other over the network. Wiring this up for real
and driving a transaction through it surfaced three more:

1. **A legitimate decline crashed the calling service.**
   `TransactionController.authorize()` returned HTTP 400 for any declined
   transaction (fraud, insufficient funds, limit exceeded — a normal
   business outcome for a well-formed request, not a client error). Feign
   treats any non-2xx response as an exception by default, so the moment
   `pos-terminal-service` became a real Feign caller, every decline anywhere
   in the chain surfaced to the terminal as "System malfunction" instead of
   the real reason. Fixed by always returning `200 OK` with the decline
   reason in the body — the same way real payment APIs like Stripe model
   it — and updated the tests that had locked in the old `400` behavior.
2. **An oversized error message crashed the save that was supposed to
   record the failure.** Feign exceptions embed the failing URL and
   response body in `getMessage()`, which routinely exceeds 255 characters.
   `Transaction.responseMessage` is a plain `VARCHAR(255)` column, so
   storing `"System error: " + e.getMessage()` unmodified could throw a
   *second*, uncaught `DataIntegrityViolationException` on top of the first
   failure, turning what should have been a graceful `FAILED` transaction
   record into a raw `500`. Fixed by truncating the message before storing
   it.
3. **The terminal discarded the real decline reason.** `POSTransactionService`
   mapped the response code back to a generic ISO 8583 description
   (`ResponseCode.fromCode("03").getMessage()` → `"Invalid merchant"`)
   instead of using the specific reason `transaction-service` actually
   returned (`"Daily limit exceeded"`). The receipt and API response now
   prefer the specific message, falling back to the generic code
   description only when no specific message comes back.

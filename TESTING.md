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
| 10. Transaction record is created and updated | `transaction-service` `TransactionProcessingService` | `TransactionProcessingServiceTest`, plus the integration test below |
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

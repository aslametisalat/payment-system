# Getting Started with the Payment System

## 📥 Installation & Setup

### Prerequisites
- Java 17 or higher
- Maven 3.8 or higher
- Git (optional)
- 8GB RAM recommended
- Ports 8080-8088, 8761, 8888 available

### Step 1: Extract the Project
```bash
# Download and extract payment-system.tar.gz
tar -xzf payment-system.tar.gz
cd payment-system
```

### Step 2: Build the Project
```bash
# Build all services (this may take 5-10 minutes first time)
mvn clean install

# Expected output:
# [INFO] BUILD SUCCESS
# [INFO] Total time: XX:XX min
```

### Step 3: Start the System
```bash
# Make startup script executable
chmod +x start-all.sh

# Start all services
./start-all.sh

# This will start all 11 services in sequence
# Wait for the message: "System is ready for use!"
```

### Step 4: Verify Services
```bash
# Check Eureka Dashboard
open http://localhost:8761

# Check API Gateway health
curl http://localhost:8080/actuator/health

# Expected response: {"status":"UP"}
```

## 🎯 First Transaction - Step by Step

### Step 0: Get an auth token
Every service now rejects `/api/**` calls without a valid token (see
"🔒 Production-Hardening Features" below) - get one first and reuse it for
every curl command in this guide:
```bash
TOKEN=$(curl -s -X POST http://localhost:8089/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}' | python3 -c "import json,sys;print(json.load(sys.stdin)['accessToken'])")
```

### Step 1: Create a Merchant
```bash
curl -X POST http://localhost:8080/api/merchants \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{
    "businessName": "My Coffee Shop",
    "email": "shop@example.com",
    "phone": "+1234567890",
    "address": "123 Main Street",
    "taxId": "12-3456789",
    "merchantCategoryCode": "5814",
    "dailyLimit": 10000,
    "monthlyLimit": 300000
  }'
```

**Copy the `id` from the response!**

### Step 2: Activate the Merchant
```bash
# Replace {MERCHANT_ID} with the id from Step 1
curl -X PUT "http://localhost:8080/api/merchants/{MERCHANT_ID}/status?status=ACTIVE" \
  -H "Authorization: Bearer $TOKEN"
```

### Step 3: Issue a Test Card
```bash
curl -X POST http://localhost:8080/api/cards \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{
    "cardholderName": "John Doe",
    "cardType": "CREDIT",
    "network": "VISA",
    "creditLimit": 5000
  }'
```

**Copy the `id` from the response!**

### Step 4: Get Card Details
To get the actual card number and CVV (needed for testing):

1. Open H2 Console: http://localhost:8083/h2-console
2. JDBC URL: `jdbc:h2:mem:issuerdb`
3. Username: `sa`
4. Password: (leave blank)
5. Click "Connect"
6. Run query: `SELECT * FROM CARDS;`
7. Note the `CARD_NUMBER` and `CVV`

### Step 5: Process Your First Transaction
```bash
# Replace values with your actual data
curl -X POST http://localhost:8080/api/transactions/authorize \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{
    "merchantId": "your-merchant-id",
    "cardNumber": "card-number-from-database",
    "cvv": "cvv-from-database",
    "type": "PURCHASE",
    "amount": 99.99,
    "currency": "USD"
  }'
```

**Success!** You should see:
```json
{
  "id": "transaction-id",
  "status": "AUTHORIZED",
  "amount": 99.99,
  "authorizationCode": "123456",
  "responseMessage": "Approved"
}
```

## 📊 Explore the System

All of these need the same `-H "Authorization: Bearer $TOKEN"` from Step 0 above.

### View All Merchants
```bash
curl http://localhost:8080/api/merchants -H "Authorization: Bearer $TOKEN"
```

### View All Cards
```bash
curl http://localhost:8080/api/cards -H "Authorization: Bearer $TOKEN"
```

### View All Transactions
```bash
curl http://localhost:8080/api/transactions -H "Authorization: Bearer $TOKEN"
```

### View Merchant's Transactions
```bash
curl http://localhost:8080/api/transactions/merchant/{MERCHANT_ID} -H "Authorization: Bearer $TOKEN"
```

### Generate Reports
```bash
curl http://localhost:8080/api/reports/transactions/{MERCHANT_ID} -H "Authorization: Bearer $TOKEN"
```

## 🔍 Monitoring & Debugging

### Service Registry (Eureka)
- URL: http://localhost:8761
- Shows all registered services
- Check service status

### H2 Database Consoles
Each service has its own database:

| Service | URL | JDBC URL |
|---------|-----|----------|
| Merchant | http://localhost:8081/h2-console | jdbc:h2:mem:merchantdb |
| Issuer | http://localhost:8083/h2-console | jdbc:h2:mem:issuerdb |
| Transaction | http://localhost:8085/h2-console | jdbc:h2:mem:transactiondb |
| Settlement | http://localhost:8086/h2-console | jdbc:h2:mem:settlementdb |

All use: Username: `sa`, Password: (blank)

### Swagger API Documentation
- Merchant Service: http://localhost:8081/swagger-ui.html
- Issuer Service: http://localhost:8083/swagger-ui.html
- Transaction Service: http://localhost:8085/swagger-ui.html

### View Logs
```bash
# All logs are in the logs/ directory
tail -f logs/transaction-service.log
tail -f logs/issuer-service.log
tail -f logs/merchant-service.log
```

## 🧪 Test Scenarios

### Scenario 1: Successful Purchase
```bash
# Use a card with sufficient balance
# Expected: Status = AUTHORIZED
```

### Scenario 2: Insufficient Funds
```bash
# Try to charge more than available balance
curl -X POST http://localhost:8080/api/transactions/authorize \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{
    "merchantId": "{MERCHANT_ID}",
    "cardNumber": "{CARD_NUMBER}",
    "cvv": "{CVV}",
    "type": "PURCHASE",
    "amount": 99999.99,
    "currency": "USD"
  }'
# Expected: Status = DECLINED, Message = "Insufficient funds"
```

### Scenario 3: Invalid CVV
```bash
# Use wrong CVV
curl -X POST http://localhost:8080/api/transactions/authorize \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{
    "merchantId": "{MERCHANT_ID}",
    "cardNumber": "{CARD_NUMBER}",
    "cvv": "999",
    "type": "PURCHASE",
    "amount": 50.00,
    "currency": "USD"
  }'
# Expected: Status = DECLINED, Message = "Invalid CVV"
```

### Scenario 4: Block and Try Card
```bash
# Block the card
curl -X PUT http://localhost:8080/api/cards/{CARD_ID}/block -H "Authorization: Bearer $TOKEN"

# Try to use it
curl -X POST http://localhost:8080/api/transactions/authorize \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{
    "merchantId": "{MERCHANT_ID}",
    "cardNumber": "{CARD_NUMBER}",
    "cvv": "{CVV}",
    "type": "PURCHASE",
    "amount": 50.00,
    "currency": "USD"
  }'
# Expected: Status = DECLINED, Message = "Card is blocked"
```

## 🔒 Production-Hardening Features

Six things a real payment platform needs that a "happy path" demo usually
skips - all actually wired up and running here, with a dummy/demo value
standing in wherever real infrastructure or credentials would normally go:

1. **Circuit breakers + retry (Resilience4j) + Feign timeouts.** Every
   Feign call to another service goes through a per-target circuit breaker
   (`TransactionProcessingService.callStep()`, `POSTransactionService
   .resilientAuthorize()`), so a dead dependency fails fast instead of
   hanging every request behind it. Retries are only enabled on hops that
   are actually safe to repeat (`network-service`, `acquirer-service` -
   both side-effect-free; see the comment above
   `RETRYABLE_TARGETS` in `TransactionProcessingService` for why
   `merchant-service`/`issuer-service` are deliberately excluded).
   **Try it:** kill `acquirer-service` mid-session and submit a few
   transactions - the dashboard's step trace will show `FAILED` at
   `acquirer-service` with a circuit-breaker message once it trips open,
   instead of every request hanging for the full Feign timeout.

2. **Idempotency keys.** `POSTransactionService` derives a key from
   STAN+RRN (the real ISO 8583 fields meant for exactly this) and sends it
   on every authorize call; `TransactionProcessingService` recognizes a
   repeated key and returns the original result instead of authorizing
   twice - what actually makes retrying step 1 safe.

3. **Externalized crypto keys.** `PIN_ENCRYPTION_KEY`/`MAC_KEY`/
   `CARD_MASTER_KEY` used to be `private static final String` literals in
   `POSTransactionService.java` and `CardService.java`. They're now
   `payment.security.*` values in each service's `application.yml` -
   still demo values (a real system pulls these from an HSM/vault at
   runtime, never a YAML file), but no longer hardcoded in source.

4. **Distributed tracing (Micrometer Tracing + Brave).** Every request gets
   a trace ID that's the same across every service it passes through - POS
   → transaction-service → merchant/acquirer/network/issuer-service all
   log the identical ID (see each service's console output:
   `[traceId,spanId]`). No Zipkin server runs anywhere in this project (the
   exporter fails to reach one silently), but the trace ID in the logs is
   the actual payoff - `grep <traceId> logs/*.log` shows you one
   transaction's whole path across every service, in order, instead of
   guessing from timestamps.

5. **Async messaging (embedded ActiveMQ Artemis).** `transaction-service`
   hosts a real broker (`EmbeddedBrokerConfig`) and publishes a
   `TransactionCompletedEvent` after every transaction reaches a terminal
   state, off the synchronous authorization path.
   `settlement-service`/`reporting-service`/`notification-service` each
   consume it independently (one queue per consumer, so all three get
   every event - see the comment on `TRANSACTION_COMPLETED_QUEUES`).
   `notification-service`'s listener calls a `sendTransactionAlert()`
   method that existed before this but was dead code, never called by
   anything. `reporting-service` exposes what it builds from these events
   at `GET /api/reports/live-activity`.

6. **JWT authentication.** Every `/api/**` endpoint on every service now
   rejects requests without a valid bearer token (`JwtAuthenticationFilter`,
   shared via the `common` module and component-scanned into each service).
   `security-service` issues demo tokens (`POST /api/auth/token`, username
   `demo` / password `demo123` - see `AuthController`'s comment for why
   that's on purpose, not an oversight). `transaction-service` and
   `pos-terminal-service` relay the caller's token onto their own outgoing
   Feign calls (`FeignAuthRelayConfig`), so one token authenticates a
   request across the entire chain - POS → transaction → merchant/
   acquirer/network/issuer - not just the first hop. There's no
   role/permission model (every valid token can call every endpoint) -
   this stops at "is there a validly-signed token," matching this
   project's single-tenant demo scope.

## 🚀 Advanced Usage

### Using the dashboard (easiest way to see it happen)

Once `service-registry`, `merchant-service`, `acquirer-service`,
`network-service`, `issuer-service`, `transaction-service` and
`pos-terminal-service` are all up, open:

```
http://localhost:8091/dashboard/index.html
```

It's a single static page (no separate install, no build step) that lets
you drive the whole flow with your own inputs and *see* it happen instead
of reading logs:

1. **Create + activate a merchant** and **issue a card** - two buttons, no
   curl needed.
2. **Submit a transaction** - amount, card read method, PIN, all editable -
   through the real POS entry point.
3. **See the result as a step-by-step trace**: a colored row per hop
   (Merchant → Acquirer → Network → Issuer), showing exactly which service
   approved, declined, or failed the transaction, its message, and how long
   it took.
4. **Watch a live table of every transaction** anyone sends through the
   system (from the dashboard, curl, Postman, or `test-e2e.sh`), color-coded
   green/red/gray by outcome, with the same trace available by clicking any
   row.

This is the fastest way to answer "why did my transaction fail" - the
trace tells you which of the four services said no, and why, without
tailing log files.

### Using Postman

Import `postman-collection.json` (repo root) into Postman. It's organized
so you can call each controller directly, not just through the full flow:

1. **1. Setup** - create + activate a merchant, issue a card. Run these
   three first; their test scripts capture `merchantId`/`cardNumber`/`cvv`
   into collection variables automatically, for every other request to use.
2. **2. Call Each Controller Directly** - Merchant's limit check, Acquirer's
   fraud screening, Network's BIN routing, Issuer's authorize/block/unblock
   - the same four hops `transaction-service` calls internally, but one at a
   time so you can see exactly what each one does on its own.
3. **3. Full Orchestrated Flow** - the real `/api/transactions/authorize`
   entry point (approved and declined examples), plus get/refund.
4. **4. POS Terminal** - the simulated card terminal, chip+PIN and
   magnetic-stripe, feeding into the same orchestrated flow over real HTTP.

Every request has a description explaining what it's testing and what to
try changing. Swagger UI is also available per service once it's running,
at `http://localhost:{port}/swagger-ui.html` (e.g. `:8082` for
acquirer-service) - useful for browsing a single service's schema without
the full collection.

### Load Testing

```bash
# Install Apache Bench
sudo apt-get install apache2-utils

# Create test data file (transaction.json)
cat > transaction.json << 'EOF'
{
  "merchantId": "your-merchant-id",
  "cardNumber": "4123456789012345",
  "cvv": "123",
  "type": "PURCHASE",
  "amount": 10.00,
  "currency": "USD"
}
EOF

# Run load test (100 requests, 10 concurrent)
ab -n 100 -c 10 -p transaction.json -T application/json \
  http://localhost:8080/api/transactions/authorize
```

## 🛠️ Troubleshooting

### Services Won't Start
```bash
# Check if ports are in use
netstat -tlnp | grep -E ':(8080|8081|8082|8083|8084|8085|8086|8087|8088|8089|8091|8761|8888)'

# Kill processes on specific port if needed
kill -9 $(lsof -ti:8080)
```

**On Windows, `./start-all.sh` stops partway with `cygheap read copy
failed` or `fork: retry: Resource temporarily unavailable`:** this is Git
Bash's fork emulation running out of headroom after backgrounding many
`mvn` processes - not a code issue. Either run `.\start-all.ps1` from
PowerShell instead (see the Windows FAQ above), or start just the missing
service directly in `cmd.exe`/PowerShell (not another Git Bash background
job): `cd <service> && mvn spring-boot:run`.

### Service Not Registered with Eureka
```bash
# Wait 30-60 seconds after service starts
# Check Eureka dashboard: http://localhost:8761
# Restart the specific service if needed
```

### Transaction Declined
```bash
# Check card balance in H2 console:
SELECT * FROM CARDS WHERE ID = 'your-card-id';

# Check if card is active and not blocked
# Verify CVV matches
# Ensure sufficient available_balance
```

### Build Failures
```bash
# Clean and rebuild
mvn clean install -DskipTests

# If still failing, try building services individually
cd merchant-service && mvn clean install
cd ../issuer-service && mvn clean install
# etc...
```

## 📚 Learning Resources

### Recommended Reading Order
1. **README.md** - Project overview
2. **ARCHITECTURE.md** - System design
3. **API-TESTING-GUIDE.md** - Detailed API testing
4. **This file** - Getting started

### Key Concepts to Understand
- **Microservices**: Independent, deployable services
- **Service Discovery**: How services find each other (Eureka)
- **API Gateway**: Single entry point for all requests
- **RESTful APIs**: HTTP-based service communication
- **JPA/Hibernate**: Database interaction
- **DTO Pattern**: Data transfer between layers
- **Spring Cloud**: Microservices infrastructure

### Hands-On Exercises
1. Create 5 merchants and 10 cards
2. Process 20 successful transactions
3. Try 5 declined transactions (different reasons)
4. Check settlement data
5. Generate reports
6. Modify code to add new features

## 🎓 Next Steps

### Beginner Level
- ✅ Complete first transaction flow
- ✅ Explore all CRUD operations
- ✅ Understand database schema
- ✅ Read service logs

### Intermediate Level
- Add new fields to entities
- Implement new endpoints
- Add validation rules
- Customize authorization logic
- Add new card networks

### Advanced Level
- ✅ JWT authentication (see "🔒 Production-Hardening Features" above)
- ✅ Circuit breakers (Resilience4j) (see "🔒 Production-Hardening Features" above)
- ✅ Async messaging (embedded ActiveMQ Artemis) (see "🔒 Production-Hardening Features" above)
- Add Redis caching
- Swap the embedded broker for a real Kafka/RabbitMQ cluster
- Add a real identity provider (Okta/Auth0/Keycloak) behind `AuthController`
- Deploy to Kubernetes
- Point `management.zipkin.tracing.endpoint` at a real Zipkin/Tempo/Jaeger collector
- Implement fraud detection
- Add integration tests

## 💡 Common Questions

**Q: Can I use this in production?**  
A: This is a learning project. It now has *demo-grade* versions of several
things a real deployment needs (see "🔒 Production-Hardening Features"
above: JWT auth, circuit breakers/retries, an embedded message broker,
distributed tracing, idempotency keys, externalized keys) - the mechanics
are real, but every credential/secret/broker is a local stand-in. Before
production you'd still need:
- PostgreSQL/MySQL instead of H2
- A real identity provider behind `security-service`'s `AuthController`
  (Okta/Auth0/Keycloak), not one hardcoded demo login
- Real secrets/key management (HSM or vault) instead of `application.yml`
  values
- A real Kafka/RabbitMQ cluster instead of the embedded Artemis broker
- A real Zipkin/Tempo/Jaeger collector instead of `management.zipkin
  .tracing.endpoint` pointing at nothing
- Redis caching
- Proper logging (ELK stack)
- Monitoring (Prometheus, Grafana)
- Load balancers
- CI/CD pipelines

**Q: How do I stop all services?**  
A: Press `Ctrl+C` in the terminal where you ran `start-all.sh`, or:
```bash
# Kill all Java processes
pkill -f spring-boot:run
```

**Q: Can I run this on Windows?**  
A: Yes! You'll need:
- Git Bash or WSL2
- Java 17+
- Maven 3.8+

If `./start-all.sh` in Git Bash fails partway through with `cygheap read
copy failed` / `fork: retry: Resource temporarily unavailable`, that's Git
Bash's fork emulation running out of headroom after backgrounding 12+ `mvn`
processes in one shell - not a problem with the services. Use
`start-all.ps1` instead (from a PowerShell prompt, not Git Bash):

```powershell
.\start-all.ps1
```

It starts each service as its own native Windows process instead of a
forked bash job, which avoids the issue entirely. Each service opens in
its own minimized console window (close a window to stop that service).

**Q: How much does it cost to run?**  
A: Free! Everything runs locally. No cloud costs.

**Q: Where can I get help?**  
A: Check the documentation files, examine the code, use H2 consoles to inspect data, and review the logs.

## 🎉 Success Checklist

- [ ] All services started successfully
- [ ] Eureka shows all 11 services registered
- [ ] Created at least one merchant
- [ ] Issued at least one card
- [ ] Processed a successful transaction
- [ ] Processed a declined transaction
- [ ] Viewed data in H2 consoles
- [ ] Accessed Swagger documentation
- [ ] Generated a report
- [ ] Understood the payment flow

---

**Congratulations!** You now have a complete POS payment system running. 

Start experimenting, break things, fix them, and learn! 🚀

For detailed API documentation, see **API-TESTING-GUIDE.md**  
For architecture details, see **ARCHITECTURE.md**

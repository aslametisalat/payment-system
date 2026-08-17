# Payment System API Testing Guide

## Quick Start Guide

### 1. Start All Services
```bash
cd /home/claude/payment-system
chmod +x start-all.sh
./start-all.sh
```

Wait for all services to start (about 2-3 minutes).

### 2. Verify Services are Running
```bash
# Check Eureka Dashboard
curl http://localhost:8761

# Check API Gateway
curl http://localhost:8080/actuator/health
```

## Complete Payment Flow Testing

### Step 1: Create a Merchant

```bash
curl -X POST http://localhost:8080/api/merchants \
  -H "Content-Type: application/json" \
  -d '{
    "businessName": "Tech Store",
    "email": "techstore@example.com",
    "phone": "+1234567890",
    "address": "123 Tech Street, San Francisco, CA",
    "taxId": "12-3456789",
    "merchantCategoryCode": "5732",
    "dailyLimit": 50000,
    "monthlyLimit": 1000000,
    "accountNumber": "1234567890",
    "routingNumber": "021000021",
    "bankName": "Chase Bank"
  }'
```

**Expected Response:**
```json
{
  "id": "generated-merchant-id",
  "businessName": "Tech Store",
  "email": "techstore@example.com",
  "status": "PENDING_VERIFICATION",
  "dailyLimit": 50000,
  "monthlyLimit": 1000000,
  "currentDailyVolume": 0,
  "currentMonthlyVolume": 0
}
```

Save the `id` from the response - you'll need it!

### Step 2: Activate the Merchant

```bash
curl -X PUT "http://localhost:8080/api/merchants/{MERCHANT_ID}/status?status=ACTIVE" \
  -H "Content-Type: application/json"
```

### Step 3: Issue a Card

```bash
curl -X POST http://localhost:8080/api/cards \
  -H "Content-Type: application/json" \
  -d '{
    "cardholderName": "John Doe",
    "cardType": "CREDIT",
    "network": "VISA",
    "creditLimit": 10000
  }'
```

**Expected Response:**
```json
{
  "id": "generated-card-id",
  "cardNumber": "4***********1234",
  "cardholderName": "John Doe",
  "expiryDate": "2027-02-05",
  "cardType": "CREDIT",
  "network": "VISA",
  "creditLimit": 10000,
  "availableBalance": 10000,
  "active": true
}
```

### Step 4: Get Full Card Details (for testing)

```bash
curl http://localhost:8080/api/cards/{CARD_ID}
```

**Note:** In the H2 console, you can see the actual card number and CVV:
- Go to: http://localhost:8083/h2-console
- JDBC URL: `jdbc:h2:mem:issuerdb`
- Username: `sa`
- Password: (leave blank)
- Query: `SELECT * FROM CARDS;`

### Step 5: Process a Transaction

```bash
curl -X POST http://localhost:8080/api/transactions/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "{MERCHANT_ID}",
    "cardNumber": "4123456789012345",
    "cvv": "123",
    "type": "PURCHASE",
    "amount": 99.99,
    "currency": "USD",
    "terminalId": "TERM001"
  }'
```

**Expected Successful Response:**
```json
{
  "id": "transaction-id",
  "merchantId": "merchant-id",
  "type": "PURCHASE",
  "status": "AUTHORIZED",
  "amount": 99.99,
  "currency": "USD",
  "authorizationCode": "123456",
  "responseMessage": "Approved",
  "createdAt": "2026-02-05T10:30:00"
}
```

**Expected Declined Response (insufficient funds):**
```json
{
  "id": "transaction-id",
  "merchantId": "merchant-id",
  "type": "PURCHASE",
  "status": "DECLINED",
  "amount": 99.99,
  "currency": "USD",
  "authorizationCode": null,
  "responseMessage": "Insufficient funds",
  "createdAt": "2026-02-05T10:30:00"
}
```

### Step 6: Check Transaction Status

```bash
curl http://localhost:8080/api/transactions/{TRANSACTION_ID}
```

### Step 7: View Merchant Transactions

```bash
curl http://localhost:8080/api/transactions/merchant/{MERCHANT_ID}
```

### Step 8: View Settlement Information

```bash
curl http://localhost:8080/api/settlements/merchant/{MERCHANT_ID}
```

### Step 9: Generate Reports

```bash
curl http://localhost:8080/api/reports/transactions/{MERCHANT_ID}
```

**Expected Response:**
```json
{
  "totalTransactions": 100,
  "totalVolume": 50000.00,
  "approvedCount": 95,
  "declinedCount": 5,
  "approvalRate": 95.0
}
```

## Testing Different Scenarios

### Scenario 1: Successful Purchase
```bash
# Create merchant -> Issue card -> Process transaction with sufficient funds
# Expected: Transaction AUTHORIZED
```

### Scenario 2: Declined Transaction (Insufficient Funds)
```bash
# Try to charge more than the available balance
curl -X POST http://localhost:8080/api/transactions/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "{MERCHANT_ID}",
    "cardNumber": "{CARD_NUMBER}",
    "cvv": "{CVV}",
    "type": "PURCHASE",
    "amount": 99999.99,
    "currency": "USD"
  }'
# Expected: Transaction DECLINED
```

### Scenario 3: Invalid CVV
```bash
curl -X POST http://localhost:8080/api/transactions/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "{MERCHANT_ID}",
    "cardNumber": "{CARD_NUMBER}",
    "cvv": "999",
    "type": "PURCHASE",
    "amount": 50.00,
    "currency": "USD"
  }'
# Expected: Transaction DECLINED - "Invalid CVV"
```

### Scenario 4: Block a Card
```bash
curl -X PUT http://localhost:8080/api/cards/{CARD_ID}/block

# Then try to use it
curl -X POST http://localhost:8080/api/transactions/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "{MERCHANT_ID}",
    "cardNumber": "{CARD_NUMBER}",
    "cvv": "{CVV}",
    "type": "PURCHASE",
    "amount": 50.00
  }'
# Expected: Transaction DECLINED - "Card is blocked"
```

## Testing with Postman

### Import this collection:

1. Create a new collection called "Payment System"
2. Add environment variables:
   - `gateway_url`: http://localhost:8080
   - `merchant_id`: (save from response)
   - `card_id`: (save from response)
   - `card_number`: (get from H2 console)
   - `cvv`: (get from H2 console)

### Example Postman Requests:

**1. Create Merchant**
- Method: POST
- URL: `{{gateway_url}}/api/merchants`
- Body: (see Step 1 above)

**2. Issue Card**
- Method: POST
- URL: `{{gateway_url}}/api/cards`
- Body: (see Step 3 above)

**3. Process Transaction**
- Method: POST
- URL: `{{gateway_url}}/api/transactions/authorize`
- Body: (see Step 5 above)

## Database Access

Each service has its own H2 console:

| Service | Console URL | JDBC URL |
|---------|-------------|----------|
| Merchant | http://localhost:8081/h2-console | jdbc:h2:mem:merchantdb |
| Acquirer | http://localhost:8082/h2-console | jdbc:h2:mem:acquirerdb |
| Issuer | http://localhost:8083/h2-console | jdbc:h2:mem:issuerdb |
| Network | http://localhost:8084/h2-console | jdbc:h2:mem:networkdb |
| Transaction | http://localhost:8085/h2-console | jdbc:h2:mem:transactiondb |
| Settlement | http://localhost:8086/h2-console | jdbc:h2:mem:settlementdb |

Username: `sa`  
Password: (leave blank)

## Useful Queries

### View all merchants:
```sql
SELECT * FROM MERCHANTS;
```

### View all cards:
```sql
SELECT * FROM CARDS;
```

### View all transactions:
```sql
SELECT * FROM TRANSACTIONS;
```

### View settlements:
```sql
SELECT * FROM SETTLEMENT;
```

## Monitoring

### Eureka Dashboard
http://localhost:8761

Shows all registered services and their status.

### Health Checks
```bash
# Check all services
for port in 8761 8888 8080 8081 8082 8083 8084 8085 8086 8087 8088 8089 8091; do
  echo "Port $port:"
  curl -s http://localhost:$port/actuator/health | jq .
done
```

### Service Info
```bash
curl http://localhost:8080/actuator/info
```

## Swagger UI

Access API documentation:

- API Gateway: http://localhost:8080/swagger-ui.html
- Merchant Service: http://localhost:8081/swagger-ui.html
- Issuer Service: http://localhost:8083/swagger-ui.html
- Transaction Service: http://localhost:8085/swagger-ui.html

## Troubleshooting

### Services not starting?
1. Check if ports are available
2. Check logs in `/home/claude/payment-system/logs/`
3. Ensure Java 17+ is installed
4. Verify Maven is installed

### Transaction declined?
1. Check card balance in H2 console
2. Verify CVV matches
3. Check if card is active
4. Ensure merchant is ACTIVE

### Can't connect to services?
1. Ensure all services registered with Eureka
2. Wait 30-60 seconds after startup
3. Check Eureka dashboard at http://localhost:8761

## Load Testing

### Using Apache Bench
```bash
# Install ab
sudo apt-get install apache2-utils

# Test transaction endpoint
ab -n 100 -c 10 -p transaction.json -T application/json \
  http://localhost:8080/api/transactions/authorize
```

### Sample load test data (transaction.json):
```json
{
  "merchantId": "your-merchant-id",
  "cardNumber": "4123456789012345",
  "cvv": "123",
  "type": "PURCHASE",
  "amount": 10.00,
  "currency": "USD"
}
```

## Best Practices for Learning

1. **Start Simple**: Begin with creating a merchant and issuing a card
2. **Use H2 Console**: Inspect database state after each operation
3. **Check Logs**: Watch service logs to understand the flow
4. **Experiment**: Try different scenarios (declined transactions, blocked cards)
5. **Monitor**: Use Eureka to see service health
6. **Build Gradually**: Start with one service, understand it, then move to the next

## What You'll Learn

- Microservices architecture
- Service discovery with Eureka
- API Gateway pattern
- Inter-service communication (Feign)
- Transaction processing flow
- Payment authorization
- Settlement reconciliation
- RESTful API design
- Database per service pattern
- Event-driven architecture basics

## Next Steps

1. Implement actual email/SMS notifications
2. Add Redis caching
3. Implement Kafka for event streaming
4. Add JWT authentication
5. Implement rate limiting
6. Add circuit breakers (Resilience4j)
7. Set up monitoring (Prometheus + Grafana)
8. Deploy to Kubernetes
9. Add comprehensive unit tests
10. Implement fraud detection

---

**Happy Learning! 🚀**

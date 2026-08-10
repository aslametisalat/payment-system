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

### Step 1: Create a Merchant
```bash
curl -X POST http://localhost:8080/api/merchants \
  -H "Content-Type: application/json" \
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
curl -X PUT "http://localhost:8080/api/merchants/{MERCHANT_ID}/status?status=ACTIVE"
```

### Step 3: Issue a Test Card
```bash
curl -X POST http://localhost:8080/api/cards \
  -H "Content-Type: application/json" \
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

1. Open H2 Console: http://localhost:8092/h2-console
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
  -H "Content-Type: application/json" \
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

### View All Merchants
```bash
curl http://localhost:8080/api/merchants
```

### View All Cards
```bash
curl http://localhost:8080/api/cards
```

### View All Transactions
```bash
curl http://localhost:8080/api/transactions
```

### View Merchant's Transactions
```bash
curl http://localhost:8080/api/transactions/merchant/{MERCHANT_ID}
```

### Generate Reports
```bash
curl http://localhost:8080/api/reports/transactions/{MERCHANT_ID}
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
| Issuer | http://localhost:8092/h2-console | jdbc:h2:mem:issuerdb |
| Transaction | http://localhost:8085/h2-console | jdbc:h2:mem:transactiondb |
| Settlement | http://localhost:8086/h2-console | jdbc:h2:mem:settlementdb |

All use: Username: `sa`, Password: (blank)

### Swagger API Documentation
- Merchant Service: http://localhost:8081/swagger-ui.html
- Issuer Service: http://localhost:8092/swagger-ui.html
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
  -H "Content-Type: application/json" \
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
  -H "Content-Type: application/json" \
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
curl -X PUT http://localhost:8080/api/cards/{CARD_ID}/block

# Try to use it
curl -X POST http://localhost:8080/api/transactions/authorize \
  -H "Content-Type: application/json" \
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

## 🚀 Advanced Usage

### Using Postman

1. Import the API endpoints:
   - Create a new Postman collection
   - Set base URL variable: `{{base_url}}` = `http://localhost:8080`
   
2. Create environment variables:
   ```
   merchant_id = (from create merchant response)
   card_id = (from create card response)
   card_number = (from H2 console)
   cvv = (from H2 console)
   ```

3. Test the complete flow:
   - Create Merchant → Activate → Issue Card → Process Transaction

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
netstat -tlnp | grep -E ':(8080|8081|8082|8084|8085|8086|8087|8088|8089|8091|8092|8761|8888)'

# Kill processes on specific port if needed
kill -9 $(lsof -ti:8080)
```

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
- Implement JWT authentication
- Add Redis caching
- Implement Kafka messaging
- Add circuit breakers (Resilience4j)
- Deploy to Kubernetes
- Add monitoring (Prometheus + Grafana)
- Implement fraud detection
- Add integration tests

## 💡 Common Questions

**Q: Can I use this in production?**  
A: This is a learning project. For production, you need:
- PostgreSQL/MySQL instead of H2
- Security (JWT, OAuth2)
- Redis caching
- Kafka for messaging
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

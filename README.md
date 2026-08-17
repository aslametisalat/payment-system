# Complete POS Payment System

A comprehensive microservices-based Point of Sale (POS) payment processing system built with Spring Boot and Spring Cloud.

## System Architecture

This payment system implements a complete payment processing ecosystem with the following components:

### Core Services

1. **Service Registry (Eureka)** - Service discovery and registration
2. **Config Server** - Centralized configuration management
3. **API Gateway** - Single entry point with routing, authentication, and rate limiting
4. **Common Module** - Shared DTOs, utilities, and constants

### Business Services

5. **Merchant Service** - Merchant onboarding, management, and POS terminal operations
6. **Acquirer Service** - Payment acquisition and merchant banking
7. **Issuer Service** - Card issuance and cardholder account management
8. **Network Service** - Payment network routing (like Visa/Mastercard)
9. **Transaction Service** - Transaction processing and lifecycle management
10. **Settlement Service** - Financial settlement between parties
11. **Reporting Service** - Analytics, reporting, and dashboards
12. **Notification Service** - Multi-channel notifications (SMS, Email, Push)

## Payment Flow

```
POS Terminal → Merchant Service → API Gateway → Acquirer Service 
    → Network Service → Issuer Service → Card Validation
    → Authorization → Transaction Service → Settlement Service
```

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Configuration**: Spring Cloud Config
- **Database**: H2 (dev), PostgreSQL (prod ready)
- **Messaging**: Spring Events / Kafka ready
- **Security**: Spring Security + JWT
- **Monitoring**: Spring Actuator
- **API Documentation**: SpringDoc OpenAPI

## Features

### Merchant Service
- Merchant registration and KYC
- POS terminal management
- Transaction monitoring
- Settlement reconciliation
- Merchant portal APIs

### Acquirer Service
- Payment acquisition
- Merchant relationship management
- Transaction routing
- Chargeback handling
- Risk management

### Issuer Service
- Card issuance (Credit/Debit)
- Cardholder management
- Account balance management
- Transaction authorization
- Fraud detection

### Network Service
- Payment routing (domestic/international)
- Network fee calculation
- Multi-scheme support (Visa, Mastercard, etc.)
- 3DS authentication
- Tokenization

### Transaction Service
- Authorization requests
- Transaction state management
- Reversal and refund processing
- Transaction history
- Real-time processing

### Settlement Service
- Daily settlement batches
- Net settlement calculation
- Merchant payouts
- Reconciliation
- Dispute management

### Reporting Service
- Transaction reports
- Settlement reports
- Merchant analytics
- Revenue analysis
- Custom report generation

### Notification Service
- Transaction alerts
- Settlement notifications
- System alerts
- Email/SMS/Push notifications
- Webhook support

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker (optional)

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd payment-system
```

2. Build all services:
```bash
mvn clean install
```

3. Start services in order:
```bash
chmod +x start-all.sh
./start-all.sh
```

Or manually:
```bash
# 1. Service Registry
cd service-registry && mvn spring-boot:run &

# 2. Config Server
cd config-server && mvn spring-boot:run &

# Wait 30 seconds for registry and config to start

# 3. All other services
cd api-gateway && mvn spring-boot:run &
cd merchant-service && mvn spring-boot:run &
cd acquirer-service && mvn spring-boot:run &
cd issuer-service && mvn spring-boot:run &
cd network-service && mvn spring-boot:run &
cd transaction-service && mvn spring-boot:run &
cd settlement-service && mvn spring-boot:run &
cd reporting-service && mvn spring-boot:run &
cd notification-service && mvn spring-boot:run &
cd security-service && mvn spring-boot:run &
cd pos-terminal-service && mvn spring-boot:run &
```

## Service Ports

| Service | Port |
|---------|------|
| Service Registry | 8761 |
| Config Server | 8888 |
| API Gateway | 8080 |
| Merchant Service | 8081 |
| Acquirer Service | 8082 |
| Network Service | 8084 |
| Transaction Service | 8085 |
| Settlement Service | 8086 |
| Reporting Service | 8087 |
| Notification Service | 8088 |
| Security Service | 8089 |
| POS Terminal Service | 8091 |
| Issuer Service | 8083 |

## API Documentation

After starting the services, access Swagger UI:
- API Gateway: http://localhost:8080/swagger-ui.html
- Individual services: http://localhost:{port}/swagger-ui.html

## Testing Payment Flow

### 1. Create a Merchant
```bash
curl -X POST http://localhost:8080/api/merchants \
  -H "Content-Type: application/json" \
  -d '{
    "businessName": "Coffee Shop",
    "email": "shop@coffee.com",
    "phone": "+1234567890",
    "address": "123 Main St"
  }'
```

### 2. Create a Card
```bash
curl -X POST http://localhost:8080/api/cards \
  -H "Content-Type: application/json" \
  -d '{
    "cardholderName": "John Doe",
    "cardType": "CREDIT",
    "creditLimit": 5000.00
  }'
```

### 3. Process a Payment
```bash
curl -X POST http://localhost:8080/api/transactions/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "merchant-id",
    "cardNumber": "card-number",
    "amount": 100.00,
    "currency": "USD"
  }'
```

## Configuration

Configuration files are located in `config-server/src/main/resources/config/`

### Environment-specific configs:
- `application.yml` - Common configuration
- `application-dev.yml` - Development
- `application-prod.yml` - Production

## Monitoring

Access Eureka Dashboard: http://localhost:8761

Health check endpoints:
- http://localhost:{port}/actuator/health
- http://localhost:{port}/actuator/info

## Security

- JWT-based authentication
- Role-based access control (RBAC)
- API rate limiting
- Transaction encryption
- PCI DSS compliance ready

## Database Schema

Each service has its own database:
- `merchant_db` - Merchant data
- `acquirer_db` - Acquirer data
- `issuer_db` - Card and account data
- `transaction_db` - Transaction records
- `settlement_db` - Settlement data
- `reporting_db` - Analytics data

## Best Practices Implemented

1. **Circuit Breaker Pattern** - Resilience4j for fault tolerance
2. **API Versioning** - URI versioning for backward compatibility
3. **Centralized Logging** - SLF4J with structured logging
4. **Distributed Tracing** - Spring Cloud Sleuth ready
5. **Event-Driven Architecture** - Async processing for notifications
6. **Idempotency** - Duplicate transaction prevention
7. **Rate Limiting** - Protection against abuse
8. **Caching** - Redis-ready for performance
9. **Database Migration** - Flyway/Liquibase ready
10. **API Documentation** - OpenAPI 3.0 specification

## Learning Objectives

This system teaches:
- Microservices architecture
- Payment processing workflows
- Financial transaction handling
- Service-to-service communication
- API gateway patterns
- Service discovery
- Configuration management
- Security in distributed systems
- Event-driven design
- Settlement and reconciliation
- Reporting and analytics

## Production Considerations

For production deployment:
1. Replace H2 with PostgreSQL/MySQL
2. Implement Redis for caching
3. Add Kafka for event streaming
4. Set up ELK stack for logging
5. Configure monitoring (Prometheus + Grafana)
6. Implement backup and disaster recovery
7. Set up CI/CD pipelines
8. Enable HTTPS/TLS
9. Configure load balancing
10. Implement data encryption at rest

## Contributing

This is a learning project. Feel free to:
- Add new features
- Improve existing implementations
- Fix bugs
- Enhance documentation

## License

Educational use - MIT License

## Support

For issues and questions, please create an issue in the repository.

---

**Note**: This is a complete educational implementation. For production use, additional security hardening, compliance checks, and infrastructure setup are required.

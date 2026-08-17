# Payment System Architecture

## System Overview

This is a complete Point of Sale (POS) payment processing system built using microservices architecture. It simulates real-world payment flows similar to systems used by Visa, Mastercard, and payment processors.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENT APPLICATIONS                         │
│                    (POS Terminals, Mobile Apps, Web)                │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         API GATEWAY (8080)                          │
│              • Routing  • Load Balancing  • Rate Limiting           │
└─────────┬───────────────────────────────────────────────────────────┘
          │
          │ ┌────────────────────────────────────────────────────┐
          ├─│  SERVICE REGISTRY (Eureka) - 8761                  │
          │ │  • Service Discovery  • Health Monitoring           │
          │ └────────────────────────────────────────────────────┘
          │
          │ ┌────────────────────────────────────────────────────┐
          ├─│  CONFIG SERVER - 8888                              │
          │ │  • Centralized Configuration Management            │
          │ └────────────────────────────────────────────────────┘
          │
          ├──────────────────┬──────────────────┬────────────────────┐
          │                  │                  │                    │
          ▼                  ▼                  ▼                    ▼
┌──────────────────┐  ┌──────────────┐  ┌────────────────┐  ┌──────────────┐
│ MERCHANT SERVICE │  │  ACQUIRER    │  │    ISSUER      │  │   NETWORK    │
│     (8081)       │  │  SERVICE     │  │   SERVICE      │  │   SERVICE    │
│                  │  │   (8082)     │  │    (8083)      │  │   (8084)     │
│ • Registration   │  │              │  │                │  │              │
│ • KYC/Onboarding │  │ • Merchant   │  │ • Card Issue   │  │ • Routing    │
│ • Terminal Mgmt  │  │   Banking    │  │ • Authorization│  │ • Network    │
│ • Settlements    │  │ • Risk Mgmt  │  │ • Balance Check│  │   Fees       │
│                  │  │ • Chargebacks│  │ • Fraud Check  │  │ • Multi-Card │
└──────────────────┘  └──────────────┘  └────────────────┘  └──────────────┘
          │                  │                  │                    │
          │                  │                  │                    │
          └──────────────────┴──────────────────┴────────────────────┘
                                    │
                          ┌─────────┴─────────┐
                          │                   │
                          ▼                   ▼
                 ┌──────────────────┐  ┌──────────────────┐
                 │   TRANSACTION    │  │   SETTLEMENT     │
                 │     SERVICE      │  │     SERVICE      │
                 │      (8085)      │  │      (8086)      │
                 │                  │  │                  │
                 │ • Auth Requests  │  │ • Daily Batches  │
                 │ • State Mgmt     │  │ • Reconciliation │
                 │ • Refunds        │  │ • Payouts        │
                 │ • History        │  │ • Disputes       │
                 └──────────────────┘  └──────────────────┘
                          │                   │
                          └─────────┬─────────┘
                                    │
                          ┌─────────┴─────────┐
                          │                   │
                          ▼                   ▼
                 ┌──────────────────┐  ┌──────────────────┐
                 │    REPORTING     │  │   NOTIFICATION   │
                 │     SERVICE      │  │     SERVICE      │
                 │      (8087)      │  │      (8088)      │
                 │                  │  │                  │
                 │ • Analytics      │  │ • Email          │
                 │ • Dashboards     │  │ • SMS            │
                 │ • Export         │  │ • Push Notif     │
                 └──────────────────┘  └──────────────────┘
```

## Payment Transaction Flow

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│   POS    │────▶│ MERCHANT │────▶│   API    │────▶│TRANSACTION│
│ TERMINAL │     │ SERVICE  │     │ GATEWAY  │     │  SERVICE  │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
                                                           │
                                                           ▼
                                                    ┌──────────┐
                                                    │ ACQUIRER │
                                                    │ SERVICE  │
                                                    └────┬─────┘
                                                         │
                                                         ▼
                                                    ┌──────────┐
                                                    │ NETWORK  │
                                                    │ SERVICE  │
                                                    └────┬─────┘
                                                         │
                                                         ▼
                                                    ┌──────────┐
                                                    │  ISSUER  │
                                                    │ SERVICE  │
                                                    └────┬─────┘
                                                         │
                                  ┌──────────────────────┴─────────────┐
                                  │                                    │
                          ┌───────▼────────┐                  ┌────────▼─────────┐
                          │   AUTHORIZED   │                  │    DECLINED      │
                          └───────┬────────┘                  └──────────────────┘
                                  │
                                  ▼
                          ┌──────────────┐
                          │  SETTLEMENT  │
                          │   SERVICE    │
                          └──────────────┘
```

## Service Responsibilities

### 1. Service Registry (Eureka)
- **Purpose**: Service discovery and registration
- **Responsibility**: 
  - Maintains registry of all microservices
  - Monitors service health
  - Enables dynamic service discovery
  - Load balancing information

### 2. Config Server
- **Purpose**: Centralized configuration management
- **Responsibility**:
  - Store configuration for all services
  - Environment-specific configs (dev, prod)
  - Hot reload of configurations
  - Versioned configuration

### 3. API Gateway
- **Purpose**: Single entry point for all clients
- **Responsibility**:
  - Request routing to appropriate services
  - Load balancing
  - Authentication and authorization
  - Rate limiting and throttling
  - Request/response transformation

### 4. Merchant Service
- **Purpose**: Merchant lifecycle management
- **Responsibility**:
  - Merchant registration and onboarding
  - KYC (Know Your Customer) verification
  - POS terminal management
  - Merchant account management
  - Transaction limits and controls
  - Merchant dashboard data

### 5. Acquirer Service
- **Purpose**: Acquiring bank operations
- **Responsibility**:
  - Merchant banking relationship
  - Transaction acquisition
  - Risk management and fraud screening
  - Chargeback handling
  - Merchant fee calculation
  - Settlement preparation

### 6. Issuer Service
- **Purpose**: Card issuance and cardholder management
- **Responsibility**:
  - Card issuance (Credit/Debit)
  - Cardholder account management
  - Balance and limit management
  - Transaction authorization
  - Fraud detection rules
  - Card blocking/unblocking

### 7. Network Service
- **Purpose**: Payment network routing (like Visa/Mastercard)
- **Responsibility**:
  - Route transactions between acquirer and issuer
  - Multi-network support
  - Network fee calculation
  - 3D Secure authentication
  - Tokenization services
  - International transaction handling

### 8. Transaction Service
- **Purpose**: Core transaction processing
- **Responsibility**:
  - Authorization requests
  - Transaction state management
  - Capture and settlement preparation
  - Refund processing
  - Reversal handling
  - Transaction history and audit

### 9. Settlement Service
- **Purpose**: Financial settlement and reconciliation
- **Responsibility**:
  - Daily settlement batches
  - Net settlement calculation
  - Merchant payout processing
  - Reconciliation with banks
  - Dispute management
  - Settlement reporting

### 10. Reporting Service
- **Purpose**: Analytics and business intelligence
- **Responsibility**:
  - Transaction analytics
  - Merchant performance reports
  - Revenue analysis
  - Fraud detection reports
  - Custom report generation
  - Data export functionality

### 11. Notification Service
- **Purpose**: Multi-channel notifications
- **Responsibility**:
  - Email notifications
  - SMS alerts
  - Push notifications
  - Transaction receipts
  - Settlement notifications
  - System alerts
  - Webhook callbacks

## Data Flow

### 1. Authorization Flow
```
POS → Merchant Service → API Gateway → Transaction Service 
  → Acquirer Service → Network Service → Issuer Service
  → (Card Validation) → (Balance Check) → Authorization Response
  → Network Service → Acquirer Service → Transaction Service
  → API Gateway → Merchant Service → POS
```

### 2. Settlement Flow
```
Settlement Service (Daily Job)
  → Query Transaction Service (Get settled transactions)
  → Calculate merchant settlements
  → Update Settlement records
  → Trigger bank transfers
  → Notify Merchant Service
  → Send notifications via Notification Service
```

### 3. Reporting Flow
```
Reporting Service
  → Query Transaction Service
  → Query Settlement Service
  → Query Merchant Service
  → Aggregate and analyze data
  → Generate reports
  → Cache results
  → Serve via API
```

## Database Schema

### Merchant Database
- `merchants` - Merchant information
- `terminals` - POS terminal data
- `merchant_accounts` - Banking information

### Issuer Database
- `cards` - Card details
- `card_accounts` - Account balances
- `card_transactions` - Card-specific transactions

### Transaction Database
- `transactions` - All transaction records
- `transaction_events` - Event log
- `refunds` - Refund records

### Settlement Database
- `settlements` - Settlement batches
- `settlement_items` - Individual settlement items
- `disputes` - Dispute records

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **API**: RESTful with Spring Web
- **Data Access**: Spring Data JPA
- **Database**: H2 (development), PostgreSQL (production-ready)

### Microservices Infrastructure
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Config Management**: Spring Cloud Config
- **Load Balancing**: Spring Cloud Load Balancer
- **Circuit Breaker**: Resilience4j (ready to implement)
- **Distributed Tracing**: Spring Cloud Sleuth (ready to implement)

### Communication
- **Synchronous**: REST APIs with OpenFeign
- **Asynchronous**: Spring Events (Kafka-ready)

### Documentation
- **API Docs**: SpringDoc OpenAPI 3.0 (Swagger UI)

### Monitoring
- **Health Checks**: Spring Actuator
- **Metrics**: Micrometer (ready for Prometheus)

## Design Patterns Used

1. **Microservices Pattern**: Each service is independently deployable
2. **API Gateway Pattern**: Single entry point for clients
3. **Service Discovery Pattern**: Dynamic service registration
4. **Database per Service**: Each service owns its data
5. **Saga Pattern**: Distributed transaction management
6. **CQRS**: Separation of command and query responsibilities
7. **Event-Driven**: Asynchronous communication for notifications
8. **Circuit Breaker**: Fault tolerance (ready to implement)
9. **Bulkhead**: Resource isolation
10. **Repository Pattern**: Data access abstraction

## Security Considerations

### Implemented:
- Input validation
- Data encapsulation
- Secure card number masking

### Production Requirements:
- JWT authentication
- OAuth2 authorization
- TLS/SSL encryption
- PCI DSS compliance
- Data encryption at rest
- Audit logging
- Rate limiting
- IP whitelisting
- API key management

## Scalability Features

### Horizontal Scaling:
- All services are stateless
- Can run multiple instances
- Load balanced through gateway

### Performance:
- Database indexing
- Response caching (Redis-ready)
- Async processing for notifications
- Batch processing for settlements

### Reliability:
- Service health monitoring
- Automatic failover
- Circuit breakers
- Retry mechanisms
- Timeout configurations

## Learning Path

### Beginner:
1. Understand individual service responsibilities
2. Learn RESTful API design
3. Study database schema design
4. Practice with H2 console

### Intermediate:
1. Service-to-service communication
2. Error handling and validation
3. Transaction management
4. API Gateway routing

### Advanced:
1. Implement circuit breakers
2. Add distributed tracing
3. Implement saga pattern
4. Add Kafka for event streaming
5. Implement caching layer
6. Add authentication/authorization
7. Performance optimization
8. Production deployment (K8s)

## Extension Ideas

1. **Add Payment Methods**:
   - Digital wallets (Apple Pay, Google Pay)
   - Bank transfers (ACH, SEPA)
   - Cryptocurrencies

2. **Enhanced Features**:
   - Fraud detection ML model
   - Real-time analytics dashboard
   - Multi-currency support
   - Subscription billing
   - Split payments
   - Loyalty programs

3. **Integration**:
   - Third-party payment gateways
   - Accounting systems
   - CRM systems
   - E-commerce platforms

4. **DevOps**:
   - CI/CD pipelines
   - Kubernetes deployment
   - Monitoring with Grafana
   - Log aggregation with ELK
   - Infrastructure as Code (Terraform)

---

This architecture provides a solid foundation for understanding enterprise payment systems and microservices architecture patterns.

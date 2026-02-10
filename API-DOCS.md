# API Documentation Access

## Swagger UI URLs (Spring Boot 3.x)

### API Gateway (Aggregated)
- **URL**: http://localhost:8080/swagger-ui/index.html
- **Docs**: http://localhost:8080/v3/api-docs

### Individual Services
- **Issuer Service**: http://localhost:8083/swagger-ui/index.html
- **Merchant Service**: http://localhost:8081/swagger-ui/index.html  
- **Acquirer Service**: http://localhost:8082/swagger-ui/index.html
- **Network Service**: http://localhost:8084/swagger-ui/index.html
- **Transaction Service**: http://localhost:8085/swagger-ui/index.html
- **Settlement Service**: http://localhost:8086/swagger-ui/index.html
- **Reporting Service**: http://localhost:8087/swagger-ui/index.html
- **Notification Service**: http://localhost:8088/swagger-ui/index.html
- **Security Service**: http://localhost:8089/swagger-ui/index.html

## Service Registry
- **Eureka Dashboard**: http://localhost:8761

## Config Server
- **Health Check**: http://localhost:8888/actuator/health

## Note
The correct Swagger UI path for Spring Boot 3.x is `/swagger-ui/index.html` (not `/swagger-ui.html`)
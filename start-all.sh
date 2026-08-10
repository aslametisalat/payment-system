#!/bin/bash

echo "========================================="
echo "Starting Payment System Microservices"
echo "========================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to wait for service to be ready
wait_for_service() {
    local service_name=$1
    local port=$2
    local max_attempts=15
    local attempt=1

    echo -e "${YELLOW}Waiting for $service_name to start on port $port...${NC}"
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s http://localhost:$port/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}$service_name is ready!${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo -e "\n${YELLOW}Warning: $service_name did not start in time${NC}"
    return 1
}

# Create logs directory
mkdir -p logs

# 1. Start Service Registry (Eureka)
echo -e "\n${GREEN}[1/13] Starting Service Registry...${NC}"
cd service-registry
mvn spring-boot:run > ../logs/service-registry.log 2>&1 &
cd ..
wait_for_service "Service Registry" 8761

# 2. Start Config Server
echo -e "\n${GREEN}[2/13] Starting Config Server...${NC}"
cd config-server
mvn spring-boot:run > ../logs/config-server.log 2>&1 &
cd ..
wait_for_service "Config Server" 8888

# Wait a bit for services to register
echo -e "\n${YELLOW}Waiting for core services to register...${NC}"
sleep 10

# 3. Start API Gateway
echo -e "\n${GREEN}[3/13] Starting API Gateway...${NC}"
cd api-gateway
mvn spring-boot:run > ../logs/api-gateway.log 2>&1 &
cd ..
wait_for_service "API Gateway" 8080

# 4. Start Merchant Service
echo -e "\n${GREEN}[4/13] Starting Merchant Service...${NC}"
cd merchant-service
mvn spring-boot:run > ../logs/merchant-service.log 2>&1 &
cd ..
wait_for_service "Merchant Service" 8081

# 5. Start Acquirer Service
echo -e "\n${GREEN}[5/13] Starting Acquirer Service...${NC}"
cd acquirer-service
mvn spring-boot:run > ../logs/acquirer-service.log 2>&1 &
cd ..
wait_for_service "Acquirer Service" 8082

# 6. Start Issuer Service
echo -e "\n${GREEN}[6/13] Starting Issuer Service...${NC}"
cd issuer-service
mvn spring-boot:run > ../logs/issuer-service.log 2>&1 &
cd ..
wait_for_service "Issuer Service" 8083

# 7. Start Network Service
echo -e "\n${GREEN}[7/13] Starting Network Service...${NC}"
cd network-service
mvn spring-boot:run > ../logs/network-service.log 2>&1 &
cd ..
wait_for_service "Network Service" 8084

# 8. Start Transaction Service
echo -e "\n${GREEN}[8/13] Starting Transaction Service...${NC}"
cd transaction-service
mvn spring-boot:run > ../logs/transaction-service.log 2>&1 &
cd ..
wait_for_service "Transaction Service" 8085

# 9. Start Settlement Service
echo -e "\n${GREEN}[9/13] Starting Settlement Service...${NC}"
cd settlement-service
mvn spring-boot:run > ../logs/settlement-service.log 2>&1 &
cd ..
wait_for_service "Settlement Service" 8086

# 10. Start Reporting Service
echo -e "\n${GREEN}[10/13] Starting Reporting Service...${NC}"
cd reporting-service
mvn spring-boot:run > ../logs/reporting-service.log 2>&1 &
cd ..
wait_for_service "Reporting Service" 8087

# 11. Start Notification Service
echo -e "\n${GREEN}[11/13] Starting Notification Service...${NC}"
cd notification-service
mvn spring-boot:run > ../logs/notification-service.log 2>&1 &
cd ..
wait_for_service "Notification Service" 8088

# 12. Start Security Service
echo -e "\n${GREEN}[12/13] Starting security-service...${NC}"
cd security-service
mvn spring-boot:run > ../logs/security-service.log 2>&1 &
cd ..
wait_for_service "security-service" 8089

# 13. Start POS Terminal Service
echo -e "\n${GREEN}[13/13] Starting pos-terminal-service...${NC}"
cd pos-terminal-service
mvn spring-boot:run > ../logs/pos-terminal-service.log 2>&1 &
cd ..
wait_for_service "pos-terminal-service" 8091

echo -e "\n${GREEN}=========================================${NC}"
echo -e "${GREEN}All Services Started Successfully!${NC}"
echo -e "${GREEN}=========================================${NC}"

echo -e "\n${YELLOW}Service URLs:${NC}"
echo "Service Registry: http://localhost:8761"
echo "Config Server: http://localhost:8888"
echo "API Gateway: http://localhost:8080"
echo "Merchant Service: http://localhost:8081"
echo "Acquirer Service: http://localhost:8082"
echo "Issuer Service: http://localhost:8083"
echo "Network Service: http://localhost:8084"
echo "Transaction Service: http://localhost:8085"
echo "Settlement Service: http://localhost:8086"
echo "Reporting Service: http://localhost:8087"
echo "Notification Service: http://localhost:8088"
echo "Security Service: http://localhost:8089"
echo "POS Terminal Service: http://localhost:8091"

echo -e "\n${YELLOW}API Documentation:${NC}"
echo "Swagger UI: http://localhost:8080/swagger-ui.html"

echo -e "\n${YELLOW}Logs Directory:${NC}"
echo "Check logs/ directory for service logs"

echo -e "\n${GREEN}System is ready for use!${NC}"
echo -e "\nPress Ctrl+C to stop all services\n"

# Keep script running
wait

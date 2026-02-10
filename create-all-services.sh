#!/bin/bash

# This script creates all microservices for the payment system

echo "Creating complete Payment System microservices..."

# Create directory structure
SERVICES=(
    "service-registry"
    "config-server"
    "api-gateway"
    "merchant-service"
    "acquirer-service"
    "issuer-service"
    "network-service"
    "transaction-service"
    "settlement-service"
    "reporting-service"
    "notification-service"
	"iso8583-service"
	"iso8583-service"
	"security-service"
	"pos-terminal-service"
)

for service in "${SERVICES[@]}"; do
    echo "Creating structure for $service..."
    mkdir -p "$service/src/main/java/com/payment/${service//-/}/"{controller,service,repository,model,dto,config}
    mkdir -p "$service/src/main/resources"
    mkdir -p "$service/src/test/java/com/payment/${service//-/}"
done

echo "All service structures created!"

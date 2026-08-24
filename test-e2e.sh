#!/bin/bash
#
# Live end-to-end smoke test for the real payment flow:
#
#   POS Terminal -> Transaction Service -> Merchant Service (limit check)
#                                        -> Acquirer Service (fraud check)
#                                        -> Network Service (routing)
#                                        -> Issuer Service (approve/decline)
#
# Assumes the services are already running (./start-all.sh, or each
# `mvn spring-boot:run` by hand - see GETTING-STARTED.md / TESTING.md).
# Doesn't start anything itself: it only drives real HTTP traffic through
# whatever's already up, so you can watch it happen in each service's logs.
#
# Usage: ./test-e2e.sh

set -u

SECURITY_URL="http://localhost:8089"
MERCHANT_URL="http://localhost:8081"
ISSUER_URL="http://localhost:8083"
POS_URL="http://localhost:8091"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

FAILED=0

# ---- helpers -----------------------------------------------------------

# Extract a top-level JSON string field's value: json_str "$body" fieldName
json_str() {
    echo "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -1 | sed -E 's/.*:"([^"]*)"/\1/'
}

# Extract a top-level JSON boolean field's value: json_bool "$body" fieldName
json_bool() {
    echo "$1" | grep -o "\"$2\":[a-z]*" | head -1 | sed -E 's/.*:([a-z]*)/\1/'
}

check_service() {
    local name="$1" url="$2"
    if ! curl -s -o /dev/null -f "$url"; then
        echo -e "${RED}✗ $name doesn't look like it's up at $url${NC}"
        echo "  Start it first - see start-all.sh or GETTING-STARTED.md."
        FAILED=1
    else
        echo -e "${GREEN}✓ $name is up${NC}"
    fi
}

step() { echo -e "\n${YELLOW}=== $1 ===${NC}"; }

# ---- pre-flight ----------------------------------------------------------

step "Checking required services"
check_service "security-service" "$SECURITY_URL/api/auth/health"
check_service "merchant-service" "$MERCHANT_URL/api/merchants/health"
check_service "issuer-service"   "$ISSUER_URL/api/cards/health"
check_service "pos-terminal-service" "$POS_URL/api/pos/health"
# transaction-service, acquirer-service and network-service are hit
# indirectly through the POS call below - if they're down, the calls
# below will fail with a clear reason instead of a silent 000.

if [ "$FAILED" -eq 1 ]; then
    echo -e "\n${RED}Not all services are reachable - aborting.${NC}"
    exit 1
fi

# ---- 0. auth -------------------------------------------------------------

step "0. Get a demo bearer token"
TOKEN_BODY=$(curl -s -X POST "$SECURITY_URL/api/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}')
TOKEN=$(json_str "$TOKEN_BODY" "accessToken")
if [ -z "$TOKEN" ]; then
    echo -e "${RED}✗ Could not get a token. Response:${NC}\n$TOKEN_BODY"
    exit 1
fi
AUTH_HEADER="Authorization: Bearer $TOKEN"
echo -e "${GREEN}✓ Got a token${NC} - every call below sends it; without one, every service in the flow now returns 401 (see JwtAuthenticationFilter)."

# ---- 1. merchant -----------------------------------------------------

step "1. Create + activate a merchant"
RUN_ID="$$-$RANDOM"
MERCHANT_BODY=$(curl -s -X POST "$MERCHANT_URL/api/merchants" \
  -H "Content-Type: application/json" -H "$AUTH_HEADER" \
  -d '{
    "businessName": "Coffee Shop",
    "email": "shop+e2e-'"$RUN_ID"'@coffee.com",
    "phone": "+12025550123",
    "address": "123 Main Street",
    "taxId": "12-3456789-'"$RUN_ID"'",
    "merchantCategoryCode": "5814",
    "dailyLimit": 10000,
    "monthlyLimit": 300000
  }')
MERCHANT_ID=$(json_str "$MERCHANT_BODY" "id")
if [ -z "$MERCHANT_ID" ]; then
    echo -e "${RED}✗ Merchant creation failed. Response:${NC}\n$MERCHANT_BODY"
    exit 1
fi
echo "Created merchant $MERCHANT_ID"

curl -s -o /dev/null -X PUT "$MERCHANT_URL/api/merchants/${MERCHANT_ID}/status?status=ACTIVE" -H "$AUTH_HEADER"
echo -e "${GREEN}✓ Merchant active${NC}"

# ---- 2. card -----------------------------------------------------------

step "2. Issue a card"
CARD_BODY=$(curl -s -X POST "$ISSUER_URL/api/cards" \
  -H "Content-Type: application/json" -H "$AUTH_HEADER" \
  -d '{
    "cardholderName": "John Doe",
    "cardType": "CREDIT",
    "network": "VISA",
    "creditLimit": 5000
  }')
CARD_NUMBER=$(json_str "$CARD_BODY" "cardNumber")
CVV=$(json_str "$CARD_BODY" "cvv")
if [ -z "$CARD_NUMBER" ] || [ -z "$CVV" ]; then
    echo -e "${RED}✗ Card issuance failed. Response:${NC}\n$CARD_BODY"
    exit 1
fi
echo "Issued card ending ${CARD_NUMBER: -4}"

# ---- 3. approved transaction --------------------------------------------

step "3. POS purchase: \$25.99, chip + PIN (should be APPROVED)"
APPROVE_BODY=$(curl -s -X POST "$POS_URL/api/pos/transaction" \
  -H "Content-Type: application/json" -H "$AUTH_HEADER" \
  -d "{
    \"terminalId\": \"TERM0001\", \"merchantId\": \"${MERCHANT_ID}\", \"merchantName\": \"Coffee Shop\",
    \"amount\": 2599, \"cardReadMethod\": \"CHIP\", \"requirePIN\": true, \"pin\": \"1234\",
    \"cardNumber\": \"${CARD_NUMBER}\", \"expiryDate\": \"2812\", \"cvv\": \"${CVV}\"
  }")
APPROVED=$(json_bool "$APPROVE_BODY" "approved")
RESPONSE_MSG=$(json_str "$APPROVE_BODY" "responseMessage")
AUTH_CODE=$(json_str "$APPROVE_BODY" "authorizationCode")

if [ "$APPROVED" = "true" ]; then
    echo -e "${GREEN}✓ APPROVED${NC} - auth code $AUTH_CODE ($RESPONSE_MSG)"
else
    echo -e "${RED}✗ Expected APPROVED, got: $RESPONSE_MSG${NC}"
    echo "Full response: $APPROVE_BODY"
    FAILED=1
fi

# ---- 4. declined transaction (merchant daily limit) ---------------------

step "4. POS purchase: \$9,999.00 - exceeds the merchant's \$10,000 daily limit (should be DECLINED)"
DECLINE_BODY=$(curl -s -X POST "$POS_URL/api/pos/transaction" \
  -H "Content-Type: application/json" -H "$AUTH_HEADER" \
  -d "{
    \"terminalId\": \"TERM0001\", \"merchantId\": \"${MERCHANT_ID}\", \"merchantName\": \"Coffee Shop\",
    \"amount\": 999900, \"cardReadMethod\": \"CHIP\", \"requirePIN\": true, \"pin\": \"1234\",
    \"cardNumber\": \"${CARD_NUMBER}\", \"expiryDate\": \"2812\", \"cvv\": \"${CVV}\"
  }")
DECLINED_APPROVED=$(json_bool "$DECLINE_BODY" "approved")
DECLINE_MSG=$(json_str "$DECLINE_BODY" "responseMessage")

if [ "$DECLINED_APPROVED" = "false" ]; then
    echo -e "${GREEN}✓ DECLINED as expected${NC} - reason: $DECLINE_MSG"
else
    echo -e "${RED}✗ Expected DECLINED, got approved=$DECLINED_APPROVED${NC}"
    echo "Full response: $DECLINE_BODY"
    FAILED=1
fi

# ---- 5. auth is actually enforced ---------------------------------------

step "5. Same purchase, no token this time (should be 401)"
UNAUTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$POS_URL/api/pos/transaction" \
  -H "Content-Type: application/json" \
  -d "{
    \"terminalId\": \"TERM0001\", \"merchantId\": \"${MERCHANT_ID}\", \"merchantName\": \"Coffee Shop\",
    \"amount\": 2599, \"cardReadMethod\": \"CHIP\", \"requirePIN\": true, \"pin\": \"1234\",
    \"cardNumber\": \"${CARD_NUMBER}\", \"expiryDate\": \"2812\", \"cvv\": \"${CVV}\"
  }")
if [ "$UNAUTH_STATUS" = "401" ]; then
    echo -e "${GREEN}✓ Rejected with 401 as expected${NC} - the JWT gate is actually enforced, not just decoration."
else
    echo -e "${RED}✗ Expected 401, got $UNAUTH_STATUS${NC}"
    FAILED=1
fi

# ---- summary -------------------------------------------------------------

step "Summary"
if [ "$FAILED" -eq 0 ]; then
    echo -e "${GREEN}All checks passed.${NC} The transaction just travelled through:"
    echo "  POS Terminal -> Transaction Service -> Merchant Service"
    echo "               -> Acquirer Service -> Network Service -> Issuer Service"
    echo "Check logs/*.log (or each service's own console) to see it happen,"
    echo "in order, in each service's own logs."
    exit 0
else
    echo -e "${RED}One or more checks failed - see above.${NC}"
    exit 1
fi

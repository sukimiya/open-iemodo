#!/bin/bash

# ============================================================
# User Service API Test Script
# Tests all endpoints of the user-service
# ============================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8081"
TENANT_ID="tenant_001"

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# Helper functions
print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
    ((TESTS_PASSED++))
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
    echo -e "${RED}  Response: $2${NC}"
    ((TESTS_FAILED++))
}

print_info() {
    echo -e "${YELLOW}→ $1${NC}"
}

# Test 1: Health Check
test_health() {
    print_header "Test 1: Health Check"
    
    RESPONSE=$(curl -s -X GET "${BASE_URL}/actuator/health" \
        -H "Content-Type: application/json" 2>/dev/null || echo '{"status":"ERROR"}')
    
    if echo "$RESPONSE" | grep -q '"status"'; then
        print_success "Health endpoint is accessible"
        echo "  Response: $(echo $RESPONSE | python3 -m json.tool 2>/dev/null | head -3 || echo $RESPONSE)"
    else
        print_error "Health check failed" "$RESPONSE"
    fi
}

# Test 2: User Registration
test_register() {
    print_header "Test 2: User Registration"
    
    EMAIL="test$(date +%s)@example.com"
    
    RESPONSE=$(curl -s -X POST "${BASE_URL}/uc/api/v1/auth/register" \
        -H "Content-Type: application/json" \
        -H "X-TenantID: ${TENANT_ID}" \
        -d "{
            \"email\": \"${EMAIL}\",
            \"password\": \"TestPassword123!\",
            \"displayName\": \"Test User\"
        }" 2>/dev/null || echo '{"code":500,"message":"Connection failed"}')
    
    if echo "$RESPONSE" | grep -q '"code":200\|"accessToken"'; then
        print_success "User registration successful"
        ACCESS_TOKEN=$(echo "$RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
        echo "  Email: $EMAIL"
        echo "  Token received: ${ACCESS_TOKEN:0:30}..."
        
        # Save token for subsequent tests
        echo "$ACCESS_TOKEN" > /tmp/user_service_token.txt
        echo "$EMAIL" > /tmp/user_service_email.txt
    else
        print_error "Registration failed" "$RESPONSE"
    fi
}

# Test 3: User Login
test_login() {
    print_header "Test 3: User Login"
    
    # Try to read email from previous test
    if [ -f /tmp/user_service_email.txt ]; then
        EMAIL=$(cat /tmp/user_service_email.txt)
    else
        EMAIL="test@example.com"
    fi
    
    RESPONSE=$(curl -s -X POST "${BASE_URL}/uc/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -H "X-TenantID: ${TENANT_ID}" \
        -d "{
            \"email\": \"${EMAIL}\",
            \"password\": \"TestPassword123!\"
        }" 2>/dev/null || echo '{"code":500,"message":"Connection failed"}')
    
    if echo "$RESPONSE" | grep -q '"code":200\|"accessToken"'; then
        print_success "User login successful"
        ACCESS_TOKEN=$(echo "$RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
        REFRESH_TOKEN=$(echo "$RESPONSE" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
        
        # Save tokens
        echo "$ACCESS_TOKEN" > /tmp/user_service_token.txt
        echo "$REFRESH_TOKEN" > /tmp/user_service_refresh_token.txt
        
        echo "  Access Token: ${ACCESS_TOKEN:0:30}..."
        echo "  Refresh Token: ${REFRESH_TOKEN:0:30}..."
    else
        print_error "Login failed" "$RESPONSE"
    fi
}

# Test 4: Get Current User
test_get_current_user() {
    print_header "Test 4: Get Current User"
    
    if [ ! -f /tmp/user_service_token.txt ]; then
        print_error "No access token available" "Run login test first"
        return
    fi
    
    TOKEN=$(cat /tmp/user_service_token.txt)
    USER_ID=$(cat /tmp/user_service_user_id.txt 2>/dev/null || echo "1")
    
    RESPONSE=$(curl -s -X GET "${BASE_URL}/uc/api/v1/users/me" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "X-User-ID: ${USER_ID}" \
        -H "X-TenantID: ${TENANT_ID}" 2>/dev/null || echo '{"code":500,"message":"Connection failed"}')
    
    if echo "$RESPONSE" | grep -q '"code":200\|"email"'; then
        print_success "Get current user successful"
        echo "  Response: $(echo $RESPONSE | python3 -m json.tool 2>/dev/null | head -10 || echo $RESPONSE)"
    else
        print_error "Get current user failed" "$RESPONSE"
    fi
}

# Test 5: Update User
test_update_user() {
    print_header "Test 5: Update User Profile"
    
    if [ ! -f /tmp/user_service_token.txt ]; then
        print_error "No access token available" "Run login test first"
        return
    fi
    
    TOKEN=$(cat /tmp/user_service_token.txt)
    USER_ID=$(cat /tmp/user_service_user_id.txt 2>/dev/null || echo "1")
    
    RESPONSE=$(curl -s -X PUT "${BASE_URL}/uc/api/v1/users/me" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "X-User-ID: ${USER_ID}" \
        -H "Content-Type: application/json" \
        -d '{
            "displayName": "Updated Test User",
            "avatarUrl": "https://example.com/avatar.png"
        }' 2>/dev/null || echo '{"code":500,"message":"Connection failed"}')
    
    if echo "$RESPONSE" | grep -q '"code":200\|"displayName"'; then
        print_success "Update user successful"
        echo "  Response: $(echo $RESPONSE | python3 -m json.tool 2>/dev/null | head -5 || echo $RESPONSE)"
    else
        print_error "Update user failed" "$RESPONSE"
    fi
}

# Test 6: Create Address
test_create_address() {
    print_header "Test 6: Create Address"
    
    if [ ! -f /tmp/user_service_token.txt ]; then
        print_error "No access token available" "Run login test first"
        return
    fi
    
    TOKEN=$(cat /tmp/user_service_token.txt)
    
    RESPONSE=$(curl -s -X POST "${BASE_URL}/uc/api/v1/users/addresses" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "Content-Type: application/json" \
        -d '{
            "recipientName": "John Doe",
            "phoneNumber": "+1234567890",
            "countryCode": "US",
            "province": "California",
            "city": "Los Angeles",
            "district": "Downtown",
            "streetAddress": "123 Main St",
            "postalCode": "90001",
            "isDefault": true
        }' 2>/dev/null || echo '{"code":500,"message":"Connection failed"}')
    
    if echo "$RESPONSE" | grep -q '"code":200\|"id"'; then
        print_success "Create address successful"
        ADDRESS_ID=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ ! -z "$ADDRESS_ID" ]; then
            echo "$ADDRESS_ID" > /tmp/user_service_address_id.txt
            echo "  Address ID: $ADDRESS_ID"
        fi
    else
        print_error "Create address failed" "$RESPONSE"
    fi
}

# Test 7: Get Addresses
test_get_addresses() {
    print_header "Test 7: Get User Addresses"
    
    if [ ! -f /tmp/user_service_token.txt ]; then
        print_error "No access token available" "Run login test first"
        return
    fi
    
    TOKEN=$(cat /tmp/user_service_token.txt)
    
    RESPONSE=$(curl -s -X GET "${BASE_URL}/uc/api/v1/users/addresses" \
        -H "Authorization: Bearer ${TOKEN}" 2>/dev/null || echo '{"code":500,"message":"Connection failed"}')
    
    if echo "$RESPONSE" | grep -q '"code":200\|\['; then
        print_success "Get addresses successful"
        echo "  Response: $(echo $RESPONSE | python3 -m json.tool 2>/dev/null | head -15 || echo $RESPONSE)"
    else
        print_error "Get addresses failed" "$RESPONSE"
    fi
}

# Test 8: Token Refresh
test_refresh_token() {
    print_header "Test 8: Token Refresh"
    
    if [ ! -f /tmp/user_service_refresh_token.txt ]; then
        print_error "No refresh token available" "Run login test first"
        return
    fi
    
    REFRESH_TOKEN=$(cat /tmp/user_service_refresh_token.txt)
    
    RESPONSE=$(curl -s -X POST "${BASE_URL}/uc/api/v1/auth/refresh" \
        -H "Content-Type: application/json" \
        -H "X-TenantID: ${TENANT_ID}" \
        -d "{\"refreshToken\": \"${REFRESH_TOKEN}\"}" 2>/dev/null || echo '{"code":500,"message":"Connection failed"}')
    
    if echo "$RESPONSE" | grep -q '"code":200\|"accessToken"'; then
        print_success "Token refresh successful"
        NEW_TOKEN=$(echo "$RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
        echo "$NEW_TOKEN" > /tmp/user_service_token.txt
        echo "  New Access Token: ${NEW_TOKEN:0:30}..."
    else
        print_error "Token refresh failed" "$RESPONSE"
    fi
}

# Test 9: Logout
test_logout() {
    print_header "Test 9: Logout"
    
    if [ ! -f /tmp/user_service_token.txt ]; then
        print_error "No access token available" "Run login test first"
        return
    fi
    
    TOKEN=$(cat /tmp/user_service_token.txt)
    USER_ID=$(cat /tmp/user_service_user_id.txt 2>/dev/null || echo "1")
    
    RESPONSE=$(curl -s -X POST "${BASE_URL}/uc/api/v1/auth/logout" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "X-User-ID: ${USER_ID}" 2>/dev/null || echo '{"code":500,"message":"Connection failed"}')
    
    if echo "$RESPONSE" | grep -q '"code":200\|"message"'; then
        print_success "Logout successful"
        rm -f /tmp/user_service_token.txt
    else
        print_error "Logout failed" "$RESPONSE"
    fi
}

# Test 10: Invalid Login
test_invalid_login() {
    print_header "Test 10: Invalid Login (Security Test)"
    
    RESPONSE=$(curl -s -X POST "${BASE_URL}/uc/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -H "X-TenantID: ${TENANT_ID}" \
        -d '{
            "email": "nonexistent@example.com",
            "password": "WrongPassword123!"
        }' 2>/dev/null || echo '{"code":500,"message":"Connection failed"}')
    
    if echo "$RESPONSE" | grep -q '"code":401\|"code":400\|"error"'; then
        print_success "Invalid login correctly rejected"
        echo "  Response: $RESPONSE"
    else
        print_error "Invalid login test failed" "$RESPONSE"
    fi
}

# Test 11: Missing Tenant ID
test_missing_tenant() {
    print_header "Test 11: Missing Tenant ID (Validation Test)"
    
    RESPONSE=$(curl -s -X POST "${BASE_URL}/uc/api/v1/auth/register" \
        -H "Content-Type: application/json" \
        -d '{
            "email": "test@example.com",
            "password": "TestPassword123!"
        }' 2>/dev/null || echo '{"code":500,"message":"Connection failed"}')
    
    if echo "$RESPONSE" | grep -q '"code":400\|"error"\|Missing\|required'; then
        print_success "Missing tenant ID correctly rejected"
        echo "  Response: $RESPONSE"
    else
        print_error "Missing tenant test failed" "$RESPONSE"
    fi
}

# Print summary
print_summary() {
    print_header "Test Summary"
    echo -e "${GREEN}Tests Passed: $TESTS_PASSED${NC}"
    echo -e "${RED}Tests Failed: $TESTS_FAILED${NC}"
    echo -e "${BLUE}Total Tests: $((TESTS_PASSED + TESTS_FAILED))${NC}"
    
    if [ $TESTS_FAILED -eq 0 ]; then
        echo -e "\n${GREEN}✓ All tests passed!${NC}"
        return 0
    else
        echo -e "\n${RED}✗ Some tests failed!${NC}"
        return 1
    fi
}

# Main execution
main() {
    print_header "User Service API Test Suite"
    echo "Base URL: $BASE_URL"
    echo "Tenant ID: $TENANT_ID"
    echo "Time: $(date)"
    
    # Run all tests
    test_health
    test_register
    test_login
    test_get_current_user
    test_update_user
    test_create_address
    test_get_addresses
    test_refresh_token
    test_logout
    test_invalid_login
    test_missing_tenant
    
    # Print summary
    print_summary
}

# Run main function
main "$@"

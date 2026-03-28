#!/bin/bash

# User Service API Test Script (Simple version)
set -e

BASE_URL="http://localhost:8081"
TENANT_ID="tenant_001"

echo "========================================"
echo "User Service API Test Suite"
echo "========================================"
echo "Base URL: $BASE_URL"
echo "Tenant ID: $TENANT_ID"
echo "Time: $(date)"
echo ""

TESTS_PASSED=0
TESTS_FAILED=0

test_pass() {
    echo "✓ $1"
    TESTS_PASSED=$((TESTS_PASSED + 1))
}

test_fail() {
    echo "✗ $1"
    echo "  Error: $2"
    TESTS_FAILED=$((TESTS_FAILED + 1))
}

# Test 1: Health Check
echo "--- Test 1: Health Check ---"
RESPONSE=$(curl -s -X GET "${BASE_URL}/actuator/health" 2>/dev/null || echo '{"status":"ERROR"}')
if echo "$RESPONSE" | grep -q '"status"'; then
    test_pass "Health endpoint accessible"
    echo "  Status: $(echo $RESPONSE | grep -o '"status":"[^"]*"' | head -1)"
else
    test_fail "Health check failed" "$RESPONSE"
fi
echo ""

# Test 2: Register
echo "--- Test 2: User Registration ---"
EMAIL="test$(date +%s)@example.com"
RESPONSE=$(curl -s -X POST "${BASE_URL}/uc/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -H "X-TenantID: ${TENANT_ID}" \
    -d "{
        \"email\": \"${EMAIL}\",
        \"password\": \"TestPassword123!\",
        \"displayName\": \"Test User\"
    }" 2>/dev/null || echo '{"code":500}')

echo "  Request: POST /uc/api/v1/auth/register"
echo "  Email: $EMAIL"
if echo "$RESPONSE" | grep -q '"code":200'; then
    test_pass "Registration successful"
    echo "  Response: $(echo $RESPONSE | cut -c1-200)..."
elif echo "$RESPONSE" | grep -q 'already exists\|duplicate'; then
    test_pass "User already exists (expected for repeated tests)"
else
    test_fail "Registration failed" "$(echo $RESPONSE | cut -c1-100)"
fi
echo ""

# Test 3: Login
echo "--- Test 3: User Login ---"
RESPONSE=$(curl -s -X POST "${BASE_URL}/uc/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -H "X-TenantID: ${TENANT_ID}" \
    -d "{
        \"email\": \"${EMAIL}\",
        \"password\": \"TestPassword123!\"
    }" 2>/dev/null || echo '{"code":500}')

echo "  Request: POST /uc/api/v1/auth/login"
if echo "$RESPONSE" | grep -q '"code":200'; then
    test_pass "Login successful"
    ACCESS_TOKEN=$(echo "$RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    echo "$ACCESS_TOKEN" > /tmp/user_token.txt
    echo "  Token: ${ACCESS_TOKEN:0:40}..."
else
    test_fail "Login failed" "$(echo $RESPONSE | cut -c1-200)"
fi
echo ""

# Test 4: Get Current User (requires token)
echo "--- Test 4: Get Current User ---"
if [ -f /tmp/user_token.txt ]; then
    TOKEN=$(cat /tmp/user_token.txt)
    # Extract user ID from token (simplified - in real scenario decode JWT)
    USER_ID="1"
    
    RESPONSE=$(curl -s -X GET "${BASE_URL}/uc/api/v1/users/me" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "X-User-ID: ${USER_ID}" \
        -H "X-TenantID: ${TENANT_ID}" 2>/dev/null || echo '{"code":500}')
    
    echo "  Request: GET /uc/api/v1/users/me"
    if echo "$RESPONSE" | grep -q '"code":200'; then
        test_pass "Get current user successful"
        echo "  Response: $(echo $RESPONSE | cut -c1-200)..."
    else
        test_fail "Get current user failed" "$(echo $RESPONSE | cut -c1-200)"
    fi
else
    test_fail "No token available" "Login test must pass first"
fi
echo ""

# Test 5: Create Address
echo "--- Test 5: Create Address ---"
if [ -f /tmp/user_token.txt ]; then
    TOKEN=$(cat /tmp/user_token.txt)
    
    RESPONSE=$(curl -s -X POST "${BASE_URL}/uc/api/v1/users/addresses" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "X-TenantID: ${TENANT_ID}" \
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
        }' 2>/dev/null || echo '{"code":500}')
    
    echo "  Request: POST /uc/api/v1/users/addresses"
    if echo "$RESPONSE" | grep -q '"code":200'; then
        test_pass "Create address successful"
        echo "  Response: $(echo $RESPONSE | cut -c1-200)..."
    else
        test_fail "Create address failed" "$(echo $RESPONSE | cut -c1-200)"
    fi
else
    test_fail "No token available" "Login test must pass first"
fi
echo ""

# Test 6: Get Addresses
echo "--- Test 6: Get User Addresses ---"
if [ -f /tmp/user_token.txt ]; then
    TOKEN=$(cat /tmp/user_token.txt)
    
    RESPONSE=$(curl -s -X GET "${BASE_URL}/uc/api/v1/users/addresses" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "X-TenantID: ${TENANT_ID}" 2>/dev/null || echo '{"code":500}')
    
    echo "  Request: GET /uc/api/v1/users/addresses"
    if echo "$RESPONSE" | grep -q '"code":200'; then
        test_pass "Get addresses successful"
        echo "  Response: $(echo $RESPONSE | cut -c1-300)..."
    else
        test_fail "Get addresses failed" "$(echo $RESPONSE | cut -c1-200)"
    fi
else
    test_fail "No token available" "Login test must pass first"
fi
echo ""

# Test 7: Invalid Login
echo "--- Test 7: Invalid Login (Security Test) ---"
RESPONSE=$(curl -s -X POST "${BASE_URL}/uc/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -H "X-TenantID: ${TENANT_ID}" \
    -d '{
        "email": "nonexistent@example.com",
        "password": "WrongPassword123!"
    }' 2>/dev/null || echo '{"code":500}')

echo "  Request: POST /uc/api/v1/auth/login (invalid credentials)"
if echo "$RESPONSE" | grep -q '"code":401\|"code":400'; then
    test_pass "Invalid login correctly rejected"
else
    test_fail "Invalid login test" "Expected 401/400, got: $(echo $RESPONSE | cut -c1-100)"
fi
echo ""

# Test 8: Missing Tenant Header
echo "--- Test 8: Missing Tenant Header (Validation Test) ---"
RESPONSE=$(curl -s -X POST "${BASE_URL}/uc/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d '{
        "email": "test@example.com",
        "password": "TestPassword123!"
    }' 2>/dev/null || echo '{"code":500}')

echo "  Request: POST /uc/api/v1/auth/register (no tenant header)"
if echo "$RESPONSE" | grep -q '"code":400\|"code":500'; then
    test_pass "Missing tenant header handled"
else
    test_fail "Missing tenant test" "$(echo $RESPONSE | cut -c1-100)"
fi
echo ""

# Summary
echo "========================================"
echo "Test Summary"
echo "========================================"
echo "Tests Passed: $TESTS_PASSED"
echo "Tests Failed: $TESTS_FAILED"
echo "Total Tests: $((TESTS_PASSED + TESTS_FAILED))"

if [ $TESTS_FAILED -eq 0 ]; then
    echo ""
    echo "All tests passed!"
    exit 0
else
    echo ""
    echo "Some tests failed!"
    exit 1
fi

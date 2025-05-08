#!/bin/bash

# Test script for Specific Spring Backend API
# This script tests the main endpoints of the API

# Configuration - Change this to your deployed URL
API_URL="https://specific-backend.onrender.com"
TEST_USERNAME="testuser"
TEST_FIREBASE_UID="test-firebase-uid-$(date +%s)"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Testing Specific Spring Backend API ===${NC}"
echo "API URL: $API_URL"
echo "Test Firebase UID: $TEST_FIREBASE_UID"
echo

# Test health endpoint
echo -e "${YELLOW}Testing health endpoint...${NC}"
HEALTH_RESPONSE=$(curl -s "$API_URL/health")
echo "Response: $HEALTH_RESPONSE"
if [[ "$HEALTH_RESPONSE" == *"UP"* ]]; then
  echo -e "${GREEN}Health check passed!${NC}"
else
  echo -e "${RED}Health check failed!${NC}"
fi
echo

# Test root endpoint
echo -e "${YELLOW}Testing root endpoint...${NC}"
ROOT_RESPONSE=$(curl -s "$API_URL/")
echo "Response: $ROOT_RESPONSE"
if [[ "$ROOT_RESPONSE" == *"success"* ]]; then
  echo -e "${GREEN}Root endpoint test passed!${NC}"
else
  echo -e "${RED}Root endpoint test failed!${NC}"
fi
echo

# Test user test endpoint
echo -e "${YELLOW}Testing user test endpoint...${NC}"
USER_TEST_RESPONSE=$(curl -s "$API_URL/user/test")
echo "Response: $USER_TEST_RESPONSE"
if [[ "$USER_TEST_RESPONSE" == *"working"* ]]; then
  echo -e "${GREEN}User test endpoint passed!${NC}"
else
  echo -e "${RED}User test endpoint failed!${NC}"
fi
echo

# Test user registration with test-register
echo -e "${YELLOW}Testing user registration with test-register...${NC}"
TEST_REGISTER_RESPONSE=$(curl -s -X POST "$API_URL/user/test-register?username=$TEST_USERNAME&firebaseUid=$TEST_FIREBASE_UID")
echo "Response: $TEST_REGISTER_RESPONSE"
if [[ "$TEST_REGISTER_RESPONSE" == *"id"* ]]; then
  echo -e "${GREEN}User test registration passed!${NC}"
else
  echo -e "${RED}User test registration failed!${NC}"
  echo -e "${YELLOW}Trying debug-test-register endpoint...${NC}"
  
  DEBUG_TEST_REGISTER_RESPONSE=$(curl -s -X POST "$API_URL/user/debug-test-register?username=$TEST_USERNAME&firebaseUid=$TEST_FIREBASE_UID")
  echo "Response: $DEBUG_TEST_REGISTER_RESPONSE"
  if [[ "$DEBUG_TEST_REGISTER_RESPONSE" == *"id"* ]]; then
    echo -e "${GREEN}User debug-test registration passed!${NC}"
  else
    echo -e "${RED}User debug-test registration failed!${NC}"
  fi
fi
echo

# Test user registration with JSON body
echo -e "${YELLOW}Testing user registration with JSON body...${NC}"
REGISTER_RESPONSE=$(curl -s -X POST "$API_URL/user/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$TEST_USERNAME-json\",\"firebaseUid\":\"$TEST_FIREBASE_UID-json\"}")
echo "Response: $REGISTER_RESPONSE"
if [[ "$REGISTER_RESPONSE" == *"id"* ]]; then
  echo -e "${GREEN}User registration with JSON passed!${NC}"
else
  echo -e "${RED}User registration with JSON failed!${NC}"
  echo -e "${YELLOW}Trying debug-register endpoint...${NC}"
  
  DEBUG_REGISTER_RESPONSE=$(curl -s -X POST "$API_URL/user/debug-register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$TEST_USERNAME-json\",\"firebaseUid\":\"$TEST_FIREBASE_UID-json\"}")
  echo "Response: $DEBUG_REGISTER_RESPONSE"
  if [[ "$DEBUG_REGISTER_RESPONSE" == *"id"* ]]; then
    echo -e "${GREEN}User debug registration with JSON passed!${NC}"
  else
    echo -e "${RED}User debug registration with JSON failed!${NC}"
  fi
fi
echo

echo -e "${YELLOW}=== Tests completed ===${NC}" 
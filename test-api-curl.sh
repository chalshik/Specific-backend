#!/bin/bash

# Test script for Specific Spring API
# Tests all main endpoints using curl

# Configuration
API_URL="https://specific-backend.onrender.com"  # Remote API URL
TEST_FIREBASE_UID="test-user-123" # Test Firebase UID to use for all requests
TEST_USERNAME="TestUser"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables to store IDs
DECK_ID=""
CARD_ID=""
BOOK_ID=""
REVIEW_ID=""

echo -e "${YELLOW}===== Testing Specific Spring API =====${NC}"
echo "API URL: $API_URL"
echo "Test Firebase UID: $TEST_FIREBASE_UID"
echo

# Helper function for API calls
function call_api {
    local method=$1
    local endpoint=$2
    local data=$3
    local no_firebase_uid=$4
    
    # Add firebaseUid parameter to the URL if not explicitly told not to
    if [[ "$no_firebase_uid" != "true" ]]; then
        if [[ "$endpoint" == *"?"* ]]; then
            endpoint="${endpoint}&firebaseUid=${TEST_FIREBASE_UID}"
        else
            endpoint="${endpoint}?firebaseUid=${TEST_FIREBASE_UID}"
        fi
    fi
    
    echo -e "${BLUE}[REQUEST] $method $endpoint${NC}"
    if [ -n "$data" ]; then
        echo "Data: $data"
    fi
    
    # Execute the curl command and capture the response
    local response=""
    if [ -n "$data" ]; then
        response=$(curl -s -X $method "$API_URL$endpoint" \
            -H "Content-Type: application/json" \
            -H "X-Firebase-Uid: $TEST_FIREBASE_UID" \
            -d "$data")
    else
        response=$(curl -s -X $method "$API_URL$endpoint" \
            -H "Content-Type: application/json" \
            -H "X-Firebase-Uid: $TEST_FIREBASE_UID")
    fi
    
    echo -e "${BLUE}[RESPONSE]${NC}"
    echo "$response" | jq . 2>/dev/null || echo "$response"
    echo -e "${BLUE}------------------------${NC}"
    
    echo "$response"
}

echo -e "${YELLOW}===== 1. User Registration =====${NC}"
USER_RESPONSE=$(call_api "POST" "/user/register" "{\"username\":\"$TEST_USERNAME\",\"firebaseUid\":\"$TEST_FIREBASE_UID\"}" "true")
echo "$USER_RESPONSE" | jq . 2>/dev/null || echo "$USER_RESPONSE"

sleep 1

echo -e "${YELLOW}===== 2. Get User Info =====${NC}"
call_api "GET" "/user/info"

sleep 1

echo -e "${YELLOW}===== 3. Create Deck =====${NC}"
DECK_RESPONSE=$(call_api "POST" "/anki/add-deck" "{\"title\":\"Test Deck\"}")
DECK_ID=$(echo "$DECK_RESPONSE" | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)

if [ -n "$DECK_ID" ]; then
    echo -e "${GREEN}Created deck with ID: $DECK_ID${NC}"
    
    echo -e "${YELLOW}===== 4. Get User Decks =====${NC}"
    call_api "GET" "/anki/user-decks"
    
    sleep 1
    
    echo -e "${YELLOW}===== 5. Create Card =====${NC}"
    CARD_RESPONSE=$(call_api "POST" "/api/cards/deck/$DECK_ID" "{\"front\":\"Test question\",\"back\":\"Test answer\"}")
    CARD_ID=$(echo "$CARD_RESPONSE" | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)
    
    if [ -n "$CARD_ID" ]; then
        echo -e "${GREEN}Created card with ID: $CARD_ID${NC}"
        
        echo -e "${YELLOW}===== 6. Get Cards to Study =====${NC}"
        call_api "GET" "/api/cards/study/deck/$DECK_ID"
        
        sleep 1
        
        echo -e "${YELLOW}===== 7. Update Card =====${NC}"
        call_api "PUT" "/api/cards/$CARD_ID" "{\"front\":\"Updated question\",\"back\":\"Updated answer\"}"
        
        sleep 1
        
        echo -e "${YELLOW}===== 8. Submit Card Review =====${NC}"
        REVIEW_RESPONSE=$(call_api "POST" "/api/reviews" "{\"cardId\":$CARD_ID,\"rating\":\"good\"}")
        REVIEW_ID=$(echo "$REVIEW_RESPONSE" | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)
        
        if [ -n "$REVIEW_ID" ]; then
            echo -e "${GREEN}Created review with ID: $REVIEW_ID${NC}"
            
            echo -e "${YELLOW}===== 9. Get Card Review History =====${NC}"
            call_api "GET" "/api/reviews/history/card/$CARD_ID"
            
            sleep 1
            
            echo -e "${YELLOW}===== 10. Get Deck Review Stats =====${NC}"
            call_api "GET" "/api/reviews/stats/deck/$DECK_ID"
        else
            echo -e "${RED}Failed to create review${NC}"
        fi
        
    else
        echo -e "${RED}Failed to create card${NC}"
    fi
    
else
    echo -e "${RED}Failed to create deck${NC}"
fi

sleep 1

echo -e "${YELLOW}===== 11. Create Book =====${NC}"
BOOK_RESPONSE=$(call_api "POST" "/api/books" "{\"title\":\"Test Book\",\"author\":\"Test Author\",\"language\":\"en\"}")
BOOK_ID=$(echo "$BOOK_RESPONSE" | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)

if [ -n "$BOOK_ID" ]; then
    echo -e "${GREEN}Created book with ID: $BOOK_ID${NC}"
    
    echo -e "${YELLOW}===== 12. Get User Books =====${NC}"
    call_api "GET" "/api/books"
    
    sleep 1
    
    echo -e "${YELLOW}===== 13. Get Book by ID =====${NC}"
    call_api "GET" "/api/books/$BOOK_ID"
    
    sleep 1
    
    echo -e "${YELLOW}===== 14. Update Book =====${NC}"
    call_api "PUT" "/api/books/$BOOK_ID" "{\"title\":\"Updated Book\",\"author\":\"Updated Author\",\"language\":\"en\"}"
    
    sleep 1
    
    echo -e "${YELLOW}===== 15. Search Books =====${NC}"
    call_api "GET" "/api/books/search?title=Updated"
    
else
    echo -e "${RED}Failed to create book${NC}"
fi

echo -e "${YELLOW}===== Test Script Complete =====${NC}" 
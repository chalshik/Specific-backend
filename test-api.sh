#!/bin/bash

# API Test Script for Specific Spring Backend
# Tests all endpoints including user, deck, card, book, and review functionality

# Configuration
API_URL="https://specific-backend.onrender.com"
FIREBASE_TOKEN="jq1iTgd1uEQHCGcLgfVivrlwWji2"
TEST_USERNAME="test-user-$(date +%s)"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables to store created resource IDs
USER_ID=""
DECK_ID=""
CARD_ID=""
BOOK_ID=""
REVIEW_ID=""

# Helper function to make API calls with detailed output
function call_api {
    local method=$1
    local endpoint=$2
    local data=$3
    local auth=${4:-true}
    
    echo -e "${BLUE}[REQUEST] $method $endpoint${NC}"
    if [ -n "$data" ]; then
        echo "Data: $data"
    fi
    
    local auth_header=""
    if [ "$auth" = true ]; then
        auth_header="-H \"Authorization: Bearer $FIREBASE_TOKEN\""
    fi
    
    # Build curl command with verbose output for debugging
    local cmd=""
    if [ -n "$data" ]; then
        cmd="curl -v -X $method \"$API_URL$endpoint\" -H \"Content-Type: application/json\" $auth_header -d '$data'"
    else
        cmd="curl -v -X $method \"$API_URL$endpoint\" -H \"Content-Type: application/json\" $auth_header"
    fi
    
    # Execute the curl command and capture the response
    echo "Executing: $cmd"
    local response=$(eval $cmd 2>&1)
    echo -e "${BLUE}[RESPONSE]${NC}"
    echo "$response"
    echo "------------------------"
    echo
    
    # Extract just the response body (after the empty line in verbose output)
    local body=$(echo "$response" | sed -n -e '/^{/,$p' | grep -v '^}$' | tr -d '\r' | tr -d '\n')
    if [ -z "$body" ]; then
        body=$(echo "$response" | grep -o '{.*}')
    fi
    
    # Return the response body for further processing
    echo "$body"
}

# Function to test an endpoint
function test_endpoint {
    local name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local auth=${5:-true}
    
    echo -e "${YELLOW}=== Testing $name ===${NC}"
    local response=$(call_api "$method" "$endpoint" "$data" "$auth")
    
    # Print full response for debugging
    echo "Response body: $response"
    
    # Check if response contains error
    if [[ "$response" == *"error"* ]] || [[ "$response" == *"ERROR"* ]]; then
        echo -e "${RED}✘ $name test failed${NC}"
        return 1
    else
        echo -e "${GREEN}✓ $name test passed${NC}"
        return 0
    fi
}

echo -e "${YELLOW}=== Starting API Test Suite ===${NC}"
echo "API URL: $API_URL"
echo "Firebase Token: $FIREBASE_TOKEN"
echo

# Test health endpoint
test_endpoint "Health Check" "GET" "/health" "" false

# Test user registration directly with curl for better debugging
echo -e "${YELLOW}=== Testing User Registration ===${NC}"
echo "Registering user with username: $TEST_USERNAME and firebase UID: $FIREBASE_TOKEN"
USER_RESPONSE=$(curl -v -X POST "$API_URL/user/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$TEST_USERNAME\",\"firebaseUid\":\"$FIREBASE_TOKEN\"}" 2>&1)

echo -e "${BLUE}[FULL RESPONSE]${NC}"
echo "$USER_RESPONSE"
echo "------------------------"

# Try to extract user ID from response
USER_ID_PART=$(echo "$USER_RESPONSE" | grep -o '"id":[0-9]*')
if [ -n "$USER_ID_PART" ]; then
    USER_ID=$(echo "$USER_ID_PART" | grep -o '[0-9]*')
    echo -e "${GREEN}✓ User registration successful. User ID: $USER_ID${NC}"
else
    echo -e "${RED}✘ User registration failed. Could not extract user ID.${NC}"
    # Try with debug endpoint
    echo "Trying debug registration endpoint..."
    DEBUG_RESPONSE=$(curl -v -X POST "$API_URL/user/debug-register" \
      -H "Content-Type: application/json" \
      -d "{\"username\":\"$TEST_USERNAME\",\"firebaseUid\":\"$FIREBASE_TOKEN\"}" 2>&1)
    
    echo -e "${BLUE}[DEBUG RESPONSE]${NC}"
    echo "$DEBUG_RESPONSE"
    
    USER_ID_PART=$(echo "$DEBUG_RESPONSE" | grep -o '"id":[0-9]*')
    if [ -n "$USER_ID_PART" ]; then
        USER_ID=$(echo "$USER_ID_PART" | grep -o '[0-9]*')
        echo -e "${GREEN}✓ Debug user registration successful. User ID: $USER_ID${NC}"
    else
        echo -e "${RED}✘ Debug registration also failed.${NC}"
    fi
fi

# If user ID was obtained, continue with tests that require a user
if [ -n "$USER_ID" ]; then
    echo -e "${GREEN}Proceeding with tests using User ID: $USER_ID${NC}"
    
    # Test user info endpoint
    test_endpoint "Get User Info" "GET" "/user/info" ""
    
    # Test deck creation
    echo -e "${YELLOW}=== Testing Deck Creation ===${NC}"
    DECK_RESPONSE=$(curl -v -X POST "$API_URL/deck/create" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $FIREBASE_TOKEN" \
      -d "{\"name\":\"Test Deck\",\"description\":\"A test deck created via API\"}" 2>&1)
    
    echo -e "${BLUE}[DECK CREATION RESPONSE]${NC}"
    echo "$DECK_RESPONSE"
    
    DECK_ID_PART=$(echo "$DECK_RESPONSE" | grep -o '"id":[0-9]*')
    if [ -n "$DECK_ID_PART" ]; then
        DECK_ID=$(echo "$DECK_ID_PART" | grep -o '[0-9]*')
        echo -e "${GREEN}✓ Deck creation successful. Deck ID: $DECK_ID${NC}"
        
        # Test getting deck by ID
        test_endpoint "Get Deck" "GET" "/deck/$DECK_ID" ""
        
        # Test card creation if deck was created
        if [ -n "$DECK_ID" ]; then
            echo -e "${YELLOW}=== Testing Card Creation ===${NC}"
            CARD_RESPONSE=$(curl -v -X POST "$API_URL/cards/create" \
              -H "Content-Type: application/json" \
              -H "Authorization: Bearer $FIREBASE_TOKEN" \
              -d "{\"front\":\"Test front\",\"back\":\"Test back\",\"deckId\":$DECK_ID}" 2>&1)
            
            echo -e "${BLUE}[CARD CREATION RESPONSE]${NC}"
            echo "$CARD_RESPONSE"
            
            CARD_ID_PART=$(echo "$CARD_RESPONSE" | grep -o '"id":[0-9]*')
            if [ -n "$CARD_ID_PART" ]; then
                CARD_ID=$(echo "$CARD_ID_PART" | grep -o '[0-9]*')
                echo -e "${GREEN}✓ Card creation successful. Card ID: $CARD_ID${NC}"
                
                # Test getting card by ID
                test_endpoint "Get Card" "GET" "/cards/$CARD_ID" ""
                
                # Test updating card
                test_endpoint "Update Card" "PUT" "/cards/update/$CARD_ID" "{\"front\":\"Updated front\",\"back\":\"Updated back\"}"
            else
                echo -e "${RED}✘ Card creation failed${NC}"
            fi
        fi
        
        # Test deck update
        test_endpoint "Update Deck" "PUT" "/deck/update/$DECK_ID" "{\"name\":\"Updated Deck\",\"description\":\"Updated description\"}"
    else
        echo -e "${RED}✘ Deck creation failed${NC}"
    fi
    
    # Test book creation
    echo -e "${YELLOW}=== Testing Book Creation ===${NC}"
    BOOK_RESPONSE=$(curl -v -X POST "$API_URL/book/create" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $FIREBASE_TOKEN" \
      -d "{\"title\":\"Test Book\",\"author\":\"Test Author\",\"language\":\"en\"}" 2>&1)
    
    echo -e "${BLUE}[BOOK CREATION RESPONSE]${NC}"
    echo "$BOOK_RESPONSE"
    
    BOOK_ID_PART=$(echo "$BOOK_RESPONSE" | grep -o '"id":[0-9]*')
    if [ -n "$BOOK_ID_PART" ]; then
        BOOK_ID=$(echo "$BOOK_ID_PART" | grep -o '[0-9]*')
        echo -e "${GREEN}✓ Book creation successful. Book ID: $BOOK_ID${NC}"
        
        # Test getting book by ID
        test_endpoint "Get Book" "GET" "/book/$BOOK_ID" ""
        
        # Test review creation if book was created
        if [ -n "$BOOK_ID" ]; then
            echo -e "${YELLOW}=== Testing Review Creation ===${NC}"
            REVIEW_RESPONSE=$(curl -v -X POST "$API_URL/review/create" \
              -H "Content-Type: application/json" \
              -H "Authorization: Bearer $FIREBASE_TOKEN" \
              -d "{\"rating\":5,\"bookId\":$BOOK_ID,\"content\":\"Great book!\"}" 2>&1)
            
            echo -e "${BLUE}[REVIEW CREATION RESPONSE]${NC}"
            echo "$REVIEW_RESPONSE"
            
            REVIEW_ID_PART=$(echo "$REVIEW_RESPONSE" | grep -o '"id":[0-9]*')
            if [ -n "$REVIEW_ID_PART" ]; then
                REVIEW_ID=$(echo "$REVIEW_ID_PART" | grep -o '[0-9]*')
                echo -e "${GREEN}✓ Review creation successful. Review ID: $REVIEW_ID${NC}"
                
                # Test getting reviews for a book
                test_endpoint "Get Book Reviews" "GET" "/review/book/$BOOK_ID" ""
            else
                echo -e "${RED}✘ Review creation failed${NC}"
            fi
        fi
    else
        echo -e "${RED}✘ Book creation failed${NC}"
    fi
    
    # Test translation endpoint (if available)
    test_endpoint "Translation" "POST" "/translation/translate" "{\"text\":\"Hello\",\"targetLang\":\"de\"}"
    
    # Test user endpoints
    test_endpoint "Get User Decks" "GET" "/deck/user" ""
    test_endpoint "Get User Books" "GET" "/book/user" ""
    test_endpoint "Get User Cards" "GET" "/cards/user" ""
else
    echo -e "${RED}Cannot continue with further tests without a valid user ID${NC}"
    
    # Try test-register endpoint as a fallback
    echo "Trying test-register endpoint..."
    TEST_REGISTER_RESPONSE=$(curl -v -X POST "$API_URL/user/test-register?username=$TEST_USERNAME&firebaseUid=$FIREBASE_TOKEN" 2>&1)
    
    echo -e "${BLUE}[TEST REGISTER RESPONSE]${NC}"
    echo "$TEST_REGISTER_RESPONSE"
fi

echo -e "${YELLOW}=== API Test Suite Completed ===${NC}" 
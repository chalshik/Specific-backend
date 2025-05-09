#!/bin/bash

# Configuration - Edit these values as needed
API_URL="https://specific-backend.onrender.com"
FIREBASE_UID="deployed-test-user-$(date +%s)"  # Unique ID with timestamp
USERNAME="Deployed Test User"
CONTENT_TYPE="Content-Type: application/json"

echo "===== TESTING DEPLOYED API WITH NEW USER ====="
echo "API URL: $API_URL"
echo "Firebase UID: $FIREBASE_UID"
echo "Username: $USERNAME"
echo "---------------------------------------------------------"

# Helper function to extract IDs from JSON responses
extract_id() {
    echo $1 | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
}

# Helper function to print test results
print_result() {
    local status=$1
    local message=$2
    
    if [ "$status" = "SUCCESS" ]; then
        echo "✅ SUCCESS: $message"
    else
        echo "❌ FAILED: $message"
    fi
}

# 1. Health check
echo "1. Checking API health"
HEALTH_RESPONSE=$(curl -s -X GET "$API_URL/health")
echo "Health response: $HEALTH_RESPONSE"

if [[ "$HEALTH_RESPONSE" == *"\"status\":\"UP\""* ]]; then
    print_result "SUCCESS" "API is running"
else
    print_result "FAILED" "API health check failed"
    exit 1
fi

# 2. Create a test user
echo -e "\n2. Creating test user"
USER_RESPONSE=$(curl -s -X POST "$API_URL/user/register" \
    -H "$CONTENT_TYPE" \
    -d "{\"username\":\"$USERNAME\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "User registration response: $USER_RESPONSE"

if [[ "$USER_RESPONSE" == *"ERROR"* ]]; then
    print_result "FAILED" "User registration failed"
    exit 1
else
    USER_ID=$(extract_id "$USER_RESPONSE")
    print_result "SUCCESS" "User registered with ID: $USER_ID"
fi

# 3. Get user info
echo -e "\n3. Retrieving user info"
USER_INFO_RESPONSE=$(curl -s -X GET "$API_URL/user/info?firebaseUid=$FIREBASE_UID" \
    -H "$CONTENT_TYPE")
echo "User info response: $USER_INFO_RESPONSE"

if [[ "$USER_INFO_RESPONSE" == *"ERROR"* ]]; then
    print_result "FAILED" "Getting user info failed"
else
    print_result "SUCCESS" "Retrieved user info"
fi

# 4. Create a deck
echo -e "\n4. Creating deck"
DECK_RESPONSE=$(curl -s -X POST "$API_URL/anki/add-deck" \
    -H "$CONTENT_TYPE" \
    -d "{\"title\":\"Test Deck\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "Deck creation response: $DECK_RESPONSE"

if [[ "$DECK_RESPONSE" == *"ERROR"* ]]; then
    print_result "FAILED" "Creating deck failed"
    DECK_ID=""
else
    DECK_ID=$(extract_id "$DECK_RESPONSE")
    print_result "SUCCESS" "Created deck with ID: $DECK_ID"
fi

# 5. Create a second deck for testing
echo -e "\n5. Creating second deck"
DECK2_RESPONSE=$(curl -s -X POST "$API_URL/anki/add-deck" \
    -H "$CONTENT_TYPE" \
    -d "{\"title\":\"Second Deck\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "Second deck creation response: $DECK2_RESPONSE"

if [[ "$DECK2_RESPONSE" == *"ERROR"* ]]; then
    print_result "FAILED" "Creating second deck failed"
    DECK2_ID=""
else
    DECK2_ID=$(extract_id "$DECK2_RESPONSE")
    print_result "SUCCESS" "Created second deck with ID: $DECK2_ID"
fi

# 6. Get all decks for the user
echo -e "\n6. Getting all user decks"
DECKS_RESPONSE=$(curl -s -X GET "$API_URL/anki/user-decks?firebaseUid=$FIREBASE_UID" \
    -H "$CONTENT_TYPE")
echo "Get decks response: $DECKS_RESPONSE"

if [[ "$DECKS_RESPONSE" == *"ERROR"* ]]; then
    print_result "FAILED" "Getting user decks failed"
else
    DECK_COUNT=$(echo "$DECKS_RESPONSE" | grep -o '"id"' | wc -l)
    print_result "SUCCESS" "Retrieved $DECK_COUNT decks for the user"
fi

if [ -z "$DECK_ID" ]; then
    echo "Skipping card operations because deck creation failed"
else
    # 7. Create a card with general endpoint
    echo -e "\n7. Creating card with general endpoint"
    CARD_RESPONSE=$(curl -s -X POST "$API_URL/api/cards" \
        -H "$CONTENT_TYPE" \
        -H "Authorization: $FIREBASE_UID" \
        -d "{\"front\":\"What is REST?\",\"back\":\"REpresentational State Transfer\",\"firebaseUid\":\"$FIREBASE_UID\",\"deck\":{\"id\":$DECK_ID}}")
    echo "Card creation response: $CARD_RESPONSE"

    if [[ "$CARD_RESPONSE" == *"ERROR"* ]]; then
        print_result "FAILED" "Creating card with general endpoint failed"
        CARD_ID=""
    else
        CARD_ID=$(extract_id "$CARD_RESPONSE")
        print_result "SUCCESS" "Created card with ID: $CARD_ID"
    fi

    # 8. Create a card with deck-specific endpoint
    echo -e "\n8. Creating card in specific deck"
    CARD2_RESPONSE=$(curl -s -X POST "$API_URL/api/cards/deck/$DECK_ID" \
        -H "$CONTENT_TYPE" \
        -H "Authorization: $FIREBASE_UID" \
        -d "{\"front\":\"What is Spring Boot?\",\"back\":\"A Java framework for building applications\",\"firebaseUid\":\"$FIREBASE_UID\"}")
    echo "Deck-specific card creation response: $CARD2_RESPONSE"

    if [[ "$CARD2_RESPONSE" == *"ERROR"* ]]; then
        print_result "FAILED" "Creating card in specific deck failed"
        CARD2_ID=""
    else
        CARD2_ID=$(extract_id "$CARD2_RESPONSE")
        print_result "SUCCESS" "Created card with ID: $CARD2_ID"
    fi

    # 9. Get all cards in a deck
    echo -e "\n9. Getting all cards in deck"
    CARDS_RESPONSE=$(curl -s -X GET "$API_URL/api/cards/deck/$DECK_ID?firebaseUid=$FIREBASE_UID" \
        -H "$CONTENT_TYPE")
    echo "Get cards in deck response: $CARDS_RESPONSE"

    if [[ "$CARDS_RESPONSE" == *"ERROR"* ]]; then
        print_result "FAILED" "Getting cards in deck failed"
    else
        CARDS_COUNT=$(echo "$CARDS_RESPONSE" | grep -o '"id"' | wc -l)
        print_result "SUCCESS" "Retrieved $CARDS_COUNT cards from deck $DECK_ID"
    fi

    # 10. Get specific card by ID
    if [ -n "$CARD_ID" ]; then
        echo -e "\n10. Getting card by ID"
        CARD_BY_ID_RESPONSE=$(curl -s -X GET "$API_URL/api/cards/$CARD_ID?firebaseUid=$FIREBASE_UID" \
            -H "$CONTENT_TYPE")
        echo "Get card by ID response: $CARD_BY_ID_RESPONSE"

        if [[ "$CARD_BY_ID_RESPONSE" == *"ERROR"* ]]; then
            print_result "FAILED" "Getting card by ID failed"
        else
            print_result "SUCCESS" "Retrieved card with ID: $CARD_ID"
        fi
    fi

    # 11. Update a card
    if [ -n "$CARD_ID" ]; then
        echo -e "\n11. Updating card"
        UPDATE_RESPONSE=$(curl -s -X PUT "$API_URL/api/cards/$CARD_ID" \
            -H "$CONTENT_TYPE" \
            -H "Authorization: $FIREBASE_UID" \
            -d "{\"front\":\"Updated: What is REST?\",\"back\":\"Updated: REpresentational State Transfer\",\"firebaseUid\":\"$FIREBASE_UID\",\"deck\":{\"id\":$DECK_ID}}")
        echo "Update card response: $UPDATE_RESPONSE"

        if [[ "$UPDATE_RESPONSE" == *"ERROR"* ]]; then
            print_result "FAILED" "Updating card failed"
        else
            print_result "SUCCESS" "Updated card with ID: $CARD_ID"
        fi
    fi

    # 12. Delete a card
    if [ -n "$CARD_ID" ]; then
        echo -e "\n12. Deleting card"
        DELETE_CARD_RESPONSE=$(curl -s -X DELETE "$API_URL/api/cards/$CARD_ID?firebaseUid=$FIREBASE_UID" \
            -H "$CONTENT_TYPE")
        echo "Delete card response: $DELETE_CARD_RESPONSE"

        if [[ "$DELETE_CARD_RESPONSE" == *"ERROR"* ]]; then
            print_result "FAILED" "Deleting card failed"
        else
            print_result "SUCCESS" "Deleted card with ID: $CARD_ID"
        fi
    fi
fi

# 13. Delete a deck - cleanup
if [ -n "$DECK_ID" ]; then
    echo -e "\n13. Deleting first deck"
    DELETE_DECK_RESPONSE=$(curl -s -X DELETE "$API_URL/anki/delete-deck/$DECK_ID?firebaseUid=$FIREBASE_UID" \
        -H "$CONTENT_TYPE")
    echo "Delete deck response: $DELETE_DECK_RESPONSE"

    if [[ "$DELETE_DECK_RESPONSE" == *"ERROR"* ]]; then
        print_result "FAILED" "Deleting deck failed"
    else
        print_result "SUCCESS" "Deleted deck with ID: $DECK_ID"
    fi
fi

# 14. Delete second deck - cleanup
if [ -n "$DECK2_ID" ]; then
    echo -e "\n14. Deleting second deck"
    DELETE_DECK2_RESPONSE=$(curl -s -X DELETE "$API_URL/anki/delete-deck/$DECK2_ID?firebaseUid=$FIREBASE_UID" \
        -H "$CONTENT_TYPE")
    echo "Delete second deck response: $DELETE_DECK2_RESPONSE"

    if [[ "$DELETE_DECK2_RESPONSE" == *"ERROR"* ]]; then
        print_result "FAILED" "Deleting second deck failed"
    else
        print_result "SUCCESS" "Deleted deck with ID: $DECK2_ID"
    fi
fi

echo -e "\n===== TESTING COMPLETE ====="
echo "Tests executed with Firebase UID: $FIREBASE_UID" 
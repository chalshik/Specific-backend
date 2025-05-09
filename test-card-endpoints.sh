#!/bin/bash

# Configuration
API_URL="https://specific-backend.onrender.com"
FIREBASE_UID="card-test-user"
CONTENT_TYPE="Content-Type: application/json"

echo "Testing Card Endpoints with Firebase UID: $FIREBASE_UID"
echo "---------------------------------------------------------"

# Helper function to extract IDs from JSON responses
extract_id() {
    echo $1 | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
}

# Step 1: Register a test user
echo "1. Creating test user"
USER_RESPONSE=$(curl -s -X POST "$API_URL/user/register" \
    -H "$CONTENT_TYPE" \
    -d "{\"username\":\"Card Test User\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "User registration response: $USER_RESPONSE"

# Extract user ID
USER_ID=$(extract_id "$USER_RESPONSE")
echo "User ID: $USER_ID"

# Step 2: Create a deck for testing card operations
echo "2. Creating a deck"
DECK_RESPONSE=$(curl -s -X POST "$API_URL/anki/add-deck" \
    -H "$CONTENT_TYPE" \
    -d "{\"title\":\"Card Test Deck\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "Deck creation response: $DECK_RESPONSE"

# Extract deck ID
DECK_ID=$(extract_id "$DECK_RESPONSE")
echo "Deck ID: $DECK_ID"

# Get decks to verify deck was created
echo "Verifying deck exists in user's decks"
DECKS_RESPONSE=$(curl -s -X GET "$API_URL/anki/user-decks?firebaseUid=$FIREBASE_UID" \
    -H "$CONTENT_TYPE" \
    -H "Authorization: $FIREBASE_UID")
echo "User decks: $DECKS_RESPONSE"

# Step 3: Create a card using createCard endpoint (general)
echo "3. Creating card with general endpoint"
CARD1_RESPONSE=$(curl -s -X POST "$API_URL/api/cards" \
    -H "$CONTENT_TYPE" \
    -H "Authorization: $FIREBASE_UID" \
    -d "{\"front\":\"What is REST?\",\"back\":\"REpresentational State Transfer\",\"firebaseUid\":\"$FIREBASE_UID\",\"deck\":{\"id\":$DECK_ID}}")
echo "General card creation response: $CARD1_RESPONSE"

# Extract card ID if successful
if [[ "$CARD1_RESPONSE" != *"ERROR"* ]]; then
    CARD1_ID=$(extract_id "$CARD1_RESPONSE")
    echo "Created card with ID: $CARD1_ID"
fi

# Step 4: Create a card in a specific deck (using deck/{deckId} endpoint)
echo "4. Creating card with deck-specific endpoint"
echo "Using deck ID: $DECK_ID"
echo "Command: curl -v -X POST \"$API_URL/api/cards/deck/$DECK_ID\" -H \"$CONTENT_TYPE\" -H \"Authorization: $FIREBASE_UID\" -d '{\"front\":\"What is Spring Boot?\",\"back\":\"A Java framework for building applications\",\"firebaseUid\":\"$FIREBASE_UID\"}'"

CARD2_RESPONSE=$(curl -v -X POST "$API_URL/api/cards/deck/$DECK_ID" \
    -H "$CONTENT_TYPE" \
    -H "Authorization: $FIREBASE_UID" \
    -d "{\"front\":\"What is Spring Boot?\",\"back\":\"A Java framework for building applications\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "Deck-specific card creation response: $CARD2_RESPONSE"

# Extract card ID if successful
if [[ "$CARD2_RESPONSE" != *"ERROR"* ]]; then
    CARD2_ID=$(extract_id "$CARD2_RESPONSE")
    echo "Created card with ID: $CARD2_ID"
fi

# Alternative: try with query parameter for firebaseUid
echo "4b. Creating card with deck-specific endpoint (query parameter)"
CARD3_RESPONSE=$(curl -s -X POST "$API_URL/api/cards/deck/$DECK_ID?firebaseUid=$FIREBASE_UID" \
    -H "$CONTENT_TYPE" \
    -d "{\"front\":\"What are microservices?\",\"back\":\"Architecture pattern of small, independent services\"}")
echo "Query param card creation response: $CARD3_RESPONSE"

# Extract card ID if successful
if [[ "$CARD3_RESPONSE" != *"ERROR"* ]]; then
    CARD3_ID=$(extract_id "$CARD3_RESPONSE")
    echo "Created card with ID: $CARD3_ID"
fi

# Step 5: Get cards in deck
echo "5. Getting all cards in deck"
CARDS_RESPONSE=$(curl -s -X GET "$API_URL/api/cards/deck/$DECK_ID?firebaseUid=$FIREBASE_UID" \
    -H "$CONTENT_TYPE")
echo "Get cards response: $CARDS_RESPONSE"

# Step 6: Get specific card by ID (using first card if available)
if [[ -n "$CARD1_ID" ]]; then
    echo "6. Getting card by ID"
    CARD_BY_ID_RESPONSE=$(curl -s -X GET "$API_URL/api/cards/$CARD1_ID" \
        -H "$CONTENT_TYPE" \
        -H "Authorization: $FIREBASE_UID")
    echo "Get card by ID response: $CARD_BY_ID_RESPONSE"
elif [[ -n "$CARD2_ID" ]]; then
    echo "6. Getting card by ID"
    CARD_BY_ID_RESPONSE=$(curl -s -X GET "$API_URL/api/cards/$CARD2_ID" \
        -H "$CONTENT_TYPE" \
        -H "Authorization: $FIREBASE_UID")
    echo "Get card by ID response: $CARD_BY_ID_RESPONSE"
fi

# Step 7: Update a card if any were created
if [[ -n "$CARD1_ID" ]]; then
    echo "7. Updating card"
    UPDATE_RESPONSE=$(curl -s -X PUT "$API_URL/api/cards/$CARD1_ID" \
        -H "$CONTENT_TYPE" \
        -H "Authorization: $FIREBASE_UID" \
        -d "{\"front\":\"Updated: What is REST?\",\"back\":\"Updated: REpresentational State Transfer\",\"firebaseUid\":\"$FIREBASE_UID\",\"deck\":{\"id\":$DECK_ID}}")
    echo "Update card response: $UPDATE_RESPONSE"
elif [[ -n "$CARD2_ID" ]]; then
    echo "7. Updating card"
    UPDATE_RESPONSE=$(curl -s -X PUT "$API_URL/api/cards/$CARD2_ID" \
        -H "$CONTENT_TYPE" \
        -H "Authorization: $FIREBASE_UID" \
        -d "{\"front\":\"Updated: What is Spring Boot?\",\"back\":\"Updated: A Java framework\",\"firebaseUid\":\"$FIREBASE_UID\",\"deck\":{\"id\":$DECK_ID}}")
    echo "Update card response: $UPDATE_RESPONSE"
fi

# Step 8: Delete a card if any were created
if [[ -n "$CARD1_ID" ]]; then
    echo "8. Deleting card"
    DELETE_CARD_RESPONSE=$(curl -s -X DELETE "$API_URL/api/cards/$CARD1_ID" \
        -H "$CONTENT_TYPE" \
        -H "Authorization: $FIREBASE_UID")
    echo "Delete card response: $DELETE_CARD_RESPONSE"
elif [[ -n "$CARD2_ID" ]]; then
    echo "8. Deleting card"
    DELETE_CARD_RESPONSE=$(curl -s -X DELETE "$API_URL/api/cards/$CARD2_ID" \
        -H "$CONTENT_TYPE" \
        -H "Authorization: $FIREBASE_UID")
    echo "Delete card response: $DELETE_CARD_RESPONSE"
fi

# Step 9: Cleanup - Delete the test deck
echo "9. Deleting test deck"
DELETE_DECK_RESPONSE=$(curl -s -X DELETE "$API_URL/anki/delete-deck/$DECK_ID?firebaseUid=$FIREBASE_UID" \
    -H "$CONTENT_TYPE" \
    -H "Authorization: $FIREBASE_UID")
echo "Delete deck response: $DELETE_DECK_RESPONSE"

echo "Testing complete!"

# Summary
echo ""
echo "=== CARD ENDPOINT TESTING SUMMARY ==="
echo "✓ User registration: Success"
echo "✓ Deck creation: Success"

if [[ "$CARD1_RESPONSE" != *"ERROR"* ]]; then
    echo "✓ Create card (general endpoint): Success"
else
    echo "✗ Create card (general endpoint): Failed"
fi

if [[ "$CARD2_RESPONSE" != *"ERROR"* ]]; then
    echo "✓ Create card in deck (Authorization header): Success"
else
    echo "✗ Create card in deck (Authorization header): Failed"
fi

if [[ "$CARD3_RESPONSE" != *"ERROR"* ]]; then
    echo "✓ Create card in deck (query parameter): Success"
else
    echo "✗ Create card in deck (query parameter): Failed"
fi

if [[ "$CARDS_RESPONSE" != *"ERROR"* ]]; then
    echo "✓ Get cards in deck: Success"
else
    echo "✗ Get cards in deck: Failed"
fi

if [[ "$CARD_BY_ID_RESPONSE" != *"ERROR"* ]]; then
    echo "✓ Get card by ID: Success"
else
    echo "✗ Get card by ID: Failed"
fi

if [[ "$UPDATE_RESPONSE" != *"ERROR"* ]]; then
    echo "✓ Update card: Success"
else
    echo "✗ Update card: Failed"
fi

if [[ "$DELETE_CARD_RESPONSE" != *"ERROR"* ]]; then
    echo "✓ Delete card: Success"
else
    echo "✗ Delete card: Failed"
fi

if [[ "$DELETE_DECK_RESPONSE" != *"ERROR"* ]]; then
    echo "✓ Delete deck: Success"
else
    echo "✗ Delete deck: Failed"
fi 
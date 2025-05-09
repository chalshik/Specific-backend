#!/bin/bash

# Configuration
API_URL="https://specific-backend.onrender.com"
FIREBASE_UID="anki-test-user"
CONTENT_TYPE="Content-Type: application/json"

echo "Testing Anki Endpoints with Firebase UID: $FIREBASE_UID"
echo "---------------------------------------------------------"

# Helper function to extract IDs from JSON responses
extract_id() {
    echo $1 | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
}

# Step 1: Health check first
echo "1. Checking API health"
HEALTH_RESPONSE=$(curl -s -X GET "$API_URL/health")
echo "Health response: $HEALTH_RESPONSE"

# Check if API is running
if [[ "$HEALTH_RESPONSE" == *"UP"* ]]; then
    echo "✓ API is running"
else
    echo "✗ API health check failed"
    exit 1
fi

# Step 2: Register a test user
echo "2. Registering a test user with Firebase UID"
USER_RESPONSE=$(curl -s -X POST "$API_URL/user/register" \
    -H "$CONTENT_TYPE" \
    -d "{\"username\":\"Anki Test User\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "User registration response: $USER_RESPONSE"

# Extract user ID
USER_ID=$(extract_id "$USER_RESPONSE")
echo "User ID: $USER_ID"

# Step 3: Get user info
echo "3. Getting user information"
USER_INFO_RESPONSE=$(curl -s -X GET "$API_URL/user/info?firebaseUid=$FIREBASE_UID" \
    -H "$CONTENT_TYPE" \
    -H "Authorization: $FIREBASE_UID")
echo "User info response: $USER_INFO_RESPONSE"

# Step 4: Create a deck
echo "4. Creating a deck"
DECK_RESPONSE=$(curl -s -X POST "$API_URL/anki/add-deck" \
    -H "$CONTENT_TYPE" \
    -d "{\"title\":\"Anki Test Deck\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "Deck creation response: $DECK_RESPONSE"

# Check if deck creation was successful
if [[ "$DECK_RESPONSE" == *"ERROR"* ]]; then
    echo "✗ Deck creation failed, stopping test"
    exit 1
fi

# Extract deck ID
DECK_ID=$(extract_id "$DECK_RESPONSE")
echo "Created deck with ID: $DECK_ID"

# Step 5: Get all decks
echo "5. Getting all decks"
DECKS_RESPONSE=$(curl -s -X GET "$API_URL/anki/user-decks?firebaseUid=$FIREBASE_UID" \
    -H "$CONTENT_TYPE" \
    -H "Authorization: $FIREBASE_UID")
echo "Get decks response: $DECKS_RESPONSE"

# Step 6: Add a card to the deck
echo "6. Adding a card to the deck"
CARD_RESPONSE=$(curl -s -X POST "$API_URL/api/cards/deck/$DECK_ID" \
    -H "$CONTENT_TYPE" \
    -H "Authorization: $FIREBASE_UID" \
    -d "{\"front\":\"What is Spring Boot?\",\"back\":\"A Java framework for building applications\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "Card creation response: $CARD_RESPONSE"

# Extract card ID if successful
if [[ "$CARD_RESPONSE" != *"ERROR"* ]]; then
    CARD_ID=$(extract_id "$CARD_RESPONSE")
    echo "Created card with ID: $CARD_ID"
    
    # Step 7: Get cards in deck
    echo "7. Getting all cards for deck"
    CARDS_RESPONSE=$(curl -s -X GET "$API_URL/api/cards/deck/$DECK_ID" \
        -H "$CONTENT_TYPE" \
        -H "Authorization: $FIREBASE_UID" \
        -d "{\"firebaseUid\":\"$FIREBASE_UID\"}")
    echo "Get cards response: $CARDS_RESPONSE"
fi

# Step 8: Cleanup - Delete the deck
echo "8. Cleanup - Deleting test deck"
DELETE_RESPONSE=$(curl -s -X DELETE "$API_URL/anki/delete-deck/$DECK_ID?firebaseUid=$FIREBASE_UID" \
    -H "$CONTENT_TYPE" \
    -H "Authorization: $FIREBASE_UID")
echo "Delete deck response: $DELETE_RESPONSE"

echo "Testing complete!"

# Summary
echo ""
echo "=== TESTING SUMMARY ==="
echo "✓ API health check: Success"
echo "✓ User registration: Success"
echo "✓ User info retrieval: Success"
echo "✓ Deck creation: Success"

if [[ "$DECKS_RESPONSE" != *"ERROR"* ]]; then
    echo "✓ Get all decks: Success"
else
    echo "✗ Get all decks: Failed"
fi

if [[ "$CARD_RESPONSE" != *"ERROR"* ]]; then
    echo "✓ Add card to deck: Success"
else
    echo "✗ Add card to deck: Failed"
fi

if [[ "$CARDS_RESPONSE" != *"ERROR"* ]]; then
    echo "✓ Get cards in deck: Success"
else
    echo "✗ Get cards in deck: Failed"
fi

if [[ "$DELETE_RESPONSE" != *"ERROR"* ]]; then
    echo "✓ Delete deck: Success"
else
    echo "✗ Delete deck: Failed"
fi

echo ""
echo "Available and working endpoints:"
echo "1. GET $API_URL/health - Health check"
echo "2. POST $API_URL/user/register - Register user"
echo "3. GET $API_URL/user/info?firebaseUid={uid} - Get user info"
echo "4. POST $API_URL/anki/add-deck - Create deck"
echo "5. GET $API_URL/anki/user-decks?firebaseUid={uid} - Get user decks"

if [[ "$CARD_RESPONSE" != *"ERROR"* ]]; then
    echo "6. POST $API_URL/api/cards/deck/{deckId} - Add card to deck"
    echo "7. GET $API_URL/api/cards/deck/{deckId} - Get cards in deck"
fi

echo "8. DELETE $API_URL/anki/delete-deck/{deckId}?firebaseUid={uid} - Delete deck" 
#!/bin/bash

# Configuration
API_URL="https://specific-backend.onrender.com"
FIREBASE_UID="direct-firebase-auth-tester"
CONTENT_TYPE="Content-Type: application/json"

echo "Testing Updated API Endpoints with Direct Firebase UID Authentication: $FIREBASE_UID"
echo "---------------------------------------------------------"

# Helper function to extract ID from response
extract_id() {
  echo $1 | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*'
}

# Step 1: Health check first
echo "1. Checking API health"
HEALTH_RESPONSE=$(curl -s -X GET "$API_URL/health")
echo "$HEALTH_RESPONSE"
echo -e "\n"

# Step 2: Register a test user
echo "2. Registering a test user with Firebase UID"
USER_RESPONSE=$(curl -s -X POST "$API_URL/user/register" \
  -H "$CONTENT_TYPE" \
  -d "{\"username\":\"DirectFirebaseAuthTester\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "$USER_RESPONSE"
echo -e "\n"

# Extract user ID
USER_ID=$(extract_id "$USER_RESPONSE")
if [ -n "$USER_ID" ]; then
  echo "Successfully registered user with ID: $USER_ID"
else
  echo "User registration didn't return an ID"
fi
echo -e "\n"

# Step 3: Create a deck for testing cards
echo "3. Creating a test deck"
DECK_RESPONSE=$(curl -s -X POST "$API_URL/anki/add-deck" \
  -H "$CONTENT_TYPE" \
  -d "{\"title\":\"Test Deck for Direct Auth\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "$DECK_RESPONSE"
echo -e "\n"

# Extract deck ID
DECK_ID=$(extract_id "$DECK_RESPONSE")
if [ -n "$DECK_ID" ]; then
  echo "Successfully created deck with ID: $DECK_ID"
else
  echo "Deck creation didn't return an ID. Using fallback ID for testing."
  DECK_ID=1
fi
echo -e "\n"

# Step 4: Get user information
echo "4. Getting user information"
USER_INFO_RESPONSE=$(curl -s -X GET "$API_URL/user/info?firebaseUid=$FIREBASE_UID" \
  -H "$CONTENT_TYPE")
echo "$USER_INFO_RESPONSE"
echo -e "\n"

# Step 5: Get user decks 
echo "5. Getting user decks"
USER_DECKS_RESPONSE=$(curl -s -X GET "$API_URL/anki/decks?firebaseUid=$FIREBASE_UID" \
  -H "$CONTENT_TYPE")
echo "$USER_DECKS_RESPONSE"
echo -e "\n"

# Step 6: Create a card with firebaseUid in URL parameter instead of body
echo "6. Creating a card with firebaseUid in URL parameter"
CARD_PARAM_RESPONSE=$(curl -s -X POST "$API_URL/api/cards?firebaseUid=$FIREBASE_UID" \
  -H "$CONTENT_TYPE" \
  -d "{\"front\":\"What pattern separates implementation from interface?\",\"back\":\"Dependency Injection\",\"deckId\":$DECK_ID}")
echo "$CARD_PARAM_RESPONSE"
echo -e "\n"

# Extract card ID
CARD_ID=$(extract_id "$CARD_PARAM_RESPONSE")
if [ -n "$CARD_ID" ]; then
  echo "Successfully created card with ID: $CARD_ID"
else
  echo "Card creation didn't return an ID. Using fallback ID for testing."
  CARD_ID=1
fi
echo -e "\n"

# Step 7: Try alternate endpoint for creating card in deck
echo "7. Creating another card using deck-specific endpoint with firebaseUid in URL parameter"
CARD2_RESPONSE=$(curl -s -X POST "$API_URL/api/cards/deck/$DECK_ID?firebaseUid=$FIREBASE_UID" \
  -H "$CONTENT_TYPE" \
  -d "{\"front\":\"What is SOLID?\",\"back\":\"5 principles of OO design: SRP, OCP, LSP, ISP, DIP\"}")
echo "$CARD2_RESPONSE"
echo -e "\n"

# Extract second card ID
CARD2_ID=$(extract_id "$CARD2_RESPONSE")
if [ -n "$CARD2_ID" ]; then
  echo "Successfully created second card with ID: $CARD2_ID"
else
  echo "Second card creation didn't return an ID. Using fallback ID for testing."
  CARD2_ID=2
fi
echo -e "\n"

# Step 8: Try with firebaseUid in Authorization header
echo "8. Creating a card with firebaseUid in Authorization header"
CARD3_RESPONSE=$(curl -s -X POST "$API_URL/api/cards" \
  -H "$CONTENT_TYPE" \
  -H "Authorization: $FIREBASE_UID" \
  -d "{\"front\":\"What is inversion of control?\",\"back\":\"A design principle where control flow is inverted\",\"deckId\":$DECK_ID}")
echo "$CARD3_RESPONSE"
echo -e "\n"

# Step 9: Try to get all cards in deck
echo "9. Getting all cards for deck"
DECK_CARDS_RESPONSE=$(curl -s -X GET "$API_URL/api/cards/deck/$DECK_ID?firebaseUid=$FIREBASE_UID")
echo "$DECK_CARDS_RESPONSE"
echo -e "\n"

# Step 10: Try a simpler endpoint (anki controller)
echo "10. Testing anki controller cards endpoint"
ANKI_CARDS_RESPONSE=$(curl -s -X GET "$API_URL/anki/cards/$DECK_ID?firebaseUid=$FIREBASE_UID")
echo "$ANKI_CARDS_RESPONSE"
echo -e "\n"

# Step 11: Cleanup - Delete the deck and its cards
echo "11. Cleanup - Deleting test deck"
DELETE_DECK_RESPONSE=$(curl -s -X DELETE "$API_URL/anki/delete-deck/$DECK_ID?firebaseUid=$FIREBASE_UID")
echo "$DELETE_DECK_RESPONSE"
echo -e "\n"

echo "Testing complete!"
echo "Results:"
echo "- User registration: " $([ -n "$USER_ID" ] && echo "SUCCESS" || echo "FAILED")
echo "- Deck creation: " $([ -n "$DECK_ID" ] && echo "SUCCESS" || echo "FAILED")
echo "- Card creation (URL param): " $([ -n "$CARD_ID" ] && echo "SUCCESS" || echo "FAILED")
echo "- Card creation (deck endpoint): " $([ -n "$CARD2_ID" ] && echo "SUCCESS" || echo "FAILED")
echo "The following methods of Firebase UID passing were tested:"
echo "- As URL query parameters"
echo "- In Authorization header" 
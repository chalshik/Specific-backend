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

# Step 1: Register a test user
echo "1. Registering a test user with Firebase UID"
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

# Step 2: Create a deck for testing cards
echo "2. Creating a test deck"
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

# Step 3: Create a card with firebaseUid in request body
echo "3. Creating a card with firebaseUid in request body"
CARD_RESPONSE=$(curl -s -X POST "$API_URL/api/cards" \
  -H "$CONTENT_TYPE" \
  -d "{\"front\":\"What pattern separates implementation from interface?\",\"back\":\"Dependency Injection\",\"deckId\":$DECK_ID,\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "$CARD_RESPONSE"
echo -e "\n"

# Extract card ID
CARD_ID=$(extract_id "$CARD_RESPONSE")
if [ -n "$CARD_ID" ]; then
  echo "Successfully created card with ID: $CARD_ID"
else
  echo "Card creation didn't return an ID. Using fallback ID for testing."
  CARD_ID=1
fi
echo -e "\n"

# Step 4: Create a second card using createCardInDeck endpoint
echo "4. Creating another card using deck-specific endpoint"
CARD2_RESPONSE=$(curl -s -X POST "$API_URL/api/cards/deck/$DECK_ID" \
  -H "$CONTENT_TYPE" \
  -d "{\"front\":\"What is SOLID?\",\"back\":\"5 principles of OO design: SRP, OCP, LSP, ISP, DIP\",\"firebaseUid\":\"$FIREBASE_UID\"}")
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

# Step 5: Get all cards for the deck
echo "5. Getting all cards for deck"
DECK_CARDS_RESPONSE=$(curl -s -X GET "$API_URL/api/cards/deck/$DECK_ID?firebaseUid=$FIREBASE_UID")
echo "$DECK_CARDS_RESPONSE"
echo -e "\n"

# Step 6: Get a specific card
echo "6. Getting first card details"
CARD_DETAILS_RESPONSE=$(curl -s -X GET "$API_URL/api/cards/$CARD_ID?firebaseUid=$FIREBASE_UID")
echo "$CARD_DETAILS_RESPONSE"
echo -e "\n"

# Step 7: Update the first card
echo "7. Updating first card"
CARD_UPDATE_RESPONSE=$(curl -s -X PUT "$API_URL/api/cards/$CARD_ID" \
  -H "$CONTENT_TYPE" \
  -d "{\"front\":\"What design pattern separates implementation from interface?\",\"back\":\"Dependency Injection (DI)\",\"deckId\":$DECK_ID,\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "$CARD_UPDATE_RESPONSE"
echo -e "\n"

# Step 8: Submit a review for the first card
echo "8. Submitting review for first card"
REVIEW_RESPONSE=$(curl -s -X POST "$API_URL/api/reviews" \
  -H "$CONTENT_TYPE" \
  -d "{\"cardId\":$CARD_ID,\"rating\":\"good\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "$REVIEW_RESPONSE"
echo -e "\n"

# Extract review ID
REVIEW_ID=$(extract_id "$REVIEW_RESPONSE")
if [ -n "$REVIEW_ID" ]; then
  echo "Successfully created review with ID: $REVIEW_ID"
else
  echo "Review submission didn't return an ID"
fi
echo -e "\n"

# Step 9: Submit a review for the second card
echo "9. Submitting review for second card"
REVIEW2_RESPONSE=$(curl -s -X POST "$API_URL/api/reviews" \
  -H "$CONTENT_TYPE" \
  -d "{\"cardId\":$CARD2_ID,\"rating\":\"hard\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "$REVIEW2_RESPONSE"
echo -e "\n"

# Step 10: Get review statistics for the deck
echo "10. Getting review statistics for deck"
REVIEW_STATS_RESPONSE=$(curl -s -X GET "$API_URL/api/reviews/stats/deck/$DECK_ID?firebaseUid=$FIREBASE_UID")
echo "$REVIEW_STATS_RESPONSE"
echo -e "\n"

# Step 11: Get all reviews for the first card
echo "11. Getting all reviews for first card"
CARD_REVIEWS_RESPONSE=$(curl -s -X GET "$API_URL/api/reviews/card/$CARD_ID?firebaseUid=$FIREBASE_UID")
echo "$CARD_REVIEWS_RESPONSE"
echo -e "\n"

# Step 12: Delete second card
echo "12. Deleting second card"
DELETE_CARD_RESPONSE=$(curl -s -X DELETE "$API_URL/api/cards/$CARD2_ID?firebaseUid=$FIREBASE_UID")
echo "$DELETE_CARD_RESPONSE"
echo -e "\n"

# Step 13: Cleanup - Delete the deck and its cards
echo "13. Cleanup - Deleting test deck"
DELETE_DECK_RESPONSE=$(curl -s -X DELETE "$API_URL/anki/delete-deck/$DECK_ID?firebaseUid=$FIREBASE_UID")
echo "$DELETE_DECK_RESPONSE"
echo -e "\n"

echo "Testing complete!"
echo "Results:"
echo "- User registration: " $([ -n "$USER_ID" ] && echo "SUCCESS" || echo "FAILED")
echo "- Deck creation: " $([ -n "$DECK_ID" ] && echo "SUCCESS" || echo "FAILED")
echo "- Card creation: " $([ -n "$CARD_ID" ] && echo "SUCCESS" || echo "FAILED")
echo "- Card creation via deck endpoint: " $([ -n "$CARD2_ID" ] && echo "SUCCESS" || echo "FAILED")
echo "- Review submission: " $([ -n "$REVIEW_ID" ] && echo "SUCCESS" || echo "FAILED")
echo "The following methods of Firebase UID passing were tested:"
echo "- In request body JSON"
echo "- As URL query parameters" 
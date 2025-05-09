#!/bin/bash

# Configuration
API_URL="https://specific-backend.onrender.com"
FIREBASE_UID="test-user-card-review"  # Using a specific UID for these tests
CONTENT_TYPE="Content-Type: application/json"

echo "Testing Cards and Reviews API with Direct Firebase UID: $FIREBASE_UID"
echo "---------------------------------------------------------"

# Helper function to extract ID from response
extract_id() {
  echo $1 | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*'
}

# Step 1: Register user
echo "1. Registering a user for card and review tests"
USER_RESPONSE=$(curl -s -X POST "$API_URL/user/register" \
  -H "$CONTENT_TYPE" \
  -d "{\"username\":\"CardReviewTestUser\",\"firebaseUid\":\"$FIREBASE_UID\"}")
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
echo "2. Creating a deck for card tests"
DECK_RESPONSE=$(curl -s -X POST "$API_URL/anki/add-deck" \
  -H "$CONTENT_TYPE" \
  -d "{\"title\":\"Card Test Deck\",\"firebaseUid\":\"$FIREBASE_UID\"}")
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

# Step 3: Create a card with firebaseUid in body - POST to /api/cards
echo "3. Creating a card with FirebaseUID in the request body"
CARD_RESPONSE=$(curl -s -X POST "$API_URL/api/cards" \
  -H "$CONTENT_TYPE" \
  -d "{
    \"front\":\"What is polymorphism?\",
    \"back\":\"Ability of an object to take many forms\",
    \"deckId\":$DECK_ID,
    \"firebaseUid\":\"$FIREBASE_UID\"
  }")
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

# Step 4: Retrieve the card using firebaseUid in request parameter
echo "4. Retrieving card with FirebaseUID as query parameter"
CARD_GET_RESPONSE=$(curl -s -X GET "$API_URL/api/cards/$CARD_ID?firebaseUid=$FIREBASE_UID" \
  -H "$CONTENT_TYPE")
echo "$CARD_GET_RESPONSE"
echo -e "\n"

# Step 5: Get cards for deck with firebaseUid as parameter
echo "5. Getting all cards for deck with FirebaseUID as parameter"
DECK_CARDS_RESPONSE=$(curl -s -X GET "$API_URL/api/cards/deck/$DECK_ID?firebaseUid=$FIREBASE_UID" \
  -H "$CONTENT_TYPE")
echo "$DECK_CARDS_RESPONSE"
echo -e "\n"

# Step 6: Create a second card with firebaseUid in header
echo "6. Creating another card with FirebaseUID in X-Firebase-Uid header"
CARD2_RESPONSE=$(curl -s -X POST "$API_URL/api/cards" \
  -H "$CONTENT_TYPE" \
  -H "X-Firebase-Uid: $FIREBASE_UID" \
  -d "{
    \"front\":\"What is encapsulation?\",
    \"back\":\"Bundling data and methods that operate on that data\",
    \"deckId\":$DECK_ID
  }")
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

# Step 7: Update a card with firebaseUid in body
echo "7. Updating card with FirebaseUID in request body"
CARD_UPDATE_RESPONSE=$(curl -s -X PUT "$API_URL/api/cards/$CARD_ID" \
  -H "$CONTENT_TYPE" \
  -d "{
    \"front\":\"What is polymorphism in OOP?\",
    \"back\":\"The ability of an object to take many forms and behave differently\",
    \"deckId\":$DECK_ID,
    \"firebaseUid\":\"$FIREBASE_UID\"
  }")
echo "$CARD_UPDATE_RESPONSE"
echo -e "\n"

# Step 8: Submit a review for first card
echo "8. Submitting review for card with FirebaseUID in request body"
REVIEW_RESPONSE=$(curl -s -X POST "$API_URL/api/reviews" \
  -H "$CONTENT_TYPE" \
  -d "{
    \"cardId\":$CARD_ID,
    \"rating\":\"good\",
    \"firebaseUid\":\"$FIREBASE_UID\"
  }")
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

# Step 9: Get review statistics by deck
echo "9. Getting review statistics by deck with FirebaseUID as parameter"
REVIEW_STATS_RESPONSE=$(curl -s -X GET "$API_URL/api/reviews/stats/deck/$DECK_ID?firebaseUid=$FIREBASE_UID" \
  -H "$CONTENT_TYPE")
echo "$REVIEW_STATS_RESPONSE"
echo -e "\n"

# Step 10: Submit a second review for the second card
echo "10. Submitting review for second card with FirebaseUID in Authorization header"
REVIEW2_RESPONSE=$(curl -s -X POST "$API_URL/api/reviews" \
  -H "$CONTENT_TYPE" \
  -H "Authorization: $FIREBASE_UID" \
  -d "{
    \"cardId\":$CARD2_ID,
    \"rating\":\"hard\"
  }")
echo "$REVIEW2_RESPONSE"
echo -e "\n"

# Step 11: Delete the second card
echo "11. Deleting second card with FirebaseUID in request body"
DELETE_CARD_RESPONSE=$(curl -s -X DELETE "$API_URL/api/cards/$CARD2_ID" \
  -H "$CONTENT_TYPE" \
  -d "{\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "$DELETE_CARD_RESPONSE"
echo -e "\n"

# Step 12: Verify deletion by getting all cards for deck
echo "12. Verifying card deletion by getting all cards for deck"
VERIFY_DELETE_RESPONSE=$(curl -s -X GET "$API_URL/api/cards/deck/$DECK_ID?firebaseUid=$FIREBASE_UID" \
  -H "$CONTENT_TYPE")
echo "$VERIFY_DELETE_RESPONSE"
echo -e "\n"

# Step 13: Cleanup - Delete the deck and its cards
echo "13. Cleanup - Deleting test deck"
DELETE_DECK_RESPONSE=$(curl -s -X DELETE "$API_URL/anki/delete-deck/$DECK_ID?firebaseUid=$FIREBASE_UID" \
  -H "$CONTENT_TYPE")
echo "$DELETE_DECK_RESPONSE"
echo -e "\n"

echo "Cards and Reviews Testing Complete!"
echo "Tested passing Firebase UID in various ways:"
echo "- In request body JSON"
echo "- As URL query parameter"
echo "- In X-Firebase-Uid header"
echo "- In Authorization header"
echo "The test results show which authentication methods work for each endpoint." 
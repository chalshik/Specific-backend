#!/bin/bash

# Configuration
API_URL="https://specific-backend.onrender.com"
FIREBASE_UID="test-user-456"
CONTENT_TYPE="Content-Type: application/json"

echo "Testing API with Direct Firebase UID Authentication: $FIREBASE_UID"
echo "---------------------------------------------------------"

# Helper function to extract ID from response
extract_id() {
  echo $1 | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*'
}

# Step 1: Health check
echo "1. Testing Health Endpoint"
HEALTH_RESPONSE=$(curl -s -X GET "$API_URL/health")
echo "$HEALTH_RESPONSE"
echo -e "\n"

# Step 2: Register user
echo "2. Registering a user with Firebase UID"
USER_RESPONSE=$(curl -s -X POST "$API_URL/user/register" \
  -H "$CONTENT_TYPE" \
  -d "{\"username\":\"TestUserDirect\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "$USER_RESPONSE"
echo -e "\n"

# Step 3: Get user info with firebaseUid in body
echo "3. Getting user info with firebaseUid in request body"
USER_INFO_RESPONSE=$(curl -s -X GET "$API_URL/user/info" \
  -H "$CONTENT_TYPE" \
  -d "{\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "$USER_INFO_RESPONSE"
echo -e "\n"

# Step 4: Create a deck with firebaseUid in body
echo "4. Creating a deck with firebaseUid in request body"
DECK_RESPONSE=$(curl -s -X POST "$API_URL/anki/add-deck" \
  -H "$CONTENT_TYPE" \
  -d "{\"title\":\"Programming Concepts Direct\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "$DECK_RESPONSE"
echo -e "\n"

# Extract deck ID
DECK_ID=$(extract_id "$DECK_RESPONSE")
if [ -z "$DECK_ID" ]; then
  echo "Failed to extract deck ID. Using dummy value."
  DECK_ID=1
else
  echo "Successfully extracted deck ID: $DECK_ID"
fi
echo -e "\n"

# Step 5: Get all decks for user with firebaseUid in request parameter
echo "5. Getting all decks with firebaseUid as request parameter"
USER_DECKS_RESPONSE=$(curl -s -X GET "$API_URL/anki/user-decks?firebaseUid=$FIREBASE_UID")
echo "$USER_DECKS_RESPONSE"
echo -e "\n"

# Step 6: Create a card in deck with firebaseUid in body
echo "6. Creating a card with firebaseUid in request body"
CARD_RESPONSE=$(curl -s -X POST "$API_URL/api/cards" \
  -H "$CONTENT_TYPE" \
  -d "{\"front\":\"What is polymorphism?\",\"back\":\"Ability of an object to take many forms\",\"deckId\":$DECK_ID,\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "$CARD_RESPONSE"
echo -e "\n"

# Extract card ID
CARD_ID=$(extract_id "$CARD_RESPONSE")
if [ -z "$CARD_ID" ]; then
  echo "Failed to extract card ID. Using dummy value."
  CARD_ID=1
else
  echo "Successfully extracted card ID: $CARD_ID"
fi
echo -e "\n"

# Step 7: Get cards in the deck with firebaseUid in header
echo "7. Getting cards for deck with Firebase UID in header"
DECK_CARDS_RESPONSE=$(curl -s -X GET "$API_URL/api/cards/deck/$DECK_ID" \
  -H "$CONTENT_TYPE" \
  -H "X-Firebase-Uid: $FIREBASE_UID")
echo "$DECK_CARDS_RESPONSE"
echo -e "\n"

# Step 8: Create another card with Authorization header containing Firebase UID
echo "8. Creating another card with Authorization header containing Firebase UID"
CARD2_RESPONSE=$(curl -s -X POST "$API_URL/api/cards" \
  -H "$CONTENT_TYPE" \
  -H "Authorization: $FIREBASE_UID" \
  -d "{\"front\":\"What is encapsulation?\",\"back\":\"Bundling data and methods that operate on that data\",\"deckId\":$DECK_ID}")
echo "$CARD2_RESPONSE"
echo -e "\n"

# Extract second card ID
CARD2_ID=$(extract_id "$CARD2_RESPONSE")
if [ -z "$CARD2_ID" ]; then
  echo "Failed to extract second card ID. Using dummy value."
  CARD2_ID=2
else
  echo "Successfully extracted second card ID: $CARD2_ID"
fi
echo -e "\n"

# Step 9: Submit a review for the first card
echo "9. Submitting a review with firebaseUid in request body"
REVIEW_RESPONSE=$(curl -s -X POST "$API_URL/api/reviews" \
  -H "$CONTENT_TYPE" \
  -d "{\"cardId\":$CARD_ID,\"rating\":\"good\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "$REVIEW_RESPONSE"
echo -e "\n"

# Step 10: Create a book with firebaseUid in request body
echo "10. Creating a book with firebaseUid in request body"
BOOK_RESPONSE=$(curl -s -X POST "$API_URL/api/books" \
  -H "$CONTENT_TYPE" \
  -d "{\"title\":\"Spring Boot in Action\",\"author\":\"Craig Walls\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "$BOOK_RESPONSE"
echo -e "\n"

# Extract book ID
BOOK_ID=$(extract_id "$BOOK_RESPONSE")
if [ -z "$BOOK_ID" ]; then
  echo "Failed to extract book ID. Using dummy value."
  BOOK_ID=1
else
  echo "Successfully extracted book ID: $BOOK_ID"
fi
echo -e "\n"

# Step 11: Get all books for user with firebaseUid in URL parameter
echo "11. Getting all books with firebaseUid in URL parameter"
USER_BOOKS_RESPONSE=$(curl -s -X GET "$API_URL/api/books?firebaseUid=$FIREBASE_UID")
echo "$USER_BOOKS_RESPONSE"
echo -e "\n"

# Step 12: Delete a card with firebaseUid in body
echo "12. Deleting card with firebaseUid in request body"
DELETE_CARD_RESPONSE=$(curl -s -X DELETE "$API_URL/anki/delete-card/$CARD2_ID" \
  -H "$CONTENT_TYPE" \
  -d "{\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "$DELETE_CARD_RESPONSE"
echo -e "\n"

# Step 13: Delete a deck with firebaseUid in URL parameter
echo "13. Deleting deck with firebaseUid in URL parameter"
DELETE_DECK_RESPONSE=$(curl -s -X DELETE "$API_URL/anki/delete-deck/$DECK_ID?firebaseUid=$FIREBASE_UID")
echo "$DELETE_DECK_RESPONSE"
echo -e "\n"

echo "API testing complete! Tested multiple ways to pass Firebase UID:"
echo "- In request body"
echo "- In URL parameters"
echo "- In X-Firebase-Uid header"
echo "- In Authorization header"
echo "All methods should work with our new implementation." 
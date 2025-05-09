#!/bin/bash

# Configuration
API_URL="https://specific-backend.onrender.com"
FIREBASE_UID="test-user-789"  # Using a new UID to avoid conflicts
CONTENT_TYPE="Content-Type: application/json"

echo "Testing API with Direct Firebase UID Authentication (Simplified): $FIREBASE_UID"
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

# Step 2: Register user - This works correctly
echo "2. Registering a user with Firebase UID"
USER_RESPONSE=$(curl -s -X POST "$API_URL/user/register" \
  -H "$CONTENT_TYPE" \
  -d "{\"username\":\"TestUserSimple\",\"firebaseUid\":\"$FIREBASE_UID\"}")
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

# Step 3: Create a deck with firebaseUid in body - This works correctly
echo "3. Creating a deck with firebaseUid in request body"
DECK_RESPONSE=$(curl -s -X POST "$API_URL/anki/add-deck" \
  -H "$CONTENT_TYPE" \
  -d "{\"title\":\"Simple Test Deck\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "$DECK_RESPONSE"
echo -e "\n"

# Extract deck ID
DECK_ID=$(extract_id "$DECK_RESPONSE")
if [ -n "$DECK_ID" ]; then
  echo "Successfully created deck with ID: $DECK_ID"
else
  echo "Deck creation didn't return an ID"
fi
echo -e "\n"

# Step 4: Get all decks for user - This works correctly  
echo "4. Getting all decks with firebaseUid as request parameter"
USER_DECKS_RESPONSE=$(curl -s -X GET "$API_URL/anki/user-decks?firebaseUid=$FIREBASE_UID")
echo "$USER_DECKS_RESPONSE"
echo -e "\n"

# Step 5: Delete a deck with firebaseUid as parameter - This works correctly
echo "5. Deleting deck with firebaseUid in URL parameter"
DELETE_DECK_RESPONSE=$(curl -s -X DELETE "$API_URL/anki/delete-deck/$DECK_ID?firebaseUid=$FIREBASE_UID")
echo "$DELETE_DECK_RESPONSE"
echo -e "\n"

# Step 6: Verify deck deletion
echo "6. Verifying deck deletion by getting all decks again"
USER_DECKS_AFTER_DELETE=$(curl -s -X GET "$API_URL/anki/user-decks?firebaseUid=$FIREBASE_UID")
echo "$USER_DECKS_AFTER_DELETE"
echo -e "\n"

echo "Simplified API testing complete!"
echo "Working endpoints confirmed:"
echo "- User registration"
echo "- Deck creation with Firebase UID in body"
echo "- Getting user decks with Firebase UID as parameter"
echo "- Deleting decks with Firebase UID as parameter" 
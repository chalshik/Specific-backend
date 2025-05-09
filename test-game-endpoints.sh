#!/bin/bash

# Configuration
API_URL="https://specific-backend.onrender.com"
FIREBASE_UID="game-test-user-$(date +%s)"  # Unique ID with timestamp
USERNAME="Game Test User"
CONTENT_TYPE="Content-Type: application/json"

echo "===== TESTING GAME CONTROLLER ENDPOINTS ====="
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

# 3. Create a deck with some cards for the game
echo -e "\n3. Creating a deck for game testing"
DECK_RESPONSE=$(curl -s -X POST "$API_URL/anki/add-deck" \
    -H "$CONTENT_TYPE" \
    -d "{\"title\":\"Game Test Deck\",\"firebaseUid\":\"$FIREBASE_UID\"}")
echo "Deck creation response: $DECK_RESPONSE"

if [[ "$DECK_RESPONSE" == *"ERROR"* ]]; then
    print_result "FAILED" "Creating deck failed"
    DECK_ID=""
else
    DECK_ID=$(extract_id "$DECK_RESPONSE")
    print_result "SUCCESS" "Created deck with ID: $DECK_ID"
fi

# 4. Add a few cards to the deck for testing
for i in {1..5}; do
    echo -e "\n4.$i Creating card $i for game testing"
    CARD_RESPONSE=$(curl -s -X POST "$API_URL/api/cards/deck/$DECK_ID" \
        -H "$CONTENT_TYPE" \
        -H "Authorization: $FIREBASE_UID" \
        -d "{\"front\":\"Game test question $i?\",\"back\":\"Answer $i\",\"firebaseUid\":\"$FIREBASE_UID\"}")
    
    if [[ "$CARD_RESPONSE" == *"ERROR"* ]]; then
        print_result "FAILED" "Creating card $i failed"
    else
        CARD_ID=$(extract_id "$CARD_RESPONSE")
        print_result "SUCCESS" "Created card $i with ID: $CARD_ID"
    fi
done

# 5. Test creating a game room
echo -e "\n5. Creating a game room"
GAME_ROOM_RESPONSE=$(curl -s -X POST "$API_URL/api/game/room" \
    -H "$CONTENT_TYPE" \
    -H "Authorization: $FIREBASE_UID")
echo "Game room creation response: $GAME_ROOM_RESPONSE"

if [[ "$GAME_ROOM_RESPONSE" == *"roomCode"* ]]; then
    ROOM_CODE=$(echo "$GAME_ROOM_RESPONSE" | grep -o '"roomCode":"[^"]*"' | cut -d'"' -f4)
    print_result "SUCCESS" "Created game room with code: $ROOM_CODE"
else
    print_result "FAILED" "Creating game room failed"
fi

# Note about WebSocket testing
echo -e "\n6. WebSocket functionality testing"
echo "Note: WebSocket functionality testing requires a browser client."
echo "The following WebSocket endpoints are available for testing:"
echo "- Connect endpoint: $API_URL/ws-game"
echo "- Join room: /app/game.join"
echo "- Start game: /app/game.start"
echo "- Submit answer: /app/game.submitAnswer"
echo "- Next round: /app/game.nextRound"
echo "- Leave room: /app/game.leave"

# 7. Cleanup - Delete the test deck
if [ -n "$DECK_ID" ]; then
    echo -e "\n7. Cleaning up - deleting test deck"
    DELETE_RESPONSE=$(curl -s -X DELETE "$API_URL/anki/delete-deck/$DECK_ID?firebaseUid=$FIREBASE_UID" \
        -H "$CONTENT_TYPE" \
        -H "Authorization: $FIREBASE_UID")
    echo "Delete deck response: $DELETE_RESPONSE"

    if [[ "$DELETE_RESPONSE" == *"SUCCESS"* ]]; then
        print_result "SUCCESS" "Deleted deck with ID: $DECK_ID"
    else
        print_result "FAILED" "Deleting deck failed"
    fi
fi

echo -e "\n===== TESTING COMPLETE ====="
echo "Game controller REST endpoints tested with Firebase UID: $FIREBASE_UID"
echo "WebSocket endpoints require manual testing with a client." 
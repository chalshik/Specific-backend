# Individual curl Commands for Testing Specific API

Below are individual curl commands you can run one by one to test each endpoint of the Specific API. These commands use a consistent Firebase UID to identify the user across requests.

## Configuration Variables
First, set these variables in your terminal:

```bash
API_URL="https://specific-backend.onrender.com"
FIREBASE_UID="test-user-123"
```

## 1. User Management

### Register a new user
```bash
curl -X POST "$API_URL/user/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"TestUser\",\"firebaseUid\":\"$FIREBASE_UID\"}"
```

### Get current user info
```bash
curl -X GET "$API_URL/user/info?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID"
```

### Update user
```bash
curl -X PUT "$API_URL/user/update?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID" \
  -d "{\"username\":\"UpdatedUsername\"}"
```

## 2. Deck Management

### Create a deck
```bash
curl -X POST "$API_URL/anki/add-deck?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID" \
  -d "{\"title\":\"Test Deck\"}"
```

### Get user decks
```bash
curl -X GET "$API_URL/anki/user-decks?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID"
```

### Delete a deck (replace DECK_ID with the actual ID)
```bash
curl -X DELETE "$API_URL/anki/delete-deck/DECK_ID?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID"
```

## 3. Card Management

### Create a card (replace DECK_ID with the actual ID)
```bash
curl -X POST "$API_URL/api/cards/deck/DECK_ID?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID" \
  -d "{\"front\":\"Test question\",\"back\":\"Test answer\"}"
```

### Get cards to study in a deck (replace DECK_ID with the actual ID)
```bash
curl -X GET "$API_URL/api/cards/study/deck/DECK_ID?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID"
```

### Update a card (replace CARD_ID with the actual ID)
```bash
curl -X PUT "$API_URL/api/cards/CARD_ID?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID" \
  -d "{\"front\":\"Updated question\",\"back\":\"Updated answer\"}"
```

### Delete a card (replace CARD_ID with the actual ID)
```bash
curl -X DELETE "$API_URL/api/cards/CARD_ID?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID"
```

## 4. Review Management

### Submit a review (replace CARD_ID with the actual ID)
```bash
curl -X POST "$API_URL/api/reviews?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID" \
  -d "{\"cardId\":CARD_ID,\"rating\":\"good\"}"
```

### Get card review history (replace CARD_ID with the actual ID)
```bash
curl -X GET "$API_URL/api/reviews/history/card/CARD_ID?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID"
```

### Get deck review statistics (replace DECK_ID with the actual ID)
```bash
curl -X GET "$API_URL/api/reviews/stats/deck/DECK_ID?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID"
```

## 5. Book Management

### Create a book
```bash
curl -X POST "$API_URL/api/books?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID" \
  -d "{\"title\":\"Test Book\",\"author\":\"Test Author\",\"language\":\"en\"}"
```

### Get user books
```bash
curl -X GET "$API_URL/api/books?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID"
```

### Get book by ID (replace BOOK_ID with the actual ID)
```bash
curl -X GET "$API_URL/api/books/BOOK_ID?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID"
```

### Update a book (replace BOOK_ID with the actual ID)
```bash
curl -X PUT "$API_URL/api/books/BOOK_ID?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID" \
  -d "{\"title\":\"Updated Book\",\"author\":\"Updated Author\",\"language\":\"en\"}"
```

### Search books
```bash
curl -X GET "$API_URL/api/books/search?title=Updated&firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID"
```

### Delete a book (replace BOOK_ID with the actual ID)
```bash
curl -X DELETE "$API_URL/api/books/BOOK_ID?firebaseUid=$FIREBASE_UID" \
  -H "Content-Type: application/json" \
  -H "X-Firebase-Uid: $FIREBASE_UID"
```

## 6. Health Check

### Check API health
```bash
curl -X GET "$API_URL/health" \
  -H "Content-Type: application/json"
``` 
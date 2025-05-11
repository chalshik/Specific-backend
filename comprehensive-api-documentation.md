# Specific Spring Backend API Documentation

This documentation provides detailed information about all available endpoints in the Specific language learning application's backend.

## Table of Contents

1. [Authentication and User Management](#authentication-and-user-management)
2. [Decks and Cards](#decks-and-cards)
3. [Reviews and Spaced Repetition](#reviews-and-spaced-repetition)
4. [Books](#books)
5. [Translation](#translation)
6. [Game](#game)
7. [System](#system)

---

## Authentication and User Management

### User Endpoints

**Base URL:** `/user`

#### Get User Info

Retrieves information about the currently authenticated user.

**URL:** `/user/info`

**Method:** `GET`

**Headers:**
- `X-Firebase-Uid: {your-firebase-uid}` (Required for authentication)

**Successful Response:** (200 OK)
```json
{
  "id": 1,
  "username": "username",
  "email": "user@example.com",
  "firebaseUid": "firebase123",
  "createdAt": "2023-06-19T12:34:56.789Z"
}
```

**Error Response:** (404 Not Found)
```json
{
  "status": "error",
  "message": "User not found"
}
```

#### Update User

Updates information for the currently authenticated user.

**URL:** `/user/update`

**Method:** `PUT`

**Headers:**
- `Content-Type: application/json`
- `X-Firebase-Uid: {your-firebase-uid}` (Required for authentication)

**Request Body:**
```json
{
  "username": "new_username"
}
```

**Successful Response:** (200 OK)
```json
{
  "id": 1,
  "username": "new_username",
  "email": "user@example.com",
  "firebaseUid": "firebase123",
  "createdAt": "2023-06-19T12:34:56.789Z"
}
```

**Error Response:** (404 Not Found)
```json
{
  "status": "error",
  "message": "User not found"
}
```

---

## Decks and Cards

### Deck Endpoints

**Base URL:** `/anki`

#### Create Deck

Creates a new deck for the authenticated user.

**URL:** `/anki`

**Method:** `POST`

**Headers:**
- `Content-Type: application/json`
- `X-Firebase-Uid: {your-firebase-uid}` (Required for authentication)

**Request Body:**
```json
{
  "name": "Korean Basics",
  "description": "Basic Korean vocabulary"
}
```

**Successful Response:** (200 OK)
```json
{
  "id": 1,
  "name": "Korean Basics",
  "description": "Basic Korean vocabulary",
  "createdAt": "2023-06-19T12:34:56.789Z",
  "userId": 1,
  "cardCount": 0
}
```

#### Get User Decks

Retrieves all decks for the authenticated user.

**URL:** `/anki/decks`

**Method:** `GET`

**Headers:**
- `X-Firebase-Uid: {your-firebase-uid}` (Required for authentication)

**Query Parameters:**
- `firebaseUid`: (Optional) Firebase UID for the user

**Successful Response:** (200 OK)
```json
[
  {
    "id": 1,
    "name": "Korean Basics",
    "description": "Basic Korean vocabulary",
    "createdAt": "2023-06-19T12:34:56.789Z",
    "userId": 1,
    "cardCount": 10
  },
  {
    "id": 2,
    "name": "Korean Intermediate",
    "description": "Intermediate Korean vocabulary",
    "createdAt": "2023-06-19T12:34:56.789Z",
    "userId": 1,
    "cardCount": 20
  }
]
```

#### Get Deck by ID

Retrieves a specific deck by its ID.

**URL:** `/anki/decks/{deckId}`

**Method:** `GET`

**Headers:**
- `X-Firebase-Uid: {your-firebase-uid}` (Required for authentication)

**Path Parameters:**
- `deckId`: ID of the deck to retrieve

**Successful Response:** (200 OK)
```json
{
  "id": 1,
  "name": "Korean Basics",
  "description": "Basic Korean vocabulary",
  "createdAt": "2023-06-19T12:34:56.789Z",
  "userId": 1,
  "cardCount": 10
}
```

### Card Endpoints

**Base URL:** `/api/cards`

#### Get Cards to Study by Deck

Retrieves cards that are due for review in a specific deck, according to the spaced repetition algorithm.

**URL:** `/api/cards/study/deck/{deckId}`

**Method:** `GET`

**Headers:**
- `X-Firebase-Uid: {your-firebase-uid}` (Required for authentication)

**Path Parameters:**
- `deckId`: ID of the deck to retrieve cards from

**Query Parameters:**
- `firebaseUid`: (Optional) Firebase UID for the user

**Successful Response:** (200 OK)
```json
[
  {
    "id": 1,
    "front": "안녕하세요",
    "back": "Hello",
    "deckId": 1,
    "nextReviewAt": "2023-06-19T12:34:56.789Z"
  },
  {
    "id": 2,
    "front": "감사합니다",
    "back": "Thank you",
    "deckId": 1,
    "nextReviewAt": "2023-06-19T12:34:56.789Z"
  }
]
```

#### Create Card

Creates a new card in a specific deck.

**URL:** `/api/cards/deck/{deckId}`

**Method:** `POST`

**Headers:**
- `Content-Type: application/json`
- `X-Firebase-Uid: {your-firebase-uid}` (Required for authentication)

**Path Parameters:**
- `deckId`: ID of the deck to add the card to

**Request Body:**
```json
{
  "front": "안녕하세요",
  "back": "Hello"
}
```

**Successful Response:** (200 OK)
```json
{
  "id": 1,
  "front": "안녕하세요",
  "back": "Hello",
  "deckId": 1,
  "createdAt": "2023-06-19T12:34:56.789Z"
}
```

#### Update Card

Updates an existing card.

**URL:** `/api/cards/{cardId}`

**Method:** `PUT`

**Headers:**
- `Content-Type: application/json`
- `X-Firebase-Uid: {your-firebase-uid}` (Required for authentication)

**Path Parameters:**
- `cardId`: ID of the card to update

**Request Body:**
```json
{
  "front": "안녕하세요",
  "back": "Hello (updated)"
}
```

**Successful Response:** (200 OK)
```json
{
  "id": 1,
  "front": "안녕하세요",
  "back": "Hello (updated)",
  "deckId": 1,
  "updatedAt": "2023-06-19T12:34:56.789Z"
}
```

---

## Reviews and Spaced Repetition

### Review Endpoints

**Base URL:** `/api/reviews`

#### Submit Review

Submits a review for a card, updating its spaced repetition schedule.

**URL:** `/api/reviews`

**Method:** `POST`

**Headers:**
- `Content-Type: application/json`
- `X-Firebase-Uid: {your-firebase-uid}` (Required for authentication)

**Request Body:**
```json
{
  "cardId": 1,
  "rating": "GOOD"  // Options: AGAIN, HARD, GOOD, EASY
}
```

**Successful Response:** (200 OK)
```json
{
  "id": 1,
  "cardId": 1,
  "rating": "GOOD",
  "interval": 3,
  "nextReviewAt": "2023-06-22T12:34:56.789Z"
}
```

#### Get Card Review History

Retrieves the review history for a specific card.

**URL:** `/api/reviews/card/{cardId}`

**Method:** `GET`

**Headers:**
- `X-Firebase-Uid: {your-firebase-uid}` (Required for authentication)

**Path Parameters:**
- `cardId`: ID of the card to retrieve reviews for

**Successful Response:** (200 OK)
```json
[
  {
    "id": 1,
    "cardId": 1,
    "rating": "GOOD",
    "interval": 3,
    "reviewedAt": "2023-06-19T12:34:56.789Z",
    "nextReviewAt": "2023-06-22T12:34:56.789Z"
  },
  {
    "id": 2,
    "cardId": 1,
    "rating": "EASY",
    "interval": 7,
    "reviewedAt": "2023-06-22T12:34:56.789Z",
    "nextReviewAt": "2023-06-29T12:34:56.789Z"
  }
]
```

---

## Books

### Book Endpoints

**Base URL:** `/api/books`

#### Create Book

Creates a new book for the authenticated user.

**URL:** `/api/books`

**Method:** `POST`

**Headers:**
- `Content-Type: application/json`
- `X-Firebase-Uid: {your-firebase-uid}` (Required for authentication)

**Request Body:**
```json
{
  "title": "Korean Grammar Guide",
  "author": "Author Name",
  "description": "Comprehensive Korean grammar guide"
}
```

**Successful Response:** (200 OK)
```json
{
  "id": 1,
  "title": "Korean Grammar Guide",
  "author": "Author Name",
  "description": "Comprehensive Korean grammar guide",
  "userId": 1,
  "createdAt": "2023-06-19T12:34:56.789Z"
}
```

#### Get User Books

Retrieves all books for the authenticated user.

**URL:** `/api/books`

**Method:** `GET`

**Headers:**
- `X-Firebase-Uid: {your-firebase-uid}` (Required for authentication)

**Query Parameters:**
- `firebaseUid`: (Optional) Firebase UID for the user

**Successful Response:** (200 OK)
```json
[
  {
    "id": 1,
    "title": "Korean Grammar Guide",
    "author": "Author Name",
    "description": "Comprehensive Korean grammar guide",
    "userId": 1,
    "createdAt": "2023-06-19T12:34:56.789Z"
  },
  {
    "id": 2,
    "title": "Korean Vocabulary Builder",
    "author": "Author Name",
    "description": "Vocabulary builder for Korean learners",
    "userId": 1,
    "createdAt": "2023-06-19T12:34:56.789Z"
  }
]
```

#### Search Books

Searches for books by title.

**URL:** `/api/books/search`

**Method:** `GET`

**Headers:**
- `X-Firebase-Uid: {your-firebase-uid}` (Required for authentication)

**Query Parameters:**
- `title`: Title to search for
- `firebaseUid`: (Optional) Firebase UID for the user

**Successful Response:** (200 OK)
```json
[
  {
    "id": 1,
    "title": "Korean Grammar Guide",
    "author": "Author Name",
    "description": "Comprehensive Korean grammar guide",
    "userId": 1,
    "createdAt": "2023-06-19T12:34:56.789Z"
  }
]
```

---

## Translation

### Translation Endpoints

**Base URL:** `/translation`

#### Get Translation

Translates text between languages.

**URL:** `/translation`

**Method:** `POST`

**Headers:**
- `Content-Type: application/json`

**Request Body:**
```json
{
  "text": "안녕하세요",
  "sourceLanguage": "ko",
  "targetLanguage": "en"
}
```

**Successful Response:** (200 OK)
```json
{
  "originalText": "안녕하세요",
  "translatedText": "Hello",
  "sourceLanguage": "ko",
  "targetLanguage": "en"
}
```

#### Test Translation API

Tests if the translation API is working.

**URL:** `/translation`

**Method:** `GET`

**Successful Response:** (200 OK)
```
Translation API is working!
```

---

## Game

### Game Room Endpoints

**Base URL:** `/api/game`

#### Create Game Room

Creates a new game room for multiplayer flashcard games.

**URL:** `/api/game/room`

**Method:** `POST`

**Headers:**
- `X-Firebase-Uid: {your-firebase-uid}` (Required for authentication)

**Query Parameters:**
- `firebaseUid`: (Optional) Firebase UID for the user

**Successful Response:** (200 OK)
```json
{
  "roomCode": "A1B2C3",
  "host": {
    "id": "test-user-123",
    "username": "Player-test"
  },
  "players": [
    {
      "id": "test-user-123",
      "username": "Player-test"
    }
  ],
  "status": "WAITING",
  "createdAt": "2023-06-19T12:34:56.789Z"
}
```

### WebSocket Game Endpoints

#### Connect to WebSocket

Establishes a WebSocket connection for real-time game communication.

**URL:** `/ws-game`

**Protocol:** SockJS with STOMP

**Example:**
```javascript
const socket = new SockJS('https://specific-backend.onrender.com/ws-game');
const stompClient = Stomp.over(socket);
stompClient.connect({}, frame => {
  console.log('Connected to WebSocket');
  
  // Subscribe to personal messages
  stompClient.subscribe('/user/queue/game', message => {
    const messageBody = JSON.parse(message.body);
    console.log('Received personal message:', messageBody);
  });
});
```

#### Room Join Message

Sends a message to join an existing game room.

**Destination:** `/app/game.join`

**Message Format:**
```json
{
  "type": "ROOM_JOINED",
  "roomCode": "A1B2C3",
  "senderId": "test-user-456",
  "senderUsername": "Player-test"
}
```

**Response:** 
- All users in the room will receive a message on `/topic/game.room.{roomCode}` with type `ROOM_JOINED`

#### Game Start Message

Sends a message to start the game (host only).

**Destination:** `/app/game.start`

**Message Format:**
```json
{
  "type": "GAME_STARTED",
  "roomCode": "A1B2C3",
  "senderId": "test-user-123",
  "senderUsername": "Player-test"
}
```

**Response:**
- All users in the room will receive a message on `/topic/game.room.{roomCode}` with type `GAME_STARTED`
- Shortly after, they will receive a message with type `NEXT_ROUND` containing the first card

#### Submit Answer Message

Sends a player's answer for the current card.

**Destination:** `/app/game.submitAnswer`

**Headers:**
- `roomCode`: The room code

**Message Format:**
```
(Integer) selectedOptionIndex
```

**Response:**
- All users in the room will receive a message on `/topic/game.room.{roomCode}` with type `ANSWER_SUBMITTED`

#### Leave Room Message

Sends a message to leave the current game room.

**Destination:** `/app/game.leave`

**Message Format:**
```json
{
  "type": "LEAVE_ROOM",
  "roomCode": "A1B2C3",
  "senderId": "test-user-456",
  "senderUsername": "Player-test"
}
```

**Response:**
- All users in the room will receive a message on `/topic/game.room.{roomCode}` with type `LEAVE_ROOM`

#### Next Round Message (Manual)

Manually triggers the next round (host only).

**Destination:** `/app/game.nextRound`

**Headers:**
- `roomCode`: The room code

**Response:**
- All users in the room will receive a message on `/topic/game.room.{roomCode}` with type `NEXT_ROUND`

---

## System

### Root Endpoints

**Base URL:** `/`

#### Get Root

Returns a simple message to verify the API is running.

**URL:** `/`

**Method:** `GET`

**Successful Response:** (200 OK)
```json
{
  "status": "success",
  "message": "Specific API is running"
}
```

#### Health Check

Returns detailed information about the system's health.

**URL:** `/health`

**Method:** `GET`

**Successful Response:** (200 OK)
```json
{
  "status": "UP",
  "app": "Specific Spring Backend",
  "profiles": ["development"],
  "firebaseEnabled": true,
  "database": "UP",
  "userCount": 42
}
``` 
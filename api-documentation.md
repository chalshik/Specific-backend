# Specific Spring WebSocket Game API Documentation

This document provides detailed information about all available endpoints for the WebSocket game functionality in the Specific language learning app.

## Table of Contents

1. [HTTP Endpoints](#http-endpoints)
   - [Create Game Room](#create-game-room)
   - [Health Check](#health-check)
2. [WebSocket Endpoints](#websocket-endpoints)
   - [Connect to WebSocket](#connect-to-websocket)
   - [Room Join Message](#room-join-message)
   - [Game Start Message](#game-start-message)
   - [Submit Answer Message](#submit-answer-message)
   - [Leave Room Message](#leave-room-message)
   - [Notification Message](#notification-message)
3. [WebSocket Message Types](#websocket-message-types)
   - [ROOM_CREATED](#room_created)
   - [ROOM_JOINED](#room_joined)
   - [GAME_STARTED](#game_started)
   - [NEXT_ROUND](#next_round)
   - [ANSWER_SUBMITTED](#answer_submitted)
   - [GAME_OVER](#game_over)
   - [LEAVE_ROOM](#leave_room)
   - [ERROR](#error)

---

## HTTP Endpoints

### Create Game Room

Creates a new game room and returns the room information.

**URL:** `/api/game/room`

**Method:** `POST`

**Headers:**
- `Content-Type: application/json`
- `X-Firebase-Uid: {your-firebase-uid}` (Required for authentication)

**Query Parameters:**
- `firebaseUid`: User's Firebase UID (Required)

**Example Request:**
```http
POST /api/game/room?firebaseUid=test-user-123 HTTP/1.1
Host: specific-backend.onrender.com
Content-Type: application/json
X-Firebase-Uid: test-user-123
```

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

**Error Response:** (400 Bad Request)
```json
{
  "error": "Invalid request",
  "message": "Firebase UID is required"
}
```

### Health Check

Checks if the API is running correctly.

**URL:** `/health-check` or `/api/health-check`

**Method:** `GET`

**Example Request:**
```http
GET /api/health-check HTTP/1.1
Host: specific-backend.onrender.com
```

**Successful Response:** (200 OK)
```json
{
  "status": "UP",
  "timestamp": "2023-06-19T12:34:56.789Z"
}
```

---

## WebSocket Endpoints

### Connect to WebSocket

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

### Room Join Message

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

### Game Start Message

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

### Submit Answer Message

Sends a player's answer for the current card.

**Destination:** `/app/game.answer`

**Message Format:**
```json
{
  "type": "ANSWER_SUBMITTED",
  "roomCode": "A1B2C3",
  "senderId": "test-user-456",
  "senderUsername": "Player-test",
  "content": 2
}
```

**Response:**
- All users in the room will receive a message on `/topic/game.room.{roomCode}` with type `ANSWER_SUBMITTED`

### Leave Room Message

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

### Notification Message

Sends a general notification to the room.

**Destination:** `/app/game.notify`

**Message Format:**
```json
{
  "type": "ROOM_CREATED",
  "roomCode": "A1B2C3",
  "senderId": "test-user-123",
  "senderUsername": "Player-test"
}
```

**Response:**
- All users in the room will receive a message on `/topic/game.room.{roomCode}` with the specified type

---

## WebSocket Message Types

### ROOM_CREATED

Indicates that a room has been created. Sent by the room creator.

**Example:**
```json
{
  "type": "ROOM_CREATED",
  "roomCode": "A1B2C3",
  "senderId": "test-user-123",
  "senderUsername": "Player-test"
}
```

### ROOM_JOINED

Indicates that a player has joined the room.

**Example:**
```json
{
  "type": "ROOM_JOINED",
  "roomCode": "A1B2C3",
  "senderId": "test-user-456",
  "senderUsername": "Player-test"
}
```

### GAME_STARTED

Indicates that the game has started. Sent by the room host.

**Example:**
```json
{
  "type": "GAME_STARTED",
  "roomCode": "A1B2C3",
  "senderId": "test-user-123",
  "senderUsername": "Player-test"
}
```

### NEXT_ROUND

Contains information about the next card in the game. Sent by the server when starting a new round.

**Example:**
```json
{
  "type": "NEXT_ROUND",
  "roomCode": "A1B2C3",
  "roundNumber": 1,
  "currentCard": {
    "front": "안녕하세요",
    "back": "Hello",
    "options": ["Goodbye", "Thank you", "Hello", "Good night"]
  }
}
```

### ANSWER_SUBMITTED

Indicates that a player has submitted an answer for the current card.

**Example:**
```json
{
  "type": "ANSWER_SUBMITTED",
  "roomCode": "A1B2C3",
  "senderId": "test-user-456",
  "senderUsername": "Player-test",
  "content": 2
}
```

### GAME_OVER

Indicates that the game has ended and contains the results.

**Example:**
```json
{
  "type": "GAME_OVER",
  "roomCode": "A1B2C3",
  "gameResult": {
    "winnerUsername": "Player-test",
    "scores": [
      {
        "playerId": "test-user-123",
        "username": "Player-test",
        "score": 3
      },
      {
        "playerId": "test-user-456",
        "username": "Player-456",
        "score": 1
      }
    ]
  }
}
```

### LEAVE_ROOM

Indicates that a player has left the room.

**Example:**
```json
{
  "type": "LEAVE_ROOM",
  "roomCode": "A1B2C3",
  "senderId": "test-user-456",
  "senderUsername": "Player-test"
}
```

### ERROR

Contains information about an error that occurred.

**Example:**
```json
{
  "type": "ERROR",
  "content": "Unable to start game: not enough players"
}
```

---

## Subscription Topics

### Room Topic

Receive all messages related to a specific room:

```javascript
stompClient.subscribe(`/topic/game.room.${roomCode}`, message => {
  const messageBody = JSON.parse(message.body);
  // Handle message based on messageBody.type
});
```

### User Queue

Receive personal messages directed only to you:

```javascript
stompClient.subscribe('/user/queue/game', message => {
  const messageBody = JSON.parse(message.body);
  // Handle personal message
});
``` 
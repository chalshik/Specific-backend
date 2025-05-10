# Specific Spring Backend

A Spring Boot application for language learning with spaced repetition.

## Features

- User authentication with Firebase
- Card and deck management
- Spaced repetition algorithm for flashcards
- Translation services with DeepL API

## Local Development

### Prerequisites

- Java 21
- Maven
- PostgreSQL
- Firebase Account (for authentication)
- DeepL API Key (for translations)

### Setup

1. Clone the repository
2. Create a PostgreSQL database named `english_db`
3. Place your Firebase service account JSON in `src/main/resources/`
4. Update `application.properties` with your database credentials and Firebase configuration
5. Run the application:

```bash
mvn spring-boot:run
```

## Deployment on Render

This application is configured for deployment on Render.com.

### Deployment Steps

1. Push your code to GitHub
2. Create a new Web Service on Render
3. Connect your GitHub repository
4. Select "Use render.yaml" for service configuration
5. Set environment variables:
   - `FIREBASE_CREDENTIALS_JSON`: The entire contents of your Firebase service account JSON file
   - `DEEPL_API_KEY`: Your DeepL API key

Render will automatically:
- Create a PostgreSQL database
- Build and deploy the application
- Provide a public URL for your API

## API Documentation

### Authentication

All protected endpoints require a Firebase authentication token in the Authorization header:

```
Authorization: Bearer {firebase_token}
```

### Endpoints

- `/user/register` - Register a new user
- `/user/test` - Test endpoint
- `/cards/**` - Card management endpoints (authenticated)
- `/translation/**` - Translation endpoints
- `/user/**` - User management endpoints (authenticated)

## License

[MIT License](LICENSE)

# Specific Game Room Testing Tool

This is a simple web application for testing the room creation and joining functionality of the Specific backend.

## Setup

1. Make sure you have Node.js installed on your machine.
2. Clone or download this repository.
3. Open a terminal and navigate to the project directory.
4. Run the server:

```bash
node server.js
```

5. Open a web browser and navigate to `http://localhost:3000`.

## How to Use

### Authentication

Before you can create or join a room, you need to set a Firebase UID for authentication:

1. Enter a test Firebase UID in the "Firebase UID" field (you can use "test-user-123").
2. Click "Set Firebase UID".
3. If successful, the "Create Room" and "Join Room" buttons will be enabled.

### Creating a Room

1. Click the "Create Room" button.
2. If successful, a room code will be displayed.
3. You can share this code with another user to join the room.

### Joining a Room

1. Enter a room code in the "Room Code" field.
2. Click "Join Room".
3. If successful, you will join the room.

### Starting the Game

1. Once both players have joined, the host can click "Start Game".
2. The game will begin and cards will be displayed.

### Leaving a Room

1. Click "Leave Room" to exit the current room.
2. If you're the host, the room will be deleted.
3. If you're the guest, you'll be removed from the room.

## Game Log

The Game Log panel displays all events and messages related to the game, including:
- Connection status
- Room creation/joining
- Game start
- Round information
- Answer submissions
- Game results

## Testing with Multiple Users

To test the multiplayer functionality, you can:

1. Open the application in two different browser windows.
2. Use different Firebase UIDs for each window.
3. Create a room in one window and join it from the other.

## Requirements

- A modern web browser with WebSocket support
- Node.js (for running the server)
- Internet connection (to access the Specific backend)

## Troubleshooting

- If you encounter connection issues, check that the Specific backend is running and accessible.
- Make sure you're using valid Firebase UIDs for authentication.
- Check the Game Log for error messages and debugging information.

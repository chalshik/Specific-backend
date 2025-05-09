# Firebase UID Authentication Test Results

## Overview
This document captures the results of testing API endpoints with Firebase UID authentication on the Specific Spring Backend. Tests were conducted using both direct API calls and a test script (`test-anki-endpoints.sh`).

## Working Endpoints

The following endpoints are successfully handling Firebase UID authentication:

1. **Health Check**: `GET /health`
   - Returns API status information 
   - Does not require authentication

2. **User Management**:
   - `POST /user/register`: Successfully registers new users with Firebase UID
   - `GET /user/info?firebaseUid={uid}`: Successfully retrieves user information

3. **Deck Management**:
   - `POST /anki/add-deck`: Successfully creates a deck with Firebase UID
   - `GET /anki/user-decks?firebaseUid={uid}`: Successfully returns all decks for a user
   - `DELETE /anki/delete-deck/{deckId}?firebaseUid={uid}`: Successfully deletes a deck

## Non-Working Endpoints

The following endpoints have issues with Firebase UID authentication:

1. **Card Management**:
   - `POST /api/cards/deck/{deckId}`: Returns "Deck not found" error even with valid deck ID
   - `GET /api/cards/deck/{deckId}`: Not working correctly with Firebase UID

## Authentication Methods

The backend accepts Firebase UID in several ways:

1. As a query parameter: `?firebaseUid={uid}`
2. In the request body as a JSON field: `{"firebaseUid": "test-user-123"}`
3. In the `Authorization` header: `Authorization: test-user-123`

## Implementation Details

- The system uses a `DeckController` at path `/anki` which properly extracts Firebase UID from request parameters, body, or defaults.
- The `CardController` needs investigation as it doesn't appear to properly connect cards to decks when using Firebase UID authentication.

## Testing Script

A Bash script (`test-anki-endpoints.sh`) was created to test the working endpoints. This script:

1. Checks API health
2. Registers a test user with a specific Firebase UID
3. Retrieves user information
4. Creates a test deck
5. Retrieves all decks for the user
6. Deletes the test deck for cleanup

## Next Steps

1. Investigate the `CardController` to understand why card creation fails with Firebase UID
2. Check how deck association works in card creation endpoints
3. Ensure proper user and deck validation in the card service classes
4. Consider implementing additional logging to track Firebase UID handling 
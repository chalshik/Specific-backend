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

/**
 * Specific Card Game - Configuration
 */
const CONFIG = {
    // Debug mode (enables logging)
    DEBUG: true,
    
    // API URLs - Update these to match your actual backend server
    API_URL: 'https://specific-backend.onrender.com',  // Main backend URL
    ALT_API_URL: 'http://localhost:8081',              // Alternative URL for local development
    
    // WebSocket endpoint
    WS_ENDPOINT: '/ws-game',
    
    // Socket configurations
    SOCKET: {
        // Queue for personal messages
        PERSONAL_QUEUE: '/user/queue/game',
        
        // Prefix for room topics
        ROOM_TOPIC_PREFIX: '/topic/game.room.',
        
        // Endpoints
        ENDPOINTS: {
            JOIN: '/app/game.join',
            START: '/app/game.start',
            SUBMIT_ANSWER: '/app/game.submitAnswer',
            LEAVE: '/app/game.leave'
        }
    },
    
    // Game settings
    GAME: {
        ROUND_TRANSITION_DELAY: 2000,
        TOTAL_ROUNDS: 10
    },
    
    // Reliability configurations
    RELIABILITY: {
        VERBOSE_LOGGING: true,
        CONNECTION_RETRY_LIMIT: 3,
        CONNECTION_RETRY_DELAY: 3000
    },
    
    // CORS settings
    CORS: {
        ALLOWED_ORIGINS: ['*'],
        WITH_CREDENTIALS: true
    }
};

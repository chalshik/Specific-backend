/**
 * Game Configuration
 */
const CONFIG = {
    // Backend API URL - Change this to your actual backend URL
    API_URL: 'https://specific-backend.onrender.com',
    
    // Alternative URLs if the main one doesn't work
    ALT_API_URL: 'http://localhost:8081',
    
    // Game website URL - Access the game through this URL on your server
    GAME_URL: '/game',
    
    // WebSocket endpoint
    WS_ENDPOINT: '/ws-game',
    
    // WebSocket topics and queues
    SOCKET: {
        PERSONAL_QUEUE: '/user/queue/game',
        ROOM_TOPIC_PREFIX: '/topic/game.room.',
        ENDPOINTS: {
            JOIN: '/app/game.join',
            START: '/app/game.start',
            SUBMIT_ANSWER: '/app/game.submitAnswer',
            NEXT_ROUND: '/app/game.nextRound',
            LEAVE: '/app/game.leave'
        }
    },
    
    // Game settings
    GAME: {
        ROUND_TRANSITION_DELAY: 2000, // milliseconds
        DEFAULT_ROUNDS: 10,
        ANSWER_TIMEOUT: 20 // seconds
    },
    
    // Debug settings
    DEBUG: true
};

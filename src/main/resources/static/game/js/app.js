/**
 * Specific Card Game - Main Application
 */
document.addEventListener('DOMContentLoaded', function() {
    // State variables
    let stompClient = null;
    let firebaseUid = '';
    let playerUsername = '';
    let currentRoomCode = '';
    let isHost = false;
    let gameState = {
        roundNumber: 0,
        hostScore: 0,
        guestScore: 0,
        yourScore: 0,
        opponentScore: 0,
        selectedOption: null,
        hasAnswered: false,
        opponentAnswered: false,
        gameCards: [],
        currentCardIndex: -1,
        totalRounds: 10
    };

    // DOM Elements
    const elements = {
        // Sections
        sections: {
            login: document.getElementById('loginSection'),
            lobby: document.getElementById('lobbySection'),
            game: document.getElementById('gameSection'),
            results: document.getElementById('resultsSection')
        },
        
        // Login
        login: {
            firebaseUidInput: document.getElementById('firebaseUid'),
            loginBtn: document.getElementById('loginBtn'),
            playerInfo: document.getElementById('playerInfo'),
            playerName: document.getElementById('playerName')
        },
        
        // Create Room
        create: {
            createRoomBtn: document.getElementById('createRoomBtn'),
            roomInfo: document.getElementById('roomInfo'),
            roomCode: document.getElementById('roomCode'),
            roomPlayers: document.getElementById('roomPlayers'),
            playersList: document.getElementById('playersList'),
            startGameBtn: document.getElementById('startGameBtn'),
            leaveRoomBtn: document.getElementById('leaveRoomBtn')
        },
        
        // Join Room
        join: {
            joinRoomCodeInput: document.getElementById('joinRoomCode'),
            joinRoomBtn: document.getElementById('joinRoomBtn'),
            joinedRoomInfo: document.getElementById('joinedRoomInfo'),
            joinedRoomCode: document.getElementById('joinedRoomCode'),
            joinedRoomPlayers: document.getElementById('joinedRoomPlayers'),
            joinedPlayersList: document.getElementById('joinedPlayersList'),
            leaveJoinedRoomBtn: document.getElementById('leaveJoinedRoomBtn')
        },
        
        // Game
        game: {
            roundNumber: document.getElementById('roundNumber'),
            yourScore: document.getElementById('yourScore'),
            opponentScore: document.getElementById('opponentScore'),
            cardQuestion: document.getElementById('cardQuestion'),
            optionsContainer: document.getElementById('optionsContainer'),
            answerFeedback: document.getElementById('answerFeedback'),
            leaveGameBtn: document.getElementById('leaveGameBtn')
        },
        
        // Results
        results: {
            winnerDisplay: document.getElementById('winnerDisplay'),
            winnerText: document.getElementById('winnerText'),
            player1Name: document.getElementById('player1Name'),
            player1Score: document.getElementById('player1Score'),
            player2Name: document.getElementById('player2Name'),
            player2Score: document.getElementById('player2Score'),
            playAgainBtn: document.getElementById('playAgainBtn'),
            returnToLobbyBtn: document.getElementById('returnToLobbyBtn')
        },
        
        // Status
        status: {
            connectionStatus: document.getElementById('connectionStatus'),
            statusIcon: document.getElementById('statusIcon'),
            statusText: document.getElementById('statusText')
        },
        
        // Log
        log: document.getElementById('log')
    };

    // Initialize event listeners
    function initEventListeners() {
        // Login
        elements.login.loginBtn.addEventListener('click', handleLogin);
        
        // Create Room
        elements.create.createRoomBtn.addEventListener('click', createRoom);
        elements.create.startGameBtn.addEventListener('click', startGame);
        elements.create.leaveRoomBtn.addEventListener('click', () => leaveRoom(true));
        
        // Join Room
        elements.join.joinRoomBtn.addEventListener('click', joinRoom);
        elements.join.leaveJoinedRoomBtn.addEventListener('click', () => leaveRoom(false));
        
        // Game
        elements.game.leaveGameBtn.addEventListener('click', leaveGame);
        
        // Results
        elements.results.playAgainBtn.addEventListener('click', playAgain);
        elements.results.returnToLobbyBtn.addEventListener('click', returnToLobby);
    }

    // Show a specific section and hide others
    function showSection(sectionName) {
        Object.keys(elements.sections).forEach(key => {
            elements.sections[key].classList.remove('active');
        });
        elements.sections[sectionName].classList.add('active');
    }

    // Update connection status
    function updateConnectionStatus(status) {
        const statusMap = {
            connected: {
                icon: 'connected',
                text: 'Connected'
            },
            connecting: {
                icon: 'connecting',
                text: 'Connecting...'
            },
            disconnected: {
                icon: 'disconnected',
                text: 'Disconnected'
            }
        };
        
        const statusInfo = statusMap[status] || statusMap.disconnected;
        elements.status.statusIcon.className = 'status-icon ' + statusInfo.icon;
        elements.status.statusText.textContent = statusInfo.text;
    }

    // Log message to console
    function logMessage(message, type = 'info') {
        if (CONFIG.DEBUG) {
            console.log(`[${type.toUpperCase()}] ${message}`);
        }
        
        const logEntry = document.createElement('div');
        logEntry.className = `log-entry log-${type}`;
        logEntry.textContent = `[${new Date().toLocaleTimeString()}] ${message}`;
        elements.log.appendChild(logEntry);
        elements.log.scrollTop = elements.log.scrollHeight;
    }

    // Handle login
    function handleLogin() {
        firebaseUid = elements.login.firebaseUidInput.value.trim();
        
        if (!firebaseUid) {
            logMessage('Please enter a Firebase UID', 'error');
            return;
        }
        
        // For testing, we'll use the Firebase UID as the username
        playerUsername = 'User_' + firebaseUid.substring(0, 5);
        
        // Show player info
        elements.login.playerName.textContent = playerUsername;
        elements.login.playerInfo.style.display = 'block';
        
        // Connect to WebSocket
        connect();
        
        // Move to lobby
        showSection('lobby');
        
        logMessage(`Logged in as ${playerUsername}`, 'success');
    }

    // Connect to WebSocket server
    function connect() {
        // Don't attempt to connect if already connected
        if (stompClient !== null && stompClient.connected) {
            updateConnectionStatus(true);
            return;
        }
        
        // Update status to show connecting
        updateConnectionStatus('connecting');
        logMessage('Connecting to WebSocket server...', 'info');
        
        try {
            // Create SockJS socket
            const socket = new SockJS(`${CONFIG.API_URL}${CONFIG.WS_ENDPOINT}`);
            
            // Initialize STOMP client over the socket
            stompClient = Stomp.over(socket);
            
            // Disable debug logging in production
            if (!CONFIG.DEBUG) {
                stompClient.debug = null;
            }
            
            // Connect with user information in headers
            stompClient.connect(
                {
                    firebaseUid: firebaseUid,
                    username: playerUsername
                }, 
                onConnected, 
                onError
            );
        } catch (error) {
            logMessage(`Error initializing WebSocket: ${error.message}`, 'error');
            updateConnectionStatus(false);
            
            // Try alternative endpoint if main one fails
            setTimeout(connectAlternative, 2000);
        }
    }
    
    // Try to connect to alternative WebSocket endpoint
    function connectAlternative() {
        if (stompClient !== null && stompClient.connected) {
            return; // Already connected
        }
        
        updateConnectionStatus('connecting');
        logMessage('Trying alternative WebSocket endpoint...', 'info');
        
        try {
            // Use alternative URL
            const socket = new SockJS(`${CONFIG.ALT_API_URL}${CONFIG.WS_ENDPOINT}`);
            stompClient = Stomp.over(socket);
            
            if (!CONFIG.DEBUG) {
                stompClient.debug = null;
            }
            
            // Include user info in headers
            stompClient.connect(
                {
                    firebaseUid: firebaseUid,
                    username: playerUsername
                }, 
                onConnected, 
                onError
            );
        } catch (error) {
            logMessage(`Error connecting to alternative endpoint: ${error.message}`, 'error');
            updateConnectionStatus(false);
        }
    }
    
    // Handle successful WebSocket connection
    function onConnected() {
        updateConnectionStatus(true);
        logMessage('Connected to WebSocket server', 'success');
        
        // Subscribe to personal queue for direct messages
        stompClient.subscribe(
            CONFIG.SOCKET.PERSONAL_QUEUE, 
            onMessageReceived,
            { 
                id: 'personal-subscription',
                firebaseUid: firebaseUid,
                username: playerUsername
            }
        );
        
        // Enable UI actions that require connection
        elements.join.joinRoomBtn.disabled = false;
        elements.create.createRoomBtn.disabled = false;
        
        // If we were previously in a room, resubscribe
        if (currentRoomCode) {
            logMessage(`Resubscribing to previous room: ${currentRoomCode}`, 'info');
            subscribeToRoom(currentRoomCode);
            
            // Announce our presence in the room again
            setTimeout(() => {
                sendRoomMessage('ROOM_JOINED', {
                    content: `${playerUsername} reconnected to the room`
                });
            }, 1000);
        }
    }

    // Handle WebSocket connection error
    function onError(error) {
        updateConnectionStatus('disconnected');
        logMessage(`Connection error: ${error}`, 'error');
        
        // Disable buttons that require connection
        elements.create.createRoomBtn.disabled = true;
        elements.join.joinRoomBtn.disabled = true;
        
        // Try to reconnect after a delay
        setTimeout(connect, 5000);
    }

    // Subscribe to a room topic
    function subscribeToRoom(roomCode) {
        if (!stompClient || !stompClient.connected) {
            logMessage('Cannot subscribe: WebSocket not connected', 'error');
            return false;
        }
        
        // First unsubscribe if we're already subscribed to a room
        if (currentRoomCode && currentRoomCode !== roomCode) {
            try {
                stompClient.unsubscribe('room-subscription');
                logMessage(`Unsubscribed from previous room: ${currentRoomCode}`, 'info');
            } catch (e) {
                logMessage(`Error unsubscribing from previous room: ${e.message}`, 'warning');
            }
        }
        
        try {
            logMessage(`Subscribing to room topic: ${roomCode}`, 'info');
            stompClient.subscribe(
                CONFIG.SOCKET.ROOM_TOPIC_PREFIX + roomCode,
                onMessageReceived,
                { id: 'room-subscription' }
            );
            return true;
        } catch (e) {
            logMessage(`Error subscribing to room: ${e.message}`, 'error');
            return false;
        }
    }

    // Create a new game room
    function createRoom() {
        logMessage('Creating new room...', 'info');
        
        // Disable button to prevent multiple clicks
        elements.create.createRoomBtn.disabled = true;
        
        fetch(`${CONFIG.API_URL}/api/game/room?firebaseUid=${firebaseUid}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Firebase-Uid': firebaseUid
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to create room. Status: ${response.status}`);
            }
            return response.json();
        })
        .then(room => {
            // Store room information
            currentRoomCode = room.roomCode;
            isHost = true;
            
            logMessage(`Room created with code: ${currentRoomCode}`, 'success');
            
            // Subscribe to room topic with the proper ID
            if (!subscribeToRoom(currentRoomCode)) {
                throw new Error("Failed to subscribe to room topic");
            }
            
            // Update UI
            elements.create.roomCode.textContent = currentRoomCode;
            elements.create.roomInfo.style.display = 'block';
            elements.create.roomPlayers.style.display = 'block';
            
            // Add host to players list
            updatePlayersList();
            
            // Send a message to the room topic to announce creation
            sendRoomMessage('ROOM_CREATED');
            
            // Also send room joined message to ensure visibility
            sendRoomMessage('ROOM_JOINED');
        })
        .catch(error => {
            logMessage(`Error creating room: ${error.message}`, 'error');
            
            // Try alternative approach - direct endpoint without /api prefix
            logMessage('Trying alternative approach for room creation...', 'info');
            
            const altUrl = useAlternativeUrl 
                ? `${CONFIG.ALT_API_URL}/game/room?firebaseUid=${firebaseUid}`
                : `${CONFIG.API_URL}/game/room?firebaseUid=${firebaseUid}`;
                
            fetch(altUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Firebase-Uid': firebaseUid
                }
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Alternative approach failed. Status: ${response.status}`);
                }
                return response.json();
            })
            .then(room => {
                // Store room information
                currentRoomCode = room.roomCode;
                isHost = true;
                
                logMessage(`Room created with alternative approach. Code: ${currentRoomCode}`, 'success');
                
                // Subscribe to room topic with proper ID
                if (!subscribeToRoom(currentRoomCode)) {
                    throw new Error("Failed to subscribe to room topic");
                }
                
                // Update UI
                elements.create.roomCode.textContent = currentRoomCode;
                elements.create.roomInfo.style.display = 'block';
                elements.create.roomPlayers.style.display = 'block';
                
                // Add host to players list
                updatePlayersList();
                
                // Send a message to the room topic to announce creation
                sendRoomMessage('ROOM_CREATED');
                
                // Also send room joined message to ensure visibility
                sendRoomMessage('ROOM_JOINED');
            })
            .catch(altError => {
                logMessage(`Alternative approach also failed: ${altError.message}`, 'error');
                // Re-enable the create button
                elements.create.createRoomBtn.disabled = false;
            });
        });
    }

    // Send a message to the current room topic
    function sendRoomMessage(messageType, additionalData = {}) {
        if (!stompClient || !stompClient.connected || !currentRoomCode) {
            logMessage('Cannot send message: not connected or no active room', 'error');
            return false;
        }
        
        try {
            const message = {
                type: messageType,
                roomCode: currentRoomCode,
                senderId: firebaseUid,
                senderUsername: playerUsername,
                timestamp: Date.now(),
                ...additionalData
            };
            
            stompClient.send(
                CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, 
                {
                    firebaseUid: firebaseUid,
                    username: playerUsername
                }, 
                JSON.stringify(message)
            );
            
            logMessage(`Sent ${messageType} message to room ${currentRoomCode}`, 'info');
            return true;
        } catch (e) {
            logMessage(`Error sending ${messageType} message: ${e.message}`, 'error');
            return false;
        }
    }

    // Join an existing game room
    function joinRoom() {
        const roomCode = elements.join.joinRoomCodeInput.value.trim().toUpperCase();
        
        if (!roomCode) {
            logMessage('Please enter a room code', 'error');
            return;
        }
        
        logMessage(`Attempting to join room ${roomCode}...`, 'info');
        elements.join.joinRoomBtn.disabled = true;
        
        // Check if already in this room
        if (currentRoomCode === roomCode) {
            logMessage('Already in this room', 'warning');
            elements.join.joinRoomBtn.disabled = false;
            return;
        }
        
        // Subscribe to room topic with proper ID first to ensure we receive messages
        if (!subscribeToRoom(roomCode)) {
            logMessage('Failed to subscribe to room topic, aborting join', 'error');
            elements.join.joinRoomBtn.disabled = false;
            return;
        }
        
        try {
            // Store room information first so subscription messages work
            currentRoomCode = roomCode;
            isHost = false;
            
            // Update UI to show we're attempting to join
            elements.join.joinedRoomCode.textContent = roomCode;
            elements.join.joinedRoomInfo.style.display = 'block';
            
            // First try the server-managed join endpoint with proper headers
            logMessage('Sending join message to application endpoint', 'info');
            stompClient.send(CONFIG.SOCKET.ENDPOINTS.JOIN, {
                firebaseUid: firebaseUid,
                username: playerUsername,
                roomCode: roomCode
            }, JSON.stringify({
                type: 'JOIN_ROOM',  // Server expects JOIN_ROOM
                roomCode: roomCode,
                senderId: null,  // Let server assign this
                senderUsername: playerUsername,
                timestamp: Date.now()
            }));
            
            // Also send direct ROOM_JOINED message to the room topic for visibility
            // This helps ensure we are displayed in the room
            setTimeout(() => {
                sendRoomMessage('ROOM_JOINED', {
                    content: `${playerUsername} joined the room`
                });
            }, 500);
            
            // Set a timeout to detect if join failed
            setTimeout(() => {
                // If players list is still empty after timeout, likely join failed
                if (elements.join.joinedPlayersList.children.length <= 1) {
                    logMessage('No confirmation received for room join. Room may not exist.', 'warning');
                    
                    // Try direct join message again as a last resort
                    sendRoomMessage('ROOM_JOINED', {
                        content: `${playerUsername} joined the room (retry)`
                    });
                    
                    // Add ourselves to players list anyway as a fallback
                    addPlayerToGuestList(playerUsername, true);
                    elements.join.joinedRoomPlayers.style.display = 'block';
                }
            }, 3000);
            
        } catch (error) {
            logMessage(`Error joining room: ${error.message}`, 'error');
            // Re-enable join button if there was an error
            elements.join.joinRoomBtn.disabled = false;
        }
    }
    
    // Add a player to the guest player list
    function addPlayerToGuestList(username, isSelf = false) {
        // Check if player already exists in the list
        const existingPlayer = Array.from(elements.join.joinedPlayersList.children)
            .some(item => item.textContent.includes(username));
            
        if (existingPlayer) {
            return false;
        }
        
        const playerItem = document.createElement('li');
        playerItem.className = 'list-group-item';
        playerItem.innerHTML = `
            <i class="fas fa-user player-icon"></i>
            ${username}${isSelf ? ' (You)' : ''}
            <span class="badge ${isSelf ? 'bg-secondary' : 'bg-primary'} host-badge">
                ${isSelf ? 'Guest' : 'Host'}
            </span>
        `;
        
        elements.join.joinedPlayersList.appendChild(playerItem);
        elements.join.joinedRoomPlayers.style.display = 'block';
        return true;
    }
    
    // Add a player to the host player list
    function addPlayerToHostList(username, isGuest = true) {
        // Check if player already exists in the list
        const existingPlayer = Array.from(elements.create.playersList.children)
            .some(item => item.textContent.includes(username));
            
        if (existingPlayer) {
            return false;
        }
        
        const playerItem = document.createElement('li');
        playerItem.className = 'list-group-item';
        playerItem.innerHTML = `
            <i class="fas fa-user player-icon"></i>
            ${username}
            <span class="badge ${isGuest ? 'bg-secondary' : 'bg-primary'} host-badge">
                ${isGuest ? 'Guest' : 'Host'}
            </span>
        `;
        
        elements.create.playersList.appendChild(playerItem);
        return true;
    }

    // Handle room joined message
    function handleRoomJoined(message) {
        logMessage(`Received join notification: ${message.senderUsername} joined room: ${message.roomCode}`, 'info');
        
        // Check if this is a message for our current room
        if (message.roomCode !== currentRoomCode) {
            logMessage(`Ignoring message for different room: ${message.roomCode}`, 'warning');
            return;
        }
        
        // Debug logging for message content
        console.log("Room joined message:", message);
        
        // Update UI based on whether we're host or guest
        if (isHost) {
            // Add guest to players list if not already there
            if (addPlayerToHostList(message.senderUsername, true)) {
                logMessage(`Guest ${message.senderUsername} added to room`, 'success');
                
                // Enable start button when guest joins
                elements.create.startGameBtn.disabled = false;
                
                // Send a confirmation message to guest
                setTimeout(() => {
                    // Include host information in the confirmation
                    sendRoomMessage('ROOM_JOINED', {
                        content: `${playerUsername} is hosting the room`
                    });
                }, 500);
            }
        } else {
            // If message is about someone else
            if (message.senderUsername !== playerUsername) {
                // This is likely about the host or other players
                if (addPlayerToGuestList(message.senderUsername, false)) {
                    logMessage(`Found player in room: ${message.senderUsername}`, 'info');
                }
            } else {
                // This is about us - our join was confirmed
                if (addPlayerToGuestList(playerUsername, true)) {
                    logMessage('Join confirmed successfully', 'success');
                }
                
                // Send another confirmation to make sure host sees us
                setTimeout(() => {
                    sendRoomMessage('ROOM_JOINED', {
                        content: `${playerUsername} joined the room (confirmation)`
                    });
                }, 1000);
            }
        }
    }

    // Handle incoming WebSocket messages
    function onMessageReceived(payload) {
        try {
            const message = JSON.parse(payload.body);
            logMessage(`Received message: ${message.type}`, 'info');
            
            // Handle different message types
            switch (message.type) {
                case 'ROOM_CREATED':
                case 'ROOM_JOINED':
                    handleRoomJoined(message);
                    break;
                case 'GAME_STARTED':
                    handleGameStarted(message);
                    break;
                case 'NEXT_ROUND':
                    handleNextRound(message);
                    break;
                case 'ANSWER_SUBMITTED':
                    handleAnswerSubmitted(message);
                    break;
                case 'GAME_OVER':
                    handleGameOver(message);
                    break;
                case 'LEAVE_ROOM':
                    handlePlayerLeft(message);
                    break;
                case 'ERROR':
                    handleError(message);
                    break;
                default:
                    logMessage(`Unknown message type: ${message.type}`, 'warning');
            }
        } catch (error) {
            logMessage(`Error processing message: ${error.message}`, 'error');
            logMessage(`Problematic payload: ${payload.body}`, 'error');
        }
    }

    // Start the game
    function startGame() {
        if (!currentRoomCode) {
            logMessage('No active room', 'error');
            return;
        }
        
        logMessage('Starting game...', 'info');
        
        // Send start game message
        stompClient.send(CONFIG.SOCKET.ENDPOINTS.START, {}, JSON.stringify({
            type: 'GAME_STARTED',
            roomCode: currentRoomCode,
            senderId: firebaseUid,
            senderUsername: playerUsername
        }));
    }

    // Submit an answer
    function submitAnswer(optionIndex) {
        if (gameState.hasAnswered) {
            return;
        }
        
        logMessage(`Submitting answer: Option ${optionIndex}`, 'info');
        
        // Mark option as selected
        gameState.selectedOption = optionIndex;
        gameState.hasAnswered = true;
        
        // Disable all option buttons
        const optionButtons = elements.game.optionsContainer.querySelectorAll('.option-btn');
        optionButtons.forEach(button => {
            button.disabled = true;
            if (parseInt(button.dataset.index) === optionIndex) {
                button.classList.add('selected');
            }
        });
        
        // Send answer
        stompClient.send(
            CONFIG.SOCKET.ENDPOINTS.SUBMIT_ANSWER,
            { roomCode: currentRoomCode },
            optionIndex
        );
    }

    // Leave the current room
    function leaveRoom(isCreator) {
        if (!currentRoomCode) {
            logMessage('No active room', 'error');
            return;
        }
        
        logMessage(`Leaving room ${currentRoomCode}...`, 'info');
        
        // Send leave message to the server
        try {
            // First try the standard endpoint
            stompClient.send(CONFIG.SOCKET.ENDPOINTS.LEAVE, {
                roomCode: currentRoomCode
            }, JSON.stringify({
                type: 'LEAVE_ROOM',
                roomCode: currentRoomCode,
                senderId: firebaseUid,
                senderUsername: playerUsername
            }));
            
            // Also send to room topic directly for redundancy
            sendRoomMessage('LEAVE_ROOM', {
                content: `${playerUsername} left the room`
            });
        } catch (e) {
            logMessage(`Error sending leave message: ${e.message}`, 'warning');
        }
        
        // Unsubscribe from the room topic
        try {
            stompClient.unsubscribe('room-subscription');
            logMessage(`Unsubscribed from room: ${currentRoomCode}`, 'info');
        } catch (e) {
            logMessage(`Error unsubscribing from room: ${e.message}`, 'warning');
        }
        
        // Reset room state
        const oldRoomCode = currentRoomCode;
        resetRoomState();
        
        // Update UI
        if (isCreator) {
            elements.create.roomInfo.style.display = 'none';
            elements.create.roomPlayers.style.display = 'none';
            elements.create.playersList.innerHTML = '';
            elements.create.startGameBtn.disabled = true;
            elements.create.createRoomBtn.disabled = false;
        } else {
            elements.join.joinedRoomInfo.style.display = 'none';
            elements.join.joinedRoomPlayers.style.display = 'none';
            elements.join.joinedPlayersList.innerHTML = '';
            elements.join.joinRoomBtn.disabled = false;
        }
        
        logMessage(`Left room: ${oldRoomCode}`, 'success');
    }

    // Leave the current game
    function leaveGame() {
        leaveRoom(isHost);
        showSection('lobby');
    }

    // Play again (after game ends)
    function playAgain() {
        if (isHost) {
            // Only host can start a new game
            startGame();
        } else {
            showSection('lobby');
        }
    }

    // Return to lobby (after game ends)
    function returnToLobby() {
        showSection('lobby');
    }

    // Reset room state
    function resetRoomState() {
        currentRoomCode = '';
        isHost = false;
        
        // Reset game state
        gameState = {
            roundNumber: 0,
            hostScore: 0,
            guestScore: 0,
            yourScore: 0,
            opponentScore: 0,
            selectedOption: null,
            hasAnswered: false
        };
    }

    // Update players list
    function updatePlayersList() {
        // Clear existing lists
        elements.create.playersList.innerHTML = '';
        elements.join.joinedPlayersList.innerHTML = '';
        
        // Add the current player
        if (isHost) {
            // Add ourselves as host to the host view
            addPlayerToHostList(playerUsername, false);
            elements.create.startGameBtn.disabled = true; // Disable until guest joins
        } else {
            // Add ourselves as guest to the guest view
            addPlayerToGuestList(playerUsername, true);
        }
        
        // Ensure the proper sections are visible
        if (isHost) {
            elements.create.roomPlayers.style.display = 'block';
        } else {
            elements.join.joinedRoomPlayers.style.display = 'block';
        }
    }

    // Handle game started message
    function handleGameStarted(message) {
        logMessage('Game started', 'success');
        
        // Reset game state
        gameState.roundNumber = 0;
        gameState.hostScore = 0;
        gameState.guestScore = 0;
        gameState.yourScore = 0;
        gameState.opponentScore = 0;
        gameState.opponentAnswered = false;
        gameState.hasAnswered = false;
        
        // Initialize game cards from static data
        initializeGameCards();
        
        // Show game section
        showSection('game');
        
        // Start the first round
        startNextRound();
    }
    
    // Initialize game cards from static data
    function initializeGameCards() {
        // Make a copy of the static cards and shuffle them
        gameState.gameCards = [...STATIC_CARDS];
        shuffleArray(gameState.gameCards);
        
        // Limit to the number of rounds we want to play
        gameState.gameCards = gameState.gameCards.slice(0, gameState.totalRounds);
        gameState.currentCardIndex = -1;
    }
    
    // Shuffle an array using Fisher-Yates algorithm
    function shuffleArray(array) {
        for (let i = array.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [array[i], array[j]] = [array[j], array[i]];
        }
        return array;
    }

    // Start the next round locally
    function startNextRound() {
        // Move to next card
        gameState.currentCardIndex++;
        gameState.roundNumber++;
        gameState.hasAnswered = false;
        gameState.opponentAnswered = false;
        gameState.selectedOption = null;
        
        // Check if game is over
        if (gameState.currentCardIndex >= gameState.gameCards.length) {
            endGame();
            return;
        }
        
        const currentCard = gameState.gameCards[gameState.currentCardIndex];
        
        // Update UI
        elements.game.roundNumber.textContent = gameState.roundNumber;
        elements.game.yourScore.textContent = gameState.yourScore;
        elements.game.opponentScore.textContent = gameState.opponentScore;
        elements.game.cardQuestion.textContent = currentCard.front;
        elements.game.answerFeedback.style.display = 'none';
        
        // Create option buttons
        elements.game.optionsContainer.innerHTML = '';
        shuffleArray([...currentCard.options]).forEach((option, index) => {
            const optionCol = document.createElement('div');
            optionCol.className = 'col-md-6';
            
            const optionBtn = document.createElement('button');
            optionBtn.className = 'btn option-btn';
            optionBtn.textContent = option;
            optionBtn.dataset.index = index;
            optionBtn.dataset.value = option;
            optionBtn.addEventListener('click', () => submitAnswerLocally(option));
            
            optionCol.appendChild(optionBtn);
            elements.game.optionsContainer.appendChild(optionCol);
        });
        
        // Notify the other player that we've moved to the next round
        if (stompClient && stompClient.connected) {
            stompClient.send('/app/game.nextRound', { roomCode: currentRoomCode }, JSON.stringify({
                type: 'NEXT_ROUND',
                roomCode: currentRoomCode,
                roundNumber: gameState.roundNumber
            }));
        }
        
        logMessage(`Round ${gameState.roundNumber} started`, 'info');
    }
    
    // Handle next round message from the other player
    function handleNextRound(message) {
        // If we're already on this round, ignore
        if (message.roundNumber === gameState.roundNumber) {
            return;
        }
        
        // If we're behind, catch up
        if (message.roundNumber > gameState.roundNumber) {
            startNextRound();
        }
    }

    // Submit answer locally
    function submitAnswerLocally(selectedOption) {
        if (gameState.hasAnswered) {
            return;
        }
        
        const currentCard = gameState.gameCards[gameState.currentCardIndex];
        const isCorrect = selectedOption === currentCard.back;
        
        // Update score
        if (isCorrect) {
            gameState.yourScore++;
            if (isHost) {
                gameState.hostScore++;
            } else {
                gameState.guestScore++;
            }
        }
        
        // Mark as answered
        gameState.hasAnswered = true;
        gameState.selectedOption = selectedOption;
        
        // Update UI
        elements.game.yourScore.textContent = gameState.yourScore;
        
        // Show feedback
        const optionButtons = elements.game.optionsContainer.querySelectorAll('.option-btn');
        optionButtons.forEach(button => {
            const optionValue = button.dataset.value;
            
            // Disable all buttons
            button.disabled = true;
            
            // Mark selected option
            if (optionValue === selectedOption) {
                if (isCorrect) {
                    button.classList.add('correct');
                } else {
                    button.classList.add('incorrect');
                }
            }
            
            // Mark correct answer if user selected wrong
            if (optionValue === currentCard.back && !isCorrect) {
                button.classList.add('correct');
            }
        });
        
        // Show answer feedback
        elements.game.answerFeedback.style.display = 'block';
        if (isCorrect) {
            elements.game.answerFeedback.className = 'alert alert-success';
            elements.game.answerFeedback.textContent = 'Correct! +1 point';
        } else {
            elements.game.answerFeedback.className = 'alert alert-danger';
            elements.game.answerFeedback.textContent = `Incorrect. The correct answer is: ${currentCard.back}`;
        }
        
        // Notify the other player about our answer
        if (stompClient && stompClient.connected) {
            stompClient.send('/app/game.submitAnswer', 
                { roomCode: currentRoomCode }, 
                JSON.stringify({
                    type: 'ANSWER_SUBMITTED',
                    roomCode: currentRoomCode,
                    senderId: firebaseUid,
                    senderUsername: playerUsername,
                    content: selectedOption,
                    hostScore: gameState.hostScore,
                    guestScore: gameState.guestScore,
                    isCorrect: isCorrect
                })
            );
        }
        
        // If opponent has already answered, move to next round after delay
        if (gameState.opponentAnswered) {
            setTimeout(startNextRound, CONFIG.GAME.ROUND_TRANSITION_DELAY);
        }
        
        logMessage(`You answered: ${selectedOption} (${isCorrect ? 'Correct' : 'Incorrect'})`, 'info');
    }
    
    // Handle answer submitted message from opponent
    function handleAnswerSubmitted(message) {
        // Mark opponent as answered
        gameState.opponentAnswered = true;
        
        // Update scores
        gameState.hostScore = message.hostScore;
        gameState.guestScore = message.guestScore;
        
        // Update opponent score based on whether we're host or guest
        if (isHost) {
            gameState.opponentScore = message.guestScore;
        } else {
            gameState.opponentScore = message.hostScore;
        }
        
        // Update UI
        elements.game.opponentScore.textContent = gameState.opponentScore;
        
        logMessage(`${message.senderUsername} submitted answer: ${message.content}`, 'info');
        
        // If we've already answered, move to next round after delay
        if (gameState.hasAnswered) {
            setTimeout(startNextRound, CONFIG.GAME.ROUND_TRANSITION_DELAY);
        }
    }
    
    // End the game locally
    function endGame() {
        // Determine the winner
        let winnerUsername;
        let isWinner = false;
        
        if (gameState.hostScore > gameState.guestScore) {
            winnerUsername = isHost ? playerUsername : 'Opponent';
            isWinner = isHost;
        } else if (gameState.guestScore > gameState.hostScore) {
            winnerUsername = !isHost ? playerUsername : 'Opponent';
            isWinner = !isHost;
        } else {
            winnerUsername = 'Draw';
        }
        
        // Create game result
        const gameResult = {
            roomCode: currentRoomCode,
            hostUsername: isHost ? playerUsername : 'Opponent',
            guestUsername: !isHost ? playerUsername : 'Opponent',
            hostScore: gameState.hostScore,
            guestScore: gameState.guestScore,
            winnerUsername: winnerUsername
        };
        
        // Show game over message
        handleGameOver({
            type: 'GAME_OVER',
            gameResult: gameResult
        });
        
        // Notify the other player
        if (stompClient && stompClient.connected) {
            stompClient.send('/app/game.endGame', 
                { roomCode: currentRoomCode }, 
                JSON.stringify({
                    type: 'GAME_OVER',
                    roomCode: currentRoomCode,
                    gameResult: gameResult
                })
            );
        }
    }

    // Handle game over message
    function handleGameOver(message) {
        const result = message.gameResult;
        
        // Update final scores
        if (isHost) {
            elements.results.player1Name.textContent = result.hostUsername + ' (You)';
            elements.results.player1Score.textContent = result.hostScore;
            elements.results.player2Name.textContent = result.guestUsername;
            elements.results.player2Score.textContent = result.guestScore;
            
            // Determine if we won
            if (result.hostScore > result.guestScore) {
                elements.results.winnerText.textContent = 'You Won!';
                elements.results.winnerDisplay.className = 'alert alert-success mb-4';
            } else if (result.hostScore < result.guestScore) {
                elements.results.winnerText.textContent = 'You Lost!';
                elements.results.winnerDisplay.className = 'alert alert-danger mb-4';
            } else {
                elements.results.winnerText.textContent = 'It\'s a Draw!';
                elements.results.winnerDisplay.className = 'alert alert-warning mb-4';
            }
        } else {
            elements.results.player1Name.textContent = result.hostUsername;
            elements.results.player1Score.textContent = result.hostScore;
            elements.results.player2Name.textContent = result.guestUsername + ' (You)';
            elements.results.player2Score.textContent = result.guestScore;
            
            // Determine if we won
            if (result.guestScore > result.hostScore) {
                elements.results.winnerText.textContent = 'You Won!';
                elements.results.winnerDisplay.className = 'alert alert-success mb-4';
            } else if (result.guestScore < result.hostScore) {
                elements.results.winnerText.textContent = 'You Lost!';
                elements.results.winnerDisplay.className = 'alert alert-danger mb-4';
            } else {
                elements.results.winnerText.textContent = 'It\'s a Draw!';
                elements.results.winnerDisplay.className = 'alert alert-warning mb-4';
            }
        }
        
        // Show results section
        showSection('results');
        
        logMessage('Game over', 'info');
    }

    // Handle player left message
    function handlePlayerLeft(message) {
        logMessage(`${message.senderUsername} left the room ${message.roomCode}`, 'info');
        
        // Check if this is a message for our current room
        if (message.roomCode !== currentRoomCode) {
            logMessage(`Ignoring leave message for different room: ${message.roomCode}`, 'warning');
            return;
        }
        
        // If the player leaving is ourselves, we might have been kicked
        if (message.senderUsername === playerUsername) {
            logMessage('We were removed from the room', 'warning');
            leaveRoom(isHost);
            return;
        }
        
        // Remove the player from the appropriate list
        if (isHost) {
            // Find and remove the player from host's player list
            const playerItems = elements.create.playersList.querySelectorAll('li');
            playerItems.forEach(item => {
                if (item.textContent.includes(message.senderUsername)) {
                    item.remove();
                    logMessage(`Removed ${message.senderUsername} from players list`, 'info');
                }
            });
            
            // If we're host and all guests left, disable start button
            if (elements.create.playersList.children.length <= 1) {
                elements.create.startGameBtn.disabled = true;
            }
        } else {
            // Find and remove the player from guest's player list
            const playerItems = elements.join.joinedPlayersList.querySelectorAll('li');
            playerItems.forEach(item => {
                if (item.textContent.includes(message.senderUsername)) {
                    item.remove();
                    logMessage(`Removed ${message.senderUsername} from players list`, 'info');
                }
            });
            
            // If we're a guest and the host left, return to lobby
            if (message.senderUsername.includes('Host')) {
                logMessage('Host left the room, returning to lobby', 'warning');
                leaveRoom(false);
                return;
            }
        }
        
        // If we're in a game and a player left, return to lobby
        if (elements.sections.game.classList.contains('active')) {
            logMessage('A player left during the game, returning to lobby', 'warning');
            showSection('lobby');
        }
    }

    // Handle error message
    function handleError(message) {
        const errorContent = message.content || 'Unknown error';
        logMessage(`Error: ${errorContent}`, 'error');
        
        // Display error to user with a toast/alert
        const errorToast = document.createElement('div');
        errorToast.className = 'toast show bg-danger text-white';
        errorToast.setAttribute('role', 'alert');
        errorToast.setAttribute('aria-live', 'assertive');
        errorToast.setAttribute('aria-atomic', 'true');
        errorToast.style.position = 'fixed';
        errorToast.style.bottom = '20px';
        errorToast.style.right = '20px';
        errorToast.style.minWidth = '250px';
        errorToast.style.zIndex = '1050';
        
        errorToast.innerHTML = `
            <div class="toast-header bg-danger text-white">
                <strong class="me-auto">Error</strong>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast"></button>
            </div>
            <div class="toast-body">
                ${errorContent}
            </div>
        `;
        
        document.body.appendChild(errorToast);
        
        // Automatically remove the toast after 5 seconds
        setTimeout(() => {
            errorToast.remove();
        }, 5000);
        
        // Close button functionality
        const closeBtn = errorToast.querySelector('.btn-close');
        closeBtn.addEventListener('click', () => {
            errorToast.remove();
        });
        
        // Handle specific error types
        if (errorContent.includes('Room not found') || errorContent.includes('already full')) {
            // Reset room state if we tried to join a non-existent or full room
            if (!isHost) {
                resetRoomState();
                elements.join.joinRoomBtn.disabled = false;
                elements.join.joinedRoomInfo.style.display = 'none';
                elements.join.joinedRoomPlayers.style.display = 'none';
            }
        } else if (errorContent.includes('Failed to start game')) {
            // Re-enable start button if game start failed
            if (isHost) {
                elements.create.startGameBtn.disabled = false;
            }
        }
    }

    // Initialize the application
    function init() {
        initEventListeners();
        updateConnectionStatus('disconnected');
        logMessage('Application initialized', 'info');
    }

    // Start the application
    init();
});

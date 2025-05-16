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
        totalRounds: 15
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
            leaveGameBtn: document.getElementById('leaveGameBtn'),
            yourLabel: document.getElementById('yourLabel'),
            opponentLabel: document.getElementById('opponentLabel')
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
        elements.game.leaveGameBtn.addEventListener('click', () => leaveRoom(isHost));
        
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
        connectWebSocket();
        
        // Move to lobby
        showSection('lobby');
        
        logMessage(`Logged in as ${playerUsername}`, 'success');
    }

    // Connection retry variables
    let connectionAttempts = 0;
    const maxConnectionAttempts = 3;
    let useAlternativeUrl = false;
    
    // Connect to WebSocket server
    function connectWebSocket() {
        updateConnectionStatus('connecting');
        logMessage(`Connecting to WebSocket server (attempt ${connectionAttempts+1})...`, 'info');
        
        // Disconnect if already connected
        if (stompClient) {
            stompClient.disconnect();
        }
        
        // Choose URL based on connection attempts
        const baseUrl = useAlternativeUrl ? CONFIG.ALT_API_URL : CONFIG.API_URL;
        logMessage(`Trying to connect to ${baseUrl}${CONFIG.WS_ENDPOINT}`, 'info');
        
        try {
            // Create SockJS and STOMP client
            const socket = new SockJS(baseUrl + CONFIG.WS_ENDPOINT);
            stompClient = Stomp.over(socket);
            
            // Disable debug logs in production
            if (!CONFIG.DEBUG) {
                stompClient.debug = null;
            }
            
            // Add socket event listeners for better error handling
            socket.onclose = function() {
                logMessage('SockJS connection closed', 'warning');
            };
            
            socket.onerror = function(error) {
                logMessage(`SockJS transport error: ${error}`, 'error');
            };
            
            // Connect to WebSocket with timeout
            const connectTimeout = setTimeout(() => {
                logMessage('Connection timeout - trying again', 'error');
                handleConnectionFailure();
            }, 10000); // 10 second timeout
            
            // Connect with simplified headers - only username and Firebase UID
            const headers = {};
            if (playerUsername) headers.username = playerUsername;
            if (firebaseUid) headers.firebaseUid = firebaseUid;
            
            // Connect to WebSocket
            stompClient.connect(
                headers,
                function() {
                    clearTimeout(connectTimeout);
                    connectionAttempts = 0;
                    onConnected();
                },
                function(error) {
                    clearTimeout(connectTimeout);
                    logMessage(`STOMP error: ${error}`, 'error');
                    handleConnectionFailure();
                }
            );
        } catch (e) {
            logMessage(`Connection exception: ${e.message}`, 'error');
            handleConnectionFailure();
        }
    }
    
    // Handle connection failure with retry logic
    function handleConnectionFailure() {
        connectionAttempts++;
        
        if (connectionAttempts >= maxConnectionAttempts) {
            // Switch to alternative URL if we've tried the main URL enough times
            if (!useAlternativeUrl) {
                useAlternativeUrl = true;
                connectionAttempts = 0;
                logMessage('Switching to alternative server URL', 'warning');
                setTimeout(connectWebSocket, 1000);
            } else {
                // Both URLs failed
                updateConnectionStatus('disconnected');
                logMessage('Failed to connect to both server URLs', 'error');
                alert('Unable to connect to the game server. Please check your internet connection or try again later.');
            }
        } else {
            // Try again with exponential backoff
            const delay = Math.min(1000 * Math.pow(2, connectionAttempts), 10000);
            logMessage(`Reconnecting in ${delay/1000} seconds (attempt ${connectionAttempts})`, 'info');
            setTimeout(connectWebSocket, delay);
        }
    }

    // Handle successful WebSocket connection
    function onConnected() {
        updateConnectionStatus('connected');
        logMessage('Connected to game server', 'success');
        
        // Subscribe to personal queue for direct messages
        stompClient.subscribe(CONFIG.SOCKET.PERSONAL_QUEUE, onMessageReceived);
        
        // Enable buttons that require connection
        elements.create.createRoomBtn.disabled = false;
        elements.join.joinRoomBtn.disabled = false;
    }

    // Handle WebSocket connection error
    function onError(error) {
        updateConnectionStatus('disconnected');
        logMessage(`Connection error: ${error}`, 'error');
        
        // Disable buttons that require connection
        elements.create.createRoomBtn.disabled = true;
        elements.join.joinRoomBtn.disabled = true;
        
        // Try to reconnect after a delay
        setTimeout(connectWebSocket, 5000);
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
                'X-Firebase-Uid': firebaseUid // Adding explicit Firebase UID header
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
            
            // IMPORTANT: Unsubscribe from any previous room topics first
            try {
                if (stompClient.subscriptions && stompClient.subscriptions['room-subscription']) {
                    stompClient.unsubscribe('room-subscription');
                    logMessage('Unsubscribed from previous room topic', 'info');
                }
            } catch (e) {
                logMessage(`Error unsubscribing: ${e.message}`, 'warning');
            }
            
            // Subscribe to room topic with explicit ID for better tracking
            stompClient.subscribe(
                CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode,
                function(payload) {
                    logMessage(`[HOST] Received room message: ${payload.body.substring(0, 100)}...`, 'info');
                    onMessageReceived(payload);
                },
                { id: 'room-subscription' }
            );
            logMessage(`Subscribed to room topic: ${CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode}`, 'success');
            
            // Update UI
            elements.create.roomCode.textContent = currentRoomCode;
            elements.create.roomInfo.style.display = 'block';
            elements.create.roomPlayers.style.display = 'block';
            
            // Add host to players list
            updatePlayersList();
            
            // Send MULTIPLE messages to the room topic to announce creation
            // This redundancy improves reliability
            
            // First message - room created announcement
            stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, 
                { firebaseUid: firebaseUid },
                JSON.stringify({
                    type: 'ROOM_CREATED',
                    roomCode: currentRoomCode,
                    senderId: firebaseUid,
                    senderUsername: playerUsername,
                    timestamp: Date.now()
                })
            );
            
            // Second message - join announcement (immediately after)
            stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, 
                { firebaseUid: firebaseUid },
                JSON.stringify({
                    type: 'ROOM_JOINED',
                    roomCode: currentRoomCode,
                    senderId: firebaseUid,
                    senderUsername: playerUsername,
                    timestamp: Date.now()
                })
            );
            
            // Third message - delayed join announcement for better reliability
            setTimeout(() => {
                if (stompClient && stompClient.connected) {
                    stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, 
                        { firebaseUid: firebaseUid },
                        JSON.stringify({
                            type: 'ROOM_JOINED',
                            roomCode: currentRoomCode,
                            senderId: firebaseUid,
                            senderUsername: playerUsername,
                            timestamp: Date.now(),
                            isDelayedMessage: true
                        })
                    );
                    logMessage('Sent delayed room join message for reliability', 'info');
                }
            }, 1000);
            
            // Schedule regular host presence messages for reliability
            setupHostPresenceInterval();
        })
        .catch(error => {
            logMessage(`Error creating room: ${error.message}`, 'error');
            
            // Try alternative approach - direct endpoint without /api prefix
            logMessage('Trying alternative approach for room creation...', 'info');
            
            fetch(`${CONFIG.ALT_API_URL}/game/room?firebaseUid=${firebaseUid}`, {
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
                
                // IMPORTANT: Unsubscribe from any previous room topics first
                try {
                    if (stompClient.subscriptions && stompClient.subscriptions['room-subscription']) {
                        stompClient.unsubscribe('room-subscription');
                        logMessage('Unsubscribed from previous room topic', 'info');
                    }
                } catch (e) {
                    logMessage(`Error unsubscribing: ${e.message}`, 'warning');
                }
                
                // Subscribe to room topic with explicit ID
                stompClient.subscribe(
                    CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode,
                    function(payload) {
                        logMessage(`[HOST-ALT] Received room message: ${payload.body.substring(0, 100)}...`, 'info');
                        onMessageReceived(payload);
                    },
                    { id: 'room-subscription' }
                );
                
                // Update UI
                elements.create.roomCode.textContent = currentRoomCode;
                elements.create.roomInfo.style.display = 'block';
                elements.create.roomPlayers.style.display = 'block';
                
                // Add host to players list
                updatePlayersList();
                
                // Send messages to the room topic (same redundancy pattern as above)
                stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, 
                    { firebaseUid: firebaseUid },
                    JSON.stringify({
                        type: 'ROOM_CREATED',
                        roomCode: currentRoomCode,
                        senderId: firebaseUid,
                        senderUsername: playerUsername,
                        timestamp: Date.now()
                    })
                );
                
                stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, 
                    { firebaseUid: firebaseUid },
                    JSON.stringify({
                        type: 'ROOM_JOINED',
                        roomCode: currentRoomCode,
                        senderId: firebaseUid,
                        senderUsername: playerUsername,
                        timestamp: Date.now()
                    })
                );
                
                // Setup host presence interval
                setupHostPresenceInterval();
            })
            .catch(altError => {
                logMessage(`Alternative approach also failed: ${altError.message}`, 'error');
                // Re-enable the create button
                elements.create.createRoomBtn.disabled = false;
            });
        });
    }
    
    // Set up an interval to periodically send host presence messages
    let hostPresenceInterval = null;
    function setupHostPresenceInterval() {
        // Clear any existing interval
        if (hostPresenceInterval) {
            clearInterval(hostPresenceInterval);
            logMessage('Cleared previous host presence interval', 'info');
        }
        
        // Send an immediate presence message to quickly inform any waiting guests
        if (isHost && currentRoomCode && stompClient && stompClient.connected) {
            logMessage('Sending immediate host presence message', 'info');
            // Send a presence message to the room
            stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, 
                { firebaseUid: firebaseUid },
                JSON.stringify({
                    type: 'HOST_PRESENCE',
                    roomCode: currentRoomCode,
                    senderId: firebaseUid,
                    senderUsername: playerUsername,
                    timestamp: Date.now(),
                    isHost: true
                })
            );
        }
        
        // Set up a new interval to send messages every 5 seconds
        // More frequent than before (was 10s) to improve responsiveness
        hostPresenceInterval = setInterval(() => {
            if (isHost && currentRoomCode && stompClient && stompClient.connected) {
                // Send a presence message to the room
                stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, 
                    { firebaseUid: firebaseUid },
                    JSON.stringify({
                        type: 'HOST_PRESENCE',
                        roomCode: currentRoomCode,
                        senderId: firebaseUid,
                        senderUsername: playerUsername,
                        timestamp: Date.now(),
                        isHost: true
                    })
                );
                
                if (CONFIG.RELIABILITY.VERBOSE_LOGGING) {
                    logMessage('Sent host presence message', 'info');
                }
            } else if (hostPresenceInterval && !isHost) {
                // No longer host, clear the interval
                clearInterval(hostPresenceInterval);
                hostPresenceInterval = null;
                logMessage('Cleared host presence interval (no longer host)', 'info');
            } else if (hostPresenceInterval && !currentRoomCode) {
                // No active room, clear the interval
                clearInterval(hostPresenceInterval);
                hostPresenceInterval = null;
                logMessage('Cleared host presence interval (no active room)', 'info');
            }
        }, 5000); // Every 5 seconds
        
        logMessage('Set up host presence interval (every 5s)', 'info');
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

    // Check if room exists via REST API before joining
    function checkRoomExists(roomCode) {
        logMessage(`Checking if room ${roomCode} exists...`, 'info');
        
        return fetch(`${CONFIG.API_URL}/api/game/room/${roomCode}/exists?firebaseUid=${firebaseUid}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'X-Firebase-Uid': firebaseUid
            }
        })
        .then(response => {
            if (!response.ok) {
                if (response.status === 404) {
                    throw new Error(`Room ${roomCode} does not exist`);
                }
                throw new Error(`Failed to check room. Status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            logMessage(`Room ${roomCode} exists: ${data.exists}`, 'info');
            return data.exists;
        })
        .catch(error => {
            // Try alternative URL
            if (useAlternativeUrl) {
                throw error; // Already using alternative URL, just throw
            }
            
            logMessage(`Error checking room, trying alternative URL: ${error.message}`, 'warning');
            
            const altUrl = `${CONFIG.ALT_API_URL}/game/room/${roomCode}/exists?firebaseUid=${firebaseUid}`;
            
            return fetch(altUrl, {
                method: 'GET',
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
            .then(data => {
                logMessage(`Room ${roomCode} exists (alt check): ${data.exists}`, 'info');
                return data.exists;
            });
        });
    }

    // Join an existing game room
    function joinRoom() {
        const roomCode = elements.join.joinRoomCodeInput.value.trim().toUpperCase();
        
        if (!roomCode) {
            logMessage('Please enter a room code', 'error');
            displayError('Please enter a room code');
            return;
        }
        
        logMessage(`Attempting to join room: ${roomCode}`, 'info');
        
        // Disable join button to prevent multiple attempts
        elements.join.joinRoomBtn.disabled = true;
        
        // Add a loading indicator
        const loadingIndicator = document.createElement('div');
        loadingIndicator.id = 'join-room-loading';
        loadingIndicator.className = 'spinner-border text-primary mt-2';
        loadingIndicator.style.width = '1.5rem';
        loadingIndicator.style.height = '1.5rem';
        loadingIndicator.setAttribute('role', 'status');
        loadingIndicator.innerHTML = '<span class="visually-hidden">Loading...</span>';
        elements.join.joinRoomBtn.parentNode.appendChild(loadingIndicator);
        
        // First verify the room exists
        fetch(`${CONFIG.API_URL}/api/game/room/${roomCode}/exists`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'X-Firebase-Uid': firebaseUid
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Error checking room. Status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (!data.exists) {
                throw new Error('Room does not exist');
            }
            
            logMessage('Room exists, proceeding with join...', 'success');
            
            // Room exists, try to join using server API first
            return fetch(`${CONFIG.API_URL}/api/game/room/${roomCode}/join`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Firebase-Uid': firebaseUid
                },
                body: JSON.stringify({
                    firebaseUid: firebaseUid,
                    username: playerUsername
                })
            });
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to join room. Status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            logMessage(`Successfully joined room ${roomCode} via REST API`, 'success');
            
            // REST API join succeeded, now set up WebSocket connectivity
            attemptJoinRoom(roomCode);
        })
        .catch(error => {
            logMessage(`Error checking/joining room: ${error.message}`, 'error');
            
            // Try alternative API URL if primary failed
            logMessage('Trying alternative API for room join...', 'info');
            
            fetch(`${CONFIG.ALT_API_URL}/game/room/${roomCode}/exists`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Firebase-Uid': firebaseUid
                }
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Alternative API error. Status: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                if (!data.exists) {
                    throw new Error('Room does not exist (alternative check)');
                }
                
                // Room exists, try to join using alternative endpoint
                return fetch(`${CONFIG.ALT_API_URL}/game/room/${roomCode}/join`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-Firebase-Uid': firebaseUid
                    },
                    body: JSON.stringify({
                        firebaseUid: firebaseUid,
                        username: playerUsername
                    })
                });
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Alternative join failed. Status: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                logMessage(`Successfully joined room ${roomCode} via alternative API`, 'success');
                
                // Alternative REST API join succeeded, now set up WebSocket connectivity
                attemptJoinRoom(roomCode);
            })
            .catch(altError => {
                logMessage(`All join attempts failed: ${altError.message}`, 'error');
                displayError(`Failed to join room: ${altError.message}`);
                
                // Clean up and re-enable button
                const loadingIndicator = document.getElementById('join-room-loading');
                if (loadingIndicator) {
                    loadingIndicator.remove();
                }
                elements.join.joinRoomBtn.disabled = false;
            });
        });
    }
    
    // Function to handle WebSocket aspects of joining a room
    function attemptJoinRoom(roomCode) {
        // Store room information
        currentRoomCode = roomCode;
        isHost = false;
        
        // Update UI
        elements.join.joinedRoomCode.textContent = roomCode;
        elements.join.joinedRoomInfo.style.display = 'block';
        elements.join.joinedRoomPlayers.style.display = 'block';
        
        // Hide join form elements and show joining status
        elements.join.joinRoomBtn.style.display = 'none';
        elements.join.joinRoomCodeInput.style.display = 'none';
        const joinFormLabel = document.querySelector('label[for="joinRoomCode"]');
        if (joinFormLabel) {
            joinFormLabel.style.display = 'none';
        }
        
        // Show a joining message
        const joiningStatusDiv = document.createElement('div');
        joiningStatusDiv.id = 'joining-status';
        joiningStatusDiv.className = 'alert alert-info mt-3';
        joiningStatusDiv.innerHTML = `
            <div class="d-flex align-items-center">
                <div class="spinner-border spinner-border-sm me-2" role="status"></div>
                <span>Joining room ${roomCode}...</span>
            </div>
        `;
        elements.join.joinRoomBtn.parentNode.appendChild(joiningStatusDiv);
        
        // Unsubscribe from any previous room topics
        try {
            if (stompClient.subscriptions && stompClient.subscriptions['room-subscription']) {
                stompClient.unsubscribe('room-subscription');
                logMessage('Unsubscribed from previous room topic', 'info');
            }
        } catch (e) {
            logMessage(`Error unsubscribing: ${e.message}`, 'warning');
        }
        
        // Subscribe to room topic with explicit debug output
        logMessage(`Subscribing to ${CONFIG.SOCKET.ROOM_TOPIC_PREFIX + roomCode}`, 'info');
        stompClient.subscribe(
            CONFIG.SOCKET.ROOM_TOPIC_PREFIX + roomCode,
            function(payload) {
                logMessage(`Got room message: ${payload.body.substring(0, 100)}...`, 'info');
                onMessageReceived(payload);
            },
            { id: 'room-subscription' }
        );
        
        // Debug: Log all subscription destinations
        if (stompClient.subscriptions) {
            logMessage(`Current subscriptions: ${Object.keys(stompClient.subscriptions).join(', ')}`, 'info');
        }
        
        // Add a player indicator for ourselves immediately for better UI feedback
        const existingSelf = Array.from(elements.join.joinedPlayersList.children)
            .some(item => item.textContent.includes(playerUsername));
        
        if (!existingSelf) {
            const guestItem = document.createElement('li');
            guestItem.className = 'list-group-item';
            guestItem.innerHTML = `
                <i class="fas fa-user player-icon"></i>
                ${playerUsername} (You)
                <span class="badge bg-secondary host-badge">Guest</span>
            `;
            elements.join.joinedPlayersList.appendChild(guestItem);
            logMessage('Added self to player list for immediate feedback', 'info');
        }
        
        // IMPORTANT: Send join message to application endpoint
        logMessage('Sending explicit JOIN_ROOM message to application endpoint', 'info');
        stompClient.send(CONFIG.SOCKET.ENDPOINTS.JOIN, {
            firebaseUid: firebaseUid,
            username: playerUsername,
            roomCode: roomCode // Add room code to headers for extra reliability
        }, JSON.stringify({
            type: 'JOIN_ROOM', // THIS MUST MATCH server expectation
            roomCode: roomCode,
            senderId: firebaseUid,
            senderUsername: playerUsername,
            timestamp: Date.now()
        }));
        
        // THEN send direct message to room for redundancy
        setTimeout(function() {
            logMessage('Sending ROOM_JOINED message directly to room topic', 'info');
            stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + roomCode, {
                firebaseUid: firebaseUid,
                username: playerUsername
            }, JSON.stringify({
                type: 'ROOM_JOINED',
                roomCode: roomCode,
                senderId: firebaseUid,
                senderUsername: playerUsername,
                timestamp: Date.now()
            }));
            
            // Also send a GUEST_JOINED message specifically for the host
            stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + roomCode, {
                firebaseUid: firebaseUid,
                username: playerUsername
            }, JSON.stringify({
                type: 'GUEST_JOINED',
                roomCode: roomCode,
                senderId: firebaseUid,
                senderUsername: playerUsername,
                timestamp: Date.now()
            }));
        }, 500); // Small delay to ensure proper order
        
        // Set a timeout to detect if join failed
        joinResponseTimeout = setTimeout(() => {
            // If no host player has been added, assume join failed to complete properly
            const hasHostPlayer = Array.from(elements.join.joinedPlayersList.children)
                .some(item => item.textContent.includes('Host'));
                
            if (!hasHostPlayer) {
                logMessage('No host confirmation received, sending another GUEST_JOINED message', 'warning');
                
                // Try direct message to room topic again
                stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + roomCode, {
                    firebaseUid: firebaseUid,
                    username: playerUsername
                }, JSON.stringify({
                    type: 'GUEST_JOINED',
                    roomCode: roomCode,
                    senderId: firebaseUid,
                    senderUsername: playerUsername,
                    timestamp: Date.now(),
                    isRetry: true
                }));
            }
            
            // Remove joining status message
            const joiningStatus = document.getElementById('joining-status');
            if (joiningStatus) {
                joiningStatus.remove();
            }
            
            // Remove loading indicator
            const loadingIndicator = document.getElementById('join-room-loading');
            if (loadingIndicator) {
                loadingIndicator.remove();
            }
            
            // Show join success message
            const joinedStatusDiv = document.createElement('div');
            joinedStatusDiv.id = 'joined-status';
            joinedStatusDiv.className = hasHostPlayer ? 'alert alert-success mt-3' : 'alert alert-warning mt-3';
            joinedStatusDiv.innerHTML = hasHostPlayer ? 
                `<strong>Successfully joined room!</strong>` : 
                `<strong>Joined room, waiting for host...</strong>`;
            elements.join.joinedRoomInfo.appendChild(joinedStatusDiv);
            
            // Auto-remove success message after 5 seconds
            setTimeout(() => {
                const joinedStatus = document.getElementById('joined-status');
                if (joinedStatus) {
                    joinedStatus.remove();
                }
            }, 5000);
            
        }, 3000);
        
        // Clear any existing join timeouts
        if (joinResponseTimeout) {
            clearTimeout(joinResponseTimeout);
        }
    }

    // Handle room joined message
    function handleRoomJoined(message) {
        logMessage(`Processing room join: ${message.senderUsername} joined room: ${message.roomCode}`, 'info');
        
        // Check if this is a message for our current room
        if (message.roomCode !== currentRoomCode) {
            logMessage(`Ignoring message for different room: ${message.roomCode}`, 'warning');
            return;
        }
        
        // Remove joining status if it exists
        const joiningStatus = document.getElementById('joining-status');
        if (joiningStatus) {
            joiningStatus.remove();
        }

        // Remove loading indicator if it exists
        const loadingIndicator = document.getElementById('join-room-loading');
        if (loadingIndicator) {
            loadingIndicator.remove();
        }
        
        // Check if this is our own message
        const isSelfMessage = message.senderUsername === playerUsername || message.senderId === firebaseUid;
        
        // Debug logs for diagnosis
        logMessage(`Processing join: ${message.senderUsername}, isSelf=${isSelfMessage}, isHost=${isHost}`, 'info');
        
        // Different handling based on whether we're host or guest
        if (isHost) {
            // As host, ensure we're showing in our own player list first
            const hostExists = Array.from(elements.create.playersList.children)
                .some(item => item.textContent.includes(playerUsername) && item.textContent.includes('Host'));
                
            if (!hostExists) {
                // Add ourselves (host) to the players list
                const hostItem = document.createElement('li');
                hostItem.className = 'list-group-item';
                hostItem.innerHTML = `
                    <i class="fas fa-user player-icon"></i>
                    ${playerUsername} (You)
                    <span class="badge bg-primary host-badge">Host</span>
                `;
                elements.create.playersList.appendChild(hostItem);
                logMessage('Added self (host) to player list', 'info');
            }
            
            // Now check for the joining guest
            if (!isSelfMessage) {
                // Check if guest is already in the list
                const existingGuest = Array.from(elements.create.playersList.children)
                .some(item => item.textContent.includes(message.senderUsername));
                
                if (!existingGuest) {
                // Add guest to players list
                const guestItem = document.createElement('li');
                guestItem.className = 'list-group-item';
                guestItem.innerHTML = `
                    <i class="fas fa-user player-icon"></i>
                    ${message.senderUsername}
                    <span class="badge bg-secondary host-badge">Guest</span>
                `;
                elements.create.playersList.appendChild(guestItem);
                
                // Enable start button when a guest joins
                elements.create.startGameBtn.disabled = false;
                logMessage(`Guest ${message.senderUsername} added to room - enabling Start Game button`, 'success');
                
                // Show success message
                    showSuccessToast('Guest Joined', `${message.senderUsername} has joined your room!`, 3);
                
                // Send acknowledgement to ensure bidirectional visibility
                stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, 
                    { firebaseUid: firebaseUid },
                    JSON.stringify({
                        type: 'ROOM_JOINED',
                        roomCode: currentRoomCode,
                        senderId: firebaseUid,
                        senderUsername: playerUsername,
                        timestamp: Date.now(),
                        isHostReply: true, 
                        message: `Host ${playerUsername} acknowledges guest ${message.senderUsername}`
                    })
                );
            }
            }
        } else { // We are a guest
            // Ensure we're showing in our own player list first
            const selfExists = Array.from(elements.join.joinedPlayersList.children)
                .some(item => item.textContent.includes(playerUsername) && item.textContent.includes('You'));
                
            if (!selfExists) {
                // Add ourselves (guest) to the players list
                const guestItem = document.createElement('li');
                guestItem.className = 'list-group-item';
                guestItem.innerHTML = `
                <i class="fas fa-user player-icon"></i>
                    ${playerUsername} (You)
                    <span class="badge bg-secondary host-badge">Guest</span>
            `;
                elements.join.joinedPlayersList.appendChild(guestItem);
                logMessage('Added self (guest) to player list', 'info');
            }
            
            // If message is from another player (likely the host)
            if (!isSelfMessage) {
                // Check if host is already in the list
            const existingHost = Array.from(elements.join.joinedPlayersList.children)
                .some(item => item.textContent.includes(message.senderUsername) && 
                              item.textContent.includes('Host'));
                
            if (!existingHost) {
                    // Assume it's the host if it's not us and we're the guest
                    const hostItem = document.createElement('li');
                    hostItem.className = 'list-group-item';
                    hostItem.innerHTML = `
                        <i class="fas fa-user player-icon"></i>
                        ${message.senderUsername}
                        <span class="badge bg-primary host-badge">Host</span>
                    `;
                elements.join.joinedPlayersList.appendChild(hostItem);
                    logMessage(`Host ${message.senderUsername} added to player list`, 'success');
                    
                    // Show success message
                    showSuccessToast('Joined Room', `You've joined ${message.senderUsername}'s room!`, 3);
                
                // Ensure our UI is properly showing the room
                elements.join.joinedRoomInfo.style.display = 'block';
                elements.join.joinedRoomPlayers.style.display = 'block';
                
                // Send back another confirmation to ensure host sees us
                    sendRoomMessage('GUEST_JOINED', {
                        isGuestReply: true,
                        message: `Guest ${playerUsername} acknowledges host ${message.senderUsername}`
                    });
                }
            }
        }
        
        // Always make sure the UI is properly updated
        if (isHost) {
            elements.create.roomInfo.style.display = 'block';
            elements.create.roomPlayers.style.display = 'block';
        } else {
            elements.join.joinedRoomInfo.style.display = 'block';
            elements.join.joinedRoomPlayers.style.display = 'block';
        }
    }

    // Handle incoming WebSocket messages
    function onMessageReceived(payload) {
        try {
            let message;
            
            // Check if message is already an object (possibly pre-parsed)
            if (typeof payload.body === 'object') {
                message = payload.body;
                logMessage('Received pre-parsed message object', 'info');
            } else {
                // Try to parse the message body as JSON
                try {
                    message = JSON.parse(payload.body);
                } catch (parseError) {
                    logMessage(`Failed to parse message as JSON: ${parseError.message}`, 'error');
                    logMessage(`Raw message: ${payload.body}`, 'error');
                    return; // Cannot process unparseable messages
                }
            }
            
            // Check if message is valid
            if (!message || typeof message !== 'object') {
                logMessage(`Invalid message format: ${typeof message}`, 'error');
                return;
            }
            
            // Log the message type
            logMessage(`Received message: ${message.type || 'unknown type'}`, 'info');
            
            // Handle different message types
            switch (message.type) {
                case 'ROOM_CREATED':
                    // Add special handling for room creation
                    if (message.roomCode === currentRoomCode && !isHost) {
                        // We're joining a newly created room - treat like a join message
                        handleRoomJoined(message);
                    }
                    break;
                case 'ROOM_JOINED':
                    handleRoomJoined(message);
                    break;
                case 'GUEST_JOINED':
                    // Handle explicit GUEST_JOINED message sent by server specifically to hosts
                    if (isHost && message.roomCode === currentRoomCode) {
                        logMessage(`Received specific host notification for guest: ${message.senderUsername}`, 'success');
                        // Process same as ROOM_JOINED
                        handleRoomJoined(message);
                    }
                    break;
                case 'HOST_PRESENCE':
                    // Handle host presence message
                    if (!isHost && message.roomCode === currentRoomCode) {
                        logMessage(`Host presence detected: ${message.senderUsername}`, 'info');
                        
                        // Remove any loading indicator if still present
                        const loadingIndicator = document.getElementById('join-room-loading');
                        if (loadingIndicator) {
                            loadingIndicator.remove();
                        }
                        
                        // Make sure the host appears in the player list
                        const playersList = elements.join.joinedPlayersList;
                        let hostFound = false;
                        
                        if (playersList) {
                            const playerItems = playersList.querySelectorAll('li');
                            playerItems.forEach(item => {
                                if (item.textContent.includes('Host')) {
                                    hostFound = true;
                                }
                            });
                            
                            if (!hostFound) {
                                // Add host to player list
                            const hostItem = document.createElement('li');
                            hostItem.className = 'list-group-item';
                            hostItem.innerHTML = `
                                <i class="fas fa-user player-icon"></i>
                                ${message.senderUsername}
                                <span class="badge bg-primary host-badge">Host</span>
                            `;
                                playersList.appendChild(hostItem);
                            }
                        }
                        
                        // Make sure the room info is displayed
                        elements.join.joinedRoomInfo.style.display = 'block';
                        elements.join.joinedRoomPlayers.style.display = 'block';
                        
                        // Re-enable join button if disabled
                        elements.join.joinRoomBtn.disabled = false;
                    }
                    break;
                case 'GAME_STARTED':
                    handleGameStarted(message);
                    break;
                case 'ANSWER_SUBMITTED':
                    handleAnswerSubmitted(message);
                    break;
                case 'ROUND_SYNC':
                        handleRoundSync(message);
                    break;
                case 'NEXT_ROUND':
                    handleNextRound(message);
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
                case 'DIAGNOSTIC_PING':
                    logMessage(`Diagnostic ping received from ${message.senderUsername}`, 'info');
                    // Echo back if we're the host
                    if (isHost && message.senderId !== firebaseUid) {
                        stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, {}, JSON.stringify({
                            type: 'DIAGNOSTIC_RESPONSE',
                            roomCode: currentRoomCode,
                            senderId: firebaseUid,
                            senderUsername: playerUsername,
                            timestamp: Date.now(),
                            responseToId: message.senderId
                        }));
                    }
                    break;
                case 'DIAGNOSTIC_RESPONSE':
                    if (message.responseToId === firebaseUid) {
                        logMessage(`Diagnostic response from host: ${message.senderUsername}`, 'success');
                    }
                    break;
                default:
                    // Handle the case where message is an object without a type
                    if (message && typeof message === 'object') {
                        logMessage(`Message without proper type: ${JSON.stringify(message).substring(0, 100)}...`, 'warning');
                        
                        // Try to extract room code and username if they exist
                        if (message.roomCode && message.roomCode === currentRoomCode) {
                            if (message.senderUsername || message.senderId) {
                                // Treat as join message with fabricated type
                        handleRoomJoined({
                            ...message,
                                    type: 'ROOM_JOINED',
                                    senderUsername: message.senderUsername || 'Unknown User',
                                    senderId: message.senderId || 'unknown-id'
                        });
                            }
                        }
                    } else {
                        logMessage(`Unknown message type: ${message ? message.type : 'undefined'}`, 'warning');
                    }
            }
            
            // Always make sure the loading indicator is removed after any message
            const loadingIndicator = document.getElementById('join-room-loading');
            if (loadingIndicator && message.roomCode === currentRoomCode) {
                loadingIndicator.remove();
                logMessage('Removed join loading indicator after message received', 'info');
            }
            
        } catch (error) {
            logMessage(`Error processing message: ${error.message}`, 'error');
            if (payload && payload.body) {
                logMessage(`Problematic payload: ${typeof payload.body === 'string' ? payload.body : JSON.stringify(payload.body)}`, 'error');
            }
            
            // Remove loading indicator even on error
            const loadingIndicator = document.getElementById('join-room-loading');
            if (loadingIndicator) {
                loadingIndicator.remove();
                logMessage('Removed join loading indicator after error', 'info');
            }
        }
    }
    
    // Handle round synchronization
    function handleRoundSync(message) {
        if (message.roomCode !== currentRoomCode) {
            return; // Not for our room
        }
        
        logMessage(`Received round sync for round ${message.roundNumber}`, 'info');
        
        // Check if we need to update our round
        if (message.roundNumber > gameState.roundNumber) {
            logMessage(`We're behind (our round: ${gameState.roundNumber}, sync round: ${message.roundNumber}), catching up`, 'warning');
            startNextRound(message.currentCardIndex);
        } else if (message.roundNumber < gameState.roundNumber) {
            logMessage(`We're ahead (our round: ${gameState.roundNumber}, sync round: ${message.roundNumber}), ignoring sync`, 'warning');
        }
        
        // Update scores from host (source of truth)
        if (message.hostScore !== undefined && message.guestScore !== undefined) {
            gameState.hostScore = message.hostScore;
            gameState.guestScore = message.guestScore;
            
                if (isHost) {
                gameState.yourScore = gameState.hostScore;
                gameState.opponentScore = gameState.guestScore;
                } else {
                gameState.yourScore = gameState.guestScore;
                gameState.opponentScore = gameState.hostScore;
            }
            
            // Update score display
            document.getElementById('yourScore').textContent = gameState.yourScore;
            document.getElementById('opponentScore').textContent = gameState.opponentScore;
        }
    }
    
    // Handle answer submitted by other player
    function handleAnswerSubmitted(message) {
        if (message.roomCode !== currentRoomCode || message.senderId === firebaseUid) {
            return; // Not for our room or it's our own message
        }
        
        logMessage(`Opponent answered: ${message.optionText || 'unknown option'} (${message.isCorrect ? 'Correct' : 'Incorrect'})`, 'info');
        
        // Mark that opponent has answered
        gameState.opponentAnswered = true;
        
        // Update opponent score
        if (message.isCorrect) {
            if (isHost) {
                // We're host, so opponent is guest
                gameState.guestScore = message.guestScore || (gameState.guestScore + 1);
                gameState.opponentScore = gameState.guestScore;
            } else {
                // We're guest, so opponent is host
                gameState.hostScore = message.hostScore || (gameState.hostScore + 1);
                gameState.opponentScore = gameState.hostScore;
            }
            document.getElementById('opponentScore').textContent = gameState.opponentScore;
        }
        
        // Remove waiting notification if it exists
        const waitingToast = document.querySelector('.toast-notification');
        if (waitingToast && waitingToast.textContent.includes('Waiting for opponent')) {
            waitingToast.remove();
        }
        
        // Show notification that opponent answered
        showSuccessToast('Opponent Answered', 
            `Your opponent answered and was ${message.isCorrect ? 'correct' : 'incorrect'}.`, 3);
        
        // If we've also answered, move to next round after delay
        if (gameState.hasAnswered) {
            logMessage('Both players have answered, moving to next round soon', 'info');
            
            // Don't wait for setTimeout to complete - mark with flag
            window.nextRoundScheduled = true;
            
            setTimeout(() => {
                // Only proceed if another next round hasn't been triggered
                if (window.nextRoundScheduled) {
                    window.nextRoundScheduled = false;
            startNextRound();
                }
            }, 2000);
        }
    }

    // Start the game
    function startGame() {
        if (!currentRoomCode) {
            logMessage('No active room', 'error');
            return;
        }
        
        logMessage('Starting game...', 'info');
        
        // Disable start button to prevent multiple clicks
        elements.create.startGameBtn.disabled = true;
        
        // Add loading indicator
        const loadingIndicator = document.createElement('div');
        loadingIndicator.id = 'start-game-loading';
        loadingIndicator.className = 'spinner-border text-primary mt-2';
        loadingIndicator.style.width = '1.5rem';
        loadingIndicator.style.height = '1.5rem';
        loadingIndicator.setAttribute('role', 'status');
        loadingIndicator.innerHTML = '<span class="visually-hidden">Loading...</span>';
        elements.create.startGameBtn.parentNode.appendChild(loadingIndicator);
        
        // IMMEDIATE ACTION: Initialize static game cards regardless of server response
        initializeGameCards();
        
        try {
        // Send multiple start game messages for redundancy using different channels
        
        // 1. Send via application endpoint
        stompClient.send(CONFIG.SOCKET.ENDPOINTS.START, 
            {
                firebaseUid: firebaseUid,
                roomCode: currentRoomCode
            }, 
            JSON.stringify({
                type: 'GAME_STARTED',
                roomCode: currentRoomCode,
                senderId: firebaseUid,
                senderUsername: playerUsername,
                timestamp: Date.now()
            })
        );
        
        // 2. Send directly to room topic for redundancy
        stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, 
            {
                firebaseUid: firebaseUid,
                roomCode: currentRoomCode
            }, 
            JSON.stringify({
                type: 'GAME_STARTED',
                roomCode: currentRoomCode,
                senderId: firebaseUid,
                senderUsername: playerUsername,
                timestamp: Date.now()
            })
        );
        
        // 3. Try to start game via REST call as well (triple redundancy)
        fetch(`${CONFIG.API_URL}/api/game/room/${currentRoomCode}/start`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Firebase-Uid': firebaseUid
            },
            body: JSON.stringify({
                roomCode: currentRoomCode,
                hostId: firebaseUid,
                hostUsername: playerUsername
            })
        })
        .then(response => {
            if (response.ok) {
                logMessage('REST game start successful', 'success');
            } else {
                logMessage(`REST game start failed with status: ${response.status}`, 'warning');
            }
        })
        .catch(error => {
            logMessage(`Game start REST error: ${error.message}`, 'warning');
        });
        } catch (e) {
            logMessage(`Error sending game start messages: ${e.message}`, 'error');
        }
        
        // ALWAYS START THE GAME LOCALLY - don't wait for server response
        // This ensures the game starts even if the server is having issues
        setTimeout(() => {
            forceGameStart();
        }, 500);
        
        // Clear any existing timeouts
        if (window.gameStartTimeout) {
            clearTimeout(window.gameStartTimeout);
        }
        
        // Set a shorter timeout as backup
        window.gameStartTimeout = setTimeout(() => {
            logMessage('No game start acknowledgment received, forcing local game start', 'warning');
            forceGameStart();
        }, 2000); // Shorter timeout (2 seconds)
    }
    
    // Force the game to start locally regardless of server state
    function forceGameStart() {
        // Remove any loading indicators
        const loadingIndicator = document.getElementById('start-game-loading');
        if (loadingIndicator) {
            loadingIndicator.remove();
        }
        
        // Clear any existing game start timeout
        if (window.gameStartTimeout) {
            clearTimeout(window.gameStartTimeout);
            window.gameStartTimeout = null;
        }
        
        // Only start if we haven't already moved to the game section
        if (!elements.sections.game.classList.contains('active')) {
            logMessage('Forcing local game start', 'warning');
            
            // Reset game state with static cards
        gameState = {
            roundNumber: 0,
            hostScore: 0,
            guestScore: 0,
            yourScore: 0,
            opponentScore: 0,
            selectedOption: null,
            hasAnswered: false,
            opponentAnswered: false,
                gameCards: [...STATIC_CARDS], // Use static cards
            currentCardIndex: -1,
                totalRounds: STATIC_CARDS.length // Use all 15 questions
            };
            
            // Ensure we have deterministically shuffled cards with room code as seed
        const seed = currentRoomCode.split('').reduce((acc, char) => {
            return acc + char.charCodeAt(0);
        }, 0);
        
        deterministicShuffle(gameState.gameCards, seed);
            logMessage(`Shuffled ${gameState.gameCards.length} cards with seed ${seed}`, 'info');
        
        // Update UI
        elements.game.yourScore.textContent = 0;
        elements.game.opponentScore.textContent = 0;
        
            // Switch to game view
        showSection('game');
        
            // Show toast notification
        showSuccessToast('Game started!', 'The game has begun. Good luck!');
        
            // Start first round immediately
            setTimeout(startNextRound, 500);
        }
    }
    
    // Handle game started message
    function handleGameStarted(message) {
        if (message.roomCode !== currentRoomCode) {
            return; // Not for our room
        }
        
        logMessage(`Game started in room ${message.roomCode}`, 'success');
        showSuccessToast('Game Started', 'The game has begun! Get ready for the first question.', 5);
        
        // Initialize game state
        gameState = {
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
            totalRounds: 15
        };
        
        // Set up game with static cards
        const seed = parseInt(message.roomCode.replace(/[^0-9]/g, '')) || Date.now();
        logMessage(`Using seed ${seed} for card shuffling`, 'info');
        
        // Use static cards instead of waiting for server
        const STATIC_CARDS = window.STATIC_CARDS || []; // Use from cards.js if available
        
        if (STATIC_CARDS && STATIC_CARDS.length > 0) {
            // Initialize game with static cards
            gameState.gameCards = [...STATIC_CARDS];
            deterministicShuffle(gameState.gameCards, seed);
            logMessage(`Game initialized with ${gameState.gameCards.length} static cards`, 'success');
            
            // Switch to game section
            switchSection('gameSection');
            
            // If we're a guest, send acknowledgment to host that we've started the game
            if (!isHost) {
                sendRoomMessage({
                    type: 'GAME_START_ACK',
                    roomCode: currentRoomCode,
                    senderId: firebaseUid,
                    senderUsername: playerUsername,
                    timestamp: Date.now()
                });
            }
            
            // Start first round after a brief delay to ensure both players are ready
        setTimeout(() => {
                startNextRound(0);
        }, 1000);
        } else {
            // Fallback if static cards aren't available
            logMessage('Error: No static cards available for the game', 'error');
            showSuccessToast('Error', 'Failed to load game cards', 5);
        }
    }
    
    // Send acknowledgment that guest received game start message
    function sendGameStartAcknowledgment() {
        if (stompClient && stompClient.connected && currentRoomCode) {
            logMessage('Sending game start acknowledgment to host', 'info');
            
            // Send directly to room topic
            stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, 
                {
                    firebaseUid: firebaseUid,
                    roomCode: currentRoomCode
                }, 
                JSON.stringify({
                    type: 'GAME_START_ACK',
                    roomCode: currentRoomCode,
                    senderId: firebaseUid,
                    senderUsername: playerUsername,
                    timestamp: Date.now()
                })
            );
        }
    }

    // Handle game start acknowledgment from guests
    function handleGameStartAck(message) {
        if (message.roomCode === currentRoomCode && isHost) {
            logMessage(`Received game start acknowledgment from ${message.senderUsername}`, 'success');
            
            // Remove loading indicator if the host is waiting
            const loadingIndicator = document.getElementById('start-game-loading');
            if (loadingIndicator) {
                loadingIndicator.remove();
            }
            
            // Clear any game start timeout as we know the guest received the message
            if (window.gameStartTimeout) {
                clearTimeout(window.gameStartTimeout);
                window.gameStartTimeout = null;
                logMessage('Cleared game start timeout after receiving acknowledgment', 'info');
            }
            
            // Show success toast to host
            showSuccessToast(`${message.senderUsername} has joined the game`, 'Game is in progress for both players');
        }
    }
    
    // Initialize game cards from static data
    function initializeGameCards() {
        try {
            // Make sure STATIC_CARDS is defined and not empty
            if (!STATIC_CARDS || STATIC_CARDS.length === 0) {
                logMessage('ERROR: STATIC_CARDS is not defined or empty', 'error');
                // Fallback to hardcoded cards if STATIC_CARDS is not available
                gameState.gameCards = [
                    {
                        front: "What is the capital of France?",
                        back: "Paris",
                        options: ["Paris", "London", "Berlin", "Madrid"]
                    },
                    {
                        front: "What is 2 + 2?",
                        back: "4",
                        options: ["3", "4", "5", "6"]
                    },
                    {
                        front: "Which planet is closest to the sun?",
                        back: "Mercury",
                        options: ["Venus", "Earth", "Mars", "Mercury"]
                    }
                ];
                logMessage('Using fallback cards', 'warning');
            } else {
                // Use all 15 static cards from cards.js
                gameState.gameCards = [...STATIC_CARDS];
                logMessage(`Loaded ${gameState.gameCards.length} cards from STATIC_CARDS`, 'success');
            }
            
            // Use a simple seed if room code is not available
            let seedNumber = 12345;
            
            if (currentRoomCode) {
        // Use a consistent seed based on the room code for both players
                seedNumber = currentRoomCode.split('').reduce((acc, char) => {
            return acc + char.charCodeAt(0);
        }, 0);
            }
        
        logMessage(`Using seed ${seedNumber} for card shuffling`, 'info');
        
            // Use deterministic shuffle based on seed so all players see same order
        deterministicShuffle(gameState.gameCards, seedNumber);
        
            // Always use all 15 questions - don't limit them
            gameState.totalRounds = gameState.gameCards.length;
        gameState.currentCardIndex = -1;
        
            logMessage(`Initialized ${gameState.gameCards.length} game cards for play`, 'success');
            return true;
        } catch (error) {
            logMessage(`Error initializing game cards: ${error.message}`, 'error');
            return false;
        }
    }
    
    // Deterministic shuffle using a seed value
    function deterministicShuffle(array, seed) {
        // Simple deterministic pseudo-random number generator
        const random = (max) => {
            seed = (seed * 9301 + 49297) % 233280;
            return (seed / 233280) * max;
        };
        
        for (let i = array.length - 1; i > 0; i--) {
            const j = Math.floor(random(i + 1));
            [array[i], array[j]] = [array[j], array[i]];
        }
        
        return array;
    }
    
    // Regular shuffle using Fisher-Yates algorithm (kept for compatibility)
    function shuffleArray(array) {
        for (let i = array.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [array[i], array[j]] = [array[j], array[i]];
        }
        return array;
    }

    // Function to switch to a different section
    function switchSection(sectionId) {
        // Hide all sections first
        document.querySelectorAll('.game-section').forEach(section => {
            section.classList.remove('active');
        });
        
        // Show the requested section
        const targetSection = document.getElementById(sectionId);
        if (targetSection) {
            targetSection.classList.add('active');
            logMessage(`Switched to section: ${sectionId}`, 'info');
        } else {
            logMessage(`Section not found: ${sectionId}`, 'error');
        }
    }

    // Start next round with a specified card
    function startNextRound(forcedCardIndex = null) {
        try {
            // Increment round number
            if (forcedCardIndex !== null) {
                gameState.currentCardIndex = forcedCardIndex;
                gameState.roundNumber = forcedCardIndex + 1;
            } else {
        gameState.currentCardIndex++;
        gameState.roundNumber++;
            }
        
            // Check if we've reached the end of rounds
            if (gameState.currentCardIndex >= gameState.gameCards.length || 
                gameState.roundNumber > gameState.totalRounds) {
            endGame();
            return;
        }
        
        const currentCard = gameState.gameCards[gameState.currentCardIndex];
            logMessage(`Starting round ${gameState.roundNumber} with card: ${currentCard.question}`, 'info');
            
            // Reset round state
            gameState.selectedOption = null;
            gameState.hasAnswered = false;
            gameState.opponentAnswered = false;
            
            // Update round number display
            document.getElementById('roundNumber').textContent = gameState.roundNumber;
            
            // Display card question
            document.getElementById('cardQuestion').textContent = currentCard.question;
            
            // Get and shuffle options
            const options = [...currentCard.options];
            const optionsSeed = gameState.currentCardIndex + parseInt(currentRoomCode.replace(/[^0-9]/g, '')) || 1;
            deterministicShuffle(options, optionsSeed);
            
            // Clear previous options
            const optionsContainer = document.getElementById('optionsContainer');
            optionsContainer.innerHTML = '';
            
            // Add options to the UI
            options.forEach((option, index) => {
                const optionBtn = document.createElement('div');
                optionBtn.className = 'col-12 mb-2';
                optionBtn.innerHTML = `
                    <button class="btn btn-outline-primary option-btn w-100 py-3" data-index="${index}">
                        ${option}
                    </button>
                `;
                optionsContainer.appendChild(optionBtn);
            });
            
            // Add click event for options
            document.querySelectorAll('.option-btn').forEach(btn => {
                btn.addEventListener('click', function() {
                    const selectedIndex = parseInt(this.getAttribute('data-index'));
                    submitAnswer(selectedIndex, options[selectedIndex]);
                });
            });
            
            // Hide any previous feedback
            const answerFeedback = document.getElementById('answerFeedback');
            answerFeedback.style.display = 'none';
            
            // Send round sync message if host
            if (isHost) {
                sendRoomMessage({
                    type: 'ROUND_SYNC',
                roomCode: currentRoomCode,
                roundNumber: gameState.roundNumber,
                    currentCardIndex: gameState.currentCardIndex,
                hostScore: gameState.hostScore,
                guestScore: gameState.guestScore,
                    timestamp: Date.now()
                });
            }
            
            // Make sure we're in the game section
            switchSection('gameSection');
        } catch (error) {
            logMessage(`Error starting next round: ${error.message}`, 'error');
            showSuccessToast('Error', 'Failed to start next round', 5);
        }
    }

    // Submit an answer for the current round
    function submitAnswer(optionIndex, optionText) {
        if (gameState.hasAnswered) {
            // Already answered this round
            return;
        }
        
        logMessage(`Submitting answer: ${optionText} (index: ${optionIndex})`, 'info');
        
        // Mark this option as selected
        document.querySelectorAll('.option-btn').forEach(btn => {
            btn.disabled = true;
            if (parseInt(btn.getAttribute('data-index')) === optionIndex) {
                btn.classList.remove('btn-outline-primary');
                btn.classList.add('btn-primary');
            }
        });
        
        // Update game state
        gameState.hasAnswered = true;
        gameState.selectedOption = optionIndex;
        
        // Get correct answer
        const currentCard = gameState.gameCards[gameState.currentCardIndex];
        const correctOption = currentCard.correctOption;
        const isCorrect = optionText === correctOption;
        
        // Show feedback
        const answerFeedback = document.getElementById('answerFeedback');
        answerFeedback.className = 'alert ' + (isCorrect ? 'alert-success' : 'alert-danger');
        answerFeedback.innerHTML = isCorrect ? 
            '<i class="fas fa-check-circle"></i> Correct! +1 point' : 
            `<i class="fas fa-times-circle"></i> Incorrect! The correct answer is "${correctOption}"`;
        answerFeedback.style.display = 'block';
        
        // Update score
                if (isCorrect) {
            if (isHost) {
                gameState.hostScore++;
                gameState.yourScore = gameState.hostScore;
                } else {
                gameState.guestScore++;
                gameState.yourScore = gameState.guestScore;
            }
            document.getElementById('yourScore').textContent = gameState.yourScore;
        }
        
        // Send answer to other player
        sendRoomMessage({
                    type: 'ANSWER_SUBMITTED',
                    roomCode: currentRoomCode,
                    senderId: firebaseUid,
                    senderUsername: playerUsername,
            isHost: isHost,
            optionIndex: optionIndex,
            optionText: optionText,
            isCorrect: isCorrect,
            hostScore: isHost ? gameState.hostScore : gameState.hostScore,
            guestScore: isHost ? gameState.guestScore : gameState.guestScore,
            timestamp: Date.now()
        });
        
        // If opponent already answered, move to next round after delay
        if (gameState.opponentAnswered) {
            logMessage('Both players have answered, moving to next round soon', 'info');
            setTimeout(() => {
                startNextRound();
            }, 2000);
        } else {
            showSuccessToast('Waiting', 'Waiting for opponent to answer...', 0);
        }
    }
    
    // End the game and show results
    function endGame() {
        logMessage('Game over, showing results', 'info');
        
        // Update the results UI
        document.getElementById('player1Name').textContent = isHost ? playerUsername : 'Opponent';
        document.getElementById('player2Name').textContent = isHost ? 'Opponent' : playerUsername;
        document.getElementById('player1Score').textContent = isHost ? gameState.yourScore : gameState.opponentScore;
        document.getElementById('player2Score').textContent = isHost ? gameState.opponentScore : gameState.yourScore;
        
        // Determine winner
        const youWon = gameState.yourScore > gameState.opponentScore;
        const tie = gameState.yourScore === gameState.opponentScore;
        
        const winnerText = document.getElementById('winnerText');
        const winnerDisplay = document.getElementById('winnerDisplay');
        
        if (tie) {
            winnerText.textContent = "It's a Tie!";
            winnerDisplay.className = 'alert alert-info mb-4';
        } else if (youWon) {
            winnerText.textContent = "You Won!";
            winnerDisplay.className = 'alert alert-success mb-4';
        } else {
            winnerText.textContent = "You Lost!";
            winnerDisplay.className = 'alert alert-danger mb-4';
        }
        
        // Switch to results section
        switchSection('resultsSection');
        
        // Show a toast with the result
        if (tie) {
            showSuccessToast('Game Over', "It's a tie! Both players scored " + gameState.yourScore + " points.", 5);
        } else if (youWon) {
            showSuccessToast('Game Over', 'Congratulations! You won with ' + gameState.yourScore + ' points!', 5);
            } else {
            showSuccessToast('Game Over', 'You lost with ' + gameState.yourScore + ' points.', 5);
        }
    }
    
    // Handle next round message from server or other player
    function handleNextRound(message) {
        if (message.roomCode !== currentRoomCode) {
            return; // Not for our room
        }
        
        logMessage(`Received next round message for round ${message.roundNumber}`, 'info');
        
        // If we're already on this round or ahead, ignore
        if (message.roundNumber <= gameState.roundNumber) {
            return;
        }
        
        // Make sure the scores are in sync
        gameState.hostScore = message.hostScore;
        gameState.guestScore = message.guestScore;
        
        if (isHost) {
            gameState.yourScore = gameState.hostScore;
            gameState.opponentScore = gameState.guestScore;
        } else {
            gameState.yourScore = gameState.guestScore;
            gameState.opponentScore = gameState.hostScore;
        }
        
        // Update score display
        document.getElementById('yourScore').textContent = gameState.yourScore;
        document.getElementById('opponentScore').textContent = gameState.opponentScore;
        
        // Move to the next round
        gameState.roundNumber = message.roundNumber - 1; // Will be incremented in startNextRound
        startNextRound();
    }

    // Initialize the application
    function init() {
        initEventListeners();
        updateConnectionStatus('disconnected');
        logMessage('Application initialized', 'info');
        addDiagnosticsButton();
    }

    // Start the application
    init();

    // DEBUG FUNCTION: Allow manually triggering game start from console
    window.debugStartGame = function() {
        logMessage('DEBUG: Manually triggering game start', 'warning');
        
        // Force initialization of game cards
        initializeGameCards();
        
        // Force game start 
        forceGameStart();
        
        return 'Game start triggered. Check console for details.';
    };
    
    // DEBUG FUNCTION: Move to next round manually
    window.debugNextRound = function() {
        logMessage('DEBUG: Manually triggering next round', 'warning');
        startNextRound();
        return 'Next round triggered.';
    };
    
    // DEBUG FUNCTION: End game manually
    window.debugEndGame = function() {
        logMessage('DEBUG: Manually ending game', 'warning');
        endGame();
        return 'Game ended manually.';
    };

    // Add diagnostics button to the log section
    function addDiagnosticsButton() {
        const logSection = document.getElementById('logSection');
        if (!logSection) return;
        
        const diagButton = document.createElement('button');
        diagButton.className = 'btn btn-sm btn-warning mt-2';
        diagButton.textContent = 'Run Diagnostics';
        diagButton.addEventListener('click', runDiagnostics);
        
        const buttonContainer = document.createElement('div');
        buttonContainer.className = 'text-center mb-2';
        buttonContainer.appendChild(diagButton);
        
        logSection.insertBefore(buttonContainer, logSection.firstChild);
    }
    
    // Run diagnostics to check game state and connection
    function runDiagnostics() {
        logMessage("=== RUNNING DIAGNOSTICS ===", "info");
        
        // Check WebSocket connection
        const wsConnected = stompClient && stompClient.connected;
        logMessage(`WebSocket connected: ${wsConnected}`, wsConnected ? "success" : "error");
        
        // Log current subscriptions
        if (stompClient && stompClient.subscriptions) {
            const subscriptions = Object.keys(stompClient.subscriptions);
            logMessage(`Active subscriptions: ${subscriptions.length}`, "info");
            subscriptions.forEach(subId => {
                const sub = stompClient.subscriptions[subId];
                logMessage(`- ${subId}: ${sub.destination}`, "info");
            });
        } else {
            logMessage("No active subscriptions found", "warning");
        }
        
        // Log current room state
        logMessage(`Current room code: ${currentRoomCode || 'None'}`, "info");
        logMessage(`Is host: ${isHost}`, "info");
        
        // Count players in UI
        const hostPlayers = elements.create.playersList ? elements.create.playersList.children.length : 0;
        const guestPlayers = elements.join.joinedPlayersList ? elements.join.joinedPlayersList.children.length : 0;
        logMessage(`Players in host view: ${hostPlayers}`, "info");
        logMessage(`Players in guest view: ${guestPlayers}`, "info");
        
        // Test sending a ping to the room
        if (currentRoomCode && stompClient && stompClient.connected) {
            logMessage("Sending diagnostic ping to room...", "info");
            stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, {}, JSON.stringify({
                type: 'DIAGNOSTIC_PING',
                roomCode: currentRoomCode,
                senderId: firebaseUid,
                senderUsername: playerUsername,
                timestamp: Date.now()
            }));
            
            // Also try sending directly to application endpoint
            try {
                stompClient.send(CONFIG.SOCKET.ENDPOINTS.JOIN, {
                    firebaseUid: firebaseUid,
                    username: playerUsername,
                    roomCode: currentRoomCode
                }, JSON.stringify({
                    type: 'DIAGNOSTIC_PING',
                    roomCode: currentRoomCode,
                    senderId: firebaseUid,
                    senderUsername: playerUsername,
                    timestamp: Date.now()
                }));
                logMessage("Sent diagnostic ping to application endpoint", "success");
            } catch (e) {
                logMessage(`Error sending to application endpoint: ${e.message}`, "error");
            }
        }
        
        // Check game state
        if (elements.sections.game.classList.contains('active')) {
            logMessage(`Game round: ${gameState.roundNumber}`, "info");
            logMessage(`Host score: ${gameState.hostScore}`, "info");
            logMessage(`Guest score: ${gameState.guestScore}`, "info");
            logMessage(`Current card index: ${gameState.currentCardIndex}`, "info");
            logMessage(`Total cards: ${gameState.gameCards.length}`, "info");
            
            if (gameState.hasAnswered) {
                logMessage(`You have answered this round`, "info");
            }
            
            if (gameState.opponentAnswered) {
                logMessage(`Opponent has answered this round`, "info");
            }
        }
        
        logMessage("=== DIAGNOSTICS COMPLETE ===", "info");
        return true;
    }

    // Handle error message
    function handleError(message) {
        const errorContent = message.content || 'Unknown error';
        logMessage(`Error: ${errorContent}`, 'error');
        
        // Display error to user with a toast notification
        showSuccessToast('Error', errorContent, 5);
        
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
                
                // Remove loading indicator if it exists
                const loadingIndicator = document.getElementById('start-game-loading');
                if (loadingIndicator) {
                    loadingIndicator.remove();
                }
            }
        }
    }

    // Utility function to show a toast notification
    function showSuccessToast(title, message, seconds = 3) {
        const toastId = 'toast-' + Date.now();
        const toastHTML = `
            <div id="${toastId}" class="toast-notification ${title.toLowerCase().includes('error') ? 'error' : 'success'}">
                <div class="toast-header">
                    <strong>${title}</strong>
                    <button type="button" class="btn-close" onclick="this.parentElement.parentElement.remove()"></button>
            </div>
                <div class="toast-body">${message}</div>
            </div>
        `;
        
        // Append toast to body
        document.body.insertAdjacentHTML('beforeend', toastHTML);
        
        // Show the toast
        const toastElement = document.getElementById(toastId);
        setTimeout(() => {
            toastElement.classList.add('show');
        }, 100);
        
        // Auto-remove after specified seconds
        if (seconds > 0) {
            setTimeout(() => {
                toastElement.classList.remove('show');
                setTimeout(() => {
                    toastElement.remove();
                }, 500);
            }, seconds * 1000);
        }
        
        return toastId;
    }

    // Add required toast styles
    const toastStyles = document.createElement('style');
    toastStyles.textContent = `
        .toast-notification {
            position: fixed;
            top: 20px;
            right: 20px;
            min-width: 300px;
            max-width: 400px;
            background: white;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
            z-index: 9999;
            border-radius: 6px;
            overflow: hidden;
            opacity: 0;
            transform: translateY(-20px);
            transition: all 0.3s ease;
        }
        .toast-notification.show {
            opacity: 1;
            transform: translateY(0);
        }
        .toast-notification.success .toast-header {
            background-color: #d4edda;
            color: #155724;
        }
        .toast-notification.error .toast-header {
            background-color: #f8d7da;
            color: #721c24;
        }
        .toast-header {
            padding: 8px 15px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .toast-body {
            padding: 10px 15px;
        }
        .btn-close {
            background: transparent;
            border: none;
            font-size: 18px;
            cursor: pointer;
            padding: 0;
            width: 20px;
            height: 20px;
            position: relative;
        }
        .btn-close:before, .btn-close:after {
            content: '';
            position: absolute;
            width: 100%;
            height: 2px;
            background-color: currentColor;
            top: 50%;
            left: 0;
        }
        .btn-close:before {
            transform: rotate(45deg);
        }
        .btn-close:after {
            transform: rotate(-45deg);
        }
    `;
    document.head.appendChild(toastStyles);

    // Add a diagnostic button at bottom of the page
    const diagnosticButton = document.createElement('button');
    diagnosticButton.className = 'btn btn-sm btn-warning position-fixed';
    diagnosticButton.style.cssText = 'bottom: 20px; right: 20px; z-index: 9999;';
    diagnosticButton.textContent = 'Debug: Force Game Start';
    diagnosticButton.addEventListener('click', function() {
        if (!currentRoomCode) {
            showSuccessToast('Debug', 'You need to be in a room first', 3);
            return;
        }
        debugStartGame();
    });
    document.body.appendChild(diagnosticButton);
    
    // Debug functions that can be called from console
    window.debugStartGame = function() {
        const message = {
            type: 'GAME_STARTED',
            roomCode: currentRoomCode,
            senderId: 'debug-console',
            senderUsername: 'Debug Console',
            timestamp: Date.now()
        };
        handleGameStarted(message);
        showSuccessToast('Debug', 'Forced game start', 3);
    };
    
    window.debugNextRound = function() {
        startNextRound();
        showSuccessToast('Debug', 'Forced next round', 3);
    };
    
    window.debugEndGame = function() {
        endGame();
        showSuccessToast('Debug', 'Forced game end', 3);
    };

    // Handle player leaving the room
    function handlePlayerLeft(message) {
        if (message.roomCode !== currentRoomCode) {
            return; // Not for our room
        }
        
        logMessage(`Player left: ${message.senderUsername}`, 'info');
        
        const leftPlayerIsHost = message.isHost === true;
        const isSelf = message.senderId === firebaseUid;
        
        if (!isSelf) {
            // Show notification
            if (leftPlayerIsHost) {
                showSuccessToast('Host Left', `The host (${message.senderUsername}) has left the room.`, 5);
                
                // If host left and we're guest, return to lobby
                if (!isHost) {
                    showSuccessToast('Game Ended', 'The game has ended because the host left.', 5);
                    
                    // Reset room state and return to lobby
                    resetRoomState();
                    switchSection('lobbySection');
                }
            } else {
                showSuccessToast('Guest Left', `${message.senderUsername} has left the room.`, 5);
                
                // If guest left and we're host, disable start button
                if (isHost) {
                    elements.create.startGameBtn.disabled = true;
                    
                    // Remove the guest from player list
                    const playersList = elements.create.playersList;
                    if (playersList) {
                        const playerItems = Array.from(playersList.querySelectorAll('li'));
                        playerItems.forEach(item => {
                            if (item.textContent.includes(message.senderUsername) && 
                                !item.textContent.includes('Host')) {
                                item.remove();
                            }
                        });
                    }
                }
            }
        }
    }
    
    // Leave the current room
    function leaveRoom(asHost) {
        if (!currentRoomCode) {
            return;
        }
        
        logMessage(`Leaving room ${currentRoomCode}`, 'info');
        
        // Send leave message to both app endpoint and room topic for redundancy
        try {
            // Send to application endpoint
            if (stompClient && stompClient.connected) {
                stompClient.send(CONFIG.SOCKET.ENDPOINTS.LEAVE, 
                    {
                        firebaseUid: firebaseUid,
                        roomCode: currentRoomCode
                    }, 
                    JSON.stringify({
                        type: 'LEAVE_ROOM',
                        roomCode: currentRoomCode,
                        senderId: firebaseUid,
                        senderUsername: playerUsername,
                        timestamp: Date.now(),
                        isHost: asHost
                    })
                );
                
                // Also send directly to room topic
                stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, 
                    {
                        firebaseUid: firebaseUid,
                        roomCode: currentRoomCode
                    }, 
                    JSON.stringify({
                        type: 'LEAVE_ROOM',
                        roomCode: currentRoomCode,
                        senderId: firebaseUid,
                        senderUsername: playerUsername,
                        timestamp: Date.now(),
                        isHost: asHost
                    })
                );
            }
        } catch (e) {
            logMessage(`Error sending leave message: ${e.message}`, 'error');
        }
        
        // Unsubscribe from room topic
        try {
            if (stompClient && stompClient.subscriptions && stompClient.subscriptions['room-subscription']) {
                stompClient.unsubscribe('room-subscription');
                logMessage('Unsubscribed from room topic', 'info');
            }
        } catch (e) {
            logMessage(`Error unsubscribing from room: ${e.message}`, 'warning');
        }
        
        // Clear host presence interval if host
        if (isHost && hostPresenceInterval) {
            clearInterval(hostPresenceInterval);
            hostPresenceInterval = null;
            logMessage('Cleared host presence interval', 'info');
        }
        
        // Reset room state
        resetRoomState();
        
        // Update UI based on whether we were host or guest
        if (asHost) {
            elements.create.roomInfo.style.display = 'none';
            elements.create.roomPlayers.style.display = 'none';
            elements.create.createRoomBtn.disabled = false;
        } else {
            elements.join.joinedRoomInfo.style.display = 'none';
            elements.join.joinedRoomPlayers.style.display = 'none';
            elements.join.joinRoomBtn.disabled = false;
            elements.join.joinRoomBtn.style.display = '';
            elements.join.joinRoomCodeInput.style.display = '';
            
            // Remove any joining status
            const joiningStatus = document.getElementById('joining-status');
            if (joiningStatus) {
                joiningStatus.remove();
            }
        }
        
        // Return to lobby
        switchSection('lobbySection');
        
        showSuccessToast('Success', 'You have left the room', 3);
    }
    
    // Reset room state variables
    function resetRoomState() {
        currentRoomCode = '';
        isHost = false;
        
        // Clear player lists
        if (elements.create.playersList) {
            elements.create.playersList.innerHTML = '';
        }
        if (elements.join.joinedPlayersList) {
            elements.join.joinedPlayersList.innerHTML = '';
        }
        
        // Reset game state
        gameState = {
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
            totalRounds: 15
        };
        
        logMessage('Room state reset', 'info');
    }
});

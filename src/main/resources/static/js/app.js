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
            
            // Additional headers for authentication
            const headers = {
                username: playerUsername,
                firebaseUid: firebaseUid
            };
            
            // Set the client ID to be unique for this session
            const clientId = 'specific-client-' + Math.random().toString(36).substring(2, 15);
            
            // Connect to WebSocket with proper headers and client ID
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
                },
                clientId
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
        logMessage(`${message.senderUsername} joined the room: ${message.roomCode}`, 'info');
        
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
            // As host, add the joining user to our player list if not already there
            const existingPlayer = Array.from(elements.create.playersList.children)
                .some(item => item.textContent.includes(message.senderUsername));
                
            if (!existingPlayer && !isSelfMessage) {
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
                const successMessage = document.createElement('div');
                successMessage.id = 'guest-joined-notification';
                successMessage.className = 'alert alert-success mt-3';
                successMessage.innerHTML = `<strong>${message.senderUsername}</strong> has joined your room!`;
                elements.create.roomInfo.appendChild(successMessage);
                
                // Auto-remove success message after 3 seconds
                setTimeout(() => {
                    const notification = document.getElementById('guest-joined-notification');
                    if (notification) {
                        notification.remove();
                    }
                }, 3000);
                
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
        } else if (!isSelfMessage) {
            // As guest, handle host messages
            const isHostMessage = message.isHostReply === true;
            const isHostSender = message.type === 'ROOM_CREATED' || message.isHost === true;
            
            // Create host item for display
            const hostItem = document.createElement('li');
            hostItem.className = 'list-group-item';
            hostItem.innerHTML = `
                <i class="fas fa-user player-icon"></i>
                ${message.senderUsername}
                <span class="badge bg-primary host-badge">Host</span>
            `;
            
            // Add to joined players list if not already there
            const existingHost = Array.from(elements.join.joinedPlayersList.children)
                .some(item => item.textContent.includes(message.senderUsername) && 
                              item.textContent.includes('Host'));
                
            if (!existingHost) {
                elements.join.joinedPlayersList.appendChild(hostItem);
                logMessage(`Host ${message.senderUsername} found in room`, 'success');
                
                // Ensure our UI is properly showing the room
                elements.join.joinedRoomInfo.style.display = 'block';
                elements.join.joinedRoomPlayers.style.display = 'block';
                
                // Show success message
                const successMessage = document.createElement('div');
                successMessage.id = 'joined-success-notification';
                successMessage.className = 'alert alert-success mt-3';
                successMessage.innerHTML = `Successfully joined <strong>${message.senderUsername}'s</strong> room!`;
                elements.join.joinedRoomInfo.appendChild(successMessage);
                
                // Auto-remove success message after 3 seconds
                setTimeout(() => {
                    const notification = document.getElementById('joined-success-notification');
                    if (notification) {
                        notification.remove();
                    }
                }, 3000);
                
                // Send back another confirmation to ensure host sees us
                if (!isHostMessage) {
                    stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, {}, JSON.stringify({
                        type: 'GUEST_JOINED',
                        roomCode: currentRoomCode,
                        senderId: firebaseUid,
                        senderUsername: playerUsername,
                        timestamp: Date.now(),
                        isGuestReply: true
                    }));
                }
            }
            
            if (isHostMessage) {
                logMessage(`Host ${message.senderUsername} acknowledged our join`, 'success');
                
                // Remove any waiting for host messages
                const waitingMessage = document.getElementById('joined-status');
                if (waitingMessage && waitingMessage.textContent.includes('waiting for host')) {
                    waitingMessage.remove();
                }
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
                        logMessage(`Host ${message.senderUsername} is active in the room`, 'info');
                        
                        // If we're still having a loading indicator, remove it
                        const loadingIndicator = document.getElementById('join-room-loading');
                        if (loadingIndicator) {
                            loadingIndicator.remove();
                            logMessage('Removed join loading indicator after host presence detected', 'info');
                        }
                        
                        // Make sure host appears in the player list
                        const existingHost = Array.from(elements.join.joinedPlayersList.children)
                            .some(item => item.textContent.includes(message.senderUsername));
                            
                        if (!existingHost) {
                            const hostItem = document.createElement('li');
                            hostItem.className = 'list-group-item';
                            hostItem.innerHTML = `
                                <i class="fas fa-user player-icon"></i>
                                ${message.senderUsername}
                                <span class="badge bg-primary host-badge">Host</span>
                            `;
                            elements.join.joinedPlayersList.appendChild(hostItem);
                            logMessage(`Added host ${message.senderUsername} to player list from presence message`, 'success');
                        }
                        
                        // Make sure UI is properly showing the room
                        elements.join.joinedRoomInfo.style.display = 'block';
                        elements.join.joinedRoomPlayers.style.display = 'block';
                        
                        // Re-enable join button if it's still disabled
                        elements.join.joinRoomBtn.disabled = false;
                    }
                    break;
                case 'GAME_START_ACK':
                    // Handle game start acknowledgment from guest players
                    if (isHost && message.roomCode === currentRoomCode) {
                        handleGameStartAck(message);
                    }
                    break;
                case 'ROUND_SYNC':
                    // Handle round synchronization to ensure both players are on the same round
                    if (message.roomCode === currentRoomCode && message.senderId !== firebaseUid) {
                        handleRoundSync(message);
                    }
                    break;
                case 'GAME_STARTED':
                    // Clear any game start timeout
                    if (window.gameStartTimeout) {
                        clearTimeout(window.gameStartTimeout);
                        window.gameStartTimeout = null;
                        
                        // Remove loading indicator if it exists
                        const loadingIndicator = document.getElementById('start-game-loading');
                        if (loadingIndicator) {
                            loadingIndicator.remove();
                        }
                    }
                    
                    handleGameStarted(message);
                    break;
                case 'NEXT_ROUND':
                    // For backward compatibility with older versions
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
                    logMessage(`Unknown message type: ${message.type}`, 'warning');
                    // Try to handle anyway if it contains room info
                    if (message.roomCode === currentRoomCode && message.senderUsername) {
                        handleRoomJoined({
                            ...message,
                            type: 'ROOM_JOINED' // Treat as join message
                        });
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
            logMessage(`Problematic payload: ${payload.body}`, 'error');
            
            // Remove loading indicator even on error
            const loadingIndicator = document.getElementById('join-room-loading');
            if (loadingIndicator) {
                loadingIndicator.remove();
                logMessage('Removed join loading indicator after error', 'info');
            }
        }
    }
    
    // Handle round synchronization message to ensure both players are in sync
    function handleRoundSync(message) {
        const syncData = message.syncData;
        
        if (!syncData) {
            logMessage('Received round sync message without sync data', 'warning');
            return;
        }
        
        logMessage(`Received round sync for round ${syncData.roundNumber} (our round: ${gameState.roundNumber})`, 'info');
        
        // If we're already ahead, ignore
        if (syncData.roundNumber < gameState.roundNumber) {
            logMessage('We are ahead of the sender, ignoring sync', 'info');
            return;
        }
        
        // If we're on the same round, verify card and scores
        if (syncData.roundNumber === gameState.roundNumber) {
            // Update scores if they don't match
            if (syncData.hostScore !== gameState.hostScore || syncData.guestScore !== gameState.guestScore) {
                gameState.hostScore = syncData.hostScore;
                gameState.guestScore = syncData.guestScore;
                
                // Update our score display based on whether we're host or guest
                if (isHost) {
                    gameState.yourScore = syncData.hostScore;
                    gameState.opponentScore = syncData.guestScore;
                } else {
                    gameState.yourScore = syncData.guestScore;
                    gameState.opponentScore = syncData.hostScore;
                }
                
                // Update UI
                elements.game.yourScore.textContent = gameState.yourScore;
                elements.game.opponentScore.textContent = gameState.opponentScore;
                
                logMessage('Updated scores to match sync data', 'info');
            }
            
            // Verify we're on the same card
            const currentCard = gameState.gameCards[gameState.currentCardIndex];
            if (currentCard && currentCard.front !== syncData.cardFront) {
                logMessage('Card mismatch detected, adjusting...', 'warning');
                
                // Find the correct card in our deck
                const cardIndex = gameState.gameCards.findIndex(card => card.front === syncData.cardFront);
                if (cardIndex !== -1) {
                    gameState.currentCardIndex = cardIndex;
                    
                    // Update UI with the correct card
                    elements.game.cardQuestion.textContent = gameState.gameCards[cardIndex].front;
                    logMessage(`Adjusted to card at index ${cardIndex}`, 'success');
                }
            }
            
            return;
        }
        
        // If we're behind, catch up to the correct round
        if (syncData.roundNumber > gameState.roundNumber) {
            logMessage(`We are behind (round ${gameState.roundNumber} vs ${syncData.roundNumber}), catching up`, 'warning');
            
            // Update to match the sender's state
            gameState.roundNumber = syncData.roundNumber - 1; // -1 because startNextRound will increment
            gameState.hostScore = syncData.hostScore;
            gameState.guestScore = syncData.guestScore;
            
            // Update our score display based on whether we're host or guest
            if (isHost) {
                gameState.yourScore = syncData.hostScore;
                gameState.opponentScore = syncData.guestScore;
            } else {
                gameState.yourScore = syncData.guestScore;
                gameState.opponentScore = syncData.hostScore;
            }
            
            // Find the correct card index
            const cardIndex = gameState.gameCards.findIndex(card => card.front === syncData.cardFront);
            if (cardIndex !== -1) {
                gameState.currentCardIndex = cardIndex - 1; // -1 because startNextRound will increment
            }
            
            // Start the next round to align with the sender
            startNextRound();
        }
    }
    
    // Handle next round message from the other player (for backward compatibility)
    function handleNextRound(message) {
        // If we're already on this round, ignore
        if (message.roundNumber === gameState.roundNumber) {
            return;
        }
        
        // If we're behind, catch up
        if (message.roundNumber > gameState.roundNumber) {
            logMessage(`Received next round message: we are behind (${gameState.roundNumber} vs ${message.roundNumber})`, 'warning');
            startNextRound();
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
                return response.json();
            } else {
                logMessage(`REST game start failed with status: ${response.status}`, 'warning');
                throw new Error('Failed to start game via REST');
            }
        })
        .then(data => {
            logMessage(`Game start REST response: ${JSON.stringify(data)}`, 'info');
            // No additional handling needed since WebSocket handles the game start
        })
        .catch(error => {
            // Errors here aren't critical since we have WebSocket-based retries
            logMessage(`Game start REST error: ${error.message}`, 'warning');
        });
        
        // Set a timeout to handle no acknowledgment
        window.gameStartTimeout = setTimeout(() => {
            logMessage('No game start acknowledgment received, triggering local game start', 'warning');
            handleGameStarted({
                type: 'GAME_STARTED',
                roomCode: currentRoomCode,
                senderId: firebaseUid,
                senderUsername: playerUsername,
                timestamp: Date.now()
            });
        }, 5000); // 5 second timeout
    }
    
    // Handle game start event
    function handleGameStarted(message) {
        // Make sure the message is for our current room
        if (message.roomCode !== currentRoomCode) {
            logMessage(`Got game start for room ${message.roomCode} but we're in ${currentRoomCode}, ignoring`, 'warning');
            return;
        }
        
        logMessage(`Game started by ${message.senderUsername}`, 'success');
        
        // Clear any timers
        if (window.gameStartTimeout) {
            clearTimeout(window.gameStartTimeout);
            window.gameStartTimeout = null;
        }
        
        // Remove loading indicator if it exists
        const loadingIndicator = document.getElementById('start-game-loading');
        if (loadingIndicator) {
            loadingIndicator.remove();
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
            gameCards: [...STATIC_CARDS], // Use static cards instead of CONFIG.GAME.CARDS
            currentCardIndex: -1,
            totalRounds: 10 // Hardcode total rounds or use STATIC_CARDS.length
        };
        
        // Send acknowledgment if we're a guest
        if (!isHost) {
            sendGameStartAcknowledgment();
        }
        
        // Deterministically shuffle the cards using room code as seed
        const seed = currentRoomCode.split('').reduce((acc, char) => {
            return acc + char.charCodeAt(0);
        }, 0);
        
        deterministicShuffle(gameState.gameCards, seed);
        
        // Limit number of cards to total rounds
        if (gameState.gameCards.length > gameState.totalRounds) {
            gameState.gameCards = gameState.gameCards.slice(0, gameState.totalRounds);
        }
        
        // Update UI
        elements.game.yourScore.textContent = 0;
        elements.game.opponentScore.textContent = 0;
        
        // Update player labels based on host status
        if (isHost) {
            elements.game.yourLabel.textContent = 'Host:';
            elements.game.opponentLabel.textContent = 'Guest:';
        } else {
            elements.game.yourLabel.textContent = 'Guest:';
            elements.game.opponentLabel.textContent = 'Host:';
        }
        
        // Show game section
        showSection('game');
        
        // Show a toast notification
        showSuccessToast('Game started!', 'The game has begun. Good luck!');
        
        // Start first round after a short delay to ensure both players are in game view
        setTimeout(() => {
            startNextRound();
        }, 1000);
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
        // Use a consistent seed based on the room code for both players
        const seedNumber = currentRoomCode.split('').reduce((acc, char) => {
            return acc + char.charCodeAt(0);
        }, 0);
        
        logMessage(`Using seed ${seedNumber} for card shuffling`, 'info');
        
        // Make a copy of the static cards
        gameState.gameCards = [...STATIC_CARDS];
        
        // Use deterministic shuffle based on room code so both players see same order
        deterministicShuffle(gameState.gameCards, seedNumber);
        
        // Limit to the number of rounds we want to play
        gameState.gameCards = gameState.gameCards.slice(0, gameState.totalRounds);
        gameState.currentCardIndex = -1;
        
        logMessage(`Initialized ${gameState.gameCards.length} game cards`, 'info');
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

    // Start the next round
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
        
        // Create option buttons (using deterministic shuffle with same seed for consistency)
        elements.game.optionsContainer.innerHTML = '';
        
        // Get a seed for this specific card based on room code and round number
        const optionsSeed = currentRoomCode.split('').reduce((acc, char) => {
            return acc + char.charCodeAt(0);
        }, 0) + gameState.roundNumber * 100;
        
        // Create a copy of options to shuffle so both players see same order
        const shuffledOptions = [...currentCard.options];
        deterministicShuffle(shuffledOptions, optionsSeed);
        
        // Create option buttons with deterministically shuffled options
        shuffledOptions.forEach((option, index) => {
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
        
        // Send round synchronization message to ensure both players are on the same round
        sendRoundSyncMessage();
        
        logMessage(`Round ${gameState.roundNumber} started with card: ${currentCard.front}`, 'info');
    }
    
    // Send a round synchronization message to ensure both players are on the same round
    function sendRoundSyncMessage() {
        if (stompClient && stompClient.connected) {
            const roundSyncData = {
                roomCode: currentRoomCode,
                roundNumber: gameState.roundNumber,
                hostScore: gameState.hostScore,
                guestScore: gameState.guestScore,
                currentCardIndex: gameState.currentCardIndex,
                cardFront: gameState.gameCards[gameState.currentCardIndex].front
            };
            
            stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, 
                { 
                    firebaseUid: firebaseUid,
                    roomCode: currentRoomCode 
                }, 
                JSON.stringify({
                    type: 'ROUND_SYNC',
                    roomCode: currentRoomCode,
                    senderId: firebaseUid,
                    senderUsername: playerUsername,
                    timestamp: Date.now(),
                    syncData: roundSyncData
                })
            );
            
            logMessage(`Sent round sync message for round ${gameState.roundNumber}`, 'info');
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
        logMessage(`${message.senderUsername} left the room`, 'info');
        
        const isHostLeft = message.isHost === true;
        const isHostMessage = message.isHost === true && message.senderUsername !== playerUsername;
        const isGuestMessage = !message.isHost && message.senderUsername !== playerUsername;
        
        // Show a notification that the other player left
        const leftNotification = document.createElement('div');
        leftNotification.id = 'player-left-notification';
        leftNotification.className = 'alert alert-warning mt-3';
        leftNotification.innerHTML = `<strong>${message.senderUsername}</strong> has left the room!`;
        
        // Add notification to appropriate container
        if (isHost) {
            elements.create.roomInfo.appendChild(leftNotification);
            
            // If we're host, remove the guest from our list
            if (isGuestMessage) {
                const guestItems = Array.from(elements.create.playersList.children);
                guestItems.forEach(item => {
                    if (item.textContent.includes(message.senderUsername)) {
                        item.remove();
                        logMessage(`Removed ${message.senderUsername} from host player list`, 'info');
                    }
                });
                
                // Disable start button since guest left
                elements.create.startGameBtn.disabled = true;
            }
        } else {
            elements.join.joinedRoomInfo.appendChild(leftNotification);
            
            // If we're guest and host left, show special message
            if (isHostMessage) {
                leftNotification.className = 'alert alert-danger mt-3';
                leftNotification.innerHTML = `<strong>The host ${message.senderUsername} has left the room!</strong><br>You'll need to join another room.`;
                
                // Remove host from player list
                const hostItems = Array.from(elements.join.joinedPlayersList.children);
                hostItems.forEach(item => {
                    if (item.textContent.includes('Host')) {
                        item.remove();
                        logMessage('Removed host from guest player list', 'info');
                    }
                });
                
                // Schedule auto return to lobby form
                setTimeout(() => {
                    leaveRoom(false);
                }, 5000);
            }
        }
        
        // Auto-remove notification after 5 seconds
        setTimeout(() => {
            const notification = document.getElementById('player-left-notification');
            if (notification) {
                notification.remove();
            }
        }, 5000);
        
        // If we're in a game, handle differently
        if (elements.sections.game.classList.contains('active')) {
            // If opponent left during game
            displayError(`${message.senderUsername} left the game.`);
            
            // Return to lobby after a short delay
            setTimeout(() => {
                showSection('lobby');
                
                // Show appropriate message
                const gameEndedNotification = document.createElement('div');
                gameEndedNotification.id = 'game-ended-notification';
                gameEndedNotification.className = 'alert alert-danger mt-3';
                gameEndedNotification.innerHTML = `<strong>Game ended:</strong> ${message.senderUsername} left during the game!`;
                
                if (isHost) {
                    elements.create.roomInfo.appendChild(gameEndedNotification);
                } else {
                    elements.join.joinedRoomInfo.appendChild(gameEndedNotification);
                }
                
                // Remove notification after a delay
                setTimeout(() => {
                    const notification = document.getElementById('game-ended-notification');
                    if (notification) {
                        notification.remove();
                    }
                }, 5000);
            }, 2000);
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

    // Create a diagnostic function to help troubleshoot issues
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
        
        // Check if we need to try repairing the connection
        if (currentRoomCode && (!wsConnected || (isHost && hostPlayers < 2) || (!isHost && guestPlayers < 2))) {
            logMessage("Connection appears broken. Attempting repair...", "warning");
            
            // Try forcibly resubscribing to room topic
            try {
                if (stompClient && stompClient.connected) {
                    // Try to unsubscribe first if we have a subscription
                    try {
                        if (stompClient.subscriptions && stompClient.subscriptions['room-subscription']) {
                            stompClient.unsubscribe('room-subscription');
                        }
                    } catch (e) {
                        logMessage(`Error unsubscribing: ${e.message}`, "warning");
                    }
                    
                    // Re-subscribe with explicit ID
                    stompClient.subscribe(
                        CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode,
                        onMessageReceived,
                        { id: 'room-subscription' }
                    );
                    logMessage("Resubscribed to room topic", "success");
                    
                    // Send another join message
                    stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, {}, JSON.stringify({
                        type: 'ROOM_JOINED',
                        roomCode: currentRoomCode,
                        senderId: firebaseUid,
                        senderUsername: playerUsername,
                        timestamp: Date.now(),
                        isRepairAttempt: true
                    }));
                    logMessage("Sent repair join message", "success");
                } else {
                    logMessage("Cannot repair - WebSocket not connected", "error");
                    connectWebSocket(); // Try reconnecting
                }
            } catch (e) {
                logMessage(`Error during repair: ${e.message}`, "error");
            }
        }
        
        logMessage("=== DIAGNOSTICS COMPLETE ===", "info");
        return true;
    }

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

    // Initialize the application
    function init() {
        initEventListeners();
        updateConnectionStatus('disconnected');
        logMessage('Application initialized', 'info');
        addDiagnosticsButton();
    }

    // Display a success toast notification
    function showSuccessToast(title, message) {
        const successToast = document.createElement('div');
        successToast.className = 'toast show bg-success text-white';
        successToast.setAttribute('role', 'alert');
        successToast.setAttribute('aria-live', 'assertive');
        successToast.setAttribute('aria-atomic', 'true');
        successToast.style.position = 'fixed';
        successToast.style.bottom = '20px';
        successToast.style.right = '20px';
        successToast.style.minWidth = '250px';
        successToast.style.zIndex = '1050';
        
        successToast.innerHTML = `
            <div class="toast-header bg-success text-white">
                <strong class="me-auto">${title}</strong>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast"></button>
            </div>
            <div class="toast-body">
                ${message}
            </div>
        `;
        
        document.body.appendChild(successToast);
        
        // Automatically remove the toast after 3 seconds
        setTimeout(() => {
            successToast.remove();
        }, 3000);
        
        // Add close button functionality
        const closeBtn = successToast.querySelector('.btn-close');
        closeBtn.addEventListener('click', () => {
            successToast.remove();
        });
    }

    // Display an error message
    function displayError(message) {
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
                ${message}
            </div>
        `;
        
        document.body.appendChild(errorToast);
        
        // Automatically remove the toast after 5 seconds
        setTimeout(() => {
            errorToast.remove();
        }, 5000);
        
        // Add close button functionality
        const closeBtn = errorToast.querySelector('.btn-close');
        closeBtn.addEventListener('click', () => {
            errorToast.remove();
        });
        
        logMessage(`Error displayed: ${message}`, 'error');
    }

    // Start the application
    init();
});

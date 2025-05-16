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
        totalRounds: 10,
        syncAttempts: 0,
        lastSyncTimestamp: 0,
        gameStarted: false,
        gameEnded: false,
        startTimestamp: 0,
        lastAnswerTime: 0,
        optionsSeed: 0,
        connectionRetries: 0
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
    
    // Connect to WebSocket
    function connectWebSocket() {
        updateConnectionStatus('connecting');
        
        // For offline mode, we'll simulate a connection
        logMessage('Creating offline game session', 'info');
        
        try {
            // Create a mock stompClient
            stompClient = {
                connected: true,
                disconnect: function() {
                    this.connected = false;
                    logMessage('Disconnected from offline session', 'info');
                    updateConnectionStatus('disconnected');
                },
                send: function(destination, headers, body) {
                    logMessage(`Sent message to ${destination}`, 'info');
                    
                    // In offline mode, directly handle messages locally
                    try {
                        const message = JSON.parse(body);
                        setTimeout(() => {
                            // Simulate message echo back
                            onMessageReceived({
                                body: body
                            });
                        }, 100);
                    } catch (error) {
                        logMessage(`Error processing offline message: ${error.message}`, 'error');
                    }
                },
                subscribe: function(destination, callback, headers) {
                    logMessage(`Subscribed to ${destination}`, 'info');
                    return {
                        id: headers.id || 'offline-subscription',
                        unsubscribe: function() {
                            logMessage(`Unsubscribed from ${destination}`, 'info');
                        }
                    };
                },
                subscriptions: {}
            };
            
            // Set connected status
            updateConnectionStatus('connected');
            
            // Call onConnected after a short delay to simulate connection process
            setTimeout(() => {
                onConnected();
            }, 500);
            
            logMessage('Successfully connected to offline game session', 'success');
        } catch (error) {
            logMessage(`Error creating offline session: ${error.message}`, 'error');
            updateConnectionStatus('disconnected');
            
            // Show error message
            displayError('Could not create game session. Please refresh the page and try again.');
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
        logMessage('Session connected', 'success');
        updateConnectionStatus('connected');
        
        // In the offline version, everything is handled locally
        useAlternativeUrl = false; // Reset to default
        connectionAttempts = 0;
        
        // Enable room creation button
        if (elements.create.createRoomBtn) {
            elements.create.createRoomBtn.disabled = false;
        }
        
        // Enable join room button
        if (elements.join.joinRoomBtn) {
            elements.join.joinRoomBtn.disabled = false;
        }
        
        // Show UI elements for creating and joining rooms
        showSection('lobby'); // Should already be showing, but make sure
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
        
        try {
            // Generate a random room code
            currentRoomCode = generateRoomCode();
            isHost = true;
            
            logMessage(`Room created with code: ${currentRoomCode}`, 'success');
            
            // Update UI
            elements.create.roomCode.textContent = currentRoomCode;
            elements.create.roomInfo.style.display = 'block';
            elements.create.roomPlayers.style.display = 'block';
            
            // Add host to players list
            try {
                // Clear existing list
                if (elements.create.playersList) {
                    elements.create.playersList.innerHTML = '';
                }
                
                // Add current player (host) to the list
                const hostItem = document.createElement('li');
                hostItem.className = 'list-group-item';
                hostItem.innerHTML = `
                    <i class="fas fa-user player-icon"></i>
                    ${playerUsername}
                    <span class="badge bg-primary host-badge">Host</span>
                `;
                
                if (elements.create.playersList) {
                    elements.create.playersList.appendChild(hostItem);
                    logMessage('Added host to players list', 'info');
                } else {
                    logMessage('Could not update players list - element not found', 'error');
                }
            } catch (updateError) {
                logMessage(`Error updating players list: ${updateError.message}`, 'error');
                // Continue anyway - non-critical error
            }
            
            // Set up presence indicator for offline mode
            setupHostPresenceInterval();
            
            // Re-enable the create button
            elements.create.createRoomBtn.disabled = false;
        } catch (error) {
            logMessage(`Error creating room: ${error.message}`, 'error');
            elements.create.createRoomBtn.disabled = false;
        }
    }

    // Generate a random room code
    function generateRoomCode() {
        const characters = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // Removed similar looking characters
        let result = '';
        
        // Generate a 6-character code
        for (let i = 0; i < 6; i++) {
            result += characters.charAt(Math.floor(Math.random() * characters.length));
        }
        
        return result;
    }
    
    // Set up an interval to periodically send host presence messages
    let hostPresenceInterval = null;
    function setupHostPresenceInterval() {
        // Clear any existing interval
        if (hostPresenceInterval) {
            clearInterval(hostPresenceInterval);
            logMessage('Cleared previous host presence interval', 'info');
        }
        
        // Start new interval
        hostPresenceInterval = setInterval(() => {
            if (stompClient && stompClient.connected && currentRoomCode && isHost) {
                stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, 
                    { firebaseUid: firebaseUid },
                    JSON.stringify({
                        type: 'HOST_PRESENCE',
                        roomCode: currentRoomCode,
                        senderId: firebaseUid,
                        senderUsername: playerUsername,
                        timestamp: Date.now()
                    })
                );
            } else if (!stompClient || !stompClient.connected) {
                clearInterval(hostPresenceInterval);
                logMessage('Stopping host presence messages due to disconnection', 'warning');
            }
        }, 5000); // Send every 5 seconds
        
        logMessage('Set up host presence interval', 'info');
    }

    // Update the players list in the UI
    function updatePlayersList() {
        // Clear existing list
        if (elements.create.playersList) {
            elements.create.playersList.innerHTML = '';
        }
        
        // Add current player (host) to the list
        const hostItem = document.createElement('li');
        hostItem.className = 'list-group-item';
        hostItem.innerHTML = `
            <i class="fas fa-user player-icon"></i>
            ${playerUsername}
            <span class="badge bg-primary host-badge">Host</span>
        `;
        
        if (elements.create.playersList) {
            elements.create.playersList.appendChild(hostItem);
            logMessage('Added host to players list', 'info');
        } else {
            logMessage('Could not update players list - element not found', 'error');
        }
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
        
        try {
            // Update room information
            currentRoomCode = roomCode;
            isHost = false;
            
            // Update UI elements
            elements.join.joinedRoomCode.textContent = roomCode;
            elements.join.joinedRoomInfo.style.display = 'block';
            elements.join.joinedRoomPlayers.style.display = 'block';
            
            // Add self to player list
            const guestItem = document.createElement('li');
            guestItem.className = 'list-group-item';
            guestItem.innerHTML = `
                <i class="fas fa-user player-icon"></i>
                ${playerUsername} (You)
                <span class="badge bg-secondary host-badge">Guest</span>
            `;
            elements.join.joinedPlayersList.innerHTML = ''; // Clear existing list
            elements.join.joinedPlayersList.appendChild(guestItem);
            
            // Show waiting for host message
            const waitingMessage = document.createElement('div');
            waitingMessage.id = 'waiting-for-host';
            waitingMessage.className = 'alert alert-info mt-3';
            waitingMessage.innerHTML = `<strong>Waiting for host to start the game...</strong>`;
            elements.join.joinedRoomInfo.appendChild(waitingMessage);
            
            // Create a simulated host entry
            const hostItem = document.createElement('li');
            hostItem.className = 'list-group-item';
            hostItem.innerHTML = `
                <i class="fas fa-user player-icon"></i>
                Host
                <span class="badge bg-primary host-badge">Host</span>
            `;
            elements.join.joinedPlayersList.appendChild(hostItem);
            
            // Remove loading indicator
            if (loadingIndicator) {
                loadingIndicator.remove();
            }
            
            // Re-enable join button
            elements.join.joinRoomBtn.disabled = false;
            
            logMessage(`Successfully joined room ${roomCode}`, 'success');
        } catch (error) {
            logMessage(`Error joining room: ${error.message}`, 'error');
            displayError(`Failed to join room. Please check the room code and try again.`);
            
            // Remove loading indicator
            if (loadingIndicator) {
                loadingIndicator.remove();
            }
            
            // Re-enable join button
            elements.join.joinRoomBtn.disabled = false;
        }
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
                        isHost: true,
                        message: `Host ${playerUsername} acknowledges guest ${message.senderUsername}`
                    })
                );
            }
        } else {
            // As guest, handle host messages or add our host to the player list if not already there
            const isHostMessage = message.isHostReply === true || message.isHost === true;
            
            // Create host item for display
            const hostItem = document.createElement('li');
            hostItem.className = 'list-group-item';
            hostItem.innerHTML = `
                <i class="fas fa-user player-icon"></i>
                ${message.senderUsername}
                <span class="badge bg-primary host-badge">Host</span>
            `;
            
            // Add to joined players list if not already there and it's a host message
            const existingHost = Array.from(elements.join.joinedPlayersList.children)
                .some(item => item.textContent.includes(message.senderUsername) && 
                              item.textContent.includes('Host'));
                
            if (!existingHost && isHostMessage) {
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
            }
            
            // Send back a confirmation to ensure host sees us (but not if we're responding to a host reply)
            if (!isSelfMessage && !message.isHostReply) {
                stompClient.send(CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, {}, JSON.stringify({
                    type: 'GUEST_JOINED',
                    roomCode: currentRoomCode,
                    senderId: firebaseUid,
                    senderUsername: playerUsername,
                    timestamp: Date.now(),
                    isGuestReply: true
                }));
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
            let message;
            try {
                message = JSON.parse(payload.body);
                
                // Check for nested type issue (common error from logs)
                if (typeof message.type === 'object' && message.type.type) {
                    logMessage(`Fixing nested type issue in message: ${JSON.stringify(message.type)}`, 'warning');
                    message = message.type; // Extract the actual message from the nested structure
                }
                
                // Validate message has required fields
                if (!message.type) {
                    logMessage(`Message without proper type: ${payload.body.substring(0, 100)}...`, 'error');
                    // Try to parse it anyway if it has roomCode, might be a GAME_START_ACK
                    if (payload.body.includes('GAME_START_ACK') && payload.body.includes('roomCode')) {
                        logMessage('Attempting to salvage message as GAME_START_ACK', 'info');
                        try {
                            if (typeof message === 'string') {
                                message = JSON.parse(message);
                            }
                            message.type = 'GAME_START_ACK';
                            handleGameStartAck(message);
                        } catch (e) {
                            logMessage(`Failed to salvage message: ${e.message}`, 'error');
                        }
                    }
                    return;
                }
                
                logMessage(`Received message: ${message.type}`, 'info');
            } catch (parseError) {
                logMessage(`Error parsing message: ${parseError.message}`, 'error');
                logMessage(`Raw payload: ${payload.body.substring(0, 100)}...`, 'error');
                
                // Try to salvage the message if it looks like GAME_START_ACK
                if (payload.body.includes('GAME_START_ACK') && payload.body.includes('roomCode')) {
                    logMessage('Attempting to salvage message as GAME_START_ACK', 'info');
                    try {
                        const regexMatch = /roomCode":"([^"]+)"/.exec(payload.body);
                        if (regexMatch && regexMatch[1]) {
                            const roomCode = regexMatch[1];
                            if (roomCode === currentRoomCode) {
                                logMessage('Manually handling as game start acknowledgment', 'info');
                                handleGameStartAck({
                                    type: 'GAME_START_ACK',
                                    roomCode: roomCode,
                                    senderUsername: 'Guest Player',
                                    timestamp: Date.now()
                                });
                            }
                        }
                    } catch (e) {
                        logMessage(`Failed to salvage message: ${e.message}`, 'error');
                    }
                }
                return;
            }
            
            // Handle different message types
            switch (message.type) {
                case 'ROUND_SYNC':
                    // Handle round synchronization as a high priority message
                    handleRoundSync(message);
                    break;
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
                            
                            elements.join.joinedRoomInfo.style.display = 'block';
                            elements.join.joinedRoomPlayers.style.display = 'block';
                            
                            // Re-enable join button if it's still disabled
                            elements.join.joinRoomBtn.disabled = false;
                        }
                    }
                    break;
                case 'GAME_START_ACK':
                    // Handle game start acknowledgment from guest players
                    if (isHost && message.roomCode === currentRoomCode) {
                        handleGameStartAck(message);
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
                    
                    // Send a sync message after receiving an answer to ensure both players are up to date
                    if (gameState.hasAnswered) {
                        setTimeout(sendRoundSyncMessage, 300);
                    }
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
                default:
                    logMessage(`Unhandled message type: ${message.type}`, 'warning');
                    // If it has roomCode, try to handle as a general message
                    if (message.roomCode === currentRoomCode) {
                        logMessage('Attempting to process as a general room message', 'info');
                        // If it has a sender, update the players list
                        if (message.senderUsername && message.senderId) {
                            const isMessageFromHost = message.isHost === true;
                            if (isMessageFromHost && !isHost) {
                                // Add host to guest's list
                                const hostItem = document.createElement('li');
                                hostItem.className = 'list-group-item';
                                hostItem.innerHTML = `
                                    <i class="fas fa-user player-icon"></i>
                                    ${message.senderUsername}
                                    <span class="badge bg-primary host-badge">Host</span>
                                `;
                                
                                const existingHost = Array.from(elements.join.joinedPlayersList.children)
                                    .some(item => item.textContent.includes(message.senderUsername));
                                    
                                if (!existingHost) {
                                    elements.join.joinedPlayersList.appendChild(hostItem);
                                    elements.join.joinedRoomInfo.style.display = 'block';
                                    elements.join.joinedRoomPlayers.style.display = 'block';
                                }
                            } else if (isHost && !isMessageFromHost) {
                                // Add guest to host's list
                                const guestItem = document.createElement('li');
                                guestItem.className = 'list-group-item';
                                guestItem.innerHTML = `
                                    <i class="fas fa-user player-icon"></i>
                                    ${message.senderUsername}
                                    <span class="badge bg-secondary host-badge">Guest</span>
                                `;
                                
                                const existingGuest = Array.from(elements.create.playersList.children)
                                    .some(item => item.textContent.includes(message.senderUsername));
                                    
                                if (!existingGuest) {
                                    elements.create.playersList.appendChild(guestItem);
                                    elements.create.startGameBtn.disabled = false;
                                }
                            }
                        }
                    }
                    break;
            }
            
            // Remove loading indicator if it exists after processing any message
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
    
    // Handle round synchronization
    function handleRoundSync(message) {
        if (!message || message.roomCode !== currentRoomCode) {
            return;
        }
        
        logMessage(`Received round sync: round=${message.roundNumber}, card=${message.currentCardIndex}`, 'info');
        
        // If we're behind, catch up
        if (message.roundNumber > gameState.roundNumber) {
            logMessage(`We're behind (round ${gameState.roundNumber} vs ${message.roundNumber}), catching up`, 'warning');
            
            // Clear any waiting timeouts
            if (window.nextRoundTimeout) {
                clearTimeout(window.nextRoundTimeout);
                window.nextRoundTimeout = null;
            }
            
            if (window.answerSyncTimeout) {
                clearTimeout(window.answerSyncTimeout);
                window.answerSyncTimeout = null;
            }
            
            // Show indicator that we're syncing
            const syncAlert = document.createElement('div');
            syncAlert.id = 'sync-alert';
            syncAlert.className = 'alert alert-info';
            syncAlert.innerHTML = `
                <div class="d-flex align-items-center">
                    <div class="spinner-border spinner-border-sm me-2" role="status"></div>
                    <span>Synchronizing with other player...</span>
                </div>
            `;
            
            if (elements.game.answerFeedback.style.display === 'none') {
                elements.game.cardQuestion.parentNode.prepend(syncAlert);
            } else {
                elements.game.answerFeedback.after(syncAlert);
            }
            
            // Auto-remove after 3 seconds
            setTimeout(() => {
                const alert = document.getElementById('sync-alert');
                if (alert) alert.remove();
            }, 3000);
            
            // Update our state to match the message
            gameState.roundNumber = message.roundNumber - 1; // Will be incremented in startNextRound
            gameState.currentCardIndex = message.currentCardIndex - 1; // Will be incremented in startNextRound
            gameState.hostScore = message.hostScore || 0;
            gameState.guestScore = message.guestScore || 0;
            
            // Update scores
            if (isHost) {
                gameState.yourScore = gameState.hostScore;
                gameState.opponentScore = gameState.guestScore;
            } else {
                gameState.yourScore = gameState.guestScore;
                gameState.opponentScore = gameState.hostScore;
            }
            
            // Start the next round to catch up
            setTimeout(() => {
                startNextRound();
            }, 500);
            return;
        }
        
        // If we're ahead, let them catch up (no action needed)
        if (message.roundNumber < gameState.roundNumber) {
            logMessage(`We're ahead (round ${gameState.roundNumber} vs ${message.roundNumber}), waiting for them to catch up`, 'info');
            
            // Send our current round info again to help them sync
            setTimeout(sendRoundSyncMessage, 500);
            return;
        }
        
        // If we're on the same round, sync scores and answer states
        if (message.roundNumber === gameState.roundNumber) {
            logMessage('Syncing scores and states from same round', 'info');
            
            // Update scores
            gameState.hostScore = message.hostScore || gameState.hostScore;
            gameState.guestScore = message.guestScore || gameState.guestScore;
            
            if (isHost) {
                gameState.yourScore = gameState.hostScore;
                gameState.opponentScore = gameState.guestScore;
            } else {
                gameState.yourScore = gameState.guestScore;
                gameState.opponentScore = gameState.hostScore;
            }
            
            // Update UI
            elements.game.yourScore.textContent = gameState.yourScore;
            elements.game.opponentScore.textContent = gameState.opponentScore;
            
            // Update opponent answer state if needed
            if (message.hasAnswered && !gameState.opponentAnswered) {
                gameState.opponentAnswered = true;
                
                // If we've also answered, we need to move to next round
                if (gameState.hasAnswered) {
                    logMessage('Detected that both players have answered via sync message', 'info');
                    
                    // Remove any waiting indicators
                    const waitingIndicator = document.getElementById('waiting-indicator');
                    if (waitingIndicator) {
                        waitingIndicator.remove();
                    }
                    
                    // Show moving to next round indicator if it doesn't exist
                    if (!document.getElementById('next-round-indicator')) {
                        const nextRoundIndicator = document.createElement('div');
                        nextRoundIndicator.id = 'next-round-indicator';
                        nextRoundIndicator.className = 'alert alert-info mt-3';
                        nextRoundIndicator.innerHTML = `
                            <div class="d-flex align-items-center">
                                <div class="spinner-border spinner-border-sm me-2" role="status"></div>
                                <span>Both players answered! Moving to next question...</span>
                            </div>
                        `;
                        
                        if (elements.game.answerFeedback.style.display === 'none') {
                            elements.game.cardQuestion.parentNode.prepend(nextRoundIndicator);
                        } else {
                            elements.game.answerFeedback.after(nextRoundIndicator);
                        }
                    }
                    
                    // Schedule next round
                    if (window.nextRoundTimeout) {
                        clearTimeout(window.nextRoundTimeout);
                    }
                    
                    window.nextRoundTimeout = setTimeout(() => {
                        startNextRound();
                    }, 2000);
                } else {
                    // We haven't answered yet, but opponent has
                    if (!document.getElementById('waiting-indicator')) {
                        const waitingIndicator = document.createElement('div');
                        waitingIndicator.id = 'waiting-indicator';
                        waitingIndicator.className = 'alert alert-warning mt-3';
                        waitingIndicator.innerHTML = `
                            <div class="d-flex align-items-center">
                                <div class="spinner-border spinner-border-sm me-2" role="status"></div>
                                <span>Opponent has answered. Waiting for you to answer...</span>
                            </div>
                        `;
                        
                        if (elements.game.answerFeedback.style.display === 'none') {
                            elements.game.cardQuestion.parentNode.append(waitingIndicator);
                        } else {
                            elements.game.answerFeedback.after(waitingIndicator);
                        }
                    }
                }
            }
        }
        
        // If we're on the same round but different card somehow, resync
        if (message.roundNumber === gameState.roundNumber && 
            message.currentCardIndex !== gameState.currentCardIndex) {
            logMessage(`Card index mismatch (${gameState.currentCardIndex} vs ${message.currentCardIndex}), resyncing`, 'warning');
            
            // Update card index
            gameState.currentCardIndex = message.currentCardIndex - 1; // Will be incremented in startNextRound
            
            // Start next round to get to the correct card
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
        
        try {
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
                totalRounds: 10,
                syncAttempts: 0,
                lastSyncTimestamp: 0,
                gameStarted: true,
                gameEnded: false,
                startTimestamp: Date.now(),
                gameStartedBy: firebaseUid,
                lastAnswerTime: 0,
                optionsSeed: 0,
                connectionRetries: 0
            };
            
            // Load cards
            if (STATIC_CARDS && Array.isArray(STATIC_CARDS) && STATIC_CARDS.length > 0) {
                gameState.gameCards = JSON.parse(JSON.stringify(STATIC_CARDS));
                logMessage(`Loaded ${gameState.gameCards.length} cards from static data`, 'info');
                
                // Set total rounds based on available cards, capped at 10
                gameState.totalRounds = Math.min(gameState.gameCards.length, 10);
            } else {
                logMessage('No static cards available, using fallback cards', 'error');
                
                // Fallback cards
                gameState.gameCards = [
                    {
                        front: "What is the tallest mountain in the world?",
                        back: "Mount Everest",
                        options: ["Mount Everest", "K2", "Mount Kilimanjaro", "Denali"]
                    },
                    {
                        front: "What is the capital of France?",
                        back: "Paris",
                        options: ["Paris", "London", "Berlin", "Madrid"]
                    },
                    {
                        front: "Who wrote 'Romeo and Juliet'?",
                        back: "William Shakespeare",
                        options: ["William Shakespeare", "Charles Dickens", "Jane Austen", "Mark Twain"]
                    }
                ];
            }
            
            // Shuffle cards with seed based on room code
            const seed = currentRoomCode.split('').reduce((acc, char) => {
                return acc + char.charCodeAt(0);
            }, 0);
            
            deterministicShuffle(gameState.gameCards, seed);
            
            // Limit to total rounds
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
            
            // Start first round
            setTimeout(() => {
                startNextRound();
            }, 1000);
        } catch (error) {
            logMessage(`Error starting game: ${error.message}`, 'error');
            displayError('Failed to start the game. Please try again.');
            elements.create.startGameBtn.disabled = false;
        }
    }
    
    // Handle game start event
    function handleGameStarted(message) {
        // Make sure the message is for our current room
        if (message.roomCode !== currentRoomCode) {
            logMessage(`Got game start for room ${message.roomCode} but we're in ${currentRoomCode}, ignoring`, 'warning');
            return;
        }
        
        logMessage(`Game started by ${message.senderUsername || 'unknown user'}`, 'success');
        
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
            gameCards: [],
            currentCardIndex: -1,
            totalRounds: 10, // Default value
            gameStarted: true,
            gameEnded: false,
            startTimestamp: Date.now(),
            gameStartedBy: message.senderId || 'unknown',
            lastSyncTimestamp: 0,
            syncAttempts: 0,
            optionsSeed: 0,
            connectionRetries: 0
        };
        
        // Clone the static cards to prevent modifications to the original array
        if (STATIC_CARDS && Array.isArray(STATIC_CARDS) && STATIC_CARDS.length > 0) {
            gameState.gameCards = JSON.parse(JSON.stringify(STATIC_CARDS));
            logMessage(`Loaded ${gameState.gameCards.length} cards from static data`, 'info');
            
            // Set total rounds based on available cards, capped at 10
            gameState.totalRounds = Math.min(gameState.gameCards.length, 10);
        } else {
            logMessage('No static cards available, using fallback cards', 'error');
            
            // Fallback cards
            gameState.gameCards = [
                {
                    front: "What is the tallest mountain in the world?",
                    back: "Mount Everest",
                    options: ["Mount Everest", "K2", "Mount Kilimanjaro", "Denali"]
                },
                {
                    front: "What is the capital of France?",
                    back: "Paris",
                    options: ["Paris", "London", "Berlin", "Madrid"]
                },
                {
                    front: "Who wrote 'Romeo and Juliet'?",
                    back: "William Shakespeare",
                    options: ["William Shakespeare", "Charles Dickens", "Jane Austen", "Mark Twain"]
                }
            ];
        }
        
        // Send acknowledgment if we're a guest
        if (!isHost) {
            // Send multiple acknowledgments for reliability
            sendGameStartAcknowledgment();
            setTimeout(sendGameStartAcknowledgment, 500);
            setTimeout(sendGameStartAcknowledgment, 1500);
        }
        
        // Use deterministic shuffle with consistent seed for both players
        const seed = currentRoomCode.split('').reduce((acc, char) => {
            return acc + char.charCodeAt(0);
        }, 0);
        
        logMessage(`Shuffling cards with seed: ${seed}`, 'info');
        deterministicShuffle(gameState.gameCards, seed);
        
        // Switch to game section
        showSection('game');
        
        // Initialize UI
        elements.game.yourScore.textContent = '0';
        elements.game.opponentScore.textContent = '0';
        elements.game.roundNumber.textContent = '1';
        
        // Start the first round after a short delay to ensure both players are ready
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
        // Add a safety check for the message
        if (!message) {
            logMessage('Received empty game start acknowledgment message', 'error');
            return;
        }
        
        if (message.roomCode === currentRoomCode && isHost) {
            logMessage(`Received game start acknowledgment from ${message.senderUsername || 'guest'}`, 'success');
            
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
            showSuccessToast(`${message.senderUsername || 'Guest'} has joined the game`, 'Game is in progress for both players');
            
            // Enable start game button on the host side if it's disabled
            if (elements.create.startGameBtn.disabled) {
                elements.create.startGameBtn.disabled = false;
            }
            
            // Force start first round if it hasn't started yet
            if (gameState.gameStarted && gameState.currentCardIndex === -1) {
                logMessage('Starting next round based on game start acknowledgment', 'info');
                setTimeout(() => {
                    startNextRound();
                }, 500);
            }
            
            // Send round sync message for redundancy
            if (gameState.currentCardIndex >= 0) {
                setTimeout(sendRoundSyncMessage, 800);
            }
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
        // Clear any waiting timeouts
        if (window.nextRoundTimeout) {
            clearTimeout(window.nextRoundTimeout);
            window.nextRoundTimeout = null;
        }
        
        if (window.answerSyncTimeout) {
            clearTimeout(window.answerSyncTimeout);
            window.answerSyncTimeout = null;
        }
        
        // Remove any indicator elements
        const waitingIndicator = document.getElementById('waiting-indicator');
        if (waitingIndicator) {
            waitingIndicator.remove();
        }
        
        const nextRoundIndicator = document.getElementById('next-round-indicator');
        if (nextRoundIndicator) {
            nextRoundIndicator.remove();
        }
        
        // Reset state for the new round
        gameState.hasAnswered = false;
        gameState.opponentAnswered = false;
        gameState.selectedOption = null;
        
        // Move to next card
        gameState.currentCardIndex++;
        gameState.roundNumber++;
        
        // Clear any previous answer feedback
        elements.game.answerFeedback.style.display = 'none';
        
        // Check if game is over
        if (gameState.currentCardIndex >= gameState.gameCards.length) {
            endGame();
            return;
        }
        
        const currentCard = gameState.gameCards[gameState.currentCardIndex];
        
        if (!currentCard) {
            logMessage(`Error: No card found for index ${gameState.currentCardIndex}`, 'error');
            
            // Try to reinitialize game cards
            initializeGameCards();
            if (gameState.gameCards.length > 0) {
                gameState.currentCardIndex = 0;
                const firstCard = gameState.gameCards[0];
                logMessage(`Recovered by reinitializing cards, using first card`, 'info');
                
                // Show recovery message
                const recoveryAlert = document.createElement('div');
                recoveryAlert.className = 'alert alert-warning mb-3';
                recoveryAlert.textContent = 'Had to reset the game cards. Starting from the beginning.';
                elements.game.cardQuestion.parentNode.prepend(recoveryAlert);
                
                // Remove recovery message after 3 seconds
                setTimeout(() => {
                    recoveryAlert.remove();
                }, 3000);
                
                // Use first card
                showCardInUI(firstCard);
            } else {
                endGame();
                return;
            }
        } else {
            // Use the current card
            showCardInUI(currentCard);
        }
        
        logMessage(`Round ${gameState.roundNumber} started with card: ${currentCard.front}`, 'info');
        
        // Send round synchronization message with a slight delay to ensure UI is updated first
        setTimeout(() => {
            // Send multiple sync messages for redundancy
            sendRoundSyncMessage();
            
            // Send more sync messages with delays for reliability
            setTimeout(sendRoundSyncMessage, 500);
            setTimeout(sendRoundSyncMessage, 1500);
        }, 100);
        
        // Show the game section if not already visible
        if (!elements.sections.game.classList.contains('active')) {
            showSection('game');
        }
    }
    
    // Helper function to display a card in the UI
    function showCardInUI(card) {
        // Update UI
        elements.game.roundNumber.textContent = gameState.roundNumber;
        elements.game.yourScore.textContent = gameState.yourScore;
        elements.game.opponentScore.textContent = gameState.opponentScore;
        elements.game.cardQuestion.textContent = card.front;
        
        // Create option buttons (using deterministic shuffle with same seed for consistency)
        elements.game.optionsContainer.innerHTML = '';
        
        // Get a seed for this specific card based on room code and round number
        // This ensures both players see the same order of options
        const optionsSeed = currentRoomCode.split('').reduce((acc, char) => {
            return acc + char.charCodeAt(0);
        }, 0) + gameState.roundNumber * 100;
        
        // Create a copy of options to shuffle so both players see same order
        const shuffledOptions = [...card.options];
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
    }

    // Send a round synchronization message to ensure both players are on the same round
    function sendRoundSyncMessage() {
        if (!stompClient || !stompClient.connected) {
            logMessage('Could not send round sync: not connected', 'warning');
            return;
        }
        
        try {
            // Get current card
            const currentCard = gameState.gameCards[gameState.currentCardIndex];
            if (!currentCard) {
                logMessage('Could not send round sync: invalid card index', 'warning');
                return;
            }
            
            // Throttle sync messages to prevent flooding
            const now = Date.now();
            if (now - gameState.lastSyncTimestamp < 200) {
                logMessage('Throttling round sync message', 'info');
                return;
            }
            
            gameState.lastSyncTimestamp = now;
            gameState.syncAttempts++;
            
            // Send a simple direct message format for better compatibility
            const message = {
                type: 'ROUND_SYNC',
                roomCode: currentRoomCode,
                roundNumber: gameState.roundNumber,
                currentCardIndex: gameState.currentCardIndex,
                hostScore: gameState.hostScore,
                guestScore: gameState.guestScore,
                timestamp: now,
                senderId: firebaseUid,
                senderUsername: playerUsername,
                hasAnswered: gameState.hasAnswered,
                opponentAnswered: gameState.opponentAnswered,
                syncAttempt: gameState.syncAttempts
            };
            
            // Send message to room
            stompClient.send(
                CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode,
                { 
                    firebaseUid: firebaseUid,
                    roomCode: currentRoomCode
                },
                JSON.stringify(message)
            );
            
            logMessage(`Sent round sync for round ${gameState.roundNumber}, attempt ${gameState.syncAttempts}`, 'info');
        } catch (error) {
            logMessage(`Error sending round sync: ${error.message}`, 'error');
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
        gameState.lastAnswerTime = Date.now();
        
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
        
        // Create a complete message with all required data
        const answerMessage = {
            type: 'ANSWER_SUBMITTED',
            roomCode: currentRoomCode,
            senderId: firebaseUid,
            senderUsername: playerUsername,
            content: selectedOption,
            hostScore: gameState.hostScore,
            guestScore: gameState.guestScore,
            isCorrect: isCorrect,
            timestamp: Date.now(),
            roundNumber: gameState.roundNumber,
            currentCardIndex: gameState.currentCardIndex
        };
        
        // Send answer to room directly for more reliable delivery
        if (stompClient && stompClient.connected) {
            // Send directly to room topic for guaranteed delivery
            stompClient.send(
                CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode,
                { roomCode: currentRoomCode },
                JSON.stringify(answerMessage)
            );
            
            // Also send via application endpoint as a backup
            stompClient.send(
                '/app/game.submitAnswer',
                { roomCode: currentRoomCode },
                JSON.stringify(answerMessage)
            );
        }
        
        // If opponent has already answered, show indicator that we're moving to next round
        if (gameState.opponentAnswered) {
            logMessage('Both players have now answered', 'success');
            
            // Remove any waiting indicators
            const waitingIndicator = document.getElementById('waiting-indicator');
            if (waitingIndicator) {
                waitingIndicator.remove();
            }
            
            // Add moving to next round indicator
            const nextRoundIndicator = document.createElement('div');
            nextRoundIndicator.id = 'next-round-indicator';
            nextRoundIndicator.className = 'alert alert-info mt-3';
            nextRoundIndicator.innerHTML = `
                <div class="d-flex align-items-center">
                    <div class="spinner-border spinner-border-sm me-2" role="status"></div>
                    <span>Both players answered! Moving to next question...</span>
                </div>
            `;
            elements.game.answerFeedback.after(nextRoundIndicator);
            
            // Schedule next round with a deliberate pause
            if (window.nextRoundTimeout) {
                clearTimeout(window.nextRoundTimeout);
            }
            
            window.nextRoundTimeout = setTimeout(() => {
                startNextRound();
                // Send immediate sync message to confirm round change to other player
                setTimeout(sendRoundSyncMessage, 200);
            }, CONFIG.GAME.ROUND_TRANSITION_DELAY || 2000);
        } else {
            // If opponent hasn't answered yet, show waiting indicator
            const waitingIndicator = document.createElement('div');
            waitingIndicator.id = 'waiting-indicator';
            waitingIndicator.className = 'alert alert-warning mt-3';
            waitingIndicator.innerHTML = `
                <div class="d-flex align-items-center">
                    <div class="spinner-border spinner-border-sm me-2" role="status"></div>
                    <span>Waiting for opponent to answer...</span>
                </div>
            `;
            elements.game.answerFeedback.after(waitingIndicator);
            
            // Set a backup timeout to move to next round in case opponent message is lost
            window.answerSyncTimeout = setTimeout(() => {
                if (!gameState.opponentAnswered) {
                    logMessage('No opponent answer received after timeout, forcing next round', 'warning');
                    gameState.opponentAnswered = true; // Mark as answered anyway
                    
                    // Remove waiting indicators
                    const waitingIndicator = document.getElementById('waiting-indicator');
                    if (waitingIndicator) {
                        waitingIndicator.remove();
                    }
                    
                    // Start next round
                    startNextRound();
                }
            }, 10000); // 10 second timeout as a safety fallback
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
            logMessage('Both players have answered. Moving to next round...', 'success');
            
            // Add a clear visual indicator that we're advancing
            const waitingIndicator = document.createElement('div');
            waitingIndicator.className = 'alert alert-info';
            waitingIndicator.textContent = 'Both players answered! Moving to next question...';
            elements.game.answerFeedback.after(waitingIndicator);
            
            // Clear any existing timeouts to avoid multiple next rounds
            if (window.nextRoundTimeout) {
                clearTimeout(window.nextRoundTimeout);
            }
            
            // Set a deliberate delay then move to next round
            window.nextRoundTimeout = setTimeout(() => {
                startNextRound();
                // Send immediate sync message to confirm round change to other player
                setTimeout(sendRoundSyncMessage, 200);
            }, CONFIG.GAME.ROUND_TRANSITION_DELAY || 2000);
        } else {
            // Show waiting indicator
            if (!document.getElementById('waiting-indicator')) {
                const waitingIndicator = document.createElement('div');
                waitingIndicator.id = 'waiting-indicator';
                waitingIndicator.className = 'alert alert-warning mt-3';
                waitingIndicator.innerHTML = `
                    <div class="d-flex align-items-center">
                        <div class="spinner-border spinner-border-sm me-2" role="status"></div>
                        <span>Opponent has answered. Waiting for you to answer...</span>
                    </div>
                `;
                elements.game.answerFeedback.after(waitingIndicator);
            }
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

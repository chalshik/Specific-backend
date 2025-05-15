function sendRoundSyncMessage() {
    if (!stompClient || !stompClient.connected) {
        return;
    }
    
    try {
        const roundSyncData = {
            roomCode: currentRoomCode,
            roundNumber: gameState.roundNumber,
            hostScore: gameState.hostScore,
            guestScore: gameState.guestScore,
            currentCardIndex: gameState.currentCardIndex,
            cardFront: gameState.gameCards[gameState.currentCardIndex].front,
            totalCards: gameState.gameCards.length
        };
        
        const message = {
            type: 'ROUND_SYNC',
            roomCode: currentRoomCode,
            senderId: firebaseUid,
            senderUsername: playerUsername,
            timestamp: Date.now(),
            syncData: roundSyncData,
            senderIsHost: isHost
        };
        
        // Send to room topic
        stompClient.send(
            CONFIG.SOCKET.ROOM_TOPIC_PREFIX + currentRoomCode, 
            { roomCode: currentRoomCode }, 
            JSON.stringify(message)
        );
        
        logMessage('Sent round sync message for round ' + gameState.roundNumber, 'info');
    } catch (e) {
        logMessage('Error sending round sync: ' + e.message, 'error');
    }
}
// THIS IS THE END OF THE FUNCTION

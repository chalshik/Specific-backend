package com.Specific.Specific.Except;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

public class WebSocketErrorHandler extends StompSubProtocolErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketErrorHandler.class);

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        logger.error("WebSocket error: {}", cause.getMessage(), cause);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(cause.getMessage());
        
        String sessionId = null;
        if (clientMessage != null) {
            StompHeaderAccessor clientHeaderAccessor = MessageHeaderAccessor.getAccessor(
                    clientMessage, StompHeaderAccessor.class);
            if (clientHeaderAccessor != null) {
                sessionId = clientHeaderAccessor.getSessionId();
            }
        }
        
        if (sessionId != null) {
            accessor.setSessionId(sessionId);
        }
        
        return MessageBuilder.createMessage(cause.getMessage().getBytes(), accessor.getMessageHeaders());
    }
} 
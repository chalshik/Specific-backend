package com.Specific.Specific.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        logger.info("Configuring WebSocket message broker");
        config.enableSimpleBroker("/topic", "/queue");
        
        // Set prefix for messages from clients to application
        config.setApplicationDestinationPrefixes("/app");
        
        logger.info("WebSocket broker configured with topics /topic, /queue and application prefix /app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        logger.info("Registering STOMP endpoints");
        
        // Register the "/ws-game" endpoint with specific allowed origins
        registry.addEndpoint("/ws-game")
                .setAllowedOrigins("*")  // Allow all origins for testing - restrict in production
                .withSockJS()
                .setWebSocketEnabled(true)
                .setHeartbeatTime(25000)
                .setDisconnectDelay(5000);
                
        logger.info("STOMP endpoint /ws-game registered with all origins allowed and SockJS support");
    }
} 
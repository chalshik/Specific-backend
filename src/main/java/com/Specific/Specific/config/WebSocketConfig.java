package com.Specific.Specific.config;

import com.Specific.Specific.Except.WebSocketErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Configuration
@EnableWebSocketMessageBroker  // Enable WebSocket message handling, backed by a message broker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Bean
    public StompSubProtocolErrorHandler customErrorHandler() {
        return new WebSocketErrorHandler();
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the endpoint clients will use to connect to websocket
        registry.addEndpoint("/ws")  // Clients connect here, e.g. ws://localhost:8080/ws
                .setAllowedOriginPatterns("*") ; // Allow all origins (for dev, change in production)
               // Enable SockJS fallback options for browsers that don't support websocket
                
        // Set the error handler at the registry level
        registry.setErrorHandler(customErrorHandler());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Messages sent to destinations starting with /app will be routed to message-handling methods (controller)
        registry.setApplicationDestinationPrefixes("/app");

        // Enable a simple in-memory message broker for topics (server to clients)
        registry.enableSimpleBroker("/topic", "/queue");
    }
}

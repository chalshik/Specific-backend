package com.Specific.Specific.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.path:firebase-service-account.json}")
    private String firebaseCredentialsPath;
    
    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;
    
    private final Environment environment;
    
    public FirebaseConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        if (!firebaseEnabled) {
            logger.info("Firebase authentication is disabled for testing");
            return;
        }
        
        try {
            InputStream serviceAccount = null;
            
            // Check if FIREBASE_CREDENTIALS_JSON environment variable contains the actual JSON content
            String credentialsJson = environment.getProperty("FIREBASE_CREDENTIALS_JSON");
            if (credentialsJson != null && !credentialsJson.isEmpty() && credentialsJson.trim().startsWith("{")) {
                logger.info("Using Firebase credentials from environment variable");
                serviceAccount = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
            } else {
                try {
                    // Try to load from class path
                    logger.info("Trying to load Firebase credentials from classpath: {}", firebaseCredentialsPath);
                    serviceAccount = new ClassPathResource(firebaseCredentialsPath).getInputStream();
                } catch (IOException e) {
                    // If not found in classpath, try as a file path
                    logger.info("Trying to load Firebase credentials from file path: {}", firebaseCredentialsPath);
                    serviceAccount = new FileInputStream(firebaseCredentialsPath);
                }
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // Check if default app already exists to avoid re-initialization
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("Firebase application has been initialized");
            }
        } catch (Exception e) {
            logger.error("Firebase initialization error", e);
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }
}


package com.Specific.Specific.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.path:firebase-service-account.json}")
    private String firebaseCredentialsPath;

    @PostConstruct
    public void init() {
        try {
            InputStream serviceAccount;
            
            try {
                // First try to load from class path
                serviceAccount = new ClassPathResource(firebaseCredentialsPath).getInputStream();
            } catch (IOException e) {
                // If not found in classpath, try as a file path
                serviceAccount = new FileInputStream(firebaseCredentialsPath);
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


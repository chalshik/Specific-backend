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

    // Always disable Firebase
    private boolean firebaseEnabled = false;
    
    private final Environment environment;
    
    public FirebaseConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        // Firebase is completely disabled
        logger.info("Firebase authentication is disabled");
        
        // No Firebase initialization will occur
    }
}


package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Models.ResponseModels.ApiResponse;
import com.Specific.Specific.Services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final DataSource dataSource;
    private final Environment environment;
    private final boolean firebaseEnabled;
    
    @Autowired
    public UserController(UserService userService, DataSource dataSource, Environment environment, @Value("${firebase.enabled:false}") boolean firebaseEnabled){
        this.userService = userService;
        this.dataSource = dataSource;
        this.environment = environment;
        this.firebaseEnabled = firebaseEnabled;
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> requestBody) {
        try {
            String username = requestBody.get("username");
            String firebaseUid = requestBody.get("firebaseUid");
            
            logger.info("Received registration request with: username={}, firebaseUid={}", 
                    username, firebaseUid);
            
            if (firebaseUid == null || firebaseUid.isEmpty()) {
                logger.error("Firebase UID is null or empty");
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("firebaseUid is required and cannot be empty")
                );
            }
            
            User user = new User(username, firebaseUid);
            User savedUser = userService.addUser(user);
            logger.info("Successfully registered user with ID: {}", savedUser.getId());
            
            return ResponseEntity.ok(savedUser);
        } catch (DataAccessException e) {
            logger.error("Database access error during registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Database error. Please try again later.")
            );
        } catch (IllegalArgumentException e) {
            logger.error("Validation error during registration: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            logger.error("Error during user registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Registration error: " + e.getMessage())
            );
        }
    }
    
    @PostMapping("/debug-register")
    public ResponseEntity<?> debugRegisterUser(@RequestBody Map<String, String> requestBody) {
        try {
            String username = requestBody.get("username");
            String firebaseUid = requestBody.get("firebaseUid");
            
            logger.info("Debug registration with data: username={}, firebaseUid={}", 
                    username, firebaseUid);
            
            if (firebaseUid == null || firebaseUid.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("firebaseUid is required and cannot be empty")
                );
            }
            
            User user = new User(username, firebaseUid);
            User savedUser = userService.addUser(user);
            return ResponseEntity.ok(savedUser);
        } catch (DataAccessException e) {
            String errorMessage = "Database access error: " + e.getMessage();
            logger.error("Debug registration database error: {}", errorMessage, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(errorMessage)
            );
        } catch (Exception e) {
            logger.error("Debug registration error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Registration error: " + e.getMessage())
            );
        }
    }
    
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("User controller is working!");
    }
    
    @PostMapping("/test-register")
    public ResponseEntity<?> testRegisterUser(
            @RequestParam(required = false) String username, 
            @RequestParam String firebaseUid) {
        
        try {
            logger.info("Test registration with username={}, firebaseUid={}", username, firebaseUid);
            
            if (username == null || username.isEmpty()) {
                username = "User-" + System.currentTimeMillis(); // Default username
            }
            
            User newUser = new User(username, firebaseUid);
            User savedUser = userService.addUser(newUser);
            return ResponseEntity.ok(savedUser);
        } catch (DataAccessException e) {
            logger.error("Test registration database error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Database error: " + e.getMessage())
            );
        } catch (Exception e) {
            logger.error("Test registration error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Registration error: " + e.getMessage())
            );
        }
    }
    
    @PostMapping("/debug-test-register")
    public ResponseEntity<?> debugTestRegisterUser(
            @RequestParam(required = false) String username, 
            @RequestParam String firebaseUid) {
        
        try {
            logger.info("Debug test registration with username={}, firebaseUid={}", username, firebaseUid);
            
            if (firebaseUid == null || firebaseUid.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("firebaseUid is required and cannot be empty")
                );
            }
            
            if (username == null || username.isEmpty()) {
                username = "User-" + System.currentTimeMillis(); // Default username
            }
            
            User newUser = new User(username, firebaseUid);
            User savedUser = userService.addUser(newUser);
            return ResponseEntity.ok(savedUser);
        } catch (DataAccessException e) {
            String errorMessage = "Database error: " + e.getMessage();
            logger.error("Debug test registration database error: {}", errorMessage, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(errorMessage)
            );
        } catch (Exception e) {
            logger.error("Debug test registration error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Registration error: " + e.getMessage())
            );
        }
    }
    
    /**
     * Direct JDBC endpoint for testing database connection
     * Bypasses JPA to help diagnose connection issues
     */
    @PostMapping("/direct-register")
    public ResponseEntity<?> directRegisterUser(
            @RequestParam(required = false) String username, 
            @RequestParam String firebaseUid) {
        
        logger.info("Direct JDBC registration with username={}, firebaseUid={}", username, firebaseUid);
        Map<String, Object> response = new HashMap<>();
        
        if (username == null || username.isEmpty()) {
            username = "User-" + System.currentTimeMillis(); // Default username
        }
        
        try (Connection conn = dataSource.getConnection()) {
            logger.info("Got database connection: {}", conn);
            response.put("connection", "success");
            
            // First, check if user with firebaseUid already exists
            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT id, username FROM users WHERE firebase_uid = ?")) {
                checkStmt.setString(1, firebaseUid);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        long userId = rs.getLong("id");
                        String existingUsername = rs.getString("username");
                        logger.info("User with firebaseUid {} already exists with ID {}", firebaseUid, userId);
                        
                        response.put("status", "existing");
                        response.put("id", userId);
                        response.put("username", existingUsername);
                        response.put("firebaseUid", firebaseUid);
                        return ResponseEntity.ok(response);
                    }
                }
            }
            
            // If no existing user, create a new one
            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO users (username, firebase_uid) VALUES (?, ?) RETURNING id")) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, firebaseUid);
                
                logger.info("Executing insert statement: {}", insertStmt);
                try (ResultSet rs = insertStmt.executeQuery()) {
                    if (rs.next()) {
                        long userId = rs.getLong("id");
                        logger.info("Successfully registered user with ID: {}", userId);
                        
                        response.put("status", "created");
                        response.put("id", userId);
                        response.put("username", username);
                        response.put("firebaseUid", firebaseUid);
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("Insert succeeded but no ID was returned"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error during direct JDBC registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Direct JDBC error: " + e.getMessage()));
        }
    }
    
    /**
     * Test database connection without any modifications
     */
    @GetMapping("/db-test")
    public ResponseEntity<?> testDatabaseConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Simple test using JdbcTemplate
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            Integer testResult = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            result.put("jdbcTemplateTest", testResult != null ? "success" : "failed");
            
            // Get connection details
            try (Connection conn = dataSource.getConnection()) {
                result.put("connectionClass", conn.getClass().getName());
                result.put("autoCommit", conn.getAutoCommit());
                result.put("valid", conn.isValid(5));
                result.put("dbProduct", conn.getMetaData().getDatabaseProductName());
                result.put("dbVersion", conn.getMetaData().getDatabaseProductVersion());
                
                // Test basic table access
                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM users")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            result.put("userCount", rs.getInt(1));
                        }
                    }
                }
            }
            
            result.put("status", "success");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Database connection test failed: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", e.getMessage());
            result.put("exceptionType", e.getClass().getName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Direct authentication test endpoint that bypasses Firebase verification
     * Used for debugging authentication issues in production
     */
    @GetMapping("/auth-test")
    public ResponseEntity<?> testAuthentication(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.replace("Bearer ", "");
            result.put("tokenProvided", true);
            result.put("tokenLength", token.length());
            // Don't log the full token for security reasons
            result.put("tokenPreview", token.substring(0, Math.min(10, token.length())) + "...");
        } else {
            result.put("tokenProvided", false);
        }
        
        // Include environment details
        result.put("profiles", Arrays.toString(environment.getActiveProfiles()));
        result.put("firebaseEnabled", firebaseEnabled);
        
        return ResponseEntity.ok(result);
    }
}

package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Models.ResponseModels.ApiResponse;
import com.Specific.Specific.Services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    
    @Autowired
    public UserController(UserService userService){
        this.userService = userService;
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
}

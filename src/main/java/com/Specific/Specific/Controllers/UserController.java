package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Models.ResponseModels.ApiResponse;
import com.Specific.Specific.Services.UserService;
import com.Specific.Specific.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final Environment environment;
    private final boolean firebaseEnabled;
    private final SecurityUtils securityUtils;
    
    @Autowired
    public UserController(UserService userService, Environment environment,
                         SecurityUtils securityUtils,
                         @Value("${firebase.enabled:false}") boolean firebaseEnabled) {
        this.userService = userService;
        this.environment = environment;
        this.firebaseEnabled = firebaseEnabled;
        this.securityUtils = securityUtils;
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
    
    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo() {
        try {
            User currentUser = securityUtils.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error("User not found")
                );
            }
            return ResponseEntity.ok(currentUser);
        } catch (Exception e) {
            logger.error("Error retrieving user info: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Error retrieving user info: " + e.getMessage())
            );
        }
    }
    
    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody Map<String, String> requestBody) {
        try {
            String username = requestBody.get("username");
            User currentUser = securityUtils.getCurrentUser();
            
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error("User not found")
                );
            }
            
            if (username != null && !username.isEmpty()) {
                currentUser.setUsername(username);
            }
            
            User updatedUser = userService.updateUserUsername(username, currentUser.getId());
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            logger.error("Error updating user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Error updating user: " + e.getMessage())
            );
        }
    }
    
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser() {
        try {
            User currentUser = securityUtils.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error("User not found")
                );
            }
            
            userService.deleteUser(currentUser);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Error deleting user: " + e.getMessage())
            );
        }
    }
}

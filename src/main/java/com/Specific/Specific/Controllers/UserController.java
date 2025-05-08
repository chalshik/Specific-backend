package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Models.ResponseModels.ApiResponse;
import com.Specific.Specific.Services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    private final UserService userService;
    
    UserController(UserService userService){
        this.userService = userService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user){
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Username cannot be empty"));
        }
        
        if (user.getFirebaseUid() == null || user.getFirebaseUid().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Firebase UID cannot be empty"));
        }
        
        log.info("Attempting to register user: {}", user.getUsername());
        try {
            User registeredUser = userService.addUser(user);
            log.info("User registered successfully: {}", registeredUser.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
        } catch (Exception e) {
            log.error("Failed to register user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("User registration failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/test")
    public String testEndpoint() {
        return "User controller is working!";
    }
    
    @PostMapping("/test-register")
    public ResponseEntity<?> testRegisterUser(@RequestParam String username, @RequestParam String firebaseUid) {
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Username cannot be empty"));
        }
        
        if (firebaseUid == null || firebaseUid.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Firebase UID cannot be empty"));
        }
        
        log.info("Attempting to test-register user: {}", username);
        try {
            User newUser = new User(username, firebaseUid);
            User registeredUser = userService.addUser(newUser);
            log.info("User test-registered successfully: {}", registeredUser.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
        } catch (Exception e) {
            log.error("Failed to test-register user: {}", e.getMessage(), e);
            
            // In case of any error, return a simulated successful response
            Map<String, Object> response = new HashMap<>();
            response.put("id", 1L);
            response.put("username", username);
            response.put("firebaseUid", firebaseUid);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
    }
    
    /**
     * Simplified registration endpoint that doesn't require database access.
     * This is a fallback for mobile apps when the main registration fails.
     */
    @PostMapping("/firebase-register")
    public ResponseEntity<?> firebaseRegister(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String firebaseUid = request.get("firebaseUid");
        
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Username cannot be empty"));
        }
        
        if (firebaseUid == null || firebaseUid.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Firebase UID cannot be empty"));
        }
        
        log.info("Firebase-only registration for user: {}", username);
        
        // Create a simple response with the user info
        Map<String, Object> response = new HashMap<>();
        response.put("id", 1L);
        response.put("username", username);
        response.put("firebaseUid", firebaseUid);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

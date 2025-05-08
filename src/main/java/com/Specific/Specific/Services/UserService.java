package com.Specific.Specific.Services;

import com.Specific.Specific.Except.UserNotFoundException;
import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for managing user accounts.
 * Handles user creation, updates, and deletion.
 */
@Service
@Slf4j
public class UserService {
    private final UserRepo userRepo;
    
    // In-memory storage for development/fallback when database is unavailable
    private final Map<String, User> inMemoryUsersByFirebaseUid = new HashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    
    @Autowired
    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }
    
    /**
     * Create a new user account.
     * 
     * @param user The user entity to save
     * @return The saved user with generated ID
     */
    public User addUser(User user) {
        try {
            log.info("Attempting to save user to database: {}", user.getUsername());
            return userRepo.save(user);
        } catch (DataAccessException e) {
            log.warn("Database access failed, using in-memory storage instead: {}", e.getMessage());
            return addUserToMemory(user);
        } catch (Exception e) {
            log.error("Unexpected error saving user: {}", e.getMessage(), e);
            return addUserToMemory(user);
        }
    }
    
    private User addUserToMemory(User user) {
        // Check if user with this Firebase UID already exists
        if (inMemoryUsersByFirebaseUid.containsKey(user.getFirebaseUid())) {
            log.info("User with Firebase UID {} already exists in memory", user.getFirebaseUid());
            return inMemoryUsersByFirebaseUid.get(user.getFirebaseUid());
        }
        
        // Set a fake ID for the in-memory user
        user.setId(idCounter.getAndIncrement());
        
        // Store in memory
        inMemoryUsersByFirebaseUid.put(user.getFirebaseUid(), user);
        log.info("User saved to in-memory storage: {}", user.getUsername());
        
        return user;
    }
    
    /**
     * Delete a user account.
     * 
     * @param user The user to delete
     */
    public void deleteUser(User user) {
        try {
            userRepo.delete(user);
        } catch (DataAccessException e) {
            log.warn("Database access failed for deletion, removing from in-memory storage: {}", e.getMessage());
            inMemoryUsersByFirebaseUid.remove(user.getFirebaseUid());
        }
    }
    
    /**
     * Update a user's username.
     * 
     * @param username New username
     * @param user_id ID of the user to update
     * @return The updated user
     * @throws RuntimeException if user not found
     */
    public User updateUserUsername(String username, long user_id) {
        try {
            // Try database first
            User user = userRepo.findById(user_id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Update username and save
            user.setUsername(username);
            return userRepo.save(user);
        } catch (DataAccessException e) {
            log.warn("Database access failed for username update: {}", e.getMessage());
            
            // Try to find user in memory by id
            User memoryUser = inMemoryUsersByFirebaseUid.values().stream()
                    .filter(u -> u.getId() == user_id)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            memoryUser.setUsername(username);
            return memoryUser;
        }
    }
    
    /**
     * Find a user by Firebase UID.
     * 
     * @param firebaseUid The Firebase UID
     * @return The user
     * @throws UserNotFoundException if user not found
     */
    public User findUserByFirebaseUid(String firebaseUid) {
        try {
            return userRepo.findByFirebaseUid(firebaseUid)
                    .orElseGet(() -> {
                        // If not in database, check memory
                        User memoryUser = inMemoryUsersByFirebaseUid.get(firebaseUid);
                        if (memoryUser != null) {
                            return memoryUser;
                        }
                        throw new UserNotFoundException("User with this Firebase UID not found");
                    });
        } catch (DataAccessException e) {
            log.warn("Database access failed when finding user: {}", e.getMessage());
            
            // Try in-memory
            User memoryUser = inMemoryUsersByFirebaseUid.get(firebaseUid);
            if (memoryUser != null) {
                return memoryUser;
            }
            throw new UserNotFoundException("User with this Firebase UID not found");
        }
    }
}

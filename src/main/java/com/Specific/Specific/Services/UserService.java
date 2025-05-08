package com.Specific.Specific.Services;

import com.Specific.Specific.Except.UserNotFoundException;
import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Repository.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Service for managing user accounts.
 * Handles user creation, updates, and deletion.
 */
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepo userRepo;
    private final PlatformTransactionManager transactionManager;
    
    @Autowired
    public UserService(UserRepo userRepo, PlatformTransactionManager transactionManager) {
        this.userRepo = userRepo;
        this.transactionManager = transactionManager;
    }
    
    /**
     * Create a new user account or return existing one.
     * Uses manual transaction management to avoid JDBC commit issues.
     * 
     * @param user The user entity to save
     * @return The saved user with generated ID
     */
    public User addUser(User user) {
        // Define transaction properties
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        
        TransactionStatus status = transactionManager.getTransaction(definition);
        
        try {
            logger.info("Attempting to save user: username={}, firebaseUid={}", 
                    user.getUsername(), user.getFirebaseUid());
            
            // Validate required fields
            if (user.getFirebaseUid() == null || user.getFirebaseUid().isEmpty()) {
                logger.error("Cannot save user: firebaseUid is null or empty");
                throw new IllegalArgumentException("Firebase UID is required");
            }
            
            // Check if a user with this Firebase UID already exists - outside transaction
            Optional<User> existingUser = findExistingUser(user.getFirebaseUid());
            if (existingUser.isPresent()) {
                User existing = existingUser.get();
                logger.info("User with firebaseUid {} already exists, returning existing user", 
                        user.getFirebaseUid());
                
                // If username update needed, do it in the transaction
                if (user.getUsername() != null && !user.getUsername().isEmpty() &&
                        !user.getUsername().equals(existing.getUsername())) {
                    existing.setUsername(user.getUsername());
                    logger.info("Updating username for existing user: {}", user.getUsername());
                    User updated = userRepo.save(existing);
                    transactionManager.commit(status);
                    return updated;
                }
                
                // No changes needed, so don't use transaction
                transactionManager.rollback(status);
                return existing;
            }
            
            // Create new user
            User savedUser = userRepo.save(user);
            logger.info("Successfully saved new user with ID: {}", savedUser.getId());
            
            // Commit transaction
            transactionManager.commit(status);
            return savedUser;
            
        } catch (Exception e) {
            // Log the error
            logger.error("Error saving user: {}", e.getMessage(), e);
            
            // Roll back transaction
            transactionManager.rollback(status);
            
            // Handle specific exceptions
            if (e instanceof DataIntegrityViolationException) {
                throw new IllegalArgumentException("Could not save user: " + e.getMessage(), e);
            } else if (e instanceof DataAccessException) {
                throw new IllegalArgumentException("Database access error: " + e.getMessage(), e);
            } else {
                throw e;
            }
        }
    }
    
    /**
     * Find a user by Firebase UID without using a transaction
     */
    public Optional<User> findExistingUser(String firebaseUid) {
        try {
            return userRepo.findByFirebaseUid(firebaseUid);
        } catch (Exception e) {
            logger.warn("Error looking up user by firebaseUid: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Delete a user account.
     * 
     * @param user The user to delete
     */
    @Transactional
    public void deleteUser(User user) {
        userRepo.delete(user);
    }
    
    /**
     * Update a user's username.
     * 
     * @param username New username
     * @param user_id ID of the user to update
     * @return The updated user
     * @throws RuntimeException if user not found
     */
    @Transactional
    public User updateUserUsername(String username, long user_id) {
        // Find user or throw exception
        User user = userRepo.findById(user_id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Update username and save
        user.setUsername(username);
        return userRepo.save(user);
    }
    
    /**
     * Find a user by Firebase UID.
     * 
     * @param firebaseUid The Firebase UID
     * @return The user
     * @throws UserNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User findUserByFirebaseUid(String firebaseUid) {
        return userRepo.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new UserNotFoundException("User with this Firebase UID not found"));
    }
}

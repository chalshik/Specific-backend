package com.Specific.Specific.Services;

import com.Specific.Specific.Except.UserNotFoundException;
import com.Specific.Specific.Models.User;
import com.Specific.Specific.Repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for managing user accounts.
 * Handles user creation, updates, and deletion.
 */
@Service
public class UserService {
    private final UserRepo userRepo;
    
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
        return userRepo.save(user);
    }
    
    /**
     * Delete a user account.
     * 
     * @param user The user to delete
     */
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
    public User updateUserUsername(String username, long user_id) {
        // Find user or throw exception
        User user = userRepo.findById(user_id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Update username and save
        user.setUsername(username);
        return userRepo.save(user);
    }
    public User findUserByEmail(String firebaseId){
        User user = userRepo.findByEmail(firebaseId)
                .orElseThrow(() -> new UserNotFoundException("User with this Firebase UID not found"));
        return user;
    }
}

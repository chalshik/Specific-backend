package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Services.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private UserService userService;
    UserController(UserService userService){
        this.userService = userService;
    }
    @PostMapping("/register")
    public User registerUser(@RequestBody User user){
        return userService.addUser(user);
    }
    
    @GetMapping("/test")
    public String testEndpoint() {
        return "User controller is working!";
    }
    
    @PostMapping("/test-register")
    public User testRegisterUser(@RequestParam String username, @RequestParam String firebaseUid) {
        User newUser = new User(username, firebaseUid);
        return userService.addUser(newUser);
    }
}

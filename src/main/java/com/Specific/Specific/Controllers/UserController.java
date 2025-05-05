package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Services.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}

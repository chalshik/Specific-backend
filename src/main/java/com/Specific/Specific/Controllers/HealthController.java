package com.Specific.Specific.Controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    
    @GetMapping("/")
    public String rootPath() {
        return "Server is running!";
    }
    
    @GetMapping("/health")
    public String healthCheck() {
        return "OK";
    }
    
    @GetMapping("/api-test")
    public String apiTest() {
        return "API is working correctly!";
    }
} 
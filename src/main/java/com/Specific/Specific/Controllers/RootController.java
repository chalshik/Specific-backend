package com.Specific.Specific.Controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Simple controller to handle root-level requests and provide health check endpoints
 */
@RestController
@RequestMapping("/")
public class RootController {
    
    @GetMapping
    public String root() {
        return "Welcome to Specific Spring Backend API";
    }
    
    @GetMapping("health")
    public String health() {
        return "{\"status\":\"UP\"}";
    }
    
    @GetMapping("ping")
    public String ping() {
        return "pong";
    }
} 
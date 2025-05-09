package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.ResponseModels.ApiResponse;
import com.Specific.Specific.Repository.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple controller to handle root-level requests and provide health check endpoints
 */
@RestController
@RequestMapping("/")
public class RootController {
    private static final Logger logger = LoggerFactory.getLogger(RootController.class);
    
    private final Environment environment;
    private final UserRepo userRepo;
    private final DataSource dataSource;
    
    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;
    
    @Autowired
    public RootController(Environment environment, UserRepo userRepo, DataSource dataSource) {
        this.environment = environment;
        this.userRepo = userRepo;
        this.dataSource = dataSource;
    }
    
    @GetMapping
    public ApiResponse getRoot() {
        return ApiResponse.success("Specific API is running");
    }
    
    @GetMapping("health")
    public Map<String, Object> healthCheck() {
        logger.info("Health check endpoint called");
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("app", "Specific Spring Backend");
        status.put("profiles", Arrays.asList(environment.getActiveProfiles()));
        status.put("firebaseEnabled", firebaseEnabled);
        
        // Check database connection
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            status.put("database", "UP");
            
            // Get user count
            long userCount = userRepo.count();
            status.put("userCount", userCount);
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            status.put("database", "DOWN");
            status.put("databaseError", e.getMessage());
        }
        
        return status;
    }
} 
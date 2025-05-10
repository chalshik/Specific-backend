package com.Specific.Specific.config;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration for database-specific customizations.
 * This approach uses a more direct way to handle date calculations in queries
 * by updating the SQL queries in the repository instead of trying to register
 * custom SQL functions.
 */
@Configuration
public class DatabaseConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    
    @Value("${spring.jpa.properties.hibernate.dialect:}")
    private String hibernateDialect;
    
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            logger.info("Configuring database settings for dialect: {}", hibernateDialect);
            
            // Add any specific database properties if needed
            if (hibernateDialect.contains("PostgreSQL")) {
                // PostgreSQL specific settings
                // Note: We've moved away from custom SQL functions due to compatibility issues
                hibernateProperties.put("hibernate.jdbc.batch_size", "30");
            } else {
                // Generic settings for other databases
                hibernateProperties.put("hibernate.jdbc.batch_size", "15");
            }
            
            logger.info("Database settings configured successfully");
        };
    }
} 
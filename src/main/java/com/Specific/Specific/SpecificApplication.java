package com.Specific.Specific;

import com.Specific.Specific.Models.Card;
import com.Specific.Specific.Models.User;
import com.Specific.Specific.Repository.CardRepo;
import com.Specific.Specific.Repository.UserRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpecificApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpecificApplication.class, args);
	}

	// TEMPORARY: Create test data for API testing
	@Bean
	public CommandLineRunner setupTestData(CardRepo cardRepo, UserRepo userRepo) {
		return args -> {
			// Check if test user exists, otherwise create one
			User testUser = userRepo.findById(1L).orElse(null);
			if (testUser == null) {
				testUser = new User();
				testUser.setUsername("testuser");
				testUser.setFirebaseUid("test-firebase-uid");
				testUser = userRepo.save(testUser);
				System.out.println("Created test user with ID: " + testUser.getId());
			}
			
			// Check if test card exists, otherwise create one
			if (cardRepo.count() == 0) {
				Card testCard = new Card();
				testCard.setFront("What is the capital of France?");
				testCard.setBack("Paris");
				testCard.setContext("Geography");
				testCard.setUserId(testUser.getId());
				testCard.setDeckId(1L); // Default deck ID
				Card savedCard = cardRepo.save(testCard);
				System.out.println("Created test card with ID: " + savedCard.getId());
			}
		};
	}
}

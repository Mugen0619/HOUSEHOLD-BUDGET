package com.example.householdbudget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HouseholdBudgetApplication {

	public static void main(String[] args) {
		SpringApplication.run(HouseholdBudgetApplication.class, args);
	}

}

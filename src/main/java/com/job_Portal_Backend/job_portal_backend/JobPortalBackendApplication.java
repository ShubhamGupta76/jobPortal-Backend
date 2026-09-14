package com.job_Portal_Backend.job_portal_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// See config/AsyncConfig for @EnableAsync + the named taskExecutor bean it runs on.
@SpringBootApplication
public class JobPortalBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobPortalBackendApplication.class, args);
	}

}

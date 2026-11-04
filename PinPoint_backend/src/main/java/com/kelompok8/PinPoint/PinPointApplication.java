package com.kelompok8.PinPoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.kelompok8")
@EntityScan(basePackages = "com.kelompok8.model")
@EnableJpaRepositories(basePackages = "com.kelompok8.repository")
@EnableAsync  // Mengaktifkan @Async untuk EmailService (kirim email di background thread)
public class PinPointApplication {

	public static void main(String[] args) {
		SpringApplication.run(PinPointApplication.class, args);
	}

}

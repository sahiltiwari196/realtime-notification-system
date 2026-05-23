package com.realtime.notification.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.realtime.notification")
@EnableJpaRepositories(basePackages = "com.realtime.notification.repository")
@EntityScan("com.realtime.notification.entity")

public class RealTimeNotificationApp {
	public static void main(String[] args) {
		SpringApplication.run(RealTimeNotificationApp.class, args);
	}
}
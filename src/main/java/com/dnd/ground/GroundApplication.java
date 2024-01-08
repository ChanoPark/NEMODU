package com.dnd.ground;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableCaching
@EnableBatchProcessing
@EnableJpaAuditing
@SpringBootApplication
public class GroundApplication {
	public static void main(String[] args) {
		new SpringApplicationBuilder(GroundApplication.class).run(args);
	}
}
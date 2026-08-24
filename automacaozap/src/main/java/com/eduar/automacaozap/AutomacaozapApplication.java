package com.eduar.automacaozap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutomacaozapApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutomacaozapApplication.class, args);
	}

}

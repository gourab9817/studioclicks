package com.gourab9817.studioclicks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class StudioclicksApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudioclicksApplication.class, args);
	}

}

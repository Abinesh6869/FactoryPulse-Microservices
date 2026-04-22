package org.cts.fp_identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FpIdentityApplication {

	public static void main(String[] args) {
		SpringApplication.run(FpIdentityApplication.class, args);
	}

}

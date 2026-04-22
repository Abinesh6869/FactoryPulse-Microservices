package org.cts.fp_maintenance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FpMaintenanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FpMaintenanceApplication.class, args);
    }

}

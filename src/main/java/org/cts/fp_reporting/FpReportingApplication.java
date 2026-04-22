package org.cts.fp_reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FpReportingApplication {

    public static void main(String[] args) {
        SpringApplication.run(FpReportingApplication.class, args);
    }

}

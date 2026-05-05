package org.cts.fp_events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@org.springframework.scheduling.annotation.EnableScheduling
public class FpEventsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FpEventsApplication.class, args);
    }

}

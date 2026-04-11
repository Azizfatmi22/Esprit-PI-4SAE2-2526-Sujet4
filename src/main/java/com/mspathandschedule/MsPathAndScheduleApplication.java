package com.mspathandschedule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsPathAndScheduleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsPathAndScheduleApplication.class, args);
    }

}
